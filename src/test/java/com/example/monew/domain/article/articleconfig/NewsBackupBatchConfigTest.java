package com.example.monew.domain.article.articleconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.batch.dto.ArticleBackupDto;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@ExtendWith(MockitoExtension.class)
class NewsBackupBatchConfigTest {

  @Mock private S3Service s3Service;
  @Mock private ObjectMapper objectMapper;
  @Mock private ArticleRepository articleRepository;
  @Mock private ArticleViewRepository articleViewRepository;

  @InjectMocks
  private NewsBackupBatchConfig config;

  @Test
  @DisplayName("DTO 변환 후 JSON 업로드 확인")
  void articleS3WriterTest() throws Exception {
    ArticleBackupDto dto = ArticleBackupDto.builder()
        .id(UUID.randomUUID())
        .title("테스트 기사")
        .build();

    Chunk<ArticleBackupDto> chunk = new Chunk<>(List.of(dto));

    String expectedJson = "[{\"title\":\"테스트 기사\"}]";
    String testS3Key = "backups/test.json";

    given(objectMapper.writeValueAsString(any())).willReturn(expectedJson);

    ItemWriter<ArticleBackupDto> writer = config.articleS3Writer(testS3Key);

    writer.write(chunk);

    verify(s3Service).upload(eq(testS3Key), eq(expectedJson));
  }

  @Test
  @DisplayName("중복 체크로직")
  void duplicateCheckProcessorTest() throws Exception {
    ArticleEntity existingArticle = mock(ArticleEntity.class);
    given(existingArticle.isDeleted()).willReturn(false);
    given(articleRepository.findBySourceUrl(any())).willReturn(Optional.of(existingArticle));

    ArticleEntity newArticle = ArticleEntity.builder().sourceUrl("test.com").build();

    ArticleEntity result = config.duplicateCheckProcessor().process(newArticle);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Restore Writer 로직")
  void articleRestoreWriterTest() throws Exception {
    ArticleEntity deletedArticle = mock(ArticleEntity.class);
    given(deletedArticle.isDeleted()).willReturn(true);
    given(deletedArticle.getId()).willReturn(UUID.randomUUID());

    given(articleRepository.findBySourceUrl("deleted.com")).willReturn(Optional.of(deletedArticle));
    given(articleRepository.findBySourceUrl("new.com")).willReturn(Optional.empty());

    Chunk<ArticleEntity> chunk = new Chunk<>(List.of(
        ArticleEntity.builder().sourceUrl("deleted.com").build(),
        ArticleEntity.builder().sourceUrl("new.com").build()
    ));

    config.articleRestoreWriter().write(chunk);

    verify(articleRepository).restoreById(any());
    verify(articleRepository).save(any());
  }

  @Test
  @DisplayName("물리삭제 로직 성공")
  void articleHardDeleteWriterTest() throws Exception {
    ArticleEntity article = ArticleEntity.builder().build();
    Chunk<ArticleEntity> chunk = new Chunk<>(List.of(article));

    config.articleHardDeleteWriter().write(chunk);

    verify(articleViewRepository).deleteByArticleEntity(any());
    verify(articleRepository).delete(any());
  }
}
package com.example.monew.domain.article.articleconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

@ExtendWith(MockitoExtension.class)
class NewsBackupBatchConfigTest {

  @Mock private S3Service s3Service;
  @Mock private ObjectMapper objectMapper;
  @Mock private ArticleRepository articleRepository;
  @Mock private ArticleViewRepository articleViewRepository;

  @InjectMocks
  private NewsBackupBatchConfig config;

  @Test
  @DisplayName("JSON 변환 후 업로드 확인")
  void articleS3WriterTest() throws Exception {
    ArticleEntity article = ArticleEntity.builder().build();
    Chunk<ArticleEntity> chunk = new Chunk<>(List.of(article));
    given(objectMapper.writeValueAsString(any())).willReturn("[]");

    config.articleS3Writer().write(chunk);

    verify(s3Service).upload(anyString(), anyString());
  }

  @Test
  @DisplayName("중복 체크로직")
  void duplicateCheckProcessorTest() throws Exception {
    ArticleEntity existingArticle = mock(ArticleEntity.class);
    given(existingArticle.isDeleted()).willReturn(false);

    given(articleRepository.findBySourceUrl(any()))
        .willReturn(Optional.of(existingArticle))
        .willReturn(Optional.empty());

    ArticleEntity article1 = ArticleEntity.builder().sourceUrl("old.com").build();
    ArticleEntity result1 = config.duplicateCheckProcessor().process(article1);
    assertThat(result1).isNull();

    ArticleEntity article2 = ArticleEntity.builder().sourceUrl("new.com").build();
    ArticleEntity result2 = config.duplicateCheckProcessor().process(article2);
    assertThat(result2).isNotNull();
    assertThat(result2.getSourceUrl()).isEqualTo("new.com");
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

  @Test
  @DisplayName("jsonFileItemReader 빈 생성 및 설정 확인 (Reader 분기)")
  void jsonFileItemReaderTest() {
    String testPath = "src/test/resources/test.json";

    var reader = config.jsonFileItemReader(testPath);

    assertThat(reader).isNotNull();
    assertThat(reader).isInstanceOf(org.springframework.batch.item.file.FlatFileItemReader.class);
  }
}
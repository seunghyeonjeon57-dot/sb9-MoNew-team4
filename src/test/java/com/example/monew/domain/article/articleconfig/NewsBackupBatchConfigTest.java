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
  @DisplayName("삭제된 데이터 존재 → 통과")
  void duplicateCheckProcessor_deleted() throws Exception {
    ArticleEntity deleted = mock(ArticleEntity.class);
    given(deleted.isDeleted()).willReturn(true);

    given(articleRepository.findBySourceUrl(any()))
        .willReturn(Optional.of(deleted));

    ArticleEntity article = ArticleEntity.builder()
        .sourceUrl("test.com")
        .build();

    ArticleEntity result = config.duplicateCheckProcessor().process(article);

    assertThat(result).isNotNull();
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
  @DisplayName("이미 존재하고 삭제 안됨 → 아무 작업 안함")
  void articleRestoreWriter_skip() throws Exception {
    ArticleEntity existing = mock(ArticleEntity.class);
    given(existing.isDeleted()).willReturn(false);

    given(articleRepository.findBySourceUrl("exist.com"))
        .willReturn(Optional.of(existing));

    Chunk<ArticleEntity> chunk = new Chunk<>(List.of(
        ArticleEntity.builder().sourceUrl("exist.com").build()
    ));

    config.articleRestoreWriter().write(chunk);

    verify(articleRepository, never()).save(any());
    verify(articleRepository, never()).restoreById(any());
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
  @DisplayName("restoreProcessor - 신규 데이터 생성")
  void restoreProcessor_new() throws Exception {
    given(articleRepository.findBySourceUrl(any()))
        .willReturn(Optional.empty());

    ArticleBackupDto dto = ArticleBackupDto.builder()
        .id(UUID.randomUUID())
        .sourceUrl("new.com")
        .title("title")
        .build();

    ArticleEntity result = config.restoreProcessor().process(dto);

    assertThat(result).isNotNull();
    assertThat(result.getSourceUrl()).isEqualTo("new.com");
  }

  @Test
  @DisplayName("restoreProcessor - 이미 존재하고 삭제 안됨 → null")
  void restoreProcessor_skip() throws Exception {
    ArticleEntity existing = mock(ArticleEntity.class);
    given(existing.isDeleted()).willReturn(false);

    given(articleRepository.findBySourceUrl(any()))
        .willReturn(Optional.of(existing));

    ArticleBackupDto dto = ArticleBackupDto.builder()
        .sourceUrl("exist.com")
        .build();

    ArticleEntity result = config.restoreProcessor().process(dto);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("restoreProcessor - 삭제된 데이터 → 재생성")
  void restoreProcessor_deleted() throws Exception {
    ArticleEntity deleted = mock(ArticleEntity.class);
    given(deleted.isDeleted()).willReturn(true);

    given(articleRepository.findBySourceUrl(any()))
        .willReturn(Optional.of(deleted));

    ArticleBackupDto dto = ArticleBackupDto.builder()
        .id(UUID.randomUUID())
        .sourceUrl("deleted.com")
        .title("복구")
        .build();

    ArticleEntity result = config.restoreProcessor().process(dto);

    assertThat(result).isNotNull();
  }
}
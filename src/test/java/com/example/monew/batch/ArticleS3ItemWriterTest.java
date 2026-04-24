package com.example.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.ArticleS3ItemWriter;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class ArticleS3ItemWriterTest {

  @Mock
  private S3Service s3Service;
  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks
  private ArticleS3ItemWriter itemWriter;

  private final File tempFile = new File("Articlebackup.json");

  @AfterEach
  void tearDown() {
    if (tempFile.exists()) {
      tempFile.delete();
    }
  }

  @Test
  @DisplayName("청크를 받으면 JSON 형식으로 로컬 파일에 한 줄씩")
  void writeTest() throws Exception {
    ArticleEntity article1 = ArticleEntity.builder().title("뉴스1").build();
    ArticleEntity article2 = ArticleEntity.builder().title("뉴스2").build();
    Chunk<ArticleEntity> chunk = new Chunk<>(List.of(article1, article2));

    itemWriter.write(chunk);

    assertThat(tempFile.exists()).isTrue();

    List<String> lines = Files.readAllLines(tempFile.toPath());
    assertThat(lines).hasSize(2); // 두 건인지
    assertThat(lines.get(0)).contains("뉴스1");
    assertThat(lines.get(1)).contains("뉴스2");
  }
}
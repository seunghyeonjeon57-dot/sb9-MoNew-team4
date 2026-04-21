package com.example.monew.domain.article.batch;

import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
public class ArticleS3ItemWriter implements ItemWriter<ArticleEntity> {
  private final S3Service s3Service;
  private final ObjectMapper objectMapper;
  private final File tempFile = new File("Articlebackup.json"); // 임시 파일

  @Override
  public void write(Chunk<? extends ArticleEntity> chunk) throws Exception {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, true))) {
      for (ArticleEntity article : chunk) {
        String json = objectMapper.writeValueAsString(article);
        writer.write(json);
        writer.newLine();
      }
    }
  }

}
package com.example.monew.domain.article.batch.service;


import com.example.monew.domain.article.batch.exception.S3DownloadException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

  private final S3Client s3Client;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  // 파일 업로드 v2 방식
  public void upload(String key, String content) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType("application/json")
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromString(content, StandardCharsets.UTF_8));
  }
  public File download(String key) {
    try {
      java.nio.file.Path tempPath;
      java.nio.file.Path baseDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".monew-temp");
      if (!Files.exists(baseDir)) {
        Files.createDirectories(baseDir);
      }

      if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
        java.nio.file.attribute.FileAttribute<java.util.Set<java.nio.file.attribute.PosixFilePermission>> attr =
            java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
            );
        tempPath = Files.createTempFile(baseDir, "s3-restore-", ".json", attr);
      } else {
        tempPath = Files.createTempFile(baseDir, "s3-restore-", ".json");
        File tempFile = tempPath.toFile();
        tempFile.setReadable(true, true);
        tempFile.setWritable(true, true);
      }

      Files.deleteIfExists(tempPath);

      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();

      s3Client.getObject(getObjectRequest,
          software.amazon.awssdk.core.sync.ResponseTransformer.toFile(tempPath));

      File tempFile = tempPath.toFile();
      tempFile.deleteOnExit();
      return tempFile;

    } catch (NoSuchKeyException e) {
      log.warn("S3 파일 없음 key={}", key);
      throw new S3FileNotFoundException(key);
    } catch (Exception e) {
      log.error("S3 파일 다운로드 중 에러 발생 key={}", key, e);
      throw new S3DownloadException(key, e);
    }
  }
}
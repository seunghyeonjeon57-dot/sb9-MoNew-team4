package com.example.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.batch.exception.S3DownloadException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import com.example.monew.domain.article.batch.service.S3Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
@ExtendWith(MockitoExtension.class)
class S3Servicetest {
  @Mock
  private S3Client s3Client;

  @InjectMocks
  private S3Service s3Service;

  private final String bucketName = "test-bucket";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(s3Service, "bucket", bucketName);
  }

  @Test
  @DisplayName("다운로드 성공 - 커버리지 확보")
  void download_success() throws Exception {

    String key = "test.json";

    doAnswer(invocation -> {
      Object transformer = invocation.getArgument(1);

      if (transformer instanceof software.amazon.awssdk.core.sync.ResponseTransformer<?, ?> rt) {

        Path temp = Files.createTempFile("s3-restore-", ".json");
        Files.writeString(temp, "mock data");
      }

      return null;
    }).when(s3Client).getObject(
        any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class),
        any(software.amazon.awssdk.core.sync.ResponseTransformer.class)
    );

    File file = s3Service.download(key);

    assertThat(file).isNotNull();
    assertThat(file.getName()).startsWith("s3-restore-");
  }

  @Test
  @DisplayName("업로드 성공 테스트")
  void upload_Success() {
    String key = "test.json";
    String content = "{\"name\":\"test\"}";

    s3Service.upload(key, content);

    verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("S3에 파일이 없을 때 S3FileNotFoundException 발생")
  void download_Fail_NotFound() {
    String key = "none.json";
    doThrow(NoSuchKeyException.builder().build())
        .when(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));

    assertThatThrownBy(() -> s3Service.download(key))
        .isInstanceOf(S3FileNotFoundException.class);
  }

  @Test
  @DisplayName("다운로드 중 기타 예외 발생 시 S3DownloadException 발생")
  void download_Fail_GeneralError() {
    String key = "error.json";
    doThrow(new RuntimeException("Network Error"))
        .when(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));

    assertThatThrownBy(() -> s3Service.download(key))
        .isInstanceOf(S3DownloadException.class);
  }
}
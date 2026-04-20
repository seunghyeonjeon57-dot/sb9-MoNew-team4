package com.example.monew.batch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.example.monew.domain.article.batch.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class S3Servicetest {

  @MockitoBean
  private S3Service s3Service;

  @Test
  @DisplayName("S3 파일 업로드 및 다운로드 테스트")
  void s3UploadAndGetTest() throws Exception { // 파일 처리를 위해 Exception 추가
    String testContent = "Hello, S3! This is a backup test.";
    String s3Path = "test/integration-test.txt";

    java.io.File fakeFile = java.io.File.createTempFile("test-", ".json");
    java.nio.file.Files.writeString(fakeFile.toPath(), testContent);

    given(s3Service.download(anyString())).willReturn(fakeFile);

    s3Service.upload(s3Path, testContent);

    // String -> File
    java.io.File downloadedFile = s3Service.download(s3Path);

    String downloadedContent = java.nio.file.Files.readString(downloadedFile.toPath());

    System.out.println("다운로드된 내용: " + downloadedContent);
    assertThat(downloadedContent).isEqualTo(testContent);

    fakeFile.delete();
  }
}

package com.example.monew.batch;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.service.S3Service;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3Servicetest {

  @Mock
  private S3Service s3Service;

  @Test
  @DisplayName("S3 파일 업로드 및 다운로드 로직 검증")
  void s3UploadAndGetTest() throws Exception {
    String testContent = "Hello, S3!";
    String s3Path = "test/path.txt";
    File fakeFile = File.createTempFile("test-", ".json");
    Files.writeString(fakeFile.toPath(), testContent);

    given(s3Service.download(s3Path)).willReturn(fakeFile);

    s3Service.upload(s3Path, testContent);
    File downloadedFile = s3Service.download(s3Path);

    verify(s3Service).upload(s3Path, testContent);

    String downloadedContent = Files.readString(downloadedFile.toPath());
    assertThat(downloadedContent).isEqualTo(testContent);

    fakeFile.delete();
  }
}
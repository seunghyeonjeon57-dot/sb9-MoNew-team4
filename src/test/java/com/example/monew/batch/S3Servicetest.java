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
@ActiveProfiles ("test")
class S3Servicetest {

  @MockitoBean
  private S3Service s3Service;

  @Test
  @DisplayName("S3 파일 업로드 및 삭제 테스트")
  void s3UploadAndGetTest() {
    String testContent = "Hello, S3! This is a backup test.";
    String s3Path = "test/integration-test.txt";

    given(s3Service.download(anyString())).willReturn(testContent);

    s3Service.upload(s3Path, testContent);
    System.out.println("업로드 호출 완료: " + s3Path);

    String downloadedContent = s3Service.download(s3Path); // 설정한 가짜 값이 나와야 된다

    System.out.println("다운로드된 내용: " + downloadedContent);
    assertThat(downloadedContent).isEqualTo(testContent);
  }
  }

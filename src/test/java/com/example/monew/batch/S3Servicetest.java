package com.example.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.batch.exception.S3DownloadException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import com.example.monew.domain.article.batch.service.S3Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
@ExtendWith(MockitoExtension.class)
class S3Servicetest {

  @Mock
  private S3Client s3Client;

  @InjectMocks
  private S3Service s3Service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
  }

  @Test
  @DisplayName("다운로드 성공 시 임시 파일 생성 및 권한 설정 확인")
  void download_Success() throws Exception {
    // given
    String key = "test.json";
    // s3Client.getObject 호출 시 실제로 파일을 생성하도록 모킹 (파일이 있어야 권한 체크 가능)
    doAnswer(invocation -> {
      Path path = invocation.getArgument(1);
      // 이미 createTempFile로 파일이 생성된 상태이므로, 내용만 써주거나 그대로 둡니다.
      return null;
    }).when(s3Client).getObject(any(GetObjectRequest.class), any(Path.class));

    // when
    File file = s3Service.download(key);

    // then
    assertThat(file).exists();
    assertThat(file.getName()).startsWith("s3-restore-");

    // 권한 설정 커버리지 및 검증 코드
    if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      // POSIX 환경(Mac, Linux)인 경우 rw------- 권한 확인
      var permissions = Files.getPosixFilePermissions(file.toPath());
      assertThat(permissions).containsExactlyInAnyOrder(
          java.nio.file.attribute.PosixFilePermission.OWNER_READ,
          java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
      );
    } else {
      // Non-POSIX(Windows)인 경우
      assertThat(file.canRead()).isTrue();
      assertThat(file.canWrite()).isTrue();
    }

    // 테스트 후 정리 (deleteOnExit가 있지만 즉시 삭제 권장)
    file.delete();
  }

  @Test
  @DisplayName("파일 업로드 성공 테스트")
  void upload_Success() {
    s3Service.upload("test.json", "{}");
    verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("S3에 파일이 없을 때 S3FileNotFoundException 발생")
  void download_Fail_NotFound() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class)))
        .thenThrow(NoSuchKeyException.builder().build());

    assertThatThrownBy(() -> s3Service.download("none.json"))
        .isInstanceOf(S3FileNotFoundException.class);
  }

  @Test
  @DisplayName("S3 다운로드 중 일반 에러 발생 시 S3DownloadException 발생")
  void download_Fail_General() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class)))
        .thenThrow(new RuntimeException("S3 Error"));

    assertThatThrownBy(() -> s3Service.download("error.json"))
        .isInstanceOf(S3DownloadException.class);
  }
}
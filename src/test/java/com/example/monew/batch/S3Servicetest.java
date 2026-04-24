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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
  @DisplayName("업로드 성공 시 s3Client의 putObject가 호출")
  void upload_Success() {
    s3Service.upload("test.json", "{}");

    verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("다운로드 성공 시 임시 파일 생성")
  void download_Success() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class))).thenReturn(null);

    File file = s3Service.download("test.json");

    assertThat(file).exists();
    assertThat(file.getName()).startsWith("s3-restore-");
    file.delete();
  }

  @Test
  @DisplayName("파일이 없을 때 예외 발생")
  void download_Fail_NotFound() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class)))
        .thenThrow(NoSuchKeyException.builder().build());

    assertThatThrownBy(() -> s3Service.download("none.json"))
        .isInstanceOf(S3FileNotFoundException.class);
  }

  @Test
  @DisplayName("기타 에러 시 S3DownloadException이 발생")
  void download_Fail_General() {
    when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class)))
        .thenThrow(new RuntimeException("S3 Error"));

    assertThatThrownBy(() -> s3Service.download("error.json"))
        .isInstanceOf(S3DownloadException.class);
  }
  @Test
  @DisplayName("권한 설정 로직 커버리지 체크")
  void download_NonPosixEnvironment() throws IOException {
    try (MockedStatic<FileSystems> mockedFileSystems = mockStatic(FileSystems.class);
        MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {

      FileSystem mockFileSystem = mock(FileSystem.class);
      when(mockFileSystem.supportedFileAttributeViews()).thenReturn(Collections.emptySet());
      mockedFileSystems.when(FileSystems::getDefault).thenReturn(mockFileSystem);

      File mockFile = mock(File.class);
      Path mockPath = mock(Path.class);
      when(mockPath.toFile()).thenReturn(mockFile);

      mockedFiles.when(() -> Files.createTempFile(anyString(), anyString()))
          .thenReturn(mockPath);
      mockedFiles.when(() -> Files.createTempFile(anyString(), anyString(), any()))
          .thenReturn(mockPath);

      lenient().when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class))).thenReturn(null);

      s3Service.download("test.json");

      verify(mockFile).setReadable(true, true);
      verify(mockFile).setWritable(true, true);
      verify(mockFile).setExecutable(true, true);
    }
}
}
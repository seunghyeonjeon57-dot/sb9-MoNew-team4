package com.example.monew;

import com.example.monew.config.S3Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class MoNewApplicationTests {

  @MockitoBean
  private S3Config s3Config;

  @Test
  void contextLoads() {
  }

}

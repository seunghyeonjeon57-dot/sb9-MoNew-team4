package com.example.monew.domain.article.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

  
  @Value("${AWS_ACCESS_KEY}")
  private String newsAccessKey;
  @Value("${AWS_SECRET_KEY}")
  private String newsSecretKey;
  @Value("${AWS_REGION}")
  private String newsRegion;

  
  @Value("${AWS_LOG_ACCESS_KEY}")
  private String logAccessKey;
  @Value("${AWS_LOG_SECRET_KEY}")
  private String logSecretKey;
  @Value("${AWS_LOG_REGION}")
  private String logRegion;

  @Bean
  @Primary 
  public S3Client s3Client() {
    return createS3Client(newsAccessKey, newsSecretKey, newsRegion);
  }

  @Bean(name = "logS3Client") 
  public S3Client logS3Client() {
    return createS3Client(logAccessKey, logSecretKey, logRegion);
  }

  private S3Client createS3Client(String accessKey, String secretKey, String region) {
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .build();
  }
}
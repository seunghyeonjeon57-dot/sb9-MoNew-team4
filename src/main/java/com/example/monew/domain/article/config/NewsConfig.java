package com.example.monew.domain.article.config;

import com.example.monew.domain.article.batch.NewsRss;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NewsRss.class)
public class NewsConfig {
}
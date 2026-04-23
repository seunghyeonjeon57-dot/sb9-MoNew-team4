package com.example.monew.domain.notification.event;

import java.util.UUID;

public record ArticleRegisteredEvent(
    UUID articleId,
    String articleTitle,
    String interestName // ArticleEntity의 interest 필드값
) {}
package com.example.monew.domain.notification.event;

import java.util.UUID;

public record CommentLikedEvent(
    UUID receiverId,
    UUID commentId,
    UUID likerId,
    String likerNickname
) {}
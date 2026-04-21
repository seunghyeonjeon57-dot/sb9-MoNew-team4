package com.example.monew.domain.notification.event;

import java.util.UUID;

public record CommentLikedEvent(
    UUID receiverId, // 알림을 받을 사람 (댓글 작성자)
    UUID commentId,
    UUID linkerId
) {}
package com.example.monew.domain.comment.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class CommentLikeNotFound extends MonewException {

  public CommentLikeNotFound(ErrorCode errorCode) {
    super(errorCode);
  }
}

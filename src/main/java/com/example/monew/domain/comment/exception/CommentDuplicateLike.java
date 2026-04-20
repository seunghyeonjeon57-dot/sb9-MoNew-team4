package com.example.monew.domain.comment.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class CommentDuplicateLike extends MonewException {

  public CommentDuplicateLike(ErrorCode errorCode) {
    super(errorCode);
  }
}

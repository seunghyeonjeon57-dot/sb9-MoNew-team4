package com.example.monew.domain.comment.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class CommentNotFoundException extends MonewException {
  public CommentNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}

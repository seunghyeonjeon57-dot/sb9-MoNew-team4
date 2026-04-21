package com.example.monew.domain.comment.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class CommentContentBlank extends MonewException {

  public CommentContentBlank(ErrorCode errorCode) {
    super(errorCode);
  }
}

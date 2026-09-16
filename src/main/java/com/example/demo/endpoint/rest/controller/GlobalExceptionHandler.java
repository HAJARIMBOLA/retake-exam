package com.example.demo.endpoint.rest.controller;

import com.example.demo.model.ErrorResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
    IllegalArgumentException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class,
    MultipartException.class
  })
  public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest().body(new ErrorResponseDTO(exception.getMessage()));
  }
}

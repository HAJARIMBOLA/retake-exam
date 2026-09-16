package com.example.demo.endpoint.rest.controller;

import com.example.demo.model.SubmissionDTO;
import com.example.demo.service.SubmissionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;

  @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SubmissionDTO> createSubmission(
      @RequestParam("file") MultipartFile file, @RequestParam("email") String email) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(submissionService.createSubmission(file, email));
  }
}

package com.example.demo.mapper;

import com.example.demo.domain.Submission;
import com.example.demo.model.SubmissionDTO;

public class SubmissionMapper {

  private SubmissionMapper() {}

  public static SubmissionDTO toDTO(Submission submission) {
    return new SubmissionDTO(
        submission.getId(),
        submission.getEmail(),
        submission.getThumbnailKey(),
        submission.getCreatedAt());
  }
}

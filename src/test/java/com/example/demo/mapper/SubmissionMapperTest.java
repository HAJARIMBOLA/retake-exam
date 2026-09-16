package com.example.demo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.domain.Submission;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionMapperTest {

  @Test
  void toDTO_mapsAllFields_whenThumbnailKeyIsSet() {
    var submission =
        new Submission(
            UUID.randomUUID(), "user@example.com", "submissions/key/thumbnail.png", Instant.now());

    var dto = SubmissionMapper.toDTO(submission);

    assertThat(dto.id()).isEqualTo(submission.getId());
    assertThat(dto.email()).isEqualTo(submission.getEmail());
    assertThat(dto.thumbnailKey()).isEqualTo(submission.getThumbnailKey());
    assertThat(dto.createdAt()).isEqualTo(submission.getCreatedAt());
  }

  @Test
  void toDTO_mapsNullThumbnailKey_whenNotYetProcessed() {
    var submission = new Submission(UUID.randomUUID(), "user@example.com", null, Instant.now());

    var dto = SubmissionMapper.toDTO(submission);

    assertThat(dto.thumbnailKey()).isNull();
  }
}

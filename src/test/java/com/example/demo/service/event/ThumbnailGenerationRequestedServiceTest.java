package com.example.demo.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.domain.Submission;
import com.example.demo.endpoint.event.model.ThumbnailGenerationRequested;
import com.example.demo.exception.NotFoundException;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.image.ImageResizer;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.SubmissionRepository;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThumbnailGenerationRequestedServiceTest {

  @Mock private SubmissionRepository submissionRepository;
  @Mock private BucketComponent bucketComponent;
  @Mock private ImageResizer imageResizer;
  @Mock private Mailer mailer;

  private ThumbnailGenerationRequestedService service;

  @BeforeEach
  void setUp() {
    service =
        new ThumbnailGenerationRequestedService(
            submissionRepository, bucketComponent, imageResizer, mailer);
  }

  @Test
  void accept_generatesThumbnailAndSendsEmail_whenSubmissionExists() throws Exception {
    var submissionId = UUID.randomUUID();
    var submission = new Submission(submissionId, "user@example.com", null, Instant.now());
    var originalFile = File.createTempFile("original-", ".png");
    var thumbnailFile = File.createTempFile("thumbnail-", ".png");
    var originalBucketKey = "submissions/" + submissionId + "/original.png";
    var event = new ThumbnailGenerationRequested(submissionId, originalBucketKey);
    var downloadLink = new URL("https://example.com/download");

    when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(bucketComponent.download(originalBucketKey)).thenReturn(originalFile);
    when(imageResizer.resize(originalFile)).thenReturn(thumbnailFile);
    when(bucketComponent.presign(any(), any(Duration.class))).thenReturn(downloadLink);

    service.accept(event);

    var thumbnailKey = "submissions/" + submissionId + "/thumbnail.png";
    verify(bucketComponent).upload(thumbnailFile, thumbnailKey);

    var submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionRepository).save(submissionCaptor.capture());
    assertThat(submissionCaptor.getValue().getThumbnailKey()).isEqualTo(thumbnailKey);

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assertThat(emailCaptor.getValue().to().getAddress()).isEqualTo("user@example.com");
    assertThat(emailCaptor.getValue().htmlBody()).contains(downloadLink.toString());
  }

  @Test
  void accept_throwsNotFoundException_whenSubmissionDoesNotExist() {
    var submissionId = UUID.randomUUID();
    var event =
        new ThumbnailGenerationRequested(
            submissionId, "submissions/" + submissionId + "/original.png");
    when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.accept(event))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(submissionId.toString());

    verify(bucketComponent, never()).download(any());
    verify(mailer, never()).accept(any());
  }

  @Test
  void accept_updatesThumbnailKeyBeforeFailing_whenEmailAddressIsMalformed() throws Exception {
    var submissionId = UUID.randomUUID();
    var submission =
        new Submission(submissionId, "malformed@mail@example.com", null, Instant.now());
    var originalFile = File.createTempFile("original-", ".png");
    var thumbnailFile = File.createTempFile("thumbnail-", ".png");
    var originalBucketKey = "submissions/" + submissionId + "/original.png";
    var event = new ThumbnailGenerationRequested(submissionId, originalBucketKey);

    when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(bucketComponent.download(originalBucketKey)).thenReturn(originalFile);
    when(imageResizer.resize(originalFile)).thenReturn(thumbnailFile);

    assertThatThrownBy(() -> service.accept(event)).isInstanceOf(RuntimeException.class);

    verify(submissionRepository)
        .save(
            argThat(
                saved -> saved.getThumbnailKey() != null && saved.getId().equals(submissionId)));
    verify(mailer, never()).accept(any());
  }
}

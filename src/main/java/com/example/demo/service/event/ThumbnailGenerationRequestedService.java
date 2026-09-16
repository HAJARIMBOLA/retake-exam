package com.example.demo.service.event;

import com.example.demo.endpoint.event.model.ThumbnailGenerationRequested;
import com.example.demo.exception.NotFoundException;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.image.ImageResizer;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.SubmissionRepository;
import jakarta.mail.internet.InternetAddress;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ThumbnailGenerationRequestedService implements Consumer<ThumbnailGenerationRequested> {

  private static final Duration DOWNLOAD_LINK_EXPIRATION = Duration.ofHours(1);

  private final SubmissionRepository submissionRepository;
  private final BucketComponent bucketComponent;
  private final ImageResizer imageResizer;
  private final Mailer mailer;

  @Override
  public void accept(ThumbnailGenerationRequested event) {
    try {
      generateThumbnailAndNotify(event);
    } catch (Exception exception) {
      log.error(
          "Thumbnail generation failed for submission {}: {}",
          event.getSubmissionId(),
          exception.getMessage(),
          exception);
      throw exception instanceof RuntimeException runtimeException
          ? runtimeException
          : new RuntimeException(exception);
    }
  }

  @SneakyThrows
  private void generateThumbnailAndNotify(ThumbnailGenerationRequested event) {
    var submission =
        submissionRepository
            .findById(event.getSubmissionId())
            .orElseThrow(
                () -> new NotFoundException("Submission not found: " + event.getSubmissionId()));

    var original = bucketComponent.download(event.getOriginalBucketKey());
    var thumbnail = imageResizer.resize(original);

    var thumbnailKey = "submissions/" + submission.getId() + "/thumbnail.png";
    bucketComponent.upload(thumbnail, thumbnailKey);

    submission.setThumbnailKey(thumbnailKey);
    submissionRepository.save(submission);

    var downloadLink = bucketComponent.presign(thumbnailKey, DOWNLOAD_LINK_EXPIRATION);
    mailer.accept(
        new Email(
            new InternetAddress(submission.getEmail()),
            List.of(),
            List.of(),
            "Your thumbnail is ready",
            "<p>Your thumbnail is ready: <a href=\""
                + downloadLink
                + "\">"
                + downloadLink
                + "</a></p>",
            List.of()));
  }
}

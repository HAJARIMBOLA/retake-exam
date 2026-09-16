package com.example.demo.service;

import static com.example.demo.file.image.ImageMediaType.extensionFor;
import static com.example.demo.file.image.ImageMediaType.isSupported;

import com.example.demo.domain.Submission;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.ThumbnailGenerationRequested;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.zip.FileTyper;
import com.example.demo.mapper.SubmissionMapper;
import com.example.demo.model.SubmissionDTO;
import com.example.demo.repository.SubmissionRepository;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final BucketComponent bucketComponent;
  private final FileTyper fileTyper;
  private final EventProducer<ThumbnailGenerationRequested> eventProducer;

  @SneakyThrows
  public SubmissionDTO createSubmission(MultipartFile file, String email) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("file must not be empty");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email must not be blank");
    }

    var tempFile = toTempFile(file);
    var mediaType = fileTyper.apply(tempFile);
    if (!isSupported(mediaType)) {
      throw new IllegalArgumentException("Unsupported file type: " + mediaType);
    }

    var submission = new Submission();
    submission.setEmail(email);
    submission.setThumbnailKey(null);
    submission.setCreatedAt(Instant.now());
    var saved = submissionRepository.save(submission);

    var originalBucketKey = originalBucketKey(saved.getId(), mediaType);
    bucketComponent.upload(tempFile, originalBucketKey);

    eventProducer.accept(
        List.of(new ThumbnailGenerationRequested(saved.getId(), originalBucketKey)));

    return SubmissionMapper.toDTO(saved);
  }

  public List<SubmissionDTO> listSubmissions() {
    return submissionRepository.findAll().stream().map(SubmissionMapper::toDTO).toList();
  }

  private File toTempFile(MultipartFile file) throws IOException {
    var tempFile = File.createTempFile("submission-", ".upload");
    file.transferTo(tempFile);
    return tempFile;
  }

  private String originalBucketKey(UUID submissionId, MediaType mediaType) {
    return "submissions/" + submissionId + "/original" + extensionFor(mediaType);
  }
}

package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.domain.Submission;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.ThumbnailGenerationRequested;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.zip.FileTyper;
import com.example.demo.repository.SubmissionRepository;
import java.io.File;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

  @Mock private SubmissionRepository submissionRepository;
  @Mock private BucketComponent bucketComponent;
  @Mock private FileTyper fileTyper;
  @Mock private EventProducer<ThumbnailGenerationRequested> eventProducer;

  private SubmissionService submissionService;

  @BeforeEach
  void setUp() {
    submissionService =
        new SubmissionService(submissionRepository, bucketComponent, fileTyper, eventProducer);
  }

  @Test
  void createSubmission_savesUploadsAndPublishesEvent_whenValidPngUpload() {
    var file =
        new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());
    when(fileTyper.apply(any(File.class))).thenReturn(MediaType.IMAGE_PNG);
    when(submissionRepository.save(any(Submission.class)))
        .thenAnswer(
            invocation -> {
              Submission submission = invocation.getArgument(0);
              submission.setId(UUID.randomUUID());
              return submission;
            });

    var result = submissionService.createSubmission(file, "user@example.com");

    assertThat(result.id()).isNotNull();
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.thumbnailKey()).isNull();
    assertThat(result.createdAt()).isNotNull();

    var keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(any(File.class), keyCaptor.capture());
    assertThat(keyCaptor.getValue()).isEqualTo("submissions/" + result.id() + "/original.png");

    @SuppressWarnings("unchecked")
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventsCaptor.capture());
    var events = (List<ThumbnailGenerationRequested>) eventsCaptor.getValue();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getSubmissionId()).isEqualTo(result.id());
    assertThat(events.get(0).getOriginalBucketKey()).isEqualTo(keyCaptor.getValue());
  }

  @Test
  void createSubmission_throwsIllegalArgumentException_whenFileIsEmpty() {
    var emptyFile =
        new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);

    assertThatThrownBy(() -> submissionService.createSubmission(emptyFile, "user@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("file must not be empty");

    verify(submissionRepository, never()).save(any());
  }

  @Test
  void createSubmission_throwsIllegalArgumentException_whenFileIsNull() {
    assertThatThrownBy(() -> submissionService.createSubmission(null, "user@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("file must not be empty");

    verify(submissionRepository, never()).save(any());
  }

  @Test
  void createSubmission_throwsIllegalArgumentException_whenEmailIsBlank() {
    var file =
        new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());

    assertThatThrownBy(() -> submissionService.createSubmission(file, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email must not be blank");

    verify(submissionRepository, never()).save(any());
  }

  @Test
  void createSubmission_throwsIllegalArgumentException_whenMediaTypeUnsupported() {
    var file =
        new MockMultipartFile(
            "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());
    when(fileTyper.apply(any(File.class))).thenReturn(MediaType.APPLICATION_PDF);

    assertThatThrownBy(() -> submissionService.createSubmission(file, "user@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported file type");

    verify(submissionRepository, never()).save(any());
    verify(bucketComponent, never()).upload(any(), any());
  }
}

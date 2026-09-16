package com.example.demo.endpoint.rest.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.model.SubmissionDTO;
import com.example.demo.service.SubmissionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SubmissionController.class)
class SubmissionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private SubmissionService submissionService;

  @Test
  void createSubmission_returns201WithBody_whenRequestIsValid() throws Exception {
    var file = new MockMultipartFile("file", "image.png", "image/png", "content".getBytes());
    var dto = new SubmissionDTO(UUID.randomUUID(), "user@example.com", null, Instant.now());
    when(submissionService.createSubmission(any(), eq("user@example.com"))).thenReturn(dto);

    mockMvc
        .perform(multipart("/submissions").file(file).param("email", "user@example.com"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(dto.id().toString()))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.thumbnailKey").value(nullValue()));
  }

  @Test
  void createSubmission_returns400_whenFilePartIsMissing() throws Exception {
    mockMvc
        .perform(multipart("/submissions").param("email", "user@example.com"))
        .andExpect(status().isBadRequest());

    verify(submissionService, never()).createSubmission(any(), any());
  }

  @Test
  void createSubmission_returns400_whenEmailParamIsMissing() throws Exception {
    var file = new MockMultipartFile("file", "image.png", "image/png", "content".getBytes());

    mockMvc.perform(multipart("/submissions").file(file)).andExpect(status().isBadRequest());

    verify(submissionService, never()).createSubmission(any(), any());
  }

  @Test
  void createSubmission_returns400_whenServiceRejectsUnsupportedFileType() throws Exception {
    var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
    when(submissionService.createSubmission(any(), eq("user@example.com")))
        .thenThrow(new IllegalArgumentException("Unsupported file type: application/pdf"));

    mockMvc
        .perform(multipart("/submissions").file(file).param("email", "user@example.com"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Unsupported file type: application/pdf"));
  }

  @Test
  void listSubmissions_returns200WithBody_whenSubmissionsExist() throws Exception {
    var dto = new SubmissionDTO(UUID.randomUUID(), "user@example.com", "key.png", Instant.now());
    when(submissionService.listSubmissions()).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/submissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(dto.id().toString()))
        .andExpect(jsonPath("$[0].thumbnailKey").value("key.png"));
  }

  @Test
  void listSubmissions_returns200WithEmptyArray_whenNoneExist() throws Exception {
    when(submissionService.listSubmissions()).thenReturn(List.of());

    mockMvc
        .perform(get("/submissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }
}

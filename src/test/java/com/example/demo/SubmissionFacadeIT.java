package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.conf.FacadeIT;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.ThumbnailGenerationRequested;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.model.SubmissionDTO;
import com.example.demo.repository.SubmissionRepository;
import com.example.demo.service.event.ThumbnailGenerationRequestedService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;

class SubmissionFacadeIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private ThumbnailGenerationRequestedService thumbnailGenerationRequestedService;

  @MockBean private EventProducer<ThumbnailGenerationRequested> eventProducer;
  @MockBean private BucketComponent bucketComponent;
  @MockBean private Mailer mailer;

  @LocalServerPort private int port;

  @BeforeEach
  void disablePersistentConnectionsToAvoidLocalhostKeepAliveDesync() {
    restTemplate.getRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory());
  }

  @Test
  void submittingAnImageCreatesItThenTheWorkerProducesAndPersistsAThumbnail() throws Exception {
    var email = "user@example.com";
    var body = new LinkedMultiValueMap<String, Object>();
    body.add(
        "file",
        new ByteArrayResource(pngBytes()) {
          @Override
          public String getFilename() {
            return "test-image.png";
          }
        });
    body.add("email", email);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    var response =
        restTemplate.postForEntity(
            url("/submissions"), new HttpEntity<>(body, headers), SubmissionDTO.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    var created = response.getBody();
    assertThat(created).isNotNull();
    assertThat(created.thumbnailKey()).isNull();

    var persisted = submissionRepository.findById(created.id()).orElseThrow();
    assertThat(persisted.getEmail()).isEqualTo(email);
    assertThat(persisted.getThumbnailKey()).isNull();

    @SuppressWarnings("unchecked")
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(eventsCaptor.capture());
    var events = (List<ThumbnailGenerationRequested>) eventsCaptor.getValue();
    assertThat(events).hasSize(1);
    var event = events.get(0);
    assertThat(event.getSubmissionId()).isEqualTo(created.id());

    var originalFile = File.createTempFile("original-", ".png");
    ImageIO.write(readPng(pngBytes()), "png", originalFile);
    var downloadLink = new URL("https://example.com/thumbnail");
    when(bucketComponent.download(event.getOriginalBucketKey())).thenReturn(originalFile);
    when(bucketComponent.presign(any(), any(Duration.class))).thenReturn(downloadLink);

    thumbnailGenerationRequestedService.accept(event);

    var expectedThumbnailKey = "submissions/" + created.id() + "/thumbnail.png";
    var updated = submissionRepository.findById(created.id()).orElseThrow();
    assertThat(updated.getThumbnailKey()).isEqualTo(expectedThumbnailKey);

    verify(bucketComponent).upload(any(File.class), eq(expectedThumbnailKey));

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assertThat(emailCaptor.getValue().to().getAddress()).isEqualTo(email);
    assertThat(emailCaptor.getValue().htmlBody()).contains(downloadLink.toString());
  }

  private byte[] pngBytes() throws Exception {
    var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return outputStream.toByteArray();
  }

  private BufferedImage readPng(byte[] bytes) throws Exception {
    return ImageIO.read(new ByteArrayInputStream(bytes));
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}

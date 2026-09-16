package com.example.demo.file.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ImageMediaTypeTest {

  @Test
  void isSupported_returnsTrue_forPngAndJpeg() {
    assertThat(ImageMediaType.isSupported(MediaType.IMAGE_PNG)).isTrue();
    assertThat(ImageMediaType.isSupported(MediaType.IMAGE_JPEG)).isTrue();
  }

  @Test
  void isSupported_returnsFalse_forUnsupportedMediaType() {
    assertThat(ImageMediaType.isSupported(MediaType.APPLICATION_PDF)).isFalse();
  }

  @Test
  void extensionFor_returnsCorrectExtension_forSupportedTypes() {
    assertThat(ImageMediaType.extensionFor(MediaType.IMAGE_PNG)).isEqualTo(".png");
    assertThat(ImageMediaType.extensionFor(MediaType.IMAGE_JPEG)).isEqualTo(".jpg");
  }

  @Test
  void extensionFor_throwsIllegalArgumentException_forUnsupportedType() {
    assertThatThrownBy(() -> ImageMediaType.extensionFor(MediaType.APPLICATION_PDF))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

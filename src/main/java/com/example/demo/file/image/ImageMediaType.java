package com.example.demo.file.image;

import java.util.Set;
import org.springframework.http.MediaType;

public class ImageMediaType {

  public static final Set<MediaType> SUPPORTED = Set.of(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG);

  private ImageMediaType() {}

  public static boolean isSupported(MediaType mediaType) {
    return SUPPORTED.stream().anyMatch(supported -> supported.equals(mediaType));
  }

  public static String extensionFor(MediaType mediaType) {
    if (MediaType.IMAGE_PNG.equals(mediaType)) {
      return ".png";
    }
    if (MediaType.IMAGE_JPEG.equals(mediaType)) {
      return ".jpg";
    }
    throw new IllegalArgumentException("Unsupported image media type: " + mediaType);
  }
}

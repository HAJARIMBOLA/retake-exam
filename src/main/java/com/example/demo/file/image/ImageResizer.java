package com.example.demo.file.image;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class ImageResizer {

  public static final int THUMBNAIL_SIZE = 256;

  @SneakyThrows
  public File resize(File source) {
    var original = ImageIO.read(source);
    var scaled = original.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);

    var resized = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB);
    var graphics = resized.createGraphics();
    graphics.drawImage(scaled, 0, 0, null);
    graphics.dispose();

    var thumbnailFile = File.createTempFile("thumbnail-", ".png");
    ImageIO.write(resized, "png", thumbnailFile);
    return thumbnailFile;
  }
}

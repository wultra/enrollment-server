/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Component to image processing.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class ImageProcessor {

    /**
     * Upscale the given image if its width is smaller than the required value, otherwise the original image is returned.
     *
     * @param ownerId Owner identification for logging purpose
     * @param sourceImage the image to upscale
     * @param minimalWidth required minimal width of the image in pixels
     * @return the upscaled image if needed or the original
     */
    public Image upscaleImage(final OwnerId ownerId, final Image sourceImage, final int minimalWidth) throws PresenceCheckException {
        final String filename = sourceImage.getFilename();
        logger.debug("Attempt to upscale image: {} to minimalWidth: {} px, {}", filename, minimalWidth, ownerId);

        try {
            final BufferedImageWrapper bufferedSourceImageWrapper = readImage(new ByteArrayInputStream(sourceImage.getData()));
            final BufferedImage bufferedSourceImage = bufferedSourceImageWrapper.bufferedImage;
            final String formatName = bufferedSourceImageWrapper.formatName;
            final int currentWidth = bufferedSourceImage.getWidth();
            final int currentHeight = bufferedSourceImage.getHeight();
            logger.info("Processing image: {}, format: {}, size: {} x {} px, {}", filename, formatName, currentWidth, currentHeight, ownerId);

            if (currentWidth < minimalWidth) {
                final double aspectRatio = minimalWidth / (double) currentWidth;
                final int minimalHeight = (int) (aspectRatio * currentHeight);
                logger.info("Upscaling image: {} to minimal size: {} x {} px, {}", filename, minimalWidth, minimalHeight, ownerId);
                final java.awt.Image upscaledImage = bufferedSourceImage.getScaledInstance(minimalWidth, minimalHeight, java.awt.Image.SCALE_SMOOTH);
                final BufferedImage bufferedOutputImage = new BufferedImage(minimalWidth, minimalHeight, bufferedSourceImage.getType());
                bufferedOutputImage.getGraphics().drawImage(upscaledImage, 0, 0, null);
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedOutputImage, formatName, outputStream);

                return Image.builder()
                        .data(outputStream.toByteArray())
                        .filename(filename)
                        .build();
            } else {
                logger.debug("Returning original image: {}, {}", filename, ownerId);
                return sourceImage;
            }
        } catch (IOException e) {
            throw new PresenceCheckException("Unable to read image", e);
        }
    }

    private static BufferedImageWrapper readImage(final InputStream inputStream) throws IOException {
        try (final ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw new IOException("No reader found");
            }

            final ImageReader reader = readers.next();
            final String formatName = reader.getFormatName();
            reader.setInput(imageInputStream);
            final BufferedImage bufferedImage = reader.read(0);
            return new BufferedImageWrapper(formatName, bufferedImage);
        }
    }

    private record BufferedImageWrapper(String formatName, BufferedImage bufferedImage) {
    }
}

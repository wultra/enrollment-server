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

import com.google.common.io.Files;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Component to image processing.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class ImageProcessor {

    private static final String TYPE_PNG = "PNG";
    private static final String SUFFIX_PNG = ".png";
    private static final int KILOBYTE = 1024;

    /**
     * Upscale the given image if its width is smaller than the required value, otherwise the original image is returned.
     *
     * @param ownerId Owner identification for logging purpose
     * @param sourceImage the image to upscale
     * @param minimalWidth required minimal width of the image in pixels
     * @return the upscaled image (if needed) in PNG format or the original image in the original format
     */
    public Image upscaleImage(final OwnerId ownerId, final Image sourceImage, final int minimalWidth) throws PresenceCheckException {
        final String filename = sourceImage.getFilename();
        logger.debug("Attempt to upscale image: {} to minimalWidth: {} px, {}", filename, minimalWidth, ownerId);

        try {
            final BufferedImage bufferedSourceImage = ImageIO.read(new ByteArrayInputStream(sourceImage.getData()));
            if (bufferedSourceImage == null) {
                throw new PresenceCheckException("Unable to read image " + filename);
            }
            final int currentWidth = bufferedSourceImage.getWidth();
            final int currentHeight = bufferedSourceImage.getHeight();
            logger.info("Processing image: {}, resolution: {} x {} px, size: {} KB, {}", filename, currentWidth, currentHeight, sourceImage.getData().length / KILOBYTE, ownerId);

            if (currentWidth < minimalWidth) {
                final double aspectRatio = minimalWidth / (double) currentWidth;
                final int minimalHeight = (int) (aspectRatio * currentHeight);
                logger.info("Upscaling image: {} to minimal size: {} x {} px, {}", filename, minimalWidth, minimalHeight, ownerId);
                final java.awt.Image upscaledImage = bufferedSourceImage.getScaledInstance(minimalWidth, minimalHeight, java.awt.Image.SCALE_SMOOTH);
                final BufferedImage bufferedOutputImage = new BufferedImage(minimalWidth, minimalHeight, bufferedSourceImage.getType());
                bufferedOutputImage.getGraphics().drawImage(upscaledImage, 0, 0, null);
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedOutputImage, TYPE_PNG, outputStream);

                final String filenamePng = Files.getNameWithoutExtension(filename) + SUFFIX_PNG;

                final byte[] targetData = outputStream.toByteArray();
                logger.debug("Image: {}, size: {} KB, {}", filenamePng, targetData.length / KILOBYTE, ownerId);
                return Image.builder()
                        .data(targetData)
                        .filename(filenamePng)
                        .build();
            } else {
                logger.debug("Returning original image: {}, {}", filename, ownerId);
                return sourceImage;
            }
        } catch (IOException e) {
            throw new PresenceCheckException("Unable to read image", e);
        }
    }
}

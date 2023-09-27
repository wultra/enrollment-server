/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link ImageProcessor}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
class ImageProcessorTest {

    private static final String SELFIE_PHOTO_PATH = "/images/specimen_photo.jpg";

    private final ImageProcessor tested = new ImageProcessor();

    @Test
    void testUpscaleImage_bigEnough() throws Exception {
        final byte[] data = readFile();
        final Image image = Image.builder()
                .data(data)
                .filename("specimen_photo.jpg")
                .build();

        final Image result = tested.upscaleImage(new OwnerId(), image, 200);

        assertEquals(data.length, result.getData().length);
        assertEquals("specimen_photo.jpg", result.getFilename());
    }

    @Test
    void testUpscaleImage() throws Exception {
        testUpscaleImageInternal();
    }

    /**
     * Set system property {@code com.wultra.app.onboardingserver.impl.service.ImageProcessorTest} to {@code persist}
     * to save result into the temp directory.
     */
    @Test
    @EnabledIfSystemProperty(named = "com.wultra.app.onboardingserver.impl.service.ImageProcessorTest", matches = "persist")
    void testUpscaleImage_saveTempFile(final @TempDir(cleanup = CleanupMode.NEVER) Path tempDir) throws Exception {
        final Image result = testUpscaleImageInternal();
        final Path path = tempDir.resolve(result.getFilename());
        logger.info("Writing upscale image to {}", path);
        Files.write(path, result.getData());
    }

    private Image testUpscaleImageInternal() throws Exception {
        final byte[] data = readFile();
        final Image image = Image.builder()
                .data(data)
                .filename("specimen_photo.jpg")
                .build();

        final Image result = tested.upscaleImage(new OwnerId(), image, 400);

        assertTrue(result.getData().length > data.length);
        assertEquals("specimen_photo.png", result.getFilename());
        return result;
    }

    private static byte[] readFile() throws Exception {
        try (final InputStream inputStream = ImageProcessorTest.class.getResourceAsStream(SELFIE_PHOTO_PATH)) {
            assertNotNull(inputStream);
            return inputStream.readAllBytes();
        }
    }
}
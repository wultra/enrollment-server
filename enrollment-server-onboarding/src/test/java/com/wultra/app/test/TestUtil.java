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
package com.wultra.app.test;

import com.wultra.app.enrollmentserver.model.integration.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test utilities.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
public final class TestUtil {

    private TestUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Loads photo from a file
     * @param path Path to the file with the photo
     * @return Image with the photo data
     * @throws IOException when an error occurred
     */
    public static Image loadPhoto(final String path) throws IOException {
        final File file = new File(path);

        return Image.builder()
                .data(readImageData(path))
                .filename(file.getName())
                .build();
    }

    private static byte[] readImageData(final String path) throws IOException {
        try (InputStream stream = TestUtil.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Unable to get a stream for: " + path);
            }
            return stream.readAllBytes();
        }
    }

}

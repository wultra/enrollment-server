/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.app.test;

import com.wultra.app.enrollmentserver.model.integration.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test utilities
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
public class TestUtil {

    /**
     * Loads photo from a file
     * @param path Path to the file with the photo
     * @return Image with the photo data
     * @throws IOException when an error occurred
     */
    public static Image loadPhoto(String path) throws IOException {
        File file = new File(path);

        Image photo = new Image();
        photo.setFilename(file.getName());
        try (InputStream stream = TestUtil.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Unable to get a stream for: " + path);
            }
            photo.setData(stream.readAllBytes());
        }
        return photo;
    }

}

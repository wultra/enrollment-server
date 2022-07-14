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

import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service implementing extraction and basic verification of uploaded documents.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class DataExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(DataExtractionService.class);

    /**
     * Extract request data and return a singled document.
     * @param requestData ZIP archive with a single zipped file.
     * @return Extracted document.
     * @throws DocumentVerificationException Thrown in case input data is invalid.
     */
    public Document extractDocument(byte[] requestData) throws DocumentVerificationException {
        if (requestData == null) {
            logger.warn("Missing request data");
            throw new DocumentVerificationException("Invalid data received");
        }
        final List<Document> extractedDocuments = decompress(requestData);
        if (extractedDocuments.size() != 1) {
            // Exactly 1 document is expected to be present in the archive
            logger.warn("Input data does not contain a single document");
            throw new DocumentVerificationException("Invalid data received");
        }
        logger.info("Extracted document {} from request data", extractedDocuments);
        return extractedDocuments.get(0);
    }

    /**
     * Extract request data and return list of extracted documents.
     * @param requestData ZIP archive with one or more documents.
     * @return Extracted documents.
     * @throws DocumentVerificationException Thrown in case input data is invalid.
     */
    public List<Document> extractDocuments(byte[] requestData) throws DocumentVerificationException {
        if (requestData == null) {
            logger.warn("Missing request data");
            throw new DocumentVerificationException("Invalid data received");
        }
        final List<Document> extractedDocuments = decompress(requestData);
        logger.info("Extracted documents {} from request data", extractedDocuments);
        return extractedDocuments;
    }

    /**
     * Decompress an archive with documents.
     * @param inputData Compressed input data.
     * @return Extracted documents.
     * @throws DocumentVerificationException Thrown in case input data is invalid.
     */
    private List<Document> decompress(byte[] inputData) throws DocumentVerificationException {
        final List<Document> extractedDocuments = new ArrayList<>();
        try {
            final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(inputData));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // Directories are skipped, data is extracted from regular files
                    continue;
                }
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final byte[] buffer = new byte[1024];
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                final Document document = new Document();
                document.setFilename(entry.getName());
                document.setData(baos.toByteArray());
                extractedDocuments.add(document);
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            logger.warn(ex.getMessage(), ex);
            throw new DocumentVerificationException("Invalid data received");
        }
        return extractedDocuments;
    }

}

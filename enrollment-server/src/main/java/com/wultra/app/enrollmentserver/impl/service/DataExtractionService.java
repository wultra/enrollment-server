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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.database.DocumentDataRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentData;
import com.wultra.app.enrollmentserver.errorhandling.InvalidDocumentException;
import com.wultra.app.enrollmentserver.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.*;

/**
 * Service implementing extraction and basic verification of uploaded documents.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class DataExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(DataExtractionService.class);

    private final DocumentDataRepository documentDataRepository;

    @Autowired
    public DataExtractionService(DocumentDataRepository documentDataRepository) {
        this.documentDataRepository = documentDataRepository;
    }

    /**
     * Extract request data and return a singled document.
     * @param requestData ZIP archive with a single zipped file.
     * @return Extracted document.
     * @throws InvalidDocumentException Thrown in case input data is invalid.
     */
    public Document extractDocument(byte[] requestData) throws InvalidDocumentException {
        if (requestData == null) {
            logger.warn("Missing request data");
            throw new InvalidDocumentException();
        }
        List<Document> extractedDocuments = decompress(requestData);
        if (extractedDocuments.size() != 1) {
            // Exactly 1 document is expected to be present in the archive
            logger.warn("Input data does not contain a single document");
            throw new InvalidDocumentException();
        }
        Document document = extractedDocuments.get(0);
        // Persist extracted document
        return persistDocument(document);
    }

    /**
     * Extract request data and return list of extracted documents.
     * @param requestData ZIP archive with one or more documents.
     * @return Extracted documents.
     * @throws InvalidDocumentException Thrown in case input data is invalid.
     */
    public List<Document> extractDocuments(byte[] requestData) throws InvalidDocumentException {
        if (requestData == null) {
            logger.warn("Missing request data");
            throw new InvalidDocumentException();
        }
        List<Document> extractedDocuments = decompress(requestData);
        List<Document> persistedDocuments = new ArrayList<>();
        extractedDocuments.forEach(document -> {
            Document persistedDocument = persistDocument(document);
            persistedDocuments.add(persistedDocument);
        });
        return persistedDocuments;
    }

    /**
     * Persist a document into database.
     * @param document Document to be extracted.
     * @return Persisted document metadata.
     */
    private Document persistDocument(Document document) {
        DocumentData documentData = new DocumentData();
        documentData.setFilename(documentData.getFilename());
        documentData.setData(document.getData());
        documentData.setTimestampCreated(new Date());
        documentData = documentDataRepository.save(documentData);
        document.setId(documentData.getId());
        // Return document metadata only
        Document persistedDocument = new Document();
        persistedDocument.setFilename(documentData.getFilename());
        persistedDocument.setId(documentData.getId());
        return persistedDocument;
    }

    /**
     * Decompress an archive with documents.
     * @param inputData Compressed input data.
     * @return Extracted documents.
     * @throws InvalidDocumentException Thrown in case input data is invalid.
     */
    private List<Document> decompress(byte[] inputData) throws InvalidDocumentException {
        List<Document> extractedDocuments = new ArrayList<>();
        try {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(inputData));
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                // Directories are expected not to be present in the archive
                if (entry.isDirectory()) {
                    logger.warn("Archive with documents contains a directory, this is not allowed.");
                    throw new InvalidDocumentException();
                }
                byte[] entryData = new byte[(int) entry.getSize()];
                int i = 0;
                while (i < entryData.length) {
                    i += zis.read(entryData, i, entryData.length - i);
                }
                Document document = new Document();
                document.setFilename(entry.getName());
                document.setData(entryData);
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            logger.warn(ex.getMessage(), ex);
            throw new InvalidDocumentException();
        }
        return extractedDocuments;
    }

}

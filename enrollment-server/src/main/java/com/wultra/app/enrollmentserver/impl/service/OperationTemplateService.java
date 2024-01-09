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

import com.wultra.app.enrollmentserver.database.OperationTemplateRepository;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for working with operation templates.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
@Slf4j
public class OperationTemplateService {

    private static final String DEFAULT_LANGUAGE = "en";

    private final OperationTemplateRepository operationTemplateRepository;

    @Autowired
    public OperationTemplateService(OperationTemplateRepository operationTemplateRepository) {
        this.operationTemplateRepository = operationTemplateRepository;
    }

    /**
     * Find the operation template for the given type and language.
     * <p>
     * Falling back to EN locale and later on to any found language.
     *
     * @param operationType Operation type.
     * @param language Template language.
     * @return Found operation template or empty.
     */
    public Optional<OperationTemplateEntity> findTemplate(@NotNull String operationType, @NotNull String language) {
        return operationTemplateRepository.findFirstByLanguageAndPlaceholder(language, operationType).or(() ->
                findTemplateFallback(operationType, language));
    }

    private Optional<OperationTemplateEntity> findTemplateFallback(final String operationType, final String language) {
        if (!DEFAULT_LANGUAGE.equals(language)) {
            logger.debug("Trying fallback to EN locale for operationType={}", operationType);
            return findDefaultTemplate(operationType);
        } else {
            return findAnyTemplate(operationType);
        }
    }

    private Optional<OperationTemplateEntity> findDefaultTemplate(final String operationType) {
        return operationTemplateRepository.findFirstByLanguageAndPlaceholder(DEFAULT_LANGUAGE, operationType).or(() ->
                findAnyTemplate(operationType));
    }

    private Optional<OperationTemplateEntity> findAnyTemplate(final String operationType) {
        logger.debug("Trying fallback to any locale for operationType={}", operationType);
        return operationTemplateRepository.findFirstByPlaceholder(operationType);
    }

}

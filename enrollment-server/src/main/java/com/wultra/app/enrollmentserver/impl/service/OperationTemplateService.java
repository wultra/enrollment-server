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
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for working with operation templates.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
public class OperationTemplateService {

    private final OperationTemplateRepository operationTemplateRepository;

    @Autowired
    public OperationTemplateService(OperationTemplateRepository operationTemplateRepository) {
        this.operationTemplateRepository = operationTemplateRepository;
    }

    public OperationTemplateEntity prepareTemplate(@NotNull String operationType, @NotNull String language) throws MobileTokenConfigurationException {
        Optional<OperationTemplateEntity> operationTemplateOptional = operationTemplateRepository.findFirstByLanguageAndPlaceholder(language, operationType);
        if (operationTemplateOptional.isEmpty()) { // try fallback to EN locale
            operationTemplateOptional = operationTemplateRepository.findFirstByLanguageAndPlaceholder("en", operationType);
            if (operationTemplateOptional.isEmpty()) {
                throw new MobileTokenConfigurationException("ERR_CONFIG", "Missing " + language + " template for operation " + operationType);
            }
        }
        return operationTemplateOptional.get();
    }

}

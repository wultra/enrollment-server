/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2024 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.controller.api.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.TemplateListResponse;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.app.enrollmentserver.impl.service.OperationTemplateService;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin controller.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.admin.enabled", havingValue = "true")
@RestController
@RequestMapping(value = "api/admin")
@AllArgsConstructor
@Slf4j
public class AdminController {

    private final OperationTemplateService operationTemplateService;

    private final ObjectMapper objectMapper;

    @GetMapping("/template")
    public ObjectResponse<TemplateListResponse> templates() {
        logger.debug("Returning template list.");
        final TemplateListResponse response = new TemplateListResponse();
        response.addAll(convert(operationTemplateService.findAll()));
        return new ObjectResponse<>(response);
    }

    private List<TemplateListResponse.TemplateDetail> convert(final List<OperationTemplateEntity> source) {
        return source.stream()
                .map(this::convert)
                .toList();
    }

    private TemplateListResponse.TemplateDetail convert(final OperationTemplateEntity source) {
        return TemplateListResponse.TemplateDetail.builder()
                .name(source.getPlaceholder())
                .title(source.getTitle())
                .message(source.getMessage())
                .language(source.getLanguage())
                .attributes(convert(source.getAttributes()))
                .resultTexts(convertResultTexts(source.getResultTexts()))
                .build();
    }

    private Map<String, String> convertResultTexts(final String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        try {
            return objectMapper.readValue(source, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Unable to convert resultTexts, returning an empty collection", e);
            return Map.of();
        }
    }

    private List<Object> convert(final String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        try {
            return objectMapper.readValue(source, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Unable to convert attributes, returning an empty collection", e);
            return List.of();
        }
    }
}

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
import com.wultra.app.enrollmentserver.api.model.enrollment.response.TemplateDetailResponse;
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

import java.util.ArrayList;
import java.util.List;

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
    public ObjectResponse<List<TemplateDetailResponse>> templates() {
        logger.debug("Returning template list.");
        return new ObjectResponse<>(new ArrayList<>(convert(operationTemplateService.findAll())));
    }

    private List<TemplateDetailResponse> convert(final List<OperationTemplateEntity> source) {
        return source.stream()
                .map(this::convert)
                .toList();
    }

    private TemplateDetailResponse convert(final OperationTemplateEntity source) {
        return TemplateDetailResponse.builder()
                .name(source.getPlaceholder())
                .title(source.getTitle())
                .message(source.getMessage())
                .language(source.getLanguage())
                .attributes(convert(source.getAttributes()))
                .build();
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

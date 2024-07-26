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
package com.wultra.app.enrollmentserver.api.model.enrollment.response;

import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Template list response.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@EqualsAndHashCode(callSuper = true)
public class TemplateListResponse extends ArrayList<TemplateListResponse.TemplateDetail> {

    @Serial
    private static final long serialVersionUID = -5446919236567435144L;

    @Builder
    public record TemplateDetail(String name, String title, String message, List<Object> attributes, String language, Map<String, String> resultTexts) {
    }
}

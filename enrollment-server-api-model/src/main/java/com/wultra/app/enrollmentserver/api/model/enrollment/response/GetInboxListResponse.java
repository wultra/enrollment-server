/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

import lombok.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;

/**
 * Model class for inbox message list response.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetInboxListResponse extends ArrayList<GetInboxListResponse.InboxMessage> {

    @Serial
    private static final long serialVersionUID = -8864636740564266079L;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InboxMessage {

        private String id;
        private String type;
        private String subject;
        @ToString.Exclude
        private String summary;
        private boolean read;
        private Date timestampCreated;

    }
}

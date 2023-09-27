/*
 * PowerAuth Mobile Token Model
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
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import com.wultra.security.powerauth.lib.mtoken.model.entity.attributes.Attribute;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialization of {@link PostApprovalScreen} for review.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public class ReviewPostApprovalScreen extends PostApprovalScreen {

    @NotNull
    private ReviewPayload payload;

    @Override
    public ReviewPayload getPayload() {
        return payload;
    }

    public void setPayload(ReviewPayload payload) {
        this.payload = payload;
    }

    /**
     * Specialization of {@link Payload} for review.
     */
    @Data
    public static class ReviewPayload implements Payload {

        private List<Attribute> attributes = new ArrayList<>();
    }
}

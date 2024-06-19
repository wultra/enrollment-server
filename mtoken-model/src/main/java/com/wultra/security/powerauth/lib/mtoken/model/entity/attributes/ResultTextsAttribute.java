/*
 * PowerAuth Mobile Token Model
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
package com.wultra.security.powerauth.lib.mtoken.model.entity.attributes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Customization texts to show for {@code success}, {@code failure}, or {@code reject} operation.
 * If not present (single property or the whole object), default messages are used.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ResultTextsAttribute extends Attribute {

    private String success;

    private String failure;

    private String reject;

    /**
     * No-arg constructor.
     */
    public ResultTextsAttribute() {
        super(Type.RESULT_TEXTS);
    }

    protected ResultTextsAttribute(final Builder builder) {
        this();
        this.id = builder.id;
        this.label = builder.label;
        this.success = builder.success;
        this.failure = builder.failure;
        this.reject = builder.reject;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String label;
        private String success;
        private String failure;
        private String reject;


        public Builder id(final String value) {
            this.id = value;
            return this;
        }

        public Builder label(final String value) {
            this.label = value;
            return this;
        }

        public Builder success(final String value) {
            this.success = value;
            return this;
        }

        public Builder failure(final String value) {
            this.failure = value;
            return this;
        }

        public Builder reject(final String value) {
            this.reject = value;
            return this;
        }

        public ResultTextsAttribute build() {
            return new ResultTextsAttribute(this);
        }
    }

}

/*
 * PowerAuth Mobile Token Model
 * Copyright (C) 2023 Wultra s.r.o.
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

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Alert attribute type, represents an alert of success (green), info (blue), warning (orange), and error (red).
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertAttribute extends Attribute {

    /**
     * Alert type enum.
     */
    public enum AlertType {

        /**
         * Success alert, typically displayed in green color with checkmark sign.
         */
        SUCCESS,

        /**
         * Info alert, typically displayed in blue color with info sign.
         */
        INFO,

        /**
         * Warning alert, typically displayed in orange color with checkmark sign.
         */
        WARNING,

        /**
         * Heading attribute type, represents a visual separator.
         */
        ERROR
    }

    /**
     * Alert type.
     */
    private AlertType alertType;

    /**
     * Optional alert title.
     */
    private String title;

    /**
     * Alert message.
     */
    private String message;

    /**
     * Default constructor.
     */
    public AlertAttribute() {
        super(Type.ALERT);
    }

    /**
     * Constructor with all details.
     * @param id Attribute ID.
     * @param alertType Type of alert.
     * @param label Attribute label.
     * @param title Alert title.
     * @param message Alert message.
     */
    public AlertAttribute(String id, AlertType alertType, String label, String title, String message) {
        this();
        this.id = id;
        this.alertType = alertType;
        this.label = label;
        this.title = title;
        this.message = message;
    }

    public static AlertAttribute.Builder builder() {
        return new AlertAttribute.Builder();
    }

    public static class Builder {
        private String id;
        private String label;
        private AlertType alertType;
        private String title;
        private String message;

        public AlertAttribute.Builder id(String value) {
            this.id = value;
            return this;
        }

        public AlertAttribute.Builder label(String value) {
            this.label = value;
            return this;
        }

        public AlertAttribute.Builder alertType(AlertType value) {
            this.alertType = value;
            return this;
        }

        public AlertAttribute.Builder title(String value) {
            this.title = value;
            return this;
        }

        public AlertAttribute.Builder message(String value) {
            this.message = value;
            return this;
        }

        public AlertAttribute build() {
            return new AlertAttribute(id, alertType, label, title, message);
        }
    }

}

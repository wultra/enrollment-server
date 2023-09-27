/*
 * PowerAuth Mobile Token Model
 * Copyright (C) 2017 Wultra s.r.o.
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

import java.math.BigDecimal;

/**
 * Attribute representing a financial amount item, with attributes for amount
 * and currency, that can be rendered on a mobile application.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AmountAttribute extends Attribute {

    private BigDecimal amount;
    private String currency;
    private String amountFormatted;
    private String currencyFormatted;

    /**
     * Combine both {@link #amountFormatted} and {@link #currencyFormatted} together. Order is locale specific.
     */
    private String valueFormatted;

    /**
     * No-arg constructor.
     */
    AmountAttribute() {
        super(Type.AMOUNT);
    }

    protected AmountAttribute(final Builder builder) {
        this();
        this.id = builder.id;
        this.label = builder.label;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.amountFormatted = builder.amountFormatted;
        this.currencyFormatted = builder.currencyFormatted;
        this.valueFormatted = builder.valueFormatted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String label;
        private BigDecimal amount;
        private String currency;
        private String amountFormatted;
        private String currencyFormatted;
        private String valueFormatted;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder label(String value) {
            this.label = value;
            return this;
        }

        public Builder amount(BigDecimal value) {
            this.amount = value;
            return this;
        }

        public Builder currency(String value) {
            this.currency = value;
            return this;
        }

        public Builder amountFormatted(String value) {
            this.amountFormatted = value;
            return this;
        }

        public Builder currencyFormatted(String value) {
            this.currencyFormatted = value;
            return this;
        }

        public Builder valueFormatted(String value) {
            this.valueFormatted = value;
            return this;
        }

        public AmountAttribute build() {
            return new AmountAttribute(this);
        }
    }
}

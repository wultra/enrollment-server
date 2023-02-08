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

import java.math.BigDecimal;

/**
 * Attribute representing a financial amount item, with attributes for amount
 * and currency, that can be rendered on a mobile application.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AmountConversionAttribute extends Attribute {

    /**
     * Hint, whether conversion could be recalculated by mobile application.
     */
    private boolean dynamic;

    private BigDecimal sourceAmount;
    private String sourceCurrency;
    private String sourceAmountFormatted;
    private String sourceCurrencyFormatted;

    private BigDecimal targetAmount;
    private String targetCurrency;
    private String targetAmountFormatted;
    private String targetCurrencyFormatted;

    /**
     * No-arg constructor.
     */
    AmountConversionAttribute() {
        super(Type.AMOUNT_CONVERSION);
    }

    protected AmountConversionAttribute(final Builder builder) {
        this();
        this.dynamic = builder.dynamic;
        this.id = builder.id;
        this.label = builder.label;
        this.sourceAmount = builder.sourceAmount;
        this.sourceCurrency = builder.sourceCurrency;
        this.sourceAmountFormatted = builder.sourceAmountFormatted;
        this.sourceCurrencyFormatted = builder.sourceCurrencyFormatted;
        this.targetAmount = builder.targetAmount;
        this.targetCurrency = builder.targetCurrency;
        this.targetAmountFormatted = builder.targetAmountFormatted;
        this.targetCurrencyFormatted = builder.targetCurrencyFormatted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean dynamic;
        private String id;
        private String label;
        private BigDecimal sourceAmount;
        private String sourceCurrency;
        private String sourceAmountFormatted;
        private String sourceCurrencyFormatted;
        private BigDecimal targetAmount;
        private String targetCurrency;
        private String targetAmountFormatted;
        private String targetCurrencyFormatted;

        public Builder dynamic(boolean value) {
            this.dynamic = value;
            return this;
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder label(String value) {
            this.label = value;
            return this;
        }

        public Builder sourceAmount(BigDecimal value) {
            this.sourceAmount = value;
            return this;
        }

        public Builder sourceCurrency(String value) {
            this.sourceCurrency = value;
            return this;
        }

        public Builder sourceAmountFormatted(String value) {
            this.sourceAmountFormatted = value;
            return this;
        }

        public Builder sourceCurrencyFormatted(String value) {
            this.sourceCurrencyFormatted = value;
            return this;
        }

        public Builder targetAmount(BigDecimal value) {
            this.targetAmount = value;
            return this;
        }

        public Builder targetCurrency(String value) {
            this.targetCurrency = value;
            return this;
        }

        public Builder targetAmountFormatted(String value) {
            this.targetAmountFormatted = value;
            return this;
        }

        public Builder targetCurrencyFormatted(String value) {
            this.targetCurrencyFormatted = value;
            return this;
        }

        public AmountConversionAttribute build() {
            return new AmountConversionAttribute(this);
        }
    }
}

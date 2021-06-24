/*
 * Copyright 2017 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Default constructor.
     */
    public AmountAttribute() {
        super();
        this.setType(Type.AMOUNT);
    }

    /**
     * Constructor with all details.
     * @param id Attribute ID.
     * @param label Attribute label.
     * @param amount Amount.
     * @param currency Currency.
     * @param amountFormatted Formatted amount.
     * @param currencyFormatted  Formatted currency.
     */
    public AmountAttribute(String id, String label, BigDecimal amount, String currency, String amountFormatted, String currencyFormatted) {
        this();
        this.id = id;
        this.label = label;
        this.amount = amount;
        this.currency = currency;
        this.amountFormatted = amountFormatted;
        this.currencyFormatted = currencyFormatted;
    }

}

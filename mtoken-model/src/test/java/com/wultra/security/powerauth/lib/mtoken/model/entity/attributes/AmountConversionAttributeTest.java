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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link AmountConversionAttribute}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class AmountConversionAttributeTest {

    @Test
    void testSerializeToJson() throws Exception {
        final String expectedResult = """
                {
                  "type" : "AMOUNT_CONVERSION",
                  "id" : "operation.amountConversion",
                  "label" : "Amount Conversion",
                  "dynamic" : true,
                  "sourceAmount" : 1.26,
                  "sourceCurrency" : "ETH",
                  "sourceAmountFormatted" : "1.26",
                  "sourceCurrencyFormatted" : "ETH",
                  "sourceValueFormatted" : "1.26 ETH",
                  "targetAmount" : 1710.98,
                  "targetCurrency" : "USD",
                  "targetAmountFormatted" : "1,710.98",
                  "targetCurrencyFormatted" : "$",
                  "targetValueFormatted" : "$1,710.98"
                }""";

        final Attribute tested = AmountConversionAttribute.builder()
                .id("operation.amountConversion")
                .label("Amount Conversion")
                .dynamic(true)
                .sourceAmount(new BigDecimal("1.26"))
                .sourceAmountFormatted("1.26")
                .sourceCurrency("ETH")
                .sourceCurrencyFormatted("ETH")
                .sourceValueFormatted("1.26 ETH")
                .targetAmount(new BigDecimal("1710.98"))
                .targetAmountFormatted("1,710.98")
                .targetCurrency("USD")
                .targetCurrencyFormatted("$")
                .targetValueFormatted("$1,710.98")
                .build();

        final String result = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(tested);

        assertEquals(expectedResult, result);
    }

    @Test
    void testDeserializeFromJson() throws Exception {
        final String input = """
                {
                  "type" : "AMOUNT_CONVERSION",
                  "id" : "operation.amountConversion",
                  "label" : "Amount Conversion",
                  "dynamic" : true,
                  "sourceAmount" : 1.26,
                  "sourceCurrency" : "ETH",
                  "sourceAmountFormatted" : "1.26",
                  "sourceCurrencyFormatted" : "ETH",
                  "sourceValueFormatted" : "1.26 ETH",
                  "targetAmount" : 1710.98,
                  "targetCurrency" : "USD",
                  "targetAmountFormatted" : "1,710.98",
                  "targetCurrencyFormatted" : "$",
                  "targetValueFormatted" : "$1,710.98"
                }""";

        final Attribute expectedResult = AmountConversionAttribute.builder()
                .id("operation.amountConversion")
                .label("Amount Conversion")
                .dynamic(true)
                .sourceAmount(new BigDecimal("1.26"))
                .sourceAmountFormatted("1.26")
                .sourceCurrency("ETH")
                .sourceCurrencyFormatted("ETH")
                .sourceValueFormatted("1.26 ETH")
                .targetAmount(new BigDecimal("1710.98"))
                .targetAmountFormatted("1,710.98")
                .targetCurrency("USD")
                .targetCurrencyFormatted("$")
                .targetValueFormatted("$1,710.98")
                .build();

        final Attribute result = new ObjectMapper().readValue(input, Attribute.class);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }
}

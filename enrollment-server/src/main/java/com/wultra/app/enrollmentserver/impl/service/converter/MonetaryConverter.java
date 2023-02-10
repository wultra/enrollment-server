/*
 * PowerAuth Enrollment Server
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
package com.wultra.app.enrollmentserver.impl.service.converter;

import lombok.extern.slf4j.Slf4j;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.UnknownCurrencyException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Convert currency and amount.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
class MonetaryConverter {

    /**
     * One bitcoin is divisible to eight decimal places (100 millionths of one bitcoin), and this smallest unit is referred to as a satoshi.
     */
    private static final int NON_FIAT_FRACTIONS_DIGITS = 8;

    private MonetaryConverter() {
        // hidden constructor
    }

    /**
     * Convert the given currency code and locale.
     * If there is no specific representation for the given currency and locale, original {@code code} is returned.
     *
     * @param code code to format
     * @param locale locale to be used for the conversion
     * @return localized currency or original code if there is no mapping available
     */
    static String formatCurrency(final String code, final Locale locale) {
        try {
            return Currency.getInstance(code).getSymbol(locale);
        } catch (final IllegalArgumentException e) {
            logger.debug("No currency mapping for code={}, locale={}", code, locale);
            logger.trace("No currency mapping for code={}, locale={}", code, locale, e);
            return code;
        }
    }

    /**
     * Convert the given amount according to the given code and locale.
     *
     * @param amount amount to format
     * @param code currency code
     * @param locale locale to be used for the conversion
     * @return formatted amount
     */
    static String formatAmount(final Number amount, final String code, final Locale locale) {
        final int fractionDigits = getFractionDigits(code);

        final NumberFormat format = NumberFormat.getInstance(locale);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        return format.format(amount);
    }

    private static int getFractionDigits(String code) {
        try {
            final CurrencyUnit currencyUnit = Monetary.getCurrency(code);
            return currencyUnit.getDefaultFractionDigits();
        } catch (UnknownCurrencyException e) {
            logger.debug("No currency mapping for code={}, most probably not FIAT", code);
            logger.trace("No currency mapping for code={}", code, e);
            return NON_FIAT_FRACTIONS_DIGITS;
        }
    }
}

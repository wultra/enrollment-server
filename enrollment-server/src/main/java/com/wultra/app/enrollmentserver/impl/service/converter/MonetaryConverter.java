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
import javax.money.MonetaryException;
import java.math.RoundingMode;
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

    private static final int DEFAULT_MINIMAL_FRACTION_DIGITS = 0;
    private static final int MAXIMAL_FRACTION_DIGITS = 18;
    private static final char NON_BREAKING_SPACE = '\u00a0';
    private static final String CURRENCY_PLACEHOLDER = "¤";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.DOWN;

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
            // TODO (racansky, 2023-02-16) we should rely on javax.money.CurrencyUnit instead of java.util.Currency, but there is no support for display name yet
            // https://github.com/JavaMoney/jsr354-api/issues/58
            return Currency.getInstance(code).getSymbol(locale);
        } catch (final IllegalArgumentException e) {
            logger.debug("No currency mapping for code={}, locale={}", code, locale);
            logger.trace("No currency mapping for code={}, locale={}", code, locale, e);
            return code;
        }
    }

    /**
     * Convert the given amount according to the given code and locale.
     * Amount is rounded down if limit of maximum fraction digits reached.
     *
     * @param amount amount to format
     * @param code currency code
     * @param locale locale to be used for the conversion
     * @return formatted amount
     */
    static String formatAmount(final Number amount, final String code, final Locale locale) {
        final NumberFormat numberFormat = NumberFormat.getInstance(locale);
        customizeNumberFormat(numberFormat, code);
        return numberFormat.format(amount);
    }

    /**
     * Convert the given amount and currency according to the given code and locale.
     *
     * @param amount amount to format
     * @param code currency code
     * @param locale locale to be used for the conversion
     * @return formatted amount with localized currency or original code if there is no mapping available
     */
    static String formatValue(final Number amount, final String code, final Locale locale) {
        final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
        customizeNumberFormat(numberFormat, code);

        try {
            final String currencySymbol = Currency.getInstance(code).getSymbol(locale);
            return numberFormat.format(amount).replace(CURRENCY_PLACEHOLDER, currencySymbol);
        } catch (IllegalArgumentException e) {
            logger.debug("No currency mapping for code={}, most probably not FIAT", code);
            logger.trace("No currency mapping for code={}", code, e);
            return formatAmount(amount, code, locale) + NON_BREAKING_SPACE + code;
        }
    }

    private static void customizeNumberFormat(final NumberFormat numberFormat, final String code) {
        final int fractionDigits = getFractionDigits(code);
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(MAXIMAL_FRACTION_DIGITS);
        numberFormat.setRoundingMode(ROUNDING_MODE);
    }

    private static int getFractionDigits(String code) {
        try {
            final CurrencyUnit currencyUnit = Monetary.getCurrency(code);
            return currencyUnit.getDefaultFractionDigits();
        } catch (MonetaryException e) {
            logger.debug("No currency mapping for code={}, most probably not FIAT", code);
            logger.trace("No currency mapping for code={}", code, e);
            return DEFAULT_MINIMAL_FRACTION_DIGITS;
        }
    }
}

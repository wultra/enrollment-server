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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link MonetaryConverter}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class MonetaryConverterTest {

    @ParameterizedTest
    @CsvSource({
            "CZK, en, CZK",
            "CZK, cs, Kč",
            "USD, en, $",
            "USD, cs, US$",
            "UYU, cs, UYU",
            "UYU, en, UYU",
            "UYU, uy, UYU",
            "CAD, cs, CA$",
            "CAD, ca, CAD",
            "CAD, en, CA$",
            "NZD, en, NZ$",
            "NZD, cs, NZ$",
            "NZD, nz, NZ$",
            "BTC, cs, BTC",
            "..., cs, ..."
    })
    void testFormatCurrency(final String source, final String locale, final String expected) {
        final String result = MonetaryConverter.formatCurrency(source, new Locale(locale));
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
            "1710.9817,               CZK, en, '1,710.9817'",
            "1710.9817,               CZK, cs, '1 710,9817'",
            "1710,                    CZK, cs, '1 710,00'",
            "1710,                    USD, en, '1,710.00'",
            "1710.1,                  CZK, cs, '1 710,10'",
            "1710.1,                  USD, en, '1,710.10'",
            "1710,                    JPY, jp, '1,710'",
            "1710,                    JPY, en, '1,710'",
            "1710,                    JPY, cs, '1 710'",
            "1,                       BTC, en, '1'",
            "1.1,                     BTC, en, '1.1'",
            "0.123456789,             BTC, en, '0.123456789'",
            "0.567567567567567567567, BTC, en, '0.567567567567567567'",
            "1,                       ..., en, '1'"
    })
    void testFormatAmount(final String amount, final String code, final String locale, final String expected) {
        final String result = MonetaryConverter.formatAmount(new BigDecimal(amount), code, new Locale(locale));
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
            "1710.9867,               CZK, en, 'CZK1,710.9867'",
            "1710.9867,               CZK, cs, '1 710,9867 Kč'",
            "1710,                    CZK, cs, '1 710,00 Kč'",
            "1710,                    USD, en, '$1,710.00'",
            "1710,                    USD, cs, '1 710,00 US$'",
            "1710.1,                  CZK, cs, '1 710,10 Kč'",
            "1710.1,                  USD, en, '$1,710.10'",
            "1710.1,                  EUR, cs, '1 710,10 €'",
            "1710.1,                  EUR, en, '€1,710.10'",
            "1710,                    JPY, jp, 'JP¥ 1,710'",
            "1710,                    JPY, en, '¥1,710'",
            "1710,                    JPY, cs, '1 710 JP¥'",
            "1,                       BTC, en, '1 BTC'",
            "1.1,                     BTC, en, '1.1 BTC'",
            "0.123456789,             BTC, en, '0.123456789 BTC'",
            "0.567567567567567567567, BTC, en, '0.567567567567567567 BTC'",
            "1,                       ..., en, '1 ...'"
    })
    void testFormatValue(final String amount, final String code, final String locale, final String expected) {
        final String result = MonetaryConverter.formatValue(new BigDecimal(amount), code, new Locale(locale));
        assertEquals(expected, result);
    }
}

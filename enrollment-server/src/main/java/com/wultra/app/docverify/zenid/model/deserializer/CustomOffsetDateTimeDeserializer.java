/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
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
package com.wultra.app.docverify.zenid.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Custom {@link java.time.OffsetDateTime} deserializer which allows also iso date format
 *
 * <p>
 *     The ZenID returns simple ISO data on some date elements which are expected to be date-time (e.g. BirthDate)
 * </p>
 */
public class CustomOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        if (parser.getText() != null && parser.getText().length() == 10) { // yyyy-MM-dd
            return LocalDate.parse(parser.getText(), DateTimeFormatter.ISO_DATE)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toOffsetDateTime();
        }

        return InstantDeserializer.OFFSET_DATE_TIME.deserialize(parser, context);
    }

}

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Base class for generic attribute of mobile token form data.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AmountAttribute.class, name = "AMOUNT"),
        @JsonSubTypes.Type(value = HeadingAttribute.class, name = "HEADING"),
        @JsonSubTypes.Type(value = KeyValueAttribute.class, name = "KEY_VALUE"),
        @JsonSubTypes.Type(value = NoteAttribute.class, name = "NOTE")
})
public class Attribute {

    /**
     * Attribute type.
     */
    public enum Type {

        /**
         * Amount attribute type - represents amount and currency.
         */
        AMOUNT,

        /**
         * Key-value attribute type - represents generic single-line key value.
         */
        KEY_VALUE,

        /**
         * Note, a multi-line key-value attribute.
         */
        NOTE,

        /**
         * Heading attribute type, represents a visual separator.
         */
        HEADING,

        /**
         * Information about third-party subject.
         */
        PARTY_INFO
    }

    /**
     * Type of the attribute.
     */
    // JsonIgnore added, otherwise type was serialized twice
    @JsonIgnore
    protected Type type;

    /**
     * ID of the attribute.
     */
    protected String id;

    /**
     * Label of the attribute. Used as a base string in other attribute types (i.e., as key in KEY_VALUE attribute type).
     */
    protected String label;

}

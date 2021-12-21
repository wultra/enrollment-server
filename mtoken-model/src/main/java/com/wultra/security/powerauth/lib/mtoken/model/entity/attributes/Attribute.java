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

/**
 * Base class for generic attribute of mobile token form data.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
public class Attribute {

    /**
     * Attribute type.
     */
    public enum Type {
        AMOUNT,
        KEY_VALUE,
        NOTE,
        HEADING,
        PARTY_INFO
    }

    protected Type type;
    protected String id;
    protected String label;

}

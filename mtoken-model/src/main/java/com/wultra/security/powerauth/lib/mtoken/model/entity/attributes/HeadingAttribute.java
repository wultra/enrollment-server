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

/**
 * Attribute representing a heading.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HeadingAttribute extends Attribute {

    private int level;

    /**
     * Default constructor.
     */
    public HeadingAttribute() {
        super(Type.HEADING);
    }

    /**
     * Constructor with all details.
     * @param id Attribute ID.
     * @param label Heading text.
     */
    public HeadingAttribute(String id, String label, int level) {
        this();
        this.id = id;
        this.label = label;
        this.level = level;
    }

}

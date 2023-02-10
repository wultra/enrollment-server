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
package com.wultra.security.powerauth.lib.mtoken.model.response;

import io.getlime.core.rest.model.base.response.ObjectResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Base response object extended with information required for mobile token.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MobileTokenResponse<T> extends ObjectResponse<T> {

    private Date currentTimestamp;

    /**
     * Constructor with response object and current timestamp.
     *
     * @param responseObject Response object.
     * @param timestamp The timestamp to be set as current timestamp.
     */
    public MobileTokenResponse(T responseObject, Date timestamp) {
        super(responseObject);
        this.currentTimestamp = timestamp;
    }

}

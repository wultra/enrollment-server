/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.response.ActivationCodeResponse;
import com.wultra.security.powerauth.client.v3.InitActivationResponse;
import org.springframework.stereotype.Component;

/**
 * Converter between objects representing activation codes.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Component
public class ActivationCodeConverter {

    /**
     * Convert the PowerAuth Server InitActivationResponse into ActivationCodeResponse.
     *
     * @param iar Original response.
     * @return Converted response.
     */
    public ActivationCodeResponse convert(InitActivationResponse iar) {
        if (iar == null) {
            return null;
        }
        final ActivationCodeResponse response = new ActivationCodeResponse();
        response.setActivationId(iar.getActivationId());
        response.setActivationCode(iar.getActivationCode());
        response.setActivationSignature(iar.getActivationSignature());
        return response;
    }

}

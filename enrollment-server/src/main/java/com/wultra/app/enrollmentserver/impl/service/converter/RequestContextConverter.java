/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.impl.service.model.RequestContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Converter for HTTP request context information.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Component
public class RequestContextConverter {

    /**
     * List of HTTP headers that may contain the actual IP address
     * when hidden behind a proxy component.
     */
    private static final String[] HTTP_HEADERS_IP_ADDRESS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * Convert HTTP Servlet Request to request context representation.
     *
     * @param source HttpServletRequest instance.
     * @return Request context data.
     */
    public RequestContext convert(HttpServletRequest source) {
        if (source == null) {
            return null;
        }
        final RequestContext destination = new RequestContext();
        destination.setUserAgent(source.getHeader("User-Agent"));
        destination.setIpAddress(getClientIpAddress(source));
        return destination;
    }

    /**
     * Obtain the best-effort guess of the client IP address.
     * @param request HttpServletRequest instance.
     * @return Best-effort information about the client IP address.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) { // safety null check
            return null;
        }
        for (String header: HTTP_HEADERS_IP_ADDRESS) {
            final String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

}

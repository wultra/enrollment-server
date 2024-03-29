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
package com.wultra.app.onboardingserver.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP header interceptor for logging of correlation headers using MDC.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class HttpHeaderInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpHeaderInterceptor.class);

    private final String correlationHeaderName;
    private final String correlationHeaderValueValidation;

    /**
     * HTTP header interceptor constructor.
     * @param correlationHeaderName Correlation header name.
     * @param correlationHeaderValueValidation Correlation header value validation.
     */
    public HttpHeaderInterceptor(String correlationHeaderName, String correlationHeaderValueValidation) {
        this.correlationHeaderName = correlationHeaderName;
        this.correlationHeaderValueValidation = correlationHeaderValueValidation;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        MDC.put(correlationHeaderName, getCorrelationId(request));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        MDC.remove(correlationHeaderName);
    }

    private String getCorrelationId(HttpServletRequest request) {
        final String headerValue = request.getHeader(correlationHeaderName);
        if (headerValue == null) {
            logger.debug("Correlation header {} is null", correlationHeaderName);
            return null;
        }
        if (!headerValue.matches(correlationHeaderValueValidation)) {
            logger.warn("Correlation header {} is invalid: {}", correlationHeaderName, headerValue);
            return null;
        }
        return headerValue;
    }
}
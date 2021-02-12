/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Configuration of the Unirest class.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Configuration
public class UnirestConfiguration {

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @Value("${powerauth.push.service.client.concurrency.total}")
    private int concurrencyTotal;

    @Value("${powerauth.push.service.client.concurrency.perRoute}")
    private int concurrencyPerRoute;

    @Value("${powerauth.push.service.client.socketTimeout}")
    private int socketTimeout;

    @Value("${powerauth.push.service.client.connectTimeout}")
    private int connectTimeout;

    @PostConstruct
    public void postConstruct() {
        Unirest.config()
                .socketTimeout(socketTimeout)
                .connectTimeout(connectTimeout)
                .concurrency(concurrencyTotal, concurrencyPerRoute)
                .setObjectMapper(new ObjectMapper() {

            public String writeValue(Object value) {
                try {
                    return mapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return mapper.readValue(value, valueType);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}

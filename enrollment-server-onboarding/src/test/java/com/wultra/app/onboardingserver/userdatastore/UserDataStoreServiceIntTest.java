/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.userdatastore;

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.request.EmbeddedPhotoCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Integration test for {@link UserDataStoreService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class UserDataStoreServiceIntTest {

    @Autowired
    private UserDataStoreService tested;

    @Test
    void test() throws Exception {
        final var request = DocumentCreateRequest.builder()
                .userId("admin")
                .documentType("personal_id")
                .dataType("claims")
                .externalId("test-process-1")
                .documentData("{}")
                .attributes(Map.of("trustedImage", true))
                .photos(List.of(
                        EmbeddedPhotoCreateRequest.builder()
                                .photoType("person")
                                .photoData("ZmFjZVBob3Rv")
                                .externalId("1")
                                .build(),
                        EmbeddedPhotoCreateRequest.builder()
                                .photoType("document_front_side")
                                .photoData("aWRDYXJkRnJvbnQ=")
                                .externalId("2")
                                .build(),
                        EmbeddedPhotoCreateRequest.builder()
                                .photoType("document_back_side")
                                .photoData("aWRDYXJkQmFjaw==")
                                .externalId("3")
                                .build()
                ))
                .attachments(Collections.emptyList())
                .build();

        tested.callCreateDocument(request);
    }
}

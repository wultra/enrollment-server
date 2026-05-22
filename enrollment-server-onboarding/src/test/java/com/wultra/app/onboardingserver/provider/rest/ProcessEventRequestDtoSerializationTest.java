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
package com.wultra.app.onboardingserver.provider.rest;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests verifying that {@link ProcessEventRequestDto} serializes
 * to the JSON shape documented in {@code docs/onboarding/Events.md}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class ProcessEventRequestDtoSerializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void testSerializeProcessFinished() throws Exception {
        final var dto = new ProcessEventRequestDto();
        dto.setProcessId("8b2dfae4-d955-4d8f-a95b-2d9c5a4b0e26");
        dto.setProcessType("onboarding");
        dto.setIdentityVerificationId("d3827099-3b6c-4df9-887d-4eac402fc4f9");
        dto.setUserId("40405309-6406-4d6b-b4ef-642e52ac44f4");
        dto.setExternalUserId("629199e8-aa0d-4fc0-911c-089d53e0f608");
        dto.setType(EventTypeDto.PROCESS_FINISHED);
        dto.setEventData(ProcessFinishedEventDataDto.builder()
                .process(ProcessFinishedEventDataDto.Process.builder()
                        .status("FINISHED")
                        .errorDetail(null)
                        .deviceData(ProcessFinishedEventDataDto.DeviceData.builder()
                                .locale("EN")
                                .ipAddress(null)
                                .httpUserAgent(null)
                                .fdsData(Map.of("fdsIdentifier", "42"))
                                .build())
                        .build())
                .build());

        final String expectedJson = """
                {
                  "processId": "8b2dfae4-d955-4d8f-a95b-2d9c5a4b0e26",
                  "processType": "onboarding",
                  "identityVerificationId": "d3827099-3b6c-4df9-887d-4eac402fc4f9",
                  "userId": "40405309-6406-4d6b-b4ef-642e52ac44f4",
                  "externalUserId": "629199e8-aa0d-4fc0-911c-089d53e0f608",
                  "type": "PROCESS_FINISHED",
                  "eventData": {
                    "process": {
                      "status": "FINISHED",
                      "errorDetail": null,
                      "deviceData": {
                        "locale": "EN",
                        "ipAddress": null,
                        "httpUserAgent": null,
                        "fdsData": {"fdsIdentifier": "42"}
                      }
                    }
                  }
                }
                """;

        assertJsonEquals(expectedJson, objectMapper.writeValueAsString(dto));
    }

    @Test
    void testSerializeDocumentVerificationFinished() throws Exception {
        final var dto = new ProcessEventRequestDto();
        dto.setProcessId("p1");
        dto.setProcessType("onboarding");
        dto.setIdentityVerificationId("iv1");
        dto.setUserId("u1");
        dto.setExternalUserId("eu1");
        dto.setType(EventTypeDto.DOCUMENT_VERIFICATION_FINISHED);
        dto.setEventData(DocumentVerificationFinishedEventDataDto.builder()
                .documentVerification(DocumentVerificationFinishedEventDataDto.DocumentVerification.builder()
                        .documentVerificationId("dv-1")
                        .documentId("d-1")
                        .status("ACCEPTED")
                        .rejectReason(null)
                        .errorDetail(null)
                        .provider("Microblink")
                        .score(9)
                        .documentVerificationResult(DocumentVerificationFinishedEventDataDto.DocumentVerificationResult.builder()
                                .type("ID_CARD")
                                .country("CZ")
                                .data(DocumentVerificationFinishedEventDataDto.DocumentData.builder()
                                        .surname("Doe")
                                        .givenNames("John")
                                        .dateOfBirth("1980-01-01")
                                        .documentNumber("AB123456")
                                        .build())
                                .images(List.of(DocumentVerificationFinishedEventDataDto.Image.builder()
                                        .type("FACE")
                                        .data("base64data")
                                        .build()))
                                .rawData(Map.of("foo", "bar"))
                                .build())
                        .build())
                .build());

        final String json = objectMapper.writeValueAsString(dto);

        // verify event data shape only (other generic fields covered by previous test)
        final var node = objectMapper.readTree(json).path("eventData").path("documentVerification");
        assertEquals("dv-1", node.path("documentVerificationId").asText());
        assertEquals("d-1", node.path("documentId").asText());
        assertEquals("ACCEPTED", node.path("status").asText());
        assertEquals("Microblink", node.path("provider").asText());
        assertEquals(9, node.path("score").asInt());
        assertEquals("ID_CARD", node.path("documentVerificationResult").path("type").asText());
        assertEquals("CZ", node.path("documentVerificationResult").path("country").asText());
        assertEquals("Doe", node.path("documentVerificationResult").path("data").path("surname").asText());
        assertEquals("FACE", node.path("documentVerificationResult").path("images").path(0).path("type").asText());
        assertEquals("bar", node.path("documentVerificationResult").path("rawData").path("foo").asText());
    }

    @Test
    void testSerializeFinalDocumentVerificationFinished() throws Exception {
        final var dto = new ProcessEventRequestDto();
        dto.setType(EventTypeDto.FINAL_DOCUMENT_VERIFICATION_FINISHED);
        dto.setEventData(FinalDocumentVerificationFinishedEventDataDto.builder()
                .finalDocumentVerification(FinalDocumentVerificationFinishedEventDataDto.FinalDocumentVerification.builder()
                        .documentVerificationId("dv-final-1")
                        .status("REJECTED")
                        .rejectReason("crosscheck failed")
                        .errorDetail(null)
                        .provider("Microblink")
                        .documentIds(List.of("d-1", "d-2"))
                        .build())
                .build());

        final var node = objectMapper.readTree(objectMapper.writeValueAsString(dto))
                .path("eventData").path("finalDocumentVerification");
        assertEquals("dv-final-1", node.path("documentVerificationId").asText());
        assertEquals("REJECTED", node.path("status").asText());
        assertEquals("crosscheck failed", node.path("rejectReason").asText());
        assertEquals("Microblink", node.path("provider").asText());
        assertEquals(2, node.path("documentIds").size());
        assertEquals("d-1", node.path("documentIds").path(0).asText());
        assertEquals("d-2", node.path("documentIds").path(1).asText());
    }

    @Test
    void testSerializePresenceCheckFinished() throws Exception {
        final var dto = new ProcessEventRequestDto();
        dto.setType(EventTypeDto.PRESENCE_CHECK_FINISHED);
        dto.setEventData(PresenceCheckFinishedEventDataDto.builder()
                .presenceCheck(PresenceCheckFinishedEventDataDto.PresenceCheck.builder()
                        .status("ACCEPTED")
                        .rejectReason(null)
                        .errorDetail(null)
                        .provider("iProov")
                        .score(8)
                        .presenceCheckResult(PresenceCheckFinishedEventDataDto.PresenceCheckResult.builder()
                                .frame("base64frame")
                                .build())
                        .build())
                .build());

        final var node = objectMapper.readTree(objectMapper.writeValueAsString(dto))
                .path("eventData").path("presenceCheck");
        assertEquals("ACCEPTED", node.path("status").asText());
        assertEquals("iProov", node.path("provider").asText());
        assertEquals(8, node.path("score").asInt());
        assertEquals("base64frame", node.path("presenceCheckResult").path("frame").asText());
    }

    private void assertJsonEquals(final String expected, final String actual) throws Exception {
        assertEquals(objectMapper.readTree(expected), objectMapper.readTree(actual));
    }
}


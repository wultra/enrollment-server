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
package com.wultra.app.onboardingserver.controller.api;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.AcknowledgeApproveClientRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.AcknowledgeApproveClientResponse;
import com.wultra.app.onboardingserver.impl.AcknowledgeService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to acknowledge asynchronous actions. These endpoints are private and secured.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@RestController
@RequestMapping(value = "/api/private/client")
@AllArgsConstructor
@Slf4j
class PrivateClientController {

    private final AcknowledgeService acknowledgeService;

    /**
     * Acknowledge client approval in the case of asynchronous mode.
     *
     * @param request Request body.
     */
    @PostMapping("approve")
    public AcknowledgeApproveClientResponse acknowledgeApproveClient(final @Valid @RequestBody AcknowledgeApproveClientRequest request) {
        logger.info("action: acknowledgeApproveClient, state: initiated, processId: {}", request.processId());

        if (request.approvalResult() == AcknowledgeApproveClientRequest.ApprovalResult.WAIT) {
            logger.info("action: acknowledgeApproveClient, state: skipped, reason: approvalResult is WAIT");
            return AcknowledgeApproveClientResponse.builder()
                    .result(AcknowledgeApproveClientResponse.Result.OK)
                    .build();
        }

        final AcknowledgeApproveClientResponse response = acknowledgeService.acknowledgeApproveClient(request);

        if (response.result() == AcknowledgeApproveClientResponse.Result.OK) {
            logger.info("action: acknowledgeApproveClient, state: succeeded");
        } else {
            logger.info("action: acknowledgeApproveClient, state: failed, reason: {}", response.resultReason());
        }

        return response;
    }
}

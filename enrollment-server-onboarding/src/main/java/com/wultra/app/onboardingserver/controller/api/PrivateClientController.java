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
import com.wultra.app.enrollmentserver.api.model.onboarding.request.AcknowledgeEvaluationClientRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.AcknowledgeApproveClientResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.AcknowledgeEvaluationClientResponse;
import com.wultra.app.onboardingserver.impl.service.AcknowledgeService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.*;

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
        logger.info("", action("acknowledgeApproveClient"), stateInitiated(), kv("processId", request.processId()));

        if (request.approvalResult() == AcknowledgeApproveClientRequest.ApprovalResult.WAIT) {
            logger.info("", action("acknowledgeApproveClient"), state("skipped"), kv("reason", "approvalResult is WAIT"));
            return AcknowledgeApproveClientResponse.builder()
                    .result(AcknowledgeApproveClientResponse.Result.OK)
                    .build();
        }

        final AcknowledgeApproveClientResponse response = acknowledgeService.acknowledgeApproveClient(request);

        if (response.result() == AcknowledgeApproveClientResponse.Result.OK) {
            logger.info("", action("acknowledgeApproveClient"), stateSucceeded());
        } else {
            logger.info("", action("acknowledgeApproveClient"), stateFailed(), kv("reason", response.resultReason()));
        }

        return response;
    }

    @PostMapping("evaluate")
    public AcknowledgeEvaluationClientResponse acknowledgeEvaluationClient(final @Valid @RequestBody AcknowledgeEvaluationClientRequest request) {
        logger.info("", action("acknowledgeEvaluationClient"), stateInitiated(), kv("processId", request.processId()));

        if (request.evaluationResult() == AcknowledgeEvaluationClientRequest.EvaluationResult.WAIT) {
            logger.info("", action("acknowledgeEvaluationClient"), state("skipped"), kv("reason", "evaluationResult is WAIT"));
            return AcknowledgeEvaluationClientResponse.builder()
                    .result(AcknowledgeEvaluationClientResponse.Result.OK)
                    .build();
        }

        final AcknowledgeEvaluationClientResponse response = acknowledgeService.acknowledgeEvaluationClient(request);

        if (response.result() == AcknowledgeEvaluationClientResponse.Result.OK) {
            logger.info("", action("acknowledgeEvaluationClient"), stateSucceeded());
        } else {
            logger.info("", action("acknowledgeEvaluationClient"), stateFailed(), kv("reason", response.resultReason()));
        }

        return response;
    }
}

/*
 * PowerAuth Enrollment Server
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
package com.wultra.app.enrollmentserver.impl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wultra.app.enrollmentserver.database.OperationTemplateRepository;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.app.enrollmentserver.impl.service.converter.MobileTokenConverter;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.request.OperationListForUserRequest;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.response.OperationListResponse;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Test for {@link MobileTokenService}.
 *
 * @author Jan Dusil, jan.dusil@wultra.com
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MobileTokenServiceTest {

    @Mock
    private PowerAuthClient powerAuthClient;

    @Mock
    private MobileTokenConverter mobileTokenConverter;

    @Mock
    private OperationTemplateService operationTemplateService;

    @Mock
    private HttpCustomizationService httpCustomizationService;

    @InjectMocks
    private MobileTokenService tested;

    @Mock
    private OperationTemplateRepository templateRepository;

    private static final int OPERATION_LIST_LIMIT = 100;

    @Test
    void testOperationListForUser() throws Exception {
        final String userId = "test-user";
        final String applicationId = "21";
        final String language = "CZ";
        final String activationId = "test-activation";
        final String operationType = "login";

        final OperationListForUserRequest request = new OperationListForUserRequest();
        request.setUserId(userId);
        request.setApplications(List.of(applicationId));
        request.setPageNumber(0);
        request.setPageSize(OPERATION_LIST_LIMIT);
        request.setActivationId(activationId);

        final OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setUserId(userId);
        operationDetailResponse.setApplications(List.of(applicationId));
        operationDetailResponse.setOperationType(operationType);
        operationDetailResponse.setParameters(new HashMap<>());

        final com.wultra.security.powerauth.client.model.response.OperationListResponse response
                = new com.wultra.security.powerauth.client.model.response.OperationListResponse();
        response.add(operationDetailResponse);

        when(powerAuthClient.operationList(request, null, null)).thenReturn(response);

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setLanguage(language);
        operationTemplate.setPlaceholder(operationType);
        operationTemplate.setId(1L);
        when(operationTemplateService.findTemplate(operationType, language)).thenReturn(Optional.of(operationTemplate));

        final Operation operation = new Operation();
        operation.setName(operationType);
        when(mobileTokenConverter.convert(operationDetailResponse, operationTemplate)).thenReturn(operation);

        final OperationListResponse operationListResponse = tested.operationListForUser(userId, applicationId, language, activationId, false);

        assertNotNull(operationListResponse);
        assertEquals(1, operationListResponse.size());
        assertNotNull(operationListResponse.get(0));
        assertEquals(operationType, operationListResponse.get(0).getName());
    }

    @Test
    void testPendingOperationListForUser() throws Exception {
        final String userId = "test-user";
        final String applicationId = "21";
        final String language = "CZ";
        final String activationId = "test-activation";
        final String operationType = "login";

        final OperationListForUserRequest request = new OperationListForUserRequest();
        request.setUserId(userId);
        request.setApplications(List.of(applicationId));
        request.setPageNumber(0);
        request.setPageSize(OPERATION_LIST_LIMIT);
        request.setActivationId(activationId);

        final OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setUserId(userId);
        operationDetailResponse.setApplications(List.of(applicationId));
        operationDetailResponse.setOperationType(operationType);
        operationDetailResponse.setParameters(new HashMap<>());

        final com.wultra.security.powerauth.client.model.response.OperationListResponse response
                = new com.wultra.security.powerauth.client.model.response.OperationListResponse();
        response.add(operationDetailResponse);

        when(powerAuthClient.operationPendingList(request, null, null)).thenReturn(response);

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setLanguage(language);
        operationTemplate.setPlaceholder(operationType);
        operationTemplate.setId(1L);
        when(operationTemplateService.findTemplate(operationType, language)).thenReturn(Optional.of(operationTemplate));

        final Operation operation = new Operation();
        operation.setName(operationType);
        when(mobileTokenConverter.convert(operationDetailResponse, operationTemplate)).thenReturn(operation);

        final OperationListResponse operationListResponse = tested.operationListForUser(userId, applicationId, language, activationId, true);

        assertNotNull(operationListResponse);
        assertEquals(1, operationListResponse.size());
        assertNotNull(operationListResponse.get(0));
        assertEquals(operationType, operationListResponse.get(0).getName());
    }

    @Test
    void testOperationListForUserEmptyOperationTemplate() throws Exception {
        final String userId = "test-user";
        final String applicationId = "21";
        final String language = "CZ";
        final String activationId = "test-activation";
        final String operationType = "login";

        final OperationListForUserRequest request = new OperationListForUserRequest();
        request.setUserId(userId);
        request.setApplications(List.of(applicationId));
        request.setPageNumber(0);
        request.setPageSize(OPERATION_LIST_LIMIT);
        request.setActivationId(activationId);

        final OperationDetailResponse operationDetailResponse = new OperationDetailResponse();
        operationDetailResponse.setUserId(userId);
        operationDetailResponse.setApplications(List.of(applicationId));
        operationDetailResponse.setOperationType(operationType);
        operationDetailResponse.setParameters(new HashMap<>());

        final com.wultra.security.powerauth.client.model.response.OperationListResponse response
                = new com.wultra.security.powerauth.client.model.response.OperationListResponse();
        response.add(operationDetailResponse);

        when(powerAuthClient.operationList(request, null, null)).thenReturn(response);

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setLanguage(language);
        operationTemplate.setPlaceholder(operationType);
        operationTemplate.setId(1L);

        when(operationTemplateService.findTemplate(operationType, language)).thenReturn(Optional.empty());


        final OperationListResponse operationListResponse = tested.operationListForUser(userId, applicationId, language, activationId, false);

        assertNotNull(operationListResponse);
        assertEquals(0, operationListResponse.size());
    }

}

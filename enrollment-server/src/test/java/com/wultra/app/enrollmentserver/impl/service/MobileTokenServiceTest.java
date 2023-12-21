package com.wultra.app.enrollmentserver.impl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.wultra.app.enrollmentserver.database.OperationTemplateRepository;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import com.wultra.app.enrollmentserver.impl.service.converter.MobileTokenConverter;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void testOperationListForUser() throws PowerAuthClientException, MobileTokenConfigurationException {
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

        when(httpCustomizationService.getHttpHeaders()).thenReturn(null);
        when(httpCustomizationService.getHttpHeaders()).thenReturn(null);
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

}

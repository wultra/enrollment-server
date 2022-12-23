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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.security.powerauth.client.model.enumeration.OperationStatus;
import com.wultra.security.powerauth.client.model.enumeration.SignatureType;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.lib.mtoken.model.entity.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link MobileTokenConverter}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class MobileTokenConverterTest {

    private final MobileTokenConverter tested = new MobileTokenConverter(new ObjectMapper());

    @Test
    void testConvertUiNull() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertNull(result.getUi());
    }

    @Test
    void testConvertUiOverriddenByEnrollment() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setRiskFlags("C");

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("{\n" +
                "  \"flipButtons\": true,\n" +
                "  \"blockApprovalOnCall\": false,\n" +
                "  \"preApprovalScreen\": {\n" +
                "    \"type\": \"WARNING\",\n" +
                "    \"heading\": \"Watch out!\",\n" +
                "    \"message\": \"You may become a victim of an attack.\",\n" +
                "    \"items\": [\n" +
                "      \"You activate a new app and allow access to your accounts\",\n" +
                "      \"Make sure the activation takes place on your device\",\n" +
                "      \"If you have been prompted for this operation in connection with a payment, decline it\"\n" +
                "    ],\n" +
                "    \"approvalType\": \"SLIDER\"\n" +
                "  }\n" +
                "}");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertNotNull(result.getUi());

        final UiExtensions ui = result.getUi();
        assertEquals(true, ui.getFlipButtons());
        assertEquals(false, ui.getBlockApprovalOnCall());
        assertNotNull(ui.getPreApprovalScreen());

        final PreApprovalScreen preApprovalScreen = ui.getPreApprovalScreen();
        assertEquals(PreApprovalScreen.ScreenType.WARNING, preApprovalScreen.getType());
        assertEquals("Watch out!", preApprovalScreen.getHeading());
        assertEquals("You may become a victim of an attack.", preApprovalScreen.getMessage());
        assertEquals(PreApprovalScreen.ApprovalType.SLIDER, preApprovalScreen.getApprovalType());
        assertNotNull(preApprovalScreen.getItems());

        final List<String> items = preApprovalScreen.getItems();
        assertEquals(3, items.size());
        assertEquals("You activate a new app and allow access to your accounts", items.get(0));
    }

    @Test
    void testConvertUiRiskFlagsX() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setRiskFlags("X");

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertNotNull(result.getUi());

        final UiExtensions ui = result.getUi();
        assertEquals(true, ui.getFlipButtons());
        assertNull(ui.getBlockApprovalOnCall());
        assertNull(ui.getPreApprovalScreen());
    }

    @Test
    void testConvertUiRiskFlagsC() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setRiskFlags("C");

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertNotNull(result.getUi());

        final UiExtensions ui = result.getUi();
        assertNull(ui.getFlipButtons());
        assertEquals(true, ui.getBlockApprovalOnCall());
        assertNull(ui.getPreApprovalScreen());
    }

    @Test
    void testConvertUiRiskFlagsXCF() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setRiskFlags("XCF");

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertNotNull(result.getUi());

        final UiExtensions ui = result.getUi();
        assertEquals(true, ui.getFlipButtons());
        assertEquals(true, ui.getBlockApprovalOnCall());
        assertNotNull(ui.getPreApprovalScreen());

        final PreApprovalScreen preApprovalScreen = ui.getPreApprovalScreen();
        assertEquals(PreApprovalScreen.ScreenType.WARNING, preApprovalScreen.getType());
    }

    @Test
    void testConvertUiPostApprovalMerchantRedirect() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("{\n" +
                "  \"postApprovalScreen\": {\n" +
                "    \"heading\": \"Thank you for your order\",\n" +
                "    \"message\": \"You will be redirected to the merchant application.\",\n" +
                "    \"payload\": {\n" +
                "      \"type\": \"MERCHANT_REDIRECT\",\n" +
                "      \"redirectText\": \"Go to the application\",\n" +
                "      \"redirectUrl\": \"https://www.example.com\",\n" +
                "      \"countdown\": 5\n" +
                "    }\n" +
                "  }\n" +
                "}");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final var expectedPayload = new PostApprovalScreen.MerchantRedirectPayload();
        expectedPayload.setRedirectText("Go to the application");
        expectedPayload.setRedirectUrl("https://www.example.com");
        expectedPayload.setCountdown(5);

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(PostApprovalScreen.MerchantRedirectPayload.class)
                .isEqualTo(expectedPayload);
    }

    @Test
    void testConvertUiPostApprovalMerchantRedirectWithSubstitutedUrl() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setParameters(Map.of("userId", "666", "redirectUrl", "https://www.example.com"));

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("{\n" +
                "  \"postApprovalScreen\": {\n" +
                "    \"heading\": \"Thank you for your order\",\n" +
                "    \"message\": \"You will be redirected to the merchant application.\",\n" +
                "    \"payload\": {\n" +
                "      \"type\": \"MERCHANT_REDIRECT\",\n" +
                "      \"redirectText\": \"Go to the application\",\n" +
                "      \"redirectUrl\": \"${redirectUrl}\",\n" +
                "      \"countdown\": 5\n" +
                "    }\n" +
                "  }\n" +
                "}");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final var expectedPayload = new PostApprovalScreen.MerchantRedirectPayload();
        expectedPayload.setRedirectText("Go to the application");
        expectedPayload.setRedirectUrl("https://www.example.com");
        expectedPayload.setCountdown(5);

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(PostApprovalScreen.MerchantRedirectPayload.class)
                .isEqualTo(expectedPayload);
    }

    @Test
    void testConvertUiPostApprovalInfoMessage() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("{\n" +
                "  \"postApprovalScreen\": {\n" +
                "    \"heading\": \"Thank you for your order\",\n" +
                "    \"message\": \"You will be redirected to the merchant application.\"\n" +
                "  }\n" +
                "}");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .extracting(PostApprovalScreen::getPayload)
                .isNull();
    }

    private static OperationDetailResponse createOperationDetailResponse() {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);
        return operationDetail;
    }
}

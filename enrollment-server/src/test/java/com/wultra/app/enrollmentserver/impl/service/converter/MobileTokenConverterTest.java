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
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.entity.PreApprovalScreen;
import com.wultra.security.powerauth.lib.mtoken.model.entity.UiExtensions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link MobileTokenConverter}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class MobileTokenConverterTest {

    private MobileTokenConverter tested = new MobileTokenConverter(new ObjectMapper());

    @Test
    void testConvertUi_null() throws Exception {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();

        final Operation result = tested.convert(operationDetail, operationTemplate);

        assertNull(result.getUi());
    }

    @Test
    void testConvertUi_overriddenByEnrollment() throws Exception {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);
        operationDetail.setRiskFlags("C");

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("{\n" +
                "  \"flipButtons\": true,\n" +
                "  \"blockApprovalOnCall\": false,\n" +
                "  \"preApprovalScreen\": {\n" +
                "    \"type\": \"WARNING\",\n" +
                "    \"heading\": \"Dejte si pozor!\",\n" +
                "    \"message\": \"Můžete se stát obětí útoku.\",\n" +
                "    \"items\": [\n" +
                "      \"Aktivujete novou aplikaci a povolujete přístup k Vašim účtům\",\n" +
                "      \"Ujistěte se, že aktivace probíhá na Vašem zařízení\",\n" +
                "      \"Pokud jste byli vyzvání k této operaci v souvislosti s platbou, odmítněte ji\"\n" +
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
        assertEquals("Dejte si pozor!", preApprovalScreen.getHeading());
        assertEquals("Můžete se stát obětí útoku.", preApprovalScreen.getMessage());
        assertEquals(PreApprovalScreen.ApprovalType.SLIDER, preApprovalScreen.getApprovalType());
        assertNotNull(preApprovalScreen.getItems());

        final List<String> items = preApprovalScreen.getItems();
        assertEquals(3, items.size());
        assertEquals("Aktivujete novou aplikaci a povolujete přístup k Vašim účtům", items.get(0));
    }

    @Test
    void testConvertUi_riskFlags_X() throws Exception {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);
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
    void testConvertUi_riskFlags_C() throws Exception {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);
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
    void testConvertUi_riskFlags_XCF() throws Exception {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);
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
}

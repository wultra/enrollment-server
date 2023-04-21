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
import com.google.common.collect.ImmutableMap;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.security.powerauth.client.model.enumeration.OperationStatus;
import com.wultra.security.powerauth.client.model.enumeration.SignatureType;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.lib.mtoken.model.entity.*;
import com.wultra.security.powerauth.lib.mtoken.model.entity.attributes.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
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
        operationTemplate.setUi("""
                {
                  "flipButtons": true,
                  "blockApprovalOnCall": false,
                  "preApprovalScreen": {
                    "type": "WARNING",
                    "heading": "Watch out!",
                    "message": "You may become a victim of an attack.",
                    "items": [
                      "You activate a new app and allow access to your accounts",
                      "Make sure the activation takes place on your device",
                      "If you have been prompted for this operation in connection with a payment, decline it"
                    ],
                    "approvalType": "SLIDER"
                  },
                  "postApprovalScreen": {
                    "type": "GENERIC",
                    "heading": "Thank you for your order",
                    "message": "You may close the application now.",
                    "payload": {
                      "customMessage": "See you next time."
                    }
                  }
                }""");

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

        final PostApprovalScreen postApprovalScreen = ui.getPostApprovalScreen();
        assertNotNull(postApprovalScreen);
        assertEquals("You may close the application now.", postApprovalScreen.getMessage());
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
        operationTemplate.setUi("""
                {
                  "postApprovalScreen": {
                    "type": "MERCHANT_REDIRECT",
                    "heading": "Thank you for your order",
                    "message": "You will be redirected to the merchant application.",
                    "payload": {
                      "redirectText": "Go to the application",
                      "redirectUrl": "https://www.example.com",
                      "countdown": 5
                    }
                  }
                }""");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final var expectedPayload = new MerchantRedirectPostApprovalScreen.MerchantRedirectPayload();
        expectedPayload.setRedirectText("Go to the application");
        expectedPayload.setRedirectUrl("https://www.example.com");
        expectedPayload.setCountdown(5);

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .returns("Thank you for your order", from(PostApprovalScreen::getHeading))
                .returns("You will be redirected to the merchant application.", from(PostApprovalScreen::getMessage))
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(MerchantRedirectPostApprovalScreen.MerchantRedirectPayload.class)
                .isEqualTo(expectedPayload);
    }

    @Test
    void testConvertUiPostApprovalMerchantRedirectWithSubstitutedUrl() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setParameters(Map.of("userId", "666", "redirectUrl", "https://www.example.com"));

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("""
                {
                  "postApprovalScreen": {
                    "type": "MERCHANT_REDIRECT",
                    "heading": "Thank you for your order",
                    "message": "You will be redirected to the merchant application.",
                    "payload": {
                      "redirectText": "Go to the application",
                      "redirectUrl": "${redirectUrl}",
                      "countdown": 5
                    }
                  }
                }""");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final var expectedPayload = new MerchantRedirectPostApprovalScreen.MerchantRedirectPayload();
        expectedPayload.setRedirectText("Go to the application");
        expectedPayload.setRedirectUrl("https://www.example.com");
        expectedPayload.setCountdown(5);

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .returns("Thank you for your order", from(PostApprovalScreen::getHeading))
                .returns("You will be redirected to the merchant application.", from(PostApprovalScreen::getMessage))
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(MerchantRedirectPostApprovalScreen.MerchantRedirectPayload.class)
                .isEqualTo(expectedPayload);
    }

    @Test
    void testConvertUiPostApprovalReview() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("""
                {
                  "postApprovalScreen": {
                    "type": "REVIEW",
                    "heading": "Successful",
                    "message": "The operation was approved.",
                    "payload": {
                      "attributes": [          {
                            "type": "NOTE",
                            "id": "1",
                            "label": "test label",
                            "note": "some note"
                          }
                      ]
                    }
                  }
                }""");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final var expectedPayload = new ReviewPostApprovalScreen.ReviewPayload();
        expectedPayload.getAttributes().add(new NoteAttribute("1", "test label", "some note"));

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .returns("Successful", from(PostApprovalScreen::getHeading))
                .returns("The operation was approved.", from(PostApprovalScreen::getMessage))
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(ReviewPostApprovalScreen.ReviewPayload.class)
                .isEqualTo(expectedPayload);
    }

    @Test
    void testConvertUiPostApprovalGenericMessage() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("""
                {
                  "postApprovalScreen": {
                    "type": "GENERIC",
                    "heading": "Thank you for your order",
                    "message": "You may close the application now.",
                    "payload": {
                      "customMessage": "See you next time."
                    }
                  }
                }""");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final Map<String, Object> expectedPayload = Map.of("customMessage", "See you next time.");

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .returns("Thank you for your order", from(PostApprovalScreen::getHeading))
                .returns("You may close the application now.", from(PostApprovalScreen::getMessage))
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(GenericPostApprovalScreen.GenericPayload.class)
                .returns(expectedPayload, from(it -> ((GenericPostApprovalScreen.GenericPayload) it).getProperties()));
    }

    @Test
    void testConvertUiPostApprovalGenericMessageWithSubstitutedDangerousChars() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setParameters(Map.of("message", "\""));

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setUi("""
                {
                  "postApprovalScreen": {
                    "type": "GENERIC",
                    "heading": "Thank you for your order",
                    "message": "You may close the application now.",
                    "payload": {
                      "customMessage": "${message}"
                    }
                  }
                }""");

        final Operation result = tested.convert(operationDetail, operationTemplate);

        final Map<String, Object> expectedPayload = Map.of("customMessage", "\"");

        assertThat(result)
                .isNotNull()
                .extracting(Operation::getUi)
                .isNotNull()
                .extracting(UiExtensions::getPostApprovalScreen)
                .isNotNull()
                .returns("Thank you for your order", from(PostApprovalScreen::getHeading))
                .returns("You may close the application now.", from(PostApprovalScreen::getMessage))
                .extracting(PostApprovalScreen::getPayload)
                .isInstanceOf(GenericPostApprovalScreen.GenericPayload.class)
                .returns(expectedPayload, from(it -> ((GenericPostApprovalScreen.GenericPayload) it).getProperties()));
    }

    @Test
    void testConvertAttributes() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setParameters(ImmutableMap.<String, String>builder()
                .put("amount", "13.7")
                .put("currency", "EUR")
                .put("iban", "AT483200000012345864")
                .put("note", "Remember me")
                .put("thumbnailUrl", "https://example.com/123_thumb.jpeg")
                .put("originalUrl", "https://example.com/123.jpeg")
                .put("sourceAmount", "1.26")
                .put("sourceCurrency", "ETH")
                .put("targetAmount", "1710.98")
                .put("targetCurrency", "USD")
                .put("dynamic", "true")
                .put("partyLogoUrl", "https://example.com/img/logo/logo.svg")
                .put("partyName", "Example Ltd.")
                .put("partyDescription", "Find out more about Example...")
                .put("partyUrl", "https://example.com/hello")
                .build());

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setAttributes("""
                [
                  {
                    "id": "operation.amount",
                    "type": "AMOUNT",
                    "text": "Amount",
                    "params": {
                      "amount": "amount",
                      "currency": "currency"
                    }
                  },
                  {
                    "id": "operation.account",
                    "type": "KEY_VALUE",
                    "text": "To Account",
                    "params": {
                      "value": "iban"
                    }
                  },
                  {
                    "id": "operation.note",
                    "type": "NOTE",
                    "text": "Note",
                    "params": {
                      "note": "note"
                    }
                  },
                  {
                    "id": "operation.heading",
                    "type": "HEADING",
                    "text": "Heading"
                  },
                  {
                    "id": "operation.image",
                    "type": "IMAGE",
                    "text": "Image",
                    "params": {
                      "thumbnailUrl": "thumbnailUrl",
                      "originalUrl": "originalUrl"
                    }
                  },
                  {
                    "id": "operation.amountConversion",
                    "type": "AMOUNT_CONVERSION",
                    "text": "Amount Conversion",
                    "params": {
                      "dynamic": "dynamic",
                      "sourceAmount": "sourceAmount",
                      "sourceCurrency": "sourceCurrency",
                      "targetAmount": "targetAmount",
                      "targetCurrency": "targetCurrency"
                    }
                  },
                  {
                    "id": "operation.partyInfo",
                    "type": "PARTY_INFO",
                    "text": "Party Info",
                    "params": {
                      "logoUrl": "partyLogoUrl",
                      "name": "partyName",
                      "description": "partyDescription",
                      "websiteUrl": "partyUrl"
                    }
                  }
                ]""");

        LocaleContextHolder.setLocale(new Locale("en"));
        final Operation result = tested.convert(operationDetail, operationTemplate);

        final List<Attribute> attributes = result.getFormData().getAttributes();

        assertEquals(7, attributes.size());
        final var atributesIterator = attributes.iterator();
        assertEquals(new AmountAttribute("operation.amount", "Amount", new BigDecimal("13.7"), "EUR", "13.70", "€"), atributesIterator.next());
        assertEquals(new KeyValueAttribute("operation.account", "To Account", "AT483200000012345864"), atributesIterator.next());
        assertEquals(new NoteAttribute("operation.note", "Note", "Remember me"), atributesIterator.next());
        assertEquals(new HeadingAttribute("operation.heading", "Heading"), atributesIterator.next());
        assertEquals(new ImageAttribute("operation.image", "Image", "https://example.com/123_thumb.jpeg", "https://example.com/123.jpeg"), atributesIterator.next());
        assertEquals(AmountConversionAttribute.builder()
                .id("operation.amountConversion")
                .label("Amount Conversion")
                .dynamic(true)
                .sourceAmount(new BigDecimal("1.26"))
                .sourceAmountFormatted("1.26")
                .sourceCurrency("ETH")
                .sourceCurrencyFormatted("ETH")
                .targetAmount(new BigDecimal("1710.98"))
                .targetAmountFormatted("1,710.98")
                .targetCurrency("USD")
                .targetCurrencyFormatted("$")
                .build(), atributesIterator.next());
        assertEquals(new PartyAttribute("operation.partyInfo", "Party Info", PartyInfo.builder()
                        .logoUrl("https://example.com/img/logo/logo.svg")
                        .name("Example Ltd.")
                        .description("Find out more about Example...")
                        .websiteUrl("https://example.com/hello")
                        .build()), atributesIterator.next());
    }

    @Test
    void testConvertImageAttributeWithoutOriginalUrl() throws Exception {
        final OperationDetailResponse operationDetail = createOperationDetailResponse();
        operationDetail.setParameters(Map.of("thumbnailUrl", "https://example.com/123_thumb.jpeg"));

        final OperationTemplateEntity operationTemplate = new OperationTemplateEntity();
        operationTemplate.setAttributes("""
                [
                  {
                    "id": "operation.image",
                    "type": "IMAGE",
                    "text": "Image",
                    "params": {
                      "thumbnailUrl": "thumbnailUrl",
                      "originalUrl": "originalUrl"
                    }
                  }
                ]""");

        LocaleContextHolder.setLocale(new Locale("en"));
        final Operation result = tested.convert(operationDetail, operationTemplate);

        final List<Attribute> attributes = result.getFormData().getAttributes();

        assertEquals(1, attributes.size());
        final Attribute imageAttribute = attributes.iterator().next();
        assertEquals(new ImageAttribute("operation.image", "Image", "https://example.com/123_thumb.jpeg", null), imageAttribute);
    }

    private static OperationDetailResponse createOperationDetailResponse() {
        final OperationDetailResponse operationDetail = new OperationDetailResponse();
        operationDetail.setSignatureType(List.of(SignatureType.KNOWLEDGE));
        operationDetail.setStatus(OperationStatus.APPROVED);
        return operationDetail;
    }
}

/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateParam;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import com.wultra.security.powerauth.client.model.enumeration.SignatureType;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.lib.mtoken.model.entity.*;
import com.wultra.security.powerauth.lib.mtoken.model.entity.attributes.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Converter related to mobile token services
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Component
@Slf4j
public class MobileTokenConverter {

    private static final String RISK_FLAG_FLIP_BUTTONS = "X";
    private static final String RISK_FLAG_BLOCK_APPROVAL_ON_CALL = "C";
    private static final String RISK_FLAG_FRAUD_WARNING = "F";

    private final ObjectMapper objectMapper;

    @Autowired
    public MobileTokenConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private AllowedSignatureType convert(List<SignatureType> signatureType) {
        final AllowedSignatureType allowedSignatureType = new AllowedSignatureType();
        if (signatureType.contains(SignatureType.POSSESSION)) {
            allowedSignatureType.setType(AllowedSignatureType.Type.MULTIFACTOR_1FA);
        } else {
            allowedSignatureType.setType(AllowedSignatureType.Type.MULTIFACTOR_2FA);
            final List<String> variants = new ArrayList<>();
            if (signatureType.contains(SignatureType.POSSESSION_KNOWLEDGE)) {
                variants.add("possession_knowledge");
            }
            if (signatureType.contains(SignatureType.POSSESSION_BIOMETRY)) {
                variants.add("possession_biometry");
            }
            allowedSignatureType.setVariants(variants);
        }
        return allowedSignatureType;
    }

    /**
     * Convert operation detail from PowerAuth Server and operation template from Enrollment Server into an
     * operation in API response.
     *
     * @param operationDetail Operation detail response obtained from PowerAuth Server.
     * @param operationTemplate Operation template obtained from Enrollment Server.
     * @return Operation for API response.
     * @throws MobileTokenConfigurationException In case there is an error in configuration data.
     */
    public Operation convert(OperationDetailResponse operationDetail, OperationTemplateEntity operationTemplate) throws MobileTokenConfigurationException {
        try {
            final Map<String, String> parameters = operationDetail.getParameters();
            final StringSubstitutor sub = createStringSubstitutor(parameters);
            final UiExtensions uiExtensions = convertUiExtension(operationDetail, operationTemplate, sub);
            final FormData formData = prepareFormData(operationTemplate, parameters, sub);

            final Operation operation = new Operation();
            operation.setId(operationDetail.getId());
            operation.setName(operationDetail.getOperationType());
            operation.setAllowedSignatureType(convert(operationDetail.getSignatureType()));
            operation.setData(operationDetail.getData());
            operation.setOperationCreated(operationDetail.getTimestampCreated());
            operation.setOperationExpires(operationDetail.getTimestampExpires());
            operation.setStatus(operationDetail.getStatus().name());
            operation.setUi(uiExtensions);
            operation.setFormData(formData);

            return operation;
        } catch (JsonProcessingException e) {
            logger.debug("Unable to parse JSON with operation template parameters: {}", e.getMessage());
            logger.debug("Exception detail", e);
            throw new MobileTokenConfigurationException("ERR_CONFIG", "Invalid JSON structure for the configuration: " + e.getMessage());
        }
    }

    private FormData prepareFormData(
            final OperationTemplateEntity operationTemplate,
            final Map<String, String> parameters,
            final StringSubstitutor sub) throws JsonProcessingException {

        final FormData formData = new FormData();
        if (sub != null) {
            formData.setTitle(sub.replace(operationTemplate.getTitle()));
            formData.setMessage(sub.replace(operationTemplate.getMessage()));
        } else {
            formData.setTitle(operationTemplate.getTitle());
            formData.setMessage(operationTemplate.getMessage());
        }

        final String attributes = operationTemplate.getAttributes();
        if (attributes != null) {
            final OperationTemplateParam[] operationTemplateParams = objectMapper.readValue(attributes, OperationTemplateParam[].class);
            if (operationTemplateParams != null) {
                final List<Attribute> formDataAttributes = formData.getAttributes();
                for (OperationTemplateParam templateParam : operationTemplateParams) {
                    buildAttribute(templateParam, parameters)
                            .ifPresent(formDataAttributes::add);
                }
            }
        }
        return formData;
    }

    private static StringSubstitutor createStringSubstitutor(final Map<String, String> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return null;
        } else {
            final Map<String, String> escapedParameters = parameters.entrySet().stream()
                    .collect(toMap(Map.Entry::getKey, it -> StringEscapeUtils.escapeJson(it.getValue())));
            return new StringSubstitutor(escapedParameters);
        }
    }

    private UiExtensions convertUiExtension(
            final OperationDetailResponse operationDetail,
            final OperationTemplateEntity operationTemplate,
            final StringSubstitutor substitutor) throws JsonProcessingException {

        if (StringUtils.hasText(operationTemplate.getUi())) {
            final String uiJsonString = substitutor == null ? operationTemplate.getUi() : substitutor.replace(operationTemplate.getUi());
            logger.debug("Deserializing ui: '{}' of OperationTemplate ID: {} to UiExtensions", uiJsonString, operationTemplate.getId());
            return deserializeUiExtensions(uiJsonString, operationDetail);
        } else if (StringUtils.hasText(operationDetail.getRiskFlags())) {
            final String riskFlags = operationDetail.getRiskFlags();
            logger.debug("Converting riskFlags: '{}' of OperationDetail ID: {} to UiExtensions", riskFlags, operationDetail.getId());
            final UiExtensions ui = new UiExtensions();
            if (riskFlags.contains(RISK_FLAG_FLIP_BUTTONS)) {
                ui.setFlipButtons(true);
            }
            if (riskFlags.contains(RISK_FLAG_BLOCK_APPROVAL_ON_CALL)) {
                ui.setBlockApprovalOnCall(true);
            }
            if (riskFlags.contains(RISK_FLAG_FRAUD_WARNING)) {
                final PreApprovalScreen preApprovalScreen = new PreApprovalScreen();
                preApprovalScreen.setType(PreApprovalScreen.ScreenType.WARNING);
                preApprovalScreen.setApprovalType(PreApprovalScreen.ApprovalType.SLIDER);
                ui.setPreApprovalScreen(preApprovalScreen);
            }
            return ui;
        } else {
            return null;
        }
    }

    private UiExtensions deserializeUiExtensions(final String uiJsonString, final OperationDetailResponse operationDetail) throws JsonProcessingException {
        final UiExtensions uiExtensions = objectMapper.readValue(uiJsonString, UiExtensions.class);
        if (uiExtensions.getPreApprovalScreen() != null
                && uiExtensions.getPreApprovalScreen().getType() == PreApprovalScreen.ScreenType.QR_SCAN
                && operationDetail.getProximityOtp() == null) {

            logger.info("Template for operation ID: {} is configured to use pre-approval screen QR_SCAN, but OTP was not created", operationDetail.getId());
            uiExtensions.setPreApprovalScreen(null);
        }
        return uiExtensions;
    }

    private static Optional<Attribute> buildAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String type = templateParam.getType();
        return switch (type) {
            case "AMOUNT" ->
                buildAmountAttribute(templateParam, params);
            case "AMOUNT_CONVERSION" ->
                buildAmountConversionAttribute(templateParam, params);
            case "HEADING" ->
                Optional.of(new HeadingAttribute(templateParam.getId(), templateParam.getText()));
            case "NOTE" ->
                buildNoteAttribute(templateParam, params);
            case "KEY_VALUE" ->
                buildKeyValueAttribute(templateParam, params);
            case "IMAGE" ->
                buildImageAttribute(templateParam, params);
            case "PARTY_INFO" ->
                buildPartyInfoAttribute(templateParam, params);
            default -> { // attempt fallback to key-value type
                logger.error("Invalid operation attribute type: {}", type);
                yield buildKeyValueAttribute(templateParam, params);
            }
        };
    }

    private static Optional<Attribute> buildKeyValueAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        return fetchTemplateParamValue(templateParam, params, "value")
                .map(it -> new KeyValueAttribute(id, text, it));
    }

    private static Optional<Attribute> buildNoteAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        return fetchTemplateParamValue(templateParam, params, "note")
                .map(it -> new NoteAttribute(id, text, it));
    }

    private static Optional<Attribute> buildAmountAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        final Optional<String> amount = fetchTemplateParamValue(templateParam, params, "amount");
        if (amount.isEmpty()) {
            return Optional.empty();
        }
        final Optional<String> currency = fetchTemplateParamValue(templateParam, params, "currency");
        if (currency.isEmpty()) {
            return Optional.empty();
        }
        final BigDecimal amountRaw;
        try {
            amountRaw = new BigDecimal(amount.get());
        } catch (NumberFormatException ex) {
            logger.warn("Invalid number format: {}, skipping the AMOUNT attribute!", amount);
            return Optional.empty();
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final String currencyRaw = currency.get();
        final String currencyFormatted = MonetaryConverter.formatCurrency(currencyRaw, locale);
        final String amountFormatted = MonetaryConverter.formatAmount(amountRaw, currencyRaw, locale);
        final String valueFormatted = MonetaryConverter.formatValue(amountRaw, currencyRaw, locale);
        return Optional.of(AmountAttribute.builder()
                .id(id)
                .label(text)
                .amount(amountRaw)
                .amountFormatted(amountFormatted)
                .currency(currencyRaw)
                .currencyFormatted(currencyFormatted)
                .valueFormatted(valueFormatted)
                .build());
    }

    private static Optional<Attribute> buildAmountConversionAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        final Optional<String> sourceAmount = fetchTemplateParamValue(templateParam, params, "sourceAmount");
        final Optional<String> targetAmount = fetchTemplateParamValue(templateParam, params, "targetAmount");
        if (sourceAmount.isEmpty() || targetAmount.isEmpty()) {
            return Optional.empty();
        }
        final Optional<String> sourceCurrency = fetchTemplateParamValue(templateParam, params, "sourceCurrency");
        final Optional<String> targetCurrency = fetchTemplateParamValue(templateParam, params, "targetCurrency");
        if (sourceCurrency.isEmpty() || targetCurrency.isEmpty()) {
            return Optional.empty();
        }

        final boolean dynamic = fetchTemplateParamValue(templateParam, params, "dynamic")
                .map(Boolean::parseBoolean)
                .orElse(false);

        final BigDecimal sourceAmountRaw;
        final BigDecimal targetAmountRaw;
        try {
            sourceAmountRaw = new BigDecimal(sourceAmount.get());
            targetAmountRaw = new BigDecimal(targetAmount.get());
        } catch (NumberFormatException ex) {
            logger.warn("Invalid number format: {}, skipping the AMOUNT_CONVERSION attribute!", sourceAmount);
            return Optional.empty();
        }

        final Locale locale = LocaleContextHolder.getLocale();
        final String sourceCurrencyRaw = sourceCurrency.get();
        final String sourceCurrencyFormatted = MonetaryConverter.formatCurrency(sourceCurrencyRaw, locale);
        final String sourceAmountFormatted = MonetaryConverter.formatAmount(sourceAmountRaw, sourceCurrencyRaw, locale);
        final String sourceValueFormatted = MonetaryConverter.formatValue(sourceAmountRaw, sourceCurrencyRaw, locale);
        final String targetCurrencyRaw = targetCurrency.get();
        final String targetCurrencyFormatted = MonetaryConverter.formatCurrency(targetCurrencyRaw, locale);
        final String targetAmountFormatted = MonetaryConverter.formatAmount(targetAmountRaw, targetCurrencyRaw, locale);
        final String targetValueFormatted = MonetaryConverter.formatValue(targetAmountRaw, targetCurrencyRaw, locale);
        return Optional.of(AmountConversionAttribute.builder()
                .id(id)
                .label(text)
                .dynamic(dynamic)
                .sourceAmount(sourceAmountRaw)
                .sourceAmountFormatted(sourceAmountFormatted)
                .sourceCurrency(sourceCurrencyRaw)
                .sourceCurrencyFormatted(sourceCurrencyFormatted)
                .sourceValueFormatted(sourceValueFormatted)
                .targetAmount(targetAmountRaw)
                .targetAmountFormatted(targetAmountFormatted)
                .targetCurrency(targetCurrencyRaw)
                .targetCurrencyFormatted(targetCurrencyFormatted)
                .targetValueFormatted(targetValueFormatted)
                .build());
    }

    private static Optional<Attribute> buildImageAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        final Optional<String> thumbnailUrl = fetchTemplateParamValue(templateParam, params, "thumbnailUrl");
        if (thumbnailUrl.isEmpty()) {
            logger.warn("ThumbnailUrl is not defined for OperationTemplateParam ID: {}", templateParam.getId());
            return Optional.empty();
        }
        final Optional<String> originalUrl = fetchTemplateParamValue(templateParam, params, "originalUrl");
        if (originalUrl.isEmpty()) {
            logger.debug("OriginalUrl is not defined for OperationTemplateParam ID: {}", templateParam.getId());
        }
        return Optional.of(new ImageAttribute(id, text, thumbnailUrl.get(), originalUrl.orElse(null)));
    }

    private static Optional<Attribute> buildPartyInfoAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        final PartyInfo partyInfo = PartyInfo.builder()
                .logoUrl(fetchTemplateParamValueNullable(templateParam, params, "logoUrl"))
                .name(fetchTemplateParamValueNullable(templateParam, params, "name"))
                .description(fetchTemplateParamValueNullable(templateParam, params, "description"))
                .websiteUrl(fetchTemplateParamValueNullable(templateParam, params, "websiteUrl"))
                .build();
        return Optional.of(new PartyAttribute(id, text, partyInfo));
    }

    private static Optional<String> fetchTemplateParamValue(final OperationTemplateParam templateParam, final Map<String, String> params, final String key) {
        final String id = templateParam.getId();
        final Map<String, String> templateParams = templateParam.getParams();
        if (templateParams == null) {
            logger.warn("Params of OperationTemplateParam ID: {} is null", id);
            return Optional.empty();
        }
        if (params == null) {
            logger.warn("Params of OperationDetailResponse is null");
            return Optional.empty();
        }
        return Optional.ofNullable(templateParams.get(key))
                .map(params::get);
    }

    private static String fetchTemplateParamValueNullable(final OperationTemplateParam templateParam, final Map<String, String> params, final String key) {
        return fetchTemplateParamValue(templateParam, params, key)
                .orElse(null);
    }
}

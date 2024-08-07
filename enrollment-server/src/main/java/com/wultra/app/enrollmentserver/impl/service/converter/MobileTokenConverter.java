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
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.function.UnaryOperator;

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
            final UnaryOperator<String> substitutor = createStringSubstitutor(parameters);
            final UiExtensions uiExtensions = convertUiExtension(operationDetail, operationTemplate, substitutor);
            final FormData formData = prepareFormData(operationTemplate, parameters, substitutor);

            final Operation operation = new Operation();
            operation.setId(operationDetail.getId());
            operation.setName(operationDetail.getOperationType());
            operation.setAllowedSignatureType(convert(operationDetail.getSignatureType()));
            operation.setData(operationDetail.getData());
            operation.setOperationCreated(operationDetail.getTimestampCreated());
            operation.setOperationExpires(operationDetail.getTimestampExpires());
            operation.setStatus(operationDetail.getStatus().name());
            operation.setStatusReason(operationDetail.getStatusReason());
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
            final UnaryOperator<String> substitutor) throws JsonProcessingException {

        final FormData formData = new FormData();
        formData.setTitle(substitutor.apply(operationTemplate.getTitle()));
        formData.setMessage(substitutor.apply(operationTemplate.getMessage()));
        formData.setResultTexts(convert(substitutor.apply(operationTemplate.getResultTexts())));

        final String attributes = operationTemplate.getAttributes();
        if (attributes == null) {
            return formData;
        }

        final List<OperationTemplateParam> operationTemplateParams = objectMapper.readValue(attributes, new TypeReference<>(){});
        if (operationTemplateParams != null) {
            final List<Attribute> formDataAttributes = operationTemplateParams.stream()
                    .map(templateParam -> buildAttribute(templateParam, parameters))
                    .flatMap(Optional::stream)
                    .toList();
            formData.setAttributes(formDataAttributes);
        }
        return formData;
    }

    private ResultTexts convert(final String source) throws JsonProcessingException {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        return objectMapper.readValue(source, ResultTexts.class);
    }

    private static UnaryOperator<String> createStringSubstitutor(final Map<String, String> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return UnaryOperator.identity();
        } else {
            final Map<String, String> escapedParameters = parameters.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(toMap(Map.Entry::getKey, it -> StringEscapeUtils.escapeJson(it.getValue())));
            return new StringSubstitutorWrapper(escapedParameters);
        }
    }

    private UiExtensions convertUiExtension(
            final OperationDetailResponse operationDetail,
            final OperationTemplateEntity operationTemplate,
            final UnaryOperator<String> substitutor) throws JsonProcessingException {

        if (StringUtils.hasText(operationTemplate.getUi())) {
            final String uiJsonString = substitutor.apply(operationTemplate.getUi());
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
                buildHeadingAttribute(templateParam, params);
            case "NOTE" ->
                buildNoteAttribute(templateParam, params);
            case "KEY_VALUE" ->
                buildKeyValueAttribute(templateParam, params);
            case "ALERT" ->
                buildAlertAttribute(templateParam, params);
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

    private static Optional<Attribute> buildHeadingAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        final int level = fetchTemplateParamValue(templateParam, params, "level")
                .map(Integer::valueOf).orElse(0);
        return Optional.of(new HeadingAttribute(id, text, level));
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

    private static Optional<Attribute> buildAlertAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        final Optional<String> alertTypeOptional = fetchTemplateParamValue(templateParam, params, "type");
        final AlertAttribute.AlertType alertType = alertTypeOptional
                .map(MobileTokenConverter::convertAlertType)
                .orElse(AlertAttribute.AlertType.INFO);
        final Optional<String> titleOptional = fetchTemplateParamValue(templateParam, params, "title");
        final String title = titleOptional.orElse(null);
        final Optional<String> messageOptional = fetchTemplateParamValue(templateParam, params, "message");
        final String message = messageOptional.orElse(null);
        return Optional.of(AlertAttribute.builder()
                .id(id)
                .alertType(alertType)
                .label(text)
                .title(title)
                .message(message)
                .build()
        );
    }

    private static AlertAttribute.AlertType convertAlertType(String alertTypeValue) {
        return switch (alertTypeValue.toUpperCase()) {
            case "SUCCESS" -> AlertAttribute.AlertType.SUCCESS;
            case "WARNING" -> AlertAttribute.AlertType.WARNING;
            case "ERROR" -> AlertAttribute.AlertType.ERROR;
            default -> AlertAttribute.AlertType.INFO;
        };
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

        final Locale locale = LocaleContextHolder.getLocale();
        final String currencyRaw = currency.get();
        final String currencyFormatted = MonetaryConverter.formatCurrency(currencyRaw, locale);
        final AmountFormatted amountFormatted = createAmountFormatted(amount.get(), currencyRaw, "AMOUNT");
        return Optional.of(AmountAttribute.builder()
                .id(id)
                .label(text)
                .amount(amountFormatted.amountRaw())
                .amountFormatted(amountFormatted.amountFormatted())
                .currency(currencyRaw)
                .currencyFormatted(currencyFormatted)
                .valueFormatted(amountFormatted.valueFormatted())
                .build());
    }

    private static AmountFormatted createAmountFormatted(final String amount, final String currencyRaw, final String attribute) {
        final Locale locale = LocaleContextHolder.getLocale();

        try {
            final BigDecimal amountRaw = new BigDecimal(amount);
            final String amountFormatted = MonetaryConverter.formatAmount(amountRaw, currencyRaw, locale);
            final String valueFormatted = MonetaryConverter.formatValue(amountRaw, currencyRaw, locale);
            return new AmountFormatted(amountRaw, amountFormatted, valueFormatted);
        } catch (NumberFormatException e) {
            logger.warn("Invalid number format: {}, the raw value is not filled in into {} attribute!", amount, attribute);
            logger.trace("Invalid number format: {}, the raw value is not filled in into {} attribute!", amount, attribute, e);
            // fallback - pass 'not a number' directly to the formatted field
            final String valueFormatted = amount + " " + currencyRaw;
            return new AmountFormatted(null, amount, valueFormatted);
        }
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

        final Locale locale = LocaleContextHolder.getLocale();
        final String sourceCurrencyRaw = sourceCurrency.get();
        final AmountFormatted sourceAmountFormatted = createAmountFormatted(sourceAmount.get(), sourceCurrencyRaw, "AMOUNT_CONVERSION");

        final String targetCurrencyRaw = targetCurrency.get();
        final AmountFormatted targetAmountFormatted = createAmountFormatted(targetAmount.get(), targetCurrencyRaw, "AMOUNT_CONVERSION");

        final String sourceCurrencyFormatted = MonetaryConverter.formatCurrency(sourceCurrencyRaw, locale);
        final String targetCurrencyFormatted = MonetaryConverter.formatCurrency(targetCurrencyRaw, locale);
        return Optional.of(AmountConversionAttribute.builder()
                .id(id)
                .label(text)
                .dynamic(dynamic)
                .sourceAmount(sourceAmountFormatted.amountRaw())
                .sourceAmountFormatted(sourceAmountFormatted.amountFormatted())
                .sourceCurrency(sourceCurrencyRaw)
                .sourceCurrencyFormatted(sourceCurrencyFormatted)
                .sourceValueFormatted(sourceAmountFormatted.valueFormatted())
                .targetAmount(targetAmountFormatted.amountRaw())
                .targetAmountFormatted(targetAmountFormatted.amountFormatted())
                .targetCurrency(targetCurrencyRaw)
                .targetCurrencyFormatted(targetCurrencyFormatted)
                .targetValueFormatted(targetAmountFormatted.valueFormatted())
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
            logger.warn("Params of OperationTemplateParam is null");
            return Optional.empty();
        }
        return Optional.ofNullable(templateParams.get(key))
                .map(params::get);
    }

    private static String fetchTemplateParamValueNullable(final OperationTemplateParam templateParam, final Map<String, String> params, final String key) {
        return fetchTemplateParamValue(templateParam, params, key)
                .orElse(null);
    }

    private record AmountFormatted(BigDecimal amountRaw, String amountFormatted, String valueFormatted) {}

    private static class StringSubstitutorWrapper implements UnaryOperator<String> {
        private final StringSubstitutor substitutor;

        StringSubstitutorWrapper(final Map<String, String> escapedParameters) {
            substitutor = new StringSubstitutor(escapedParameters);
        }

        @Override
        public String apply(final String source) {
            return substitutor.replace(source);
        }
    }
}

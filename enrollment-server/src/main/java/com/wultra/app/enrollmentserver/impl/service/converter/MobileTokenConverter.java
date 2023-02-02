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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            return objectMapper.readValue(uiJsonString, UiExtensions.class);
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
                ui.setPreApprovalScreen(preApprovalScreen);
            }
            return ui;
        } else {
            return null;
        }
    }

    private static Optional<Attribute> buildAttribute(final OperationTemplateParam templateParam, final Map<String, String> params) {
        final String type = templateParam.getType();
        switch (type) {
            case "AMOUNT": {
                return buildAmountAttribute(templateParam, params);
            }
            case "HEADING": {
                return Optional.of(new HeadingAttribute(templateParam.getId(), templateParam.getText()));
            }
            case "NOTE": {
                return buildNoteAttribute(templateParam, params);
            }
            case "KEY_VALUE": {
                return buildKeyValueAttribute(templateParam, params);
            }
            default: { // attempt fallback to key-value type
                logger.error("Invalid operation attribute type: {}", type);
                return buildKeyValueAttribute(templateParam, params);
            }
        }
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
        try {
            final BigDecimal amountValue = new BigDecimal(amount.get());
            return Optional.of(new AmountAttribute(id, text, amountValue, currency.orElse(null), amount.get(), currency.orElse(null)));
        } catch (NumberFormatException ex) {
            logger.warn("Invalid number format: {}, skipping the AMOUNT attribute!", amount);
            return Optional.empty();
        }
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
        final String paramKey = templateParams.get(key);
        return Optional.ofNullable(params.get(paramKey));
    }
}

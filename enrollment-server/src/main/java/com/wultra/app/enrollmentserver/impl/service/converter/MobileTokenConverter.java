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
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            final Operation operation = new Operation();
            operation.setId(operationDetail.getId());
            operation.setName(operationDetail.getOperationType());
            operation.setAllowedSignatureType(convert(operationDetail.getSignatureType()));
            operation.setData(operationDetail.getData());
            operation.setOperationCreated(operationDetail.getTimestampCreated());
            operation.setOperationExpires(operationDetail.getTimestampExpires());
            operation.setStatus(operationDetail.getStatus().name());

            operation.setUi(convertUiExtension(operationDetail, operationTemplate));

            // Prepare title and message with substituted attributes
            final FormData formData = new FormData();
            final Map<String, String> parameters = operationDetail.getParameters();
            if (parameters != null && parameters.keySet().size() > 0) {
                final StringSubstitutor sub = new StringSubstitutor(parameters);
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
                    for (OperationTemplateParam templateParam : operationTemplateParams) {
                        final Attribute attribute = buildAttribute(templateParam, parameters);
                        if (attribute != null) {
                            formData.getAttributes().add(attribute);
                        }
                    }
                }
            }

            operation.setFormData(formData);
            return operation;
        } catch (JsonProcessingException e) {
            logger.debug("Unable to parse JSON with operation template parameters: {}", e.getMessage());
            logger.debug("Exception detail", e);
            throw new MobileTokenConfigurationException("ERR_CONFIG", "Invalid JSON structure for the configuration: " + e.getMessage());
        }
    }

    private UiExtensions convertUiExtension(final OperationDetailResponse operationDetail, final OperationTemplateEntity operationTemplate) throws JsonProcessingException {
        if (StringUtils.hasText(operationTemplate.getUi())) {
            final String uiJsonString = operationTemplate.getUi();
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

    private Attribute buildAttribute(OperationTemplateParam templateParam, Map<String, String> params) {
        final String type = templateParam.getType();
        final String id = templateParam.getId();
        final String text = templateParam.getText();
        switch (type) {
            case "AMOUNT": {
                final Map<String, String> templateParams = templateParam.getParams();
                if (templateParams == null) {
                    logger.warn("Params of OperationTemplateParam ID: {} is null", id);
                    return null;
                }
                if (params == null) {
                    logger.warn("Params of OperationDetailResponse is null");
                    return null;
                }
                final String amountKey = templateParams.get("amount");
                final String currencyKey = templateParams.get("currency");
                final String amount = params.get(amountKey);
                final String currency = params.get(currencyKey);
                try {
                    final BigDecimal amountValue = new BigDecimal(amount);
                    return new AmountAttribute(id, text, amountValue, currency, amount, currency);
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid number format: {}, skipping the AMOUNT attribute!", amount);
                    return null;
                }
            }
            case "HEADING": {
                return new HeadingAttribute(id, text);
            }
            case "NOTE": {
                final Map<String, String> templateParams = templateParam.getParams();
                if (templateParams == null) {
                    logger.warn("Params of OperationTemplateParam ID: {} is null", id);
                    return null;
                }
                if (params == null) {
                    logger.warn("Params of OperationDetailResponse is null");
                    return null;
                }
                final String noteKey = templateParams.get("note");
                final String note = params.get(noteKey);
                if (note == null) { // invalid element, does not contain note at all
                    return null;
                }
                return new NoteAttribute(id, text, note);
            }
            case "KEY_VALUE": {
                final Map<String, String> templateParams = templateParam.getParams();
                if (templateParams == null) {
                    logger.warn("Params of OperationTemplateParam ID: {} is null", id);
                    return null;
                }
                if (params == null) {
                    logger.warn("Params of OperationDetailResponse is null");
                    return null;
                }
                final String valueKey = templateParams.get("value");
                final String value = params.get(valueKey);
                if (value == null) { // invalid element, does not contain note at all
                    return null;
                }
                return new KeyValueAttribute(id, text, value);
            }
            default: { // attempt fallback to key-value type
                logger.error("Invalid operation attribute type: {}", type);
                final Map<String, String> templateParams = templateParam.getParams();
                if (templateParams == null) {
                    logger.warn("Params of OperationTemplateParam ID: {} is null", id);
                    return null;
                }
                if (params == null) {
                    logger.warn("Params of OperationDetailResponse is null");
                    return null;
                }
                final String valueKey = templateParams.get("value");
                final String value = params.get(valueKey);
                if (value == null) { // invalid element, does not contain note at all
                    return null;
                }
                return new KeyValueAttribute(id, text, value);
            }
        }
    }

}

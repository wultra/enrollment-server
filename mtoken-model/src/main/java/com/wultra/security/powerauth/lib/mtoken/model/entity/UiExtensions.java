/*
 * PowerAuth Mobile Token Model
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
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * This class represents the extensions to the user interface (UI) that can be used to customize the behavior and appearance of the mobile app during operation approval.
 */
@Data
public class UiExtensions {

    /**
     * Property that hints the mobile app that the `Approve` and `Reject` buttons should be flipped,
     * raising the CTA priority of the `Reject` button over the `Approve` button.
     */
    @Schema(description = """
            Property that hints the mobile app that the `Approve` and `Reject` buttons should be flipped,
            raising the CTA priority of the `Reject` button over the `Approve` button.
            """)
    private Boolean flipButtons;

    /**
     * Property that hints the mobile app that the operation approval should be blocked in case that
     * there is an ongoing call on the device. The UI should still allow pressing the `Approve` button,
     * but instead of displaying a PIN code / biometric authentication, user should be presented with
     * a screen informing the user that the operation cannot be approved while the user is on the phone.
     */
    @Schema(description = """
            Property that hints the mobile app that the operation approval should be blocked in case that
            there is an ongoing call on the device. The UI should still allow pressing the `Approve` button,
            but instead of displaying a PIN code / biometric authentication, user should be presented with
            a screen informing the user that the operation cannot be approved while the user is on the phone.
            """)
    private Boolean blockApprovalOnCall;

    /**
     * Property that defines a screen content that is displayed before the user sees the operation
     * approval screen. The purpose of the screen could be to provide an additional warning before
     * approving an operation, or to display generic information related to the operation approval.
     */
    @Schema(description = """
            Property that defines a screen content that is displayed before the user sees the operation
            approval screen. The purpose of the screen could be to provide an additional warning before
            approving an operation, or to display generic information related to the operation approval.
            """)
    private PreApprovalScreen preApprovalScreen;

    /**
     * Property that defines a screen content that is displayed after the user sees the operation
     * approval screen. The purpose of the screen could be to provide additional information after
     * approving an operation, or to provide an option to execute simple consecutive actions.
     */
    @Schema(description = """
            Property that defines a screen content that is displayed after the user sees the operation
            approval screen. The purpose of the screen could be to provide additional information after
            approving an operation, or to provide an option to execute simple consecutive actions.
            """)
    private PostApprovalScreen postApprovalScreen;

    /**
     * A mapping of additional data templates that can be sent to the app for further customization.
     * For example, these templates can contain specific data fields and their corresponding form format to be displayed in the app.
     */
    @Schema(description = """
            A mapping of additional data templates that can be sent to the app for further customization.
            For example, these templates can contain specific data fields and their corresponding form format to be displayed in the app.
            """)
    private Map<String, Object> templates;
}

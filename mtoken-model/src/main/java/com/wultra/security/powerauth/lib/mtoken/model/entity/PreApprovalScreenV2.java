/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Information about screen displayed before an operation approval.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
public class PreApprovalScreenV2 {

    @Schema(description = """
            This id will be used in `mobileTokenData.preApprovalScreens` when calling `/operation/cancel` or `/operation/authorize`.
            """, example = "custom_id")
    private String id;

    /**
     * Type of the pre-approval screen.
     */
    @NotNull
    private ScreenType type;

    private Boolean backButton;

    @Schema(description = "Image is stored in-app.", example = "image-label")
    private String image;

    /**
     * Approval screen heading.
     */
    @NotNull
    @Schema(description = "Approval screen heading.")
    private String heading;

    /**
     * Approval screen message displayed under heading.
     */
    @NotNull
    @Schema(description = "Approval screen message displayed under heading.")
    private String message;

    private List<Element> elements;

    private Controls  controls;

    public record Element(
            String id,
            ElementType type,
            ElementStyle style,
            String text,
            String href,
            String icon,
            ActionType action,
            @Schema(example = "REJECT")
            String actionSettings
    ) {}

    public enum ActionType {
        LINK,
        MAIL,
        PHONE
    }

    public enum ElementType {
        LIST_ITEM,
        ALERT,
        BUTTON
    }

    public enum ElementStyle {
        INFO,
        WARNING,
        DANGER
    }

    /**
     * Type of the pre-approval screen.
     */
    public enum ScreenType {
        /**
         * The purpose of the screen is to warn the user about a potential problem.
         */
        WARNING,

        /**
         * The purpose of the screen is to inform the user about a specific operation context.
         */
        INFO,

        /**
         * The purpose of the screen is to inform the user to scan QR code to perform proximity verification.
         */
        QR_SCAN
    }

    public record Controls(
            Boolean flip,
            Axis axis,
            Decline decline,
            Approve approve
    ) {}

    public record Approve(
            ApprovalType type,
            String text,
            Integer counter
    ) {}

    public record Decline(
            DeclineType type,
            String text
    ) {}

    public enum DeclineType {
        BACK,
        REJECT
    }

    public enum Axis {
        HORIZONTAL,
        VERTICAL
    }

    /**
     * Type of the approval user experience.
     */
    public enum ApprovalType {
        /**
         * The user needs to slide a UI slider ('Slide to unlock') to proceed to the operation approval screen.
         */
        SLIDER,
        BUTTON
    }

}

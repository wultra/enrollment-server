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

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Information about screen displayed before an operation approval.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
public class PreApprovalScreen {

    /**
     * Type of the pre-approval screen.
     */
    public enum ScreenType {
        /**
         * The purpose of the screen is to warn user about a potential problem.
         */
        WARNING,

        /**
         * The purpose of the screen is to inform user about a some specific operation context.
         */
        INFO
    }

    /**
     * Type of the approval user experience.
     */
    public enum ApprovalType {
        /**
         * The user needs to slide a UI slider ("Slide to unlock") to proceed to the operation approval screen.
         */
        SLIDER
    }

    /**
     * Type of the pre-approval screen.
     */
    @NotNull
    private ScreenType type;

    /**
     * Approval screen heading.
     */
    @NotNull
    private String heading;

    /**
     * Approval screen message displayed under heading.
     */
    @NotNull
    private String message;

    /**
     * List of additional text items displayed.
     */
    @NotNull
    private List<String> items;

    /**
     * Type of the approval element.
     */
    private ApprovalType approvalType;

}

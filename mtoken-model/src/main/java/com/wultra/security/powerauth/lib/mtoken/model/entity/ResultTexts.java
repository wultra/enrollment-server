/*
 * PowerAuth Mobile Token Model
 * Copyright (C) 2024 Wultra s.r.o.
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

import lombok.Data;

/**
 * Customized texts to display for {@code success}, {@code failure}, or {@code reject} operations.
 * If not provided (either as individual properties or for the entire object), default messages will be used.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
public class ResultTexts {

    private String success;

    private String failure;

    private String reject;

}

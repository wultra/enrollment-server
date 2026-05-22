/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.model.request;

/**
 * Status reported by event data.
 * <p>
 * Each event type uses only a subset of the values:
 * <ul>
 *     <li>{@link EventType#DOCUMENT_VERIFICATION_FINISHED}, {@link EventType#FINAL_DOCUMENT_VERIFICATION_FINISHED},
 *         {@link EventType#PRESENCE_CHECK_FINISHED} – {@link #ACCEPTED}, {@link #REJECTED}, {@link #FAILED}.</li>
 *     <li>{@link EventType#PROCESS_FINISHED} – {@link #FINISHED}, {@link #FAILED}.</li>
 * </ul>
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public enum EventStatus {
    ACCEPTED,
    REJECTED,
    FAILED,
    FINISHED
}

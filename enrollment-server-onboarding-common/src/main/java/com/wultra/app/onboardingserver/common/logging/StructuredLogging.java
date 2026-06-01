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

package com.wultra.app.onboardingserver.common.logging;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;

/**
 * Wultra wrapper of {@link StructuredArguments} providing convenience methods for common structured logging keys.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO (racansky, 2026-05-29) extract to wultra-core
public final class StructuredLogging {

    private StructuredLogging() {
        throw new IllegalStateException("Utility class, do not instantiate");
    }

    /**
     * Delegate to {@link StructuredArguments#kv(String, Object)}.
     *
     * @param key   the key
     * @param value the value
     * @return structured argument
     */
    public static StructuredArgument kv(final String key, final Object value) {
        return StructuredArguments.kv(key, value);
    }

    /**
     * Create a structured argument with key {@code action}.
     *
     * @param value the action name
     * @return structured argument
     */
    public static StructuredArgument action(final String value) {
        return StructuredArguments.kv("action", value);
    }

    /**
     * Create a structured argument with key {@code state}.
     *
     * @param value the state value
     * @return structured argument
     */
    public static StructuredArgument state(final String value) {
        return StructuredArguments.kv("state", value);
    }

    /**
     * Create a structured argument with key {@code state} and value {@code initiated}.
     *
     * @return structured argument
     */
    public static StructuredArgument stateInitiated() {
        return state("initiated");
    }

    /**
     * Create a structured argument with key {@code state} and value {@code failed}.
     *
     * @return structured argument
     */
    public static StructuredArgument stateFailed() {
        return state("failed");
    }

    /**
     * Create a structured argument with key {@code state} and value {@code succeeded}.
     *
     * @return structured argument
     */
    public static StructuredArgument stateSucceeded() {
        return state("succeeded");
    }

}

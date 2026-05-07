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
 *
 */
package com.wultra.app.onboardingserver.provider.model.request;

import com.wultra.core.annotations.PublicApi;
import lombok.*;

import java.util.List;

/**
 * {@link EventData} for {@link EventType#FINAL_DOCUMENT_VERIFICATION_FINISHED}.
 * Contains the overall document check result for all documents combined,
 * including additional checks (document crosscheck, document type, document country).
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Getter
@ToString
@PublicApi
@EqualsAndHashCode
public final class FinalDocumentVerificationFinishedEventData implements EventData {

    @NonNull
    private String documentVerificationId;

    @NonNull
    private String status;

    private String rejectReason;

    private String errorDetail;

    @NonNull
    private String provider;

    @NonNull
    private List<String> documentIds;
}

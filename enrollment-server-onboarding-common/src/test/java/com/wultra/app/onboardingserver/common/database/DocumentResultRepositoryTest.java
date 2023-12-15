/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.common.database;


import com.wultra.app.onboardingserver.common.configuration.ProvidersEvaluationContextExtension;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DocumentResultRepository}.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@DataJpaTest
@Import(ProvidersEvaluationContextExtension.class)
@ActiveProfiles("test")
@Sql
class DocumentResultRepositoryTest {

    @Autowired
    DocumentResultRepository tested;

    @Test
    void testProviderNameImplicitlyApplied() {
        assertThat(tested.streamAllInProgressDocumentSubmits())
                .extracting(DocumentResultEntity::getDocumentVerification)
                .extracting(DocumentVerificationEntity::getProviderName)
                .containsOnly("innovatrics");
    }

}

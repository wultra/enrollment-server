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

package com.wultra.app.enrollmentserver.database;

import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the operation localization templates.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Repository
public interface OperationTemplateRepository extends CrudRepository<OperationTemplateEntity, Long> {

    /**
     * Find an operation template by the given language and operation type.
     *
     * @param language language
     * @param placeholder operation type
     * @return operation template or empty
     */
    Optional<OperationTemplateEntity> findFirstByLanguageAndPlaceholder(String language, String placeholder);

    /**
     * Find an operation template by the given operation type.
     * <p>
     * Just a fallback method when no entry found by {@link #findFirstByLanguageAndPlaceholder(String, String)}.
     *
     * @param placeholder operation type
     * @return operation template or empty
     * @see #findFirstByLanguageAndPlaceholder(String, String)
     */
    Optional<OperationTemplateEntity> findFirstByPlaceholder(String placeholder);

}

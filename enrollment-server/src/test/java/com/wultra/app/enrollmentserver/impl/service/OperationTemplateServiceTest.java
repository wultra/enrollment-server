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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.database.OperationTemplateRepository;
import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test for {@link OperationTemplateService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class OperationTemplateServiceTest {

    @Mock
    private OperationTemplateRepository dao;

    @InjectMocks
    private OperationTemplateService tested;

    @Test
    void testFindTemplate_givenLanguage() {
        final OperationTemplateEntity entity = new OperationTemplateEntity();
        when(dao.findFirstByLanguageAndPlaceholder("cs", "myTemplate"))
                .thenReturn(Optional.of(entity));

        final Optional<OperationTemplateEntity> result = tested.findTemplate("myTemplate", "cs");

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void testFindTemplate_fallbackToEnglish() {
        final OperationTemplateEntity entity = new OperationTemplateEntity();
        when(dao.findFirstByLanguageAndPlaceholder("cs", "myTemplate"))
                .thenReturn(Optional.empty());
        when(dao.findFirstByLanguageAndPlaceholder("en", "myTemplate"))
                .thenReturn(Optional.of(entity));

        final Optional<OperationTemplateEntity> result = tested.findTemplate("myTemplate", "cs");

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void testFindTemplate_fallbackToAnyLanguage() {
        final OperationTemplateEntity entity = new OperationTemplateEntity();
        when(dao.findFirstByLanguageAndPlaceholder("cs", "myTemplate"))
                .thenReturn(Optional.empty());
        when(dao.findFirstByLanguageAndPlaceholder("en", "myTemplate"))
                .thenReturn(Optional.empty());
        when(dao.findFirstByPlaceholder("myTemplate"))
                .thenReturn(Optional.of(entity));

        final Optional<OperationTemplateEntity> result = tested.findTemplate("myTemplate", "cs");

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void testFindTemplate_fallbackToAnyLanguage_optimizationOfEnglishLocale() {
        final OperationTemplateEntity entity = new OperationTemplateEntity();
        when(dao.findFirstByLanguageAndPlaceholder("en", "myTemplate"))
                .thenReturn(Optional.empty());
        when(dao.findFirstByPlaceholder("myTemplate"))
                .thenReturn(Optional.of(entity));

        final Optional<OperationTemplateEntity> result = tested.findTemplate("myTemplate", "en");

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());

        verify(dao, times(1)).findFirstByLanguageAndPlaceholder("en", "myTemplate");
    }

    @Test
    void testFindTemplate_notFound() {
        when(dao.findFirstByLanguageAndPlaceholder("cs", "myTemplate"))
                .thenReturn(Optional.empty());
        when(dao.findFirstByLanguageAndPlaceholder("en", "myTemplate"))
                .thenReturn(Optional.empty());
        when(dao.findFirstByPlaceholder("myTemplate"))
                .thenReturn(Optional.empty());

        final Optional<OperationTemplateEntity> result = tested.findTemplate("myTemplate", "cs");

        assertFalse(result.isPresent());
    }
}

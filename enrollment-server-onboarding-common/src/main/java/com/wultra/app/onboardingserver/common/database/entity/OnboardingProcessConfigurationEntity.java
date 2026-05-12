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

package com.wultra.app.onboardingserver.common.database.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity representing configuration of the onboarding process.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Getter
@Setter
@Entity
@Table(name = "es_onboarding_process_configuration")
public class OnboardingProcessConfigurationEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 382525509835056799L;

    @Id
    @SequenceGenerator(name = "es_onboarding_process_configuration", sequenceName = "es_onboarding_process_configuration_seq")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "es_onboarding_process_configuration")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "process_type", nullable = false)
    private String processType;

    @Column(name = "config", nullable = false, columnDefinition = "TEXT")
    @Valid
    @Convert(converter = OnboardingProcessConfigurationValueConverter.class)
    private OnboardingProcessConfigurationValue configuration;

}


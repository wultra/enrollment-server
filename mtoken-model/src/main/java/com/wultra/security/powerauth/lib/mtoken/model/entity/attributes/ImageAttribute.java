/*
 * PowerAuth Mobile Token Model
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
package com.wultra.security.powerauth.lib.mtoken.model.entity.attributes;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Attribute represents an image that can be rendered on a mobile application.
 * Consists of a URL to the thumbnail and the higher resolution,
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ImageAttribute extends Attribute {

    /**
     * A mandatory URL to the thumbnail, the image in lower resolution.
     */
    private String thumbnailUrl;

    /**
     * An optional URL to the original image in higher resolution.
     */
    private String originalUrl;

    /**
     * No-arg constructor.
     */
    public ImageAttribute() {
        super(Type.IMAGE);
    }

    /**
     * Constructor with all details.
     * @param id Attribute ID.
     * @param label Attribute label.
     * @param thumbnailUrl URL to the thumbnail, the image in lower resolution. Mandatory.
     * @param originalUrl  URL to the original image in higher resolution. May be {@code null}.
     */
    public ImageAttribute(String id, String label, String thumbnailUrl, String originalUrl) {
        this();
        this.id = id;
        this.label = label;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
    }
}

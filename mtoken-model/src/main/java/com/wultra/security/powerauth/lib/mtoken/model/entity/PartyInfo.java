package com.wultra.security.powerauth.lib.mtoken.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class representing party information.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartyInfo {

    private String logoUrl;
    private String name;
    private String description;
    private String websiteUrl;

}

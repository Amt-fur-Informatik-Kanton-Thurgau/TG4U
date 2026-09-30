// SPDX-FileCopyrightText: 2026 Ubique Innovation AG and Heidi contributors
// SPDX-License-Identifier: Apache-2.0

package org.heidiverse.heidi.platformapi.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.heidiverse.heidi.platformapi.extensions.authflow.Tg4uIssuerAuthFeignClient;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticIssuerFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.mock.env.MockEnvironment;

class ConfigurationPropertyBindingTest {

    @Test
    void bindsIssuerClientsToCanonicalProperty() {
        var environment = new MockEnvironment()
                .withProperty("heidi.platform.issuer-internal-base-url", "http://issuer");

        assertThat(environment.resolveRequiredPlaceholders(urlOf(Tg4uIssuerAuthFeignClient.class)))
                .isEqualTo("http://issuer");
        assertThat(environment.resolveRequiredPlaceholders(urlOf(StaticIssuerFeignClient.class)))
                .isEqualTo("http://issuer");
    }

    private static String urlOf(Class<?> type) {
        return type.getAnnotation(FeignClient.class).url();
    }
}

// SPDX-FileCopyrightText: 2026 Ubique Innovation AG and Heidi contributors
// SPDX-License-Identifier: Apache-2.0

package org.heidiverse.heidi.issuer.extensions.authflow

import java.util.function.Supplier
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ConfigurationPropertyBindingTest {

    private val runner = ApplicationContextRunner()
        .withPropertyValues("heidi.issuer.api-key=canonical-test-key")
        .withBean(AuthFlowService::class.java, Supplier { mock(AuthFlowService::class.java) })
        .withBean(Tg4uAuthorizationService::class.java, Supplier { mock(Tg4uAuthorizationService::class.java) })
        .withBean(AuthFlowController::class.java)
        .withBean(Tg4uAuthorizationController::class.java)

    @Test
    fun bindsIssuerApiKeyToCanonicalProperty() {
        runner.run { context ->
            assertThat(context).hasNotFailed()

            val flow = context.getBean(AuthFlowController::class.java)
            val service = context.getBean(AuthFlowService::class.java)
            val flowId = UUID.randomUUID()

            assertThatCode { flow.delete("canonical-test-key", flowId) }
                .doesNotThrowAnyException()
            assertThatThrownBy { flow.delete("wrong-key", flowId) }
                .isInstanceOf(IllegalArgumentException::class.java)
            verify(service).delete(flowId)

            val authorization = context.getBean(Tg4uAuthorizationController::class.java)
            assertThatCode { authorization.context("canonical-test-key", "state") }
                .doesNotThrowAnyException()
            assertThatThrownBy { authorization.context("wrong-key", "state") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}

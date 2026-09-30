/*
 * Copyright 2025 Ubique Innovation AG
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.heidiverse.heidi.platformapi.extensions;

import org.heidiverse.heidi.platformapi.extensions.authflow.Tg4uAuthFlowController;
import org.heidiverse.heidi.platformapi.extensions.authflow.Tg4uAuthorizationCallbackController;
import org.heidiverse.heidi.platformapi.extensions.authflow.Tg4uAuthorizationProcessExtension;
import org.heidiverse.heidi.platformapi.extensions.authflow.Tg4uIssuerAuthFeignClient;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowController;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowDataService;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowEntity;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowRepository;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowService;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticIssuanceMigration;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticIssuancePublicController;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticIssuerFeignClient;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowController;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowDataService;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowEntity;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowRepository;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowService;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeOfferController;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeProcessExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * TG4U platform features packaged as a normal Spring Boot extension JAR.
 *
 * <p>The explicit imports are deliberate: an extension must not depend on a
 * broad component scan from the OSS application. This keeps the public
 * application independent from product package names and makes the JAR
 * usable by other Heidi distributions.
 */
@AutoConfiguration
@EntityScan(basePackageClasses = {
        StaticFlowEntity.class,
        StaticQrCodeFlowEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        StaticFlowRepository.class,
        StaticQrCodeFlowRepository.class
})
@EnableFeignClients(clients = {
        StaticIssuerFeignClient.class,
        Tg4uIssuerAuthFeignClient.class
})
@Import({
        Tg4uAuthFlowController.class,
        Tg4uAuthorizationCallbackController.class,
        Tg4uAuthorizationProcessExtension.class,
        StaticFlowController.class,
        StaticFlowDataService.class,
        StaticFlowService.class,
        StaticIssuanceMigration.class,
        StaticIssuancePublicController.class,
        StaticQrCodeOfferController.class,
        StaticQrCodeFlowController.class,
        StaticQrCodeFlowDataService.class,
        StaticQrCodeFlowService.class,
        StaticQrCodeProcessExtension.class
})
public class Tg4uPlatformAutoConfiguration {
}

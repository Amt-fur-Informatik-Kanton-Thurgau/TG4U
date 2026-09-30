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

import feign.Retryer;
import feign.codec.ErrorDecoder;

import org.heidiverse.heidi.shared.feign.RetryableStatusErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import java.util.Set;

public class Tg4uIssuerFeignClientConfiguration {

    @Bean
    Retryer retryer() {
        return new Retryer.Default();
    }

    @Bean
    ErrorDecoder errorDecoder() {
        return new RetryableStatusErrorDecoder(
                Set.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        HttpStatus.BAD_GATEWAY,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        HttpStatus.GATEWAY_TIMEOUT));
    }
}

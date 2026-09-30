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

import { IconRoute } from "@tabler/icons-react";
import "./tg4u-theme.css";
import { linkOptions } from "@tanstack/react-router";
import { createElement } from "react";
import { FormattedMessage } from "react-intl";
import { UserRole } from "@/lib/auth/identity";
import { registerExtension } from "@/lib/extensions";

/**
 * TG4U's issuance and verification extensions for the Heidi Platform cockpit.
 */
registerExtension({
  id: "tg4u-cockpit",
  dashboardSegments: [
    {
      blockKey: "extensions",
      segments: [
        {
          title: createElement(FormattedMessage, {
            id: "pages.settings.features.staticIssuanceFlows",
            defaultMessage: "Static Issuance Flows",
          }),
          description: createElement(FormattedMessage, {
            id: "pages.overview.staticFlows.description",
            defaultMessage: "Create reusable static issuance flows",
          }),
          icon: IconRoute,
          link: linkOptions({ to: "/static-flows" }),
          allowedRoles: [UserRole.SuperAdmin, UserRole.Admin, UserRole.Operator],
          requiredFeature: "staticIssuanceFlows",
        },
      ],
    },
  ],
  featureGroups: [
    {
      key: "extensions",
      labelId: "pages.settings.features.groups.extensions",
      defaultMessage: "Extensions",
      features: [
        {
          key: "staticIssuanceFlows",
          labelId: "pages.settings.features.staticIssuanceFlows",
          defaultMessage: "Static Issuance Flows",
          defaultValue: true,
        },
      ],
    },
  ],
  navGroups: [
    {
      groupKey: "extensions",
      labelId: "pages.settings.features.groups.extensions",
      defaultMessage: "Extensions",
      links: [
        {
          link: linkOptions({ to: "/static-flows" }),
          icon: IconRoute,
          requiredFeature: "staticIssuanceFlows",
          requiredRole: "operator",
          labelId: "pages.settings.features.staticIssuanceFlows",
          defaultMessage: "Static Issuance Flows",
        },
      ],
    },
  ],
  messages: {
    de: () => import("./translations/de.json").then((m) => m.default),
    en: () => import("./translations/en.json").then((m) => m.default),
    fr: () => import("./translations/fr.json").then((m) => m.default),
    it: () => import("./translations/it.json").then((m) => m.default),
  },
});

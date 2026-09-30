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

import { runtimeConfig } from "@tg4u/tg4u-web-extension/runtime-config";
import { fetchWithRedirect } from "@/lib/utils";
import type {
  AttributeNameOverrides,
  AttributeType,
} from "@/types/credential-schema";

export async function getStaticFlowsByTenantId(tenantId: string) {
  const res = await fetchWithRedirect(
    new URL(
      `management/v1/static-flow/overview?tenantId=${tenantId}`,
      runtimeConfig.heidiApiBaseUrl,
    ),
  );
  if (!res.ok) {
    throw new Error(`Could not fetch Static Flows! ${res.statusText}`);
  }
  return ((await res.json()) as { staticFlows: StaticFlow[] }).staticFlows;
}

export async function getStaticFlowById(uuid: string) {
  const res = await fetchWithRedirect(
    new URL(
      `management/v1/static-flow/${uuid}`,
      runtimeConfig.heidiApiBaseUrl,
    ),
  );
  if (!res.ok) {
    throw new Error(`Could not fetch Static Flow! ${res.statusText}`);
  }
  return (await res.json()) as StaticFlow;
}

export async function createStaticFlow(staticFlow: StaticFlowCreate) {
  const res = await fetchWithRedirect(
    new URL("management/v1/static-flow", runtimeConfig.heidiApiBaseUrl),
    {
      method: "POST",
      body: JSON.stringify(staticFlow),
      headers: { "Content-Type": "application/json" },
    },
  );
  if (!res.ok) {
    throw new Error(`Could not create Static Flow! ${res.statusText}`);
  }
  return (await res.json()) as StaticFlow;
}

export async function updateStaticFlow(staticFlow: StaticFlow) {
  const res = await fetchWithRedirect(
    new URL(`management/v1/static-flow`, runtimeConfig.heidiApiBaseUrl),
    {
      method: "PUT",
      body: JSON.stringify(staticFlow),
      headers: { "Content-Type": "application/json" },
    },
  );
  if (!res.ok) {
    throw new Error(`Could not update Static Flow! ${res.statusText}`);
  }
  return (await res.json()) as StaticFlow;
}

export async function deleteStaticFlow(uuid: string) {
  const res = await fetchWithRedirect(
    new URL(
      `management/v1/static-flow/${uuid}`,
      runtimeConfig.heidiApiBaseUrl,
    ),
    { method: "DELETE" },
  );
  if (!res.ok) {
    throw new Error(`Could not delete Static Flow! ${res.statusText}`);
  }
}

export type StaticFlow = {
  displayName: string;
  uuid: string;
  credentialScheme: {
    id: string;
    credentialIdentifier: string;
    version: string;
    issuanceProfileId?: string;
  };
  issuerSlug: string;
  attributes: Record<
    string,
    {
      value: string;
      isArray: boolean;
      attributeType: AttributeType;
      attributeNameOverrides: AttributeNameOverrides;
    }
  >;
  txCode: string;
};

type StaticFlowCreate = Omit<StaticFlow, "uuid" | "txCode"> & {
  includeTxCode: boolean;
};

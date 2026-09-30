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

import { useQueries, useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useAtomValue } from "jotai";
import { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { z } from "zod";
import { CredentialSchemaCombobox } from "@/components/common/credential-schema-combobox";
import { Loading } from "@/components/common/loading";
import { PageHeader } from "@/components/common/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  schemaListOptions,
  schemaOptions,
} from "@/lib/api/credential-schemas/query-options";
import type { IssuerDefinition } from "@/lib/api/issuer-definitions/api";
import { issuerDefinitionOptions } from "@/lib/api/issuer-definitions/query-options";
import { useCreateStaticFlowMutation } from "@tg4u/tg4u-web-extension/lib/api/static-flows/mutations";
import { localeAtom, selectedTenantAtom } from "@/lib/atoms";
import {
  AttributeArrayField,
  useAttributeArrayForm,
} from "@/components/common/credential-attributes/array-attribute-field";
import { AttributeField } from "@/components/common/credential-attributes/attribute-field";
import {
  type CredentialSchema,
  type CredentialSchemaLite,
  CredentialSchemaState,
} from "@/types/credential-schema";

export const Route = createFileRoute("/_authenticated/static-flows/draft")({
  component: RouteComponent,
  loader({ context: { intl } }) {
    return {
      crumb: intl.$t({
        id: "common.draft",
        defaultMessage: "Draft",
      }),
    };
  },
});

function RouteComponent() {
  const selectedTenant = useAtomValue(selectedTenantAtom);
  const { crumb } = Route.useLoaderData();
  const { data: credentialSchemas } = useSuspenseQuery(
    schemaListOptions({
      includeImages: false,
      statesToExclude: [
        CredentialSchemaState.Archived,
        CredentialSchemaState.Created,
      ],
    }),
  );
  const [selectedCredentialSchema, setselectedCredentialSchema] =
    useState<CredentialSchemaLite>();
  const [
    { data: selectedCredentialSchemaDetail, isPending: isSchemaPending },
    { data: issuer, isPending: isIssuerPending },
  ] = useQueries({
    queries: [
      {
        ...schemaOptions({ schemaId: selectedCredentialSchema?.id ?? "" }),
        enabled: !!selectedCredentialSchema?.id,
      },
      {
        ...issuerDefinitionOptions({
          id: selectedCredentialSchema?.issuerSettings.id ?? 0,
        }),
        enabled: !!selectedCredentialSchema?.issuerSettings.id,
      },
    ],
  });

  const isPending = isSchemaPending || isIssuerPending;

  const [open, onOpenChange] = useState(false);

  return (
    <>
      <PageHeader heading={selectedCredentialSchema?.displayName ?? crumb}>
        <CredentialSchemaCombobox
          popoverProps={{ open, onOpenChange }}
          schemas={credentialSchemas.filter(
            (s) => s.tenantId === selectedTenant,
          )}
          commandItemProps={(schema) => ({
            onSelect() {
              setselectedCredentialSchema(schema);
              onOpenChange(false);
            },
          })}
        >
          <FormattedMessage
            id="credentialSchemaCombobox.select"
            defaultMessage="Select Credential Schema"
          />
        </CredentialSchemaCombobox>
      </PageHeader>
      {selectedCredentialSchema ? (
        <div>
          {isPending ? (
            <Loading />
          ) : (
            <div className="mx-auto my-8 w-full max-w-xl">
              <FormComponent
                schema={selectedCredentialSchemaDetail!}
                issuer={issuer!}
              />
            </div>
          )}
        </div>
      ) : (
        <Card className="mt-4 flex justify-center border-dashed border-muted-foreground/50 py-5 text-muted-foreground">
          <FormattedMessage
            id="pages.staticFlow.draft.select"
            defaultMessage="Select a Credential Schema."
          />
        </Card>
      )}
    </>
  );
}

function FormComponent({
  schema,
  issuer,
}: {
  schema: CredentialSchema;
  issuer: IssuerDefinition;
}) {
  const { $t } = useIntl();
  const navigate = Route.useNavigate();
  const locale = useAtomValue(localeAtom);
  const formSchema = z.object({
    attributes: z.record(
      z.string(),
      z.union([z.string(), z.array(z.string())]),
    ),
    includeTxCode: z.boolean(),
    displayName: z.string().min(
      1,
      $t({
        id: "pages.staticFlow.validation.displayNameRequired",
        defaultMessage: "Display name is required",
      }),
    ),
  });
  const form = useAttributeArrayForm({
    validators: {
      onSubmit: formSchema,
    },
    defaultValues: {
      attributes: Object.fromEntries(
        Object.values(schema.attributes).map((a) => {
          if (a.isArray) {
            return [a.name, [""]];
          }
          return [a.name, ""];
        }),
      ) as Record<string, string | string[]>,
      includeTxCode: false,
      displayName: "",
    },
    async onSubmit({ value }) {
      await createStaticFlow(
        {
          displayName: value.displayName,
          attributes: Object.fromEntries(
            Object.entries(value.attributes).map(([key, value], i) => [
              key,
              {
                attributeNameOverrides:
                  schema.attributes[i]!.attributeNameOverrides,
                isArray: Array.isArray(value) ?? false,
                attributeType: schema.attributes[i]!.type,
                value: Array.isArray(value) ? JSON.stringify(value) : value,
              },
            ]),
          ),
          credentialScheme: {
            id: schema.id,
            credentialIdentifier: schema.credentialIdentifier,
            version: schema.version,
          },
          issuerSlug: issuer.slug,
          includeTxCode: value.includeTxCode,
        },
        {
          onSuccess: () => {
            navigate({ to: "/static-flows" });
          },
        },
      );
    },
  });

  const { mutateAsync: createStaticFlow } = useCreateStaticFlowMutation();

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        form.handleSubmit();
      }}
      className="flex flex-col gap-4"
    >
      <Card className="flex flex-col gap-4">
        <CardTitle>
          <FormattedMessage id="common.metadata" defaultMessage="Metadata" />
        </CardTitle>
        <form.Field name="displayName">
          {(field) => {
            const isInvalid =
              field.state.meta.isTouched && !field.state.meta.isValid;
            return (
              <Field data-invalid={isInvalid}>
                <FieldLabel htmlFor={field.name}>
                  <FormattedMessage
                    id="common.displayName"
                    defaultMessage="Display Name"
                  />
                </FieldLabel>
                <Input
                  id={field.name}
                  name={field.name}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  aria-invalid={isInvalid}
                  className="w-full"
                  placeholder={$t(
                    {
                      id: "common.enter.withValue",
                      defaultMessage: "Enter {value}",
                    },
                    {
                      value: $t({
                        id: "common.displayName",
                        defaultMessage: "Display Name",
                      }),
                    },
                  )}
                />
                {isInvalid && <FieldError errors={field.state.meta.errors} />}
              </Field>
            );
          }}
        </form.Field>
        <form.Field name="includeTxCode">
          {(field) => {
            const isInvalid =
              field.state.meta.isTouched && !field.state.meta.isValid;
            return (
              <Field orientation="inline" data-invalid={isInvalid}>
                <Checkbox
                  id={field.name}
                  name={field.name}
                  checked={field.state.value}
                  onCheckedChange={(checked) =>
                    field.handleChange(checked === true)
                  }
                  onBlur={field.handleBlur}
                  aria-invalid={isInvalid}
                />
                <FieldLabel className="font-normal" htmlFor={field.name}>
                  <FormattedMessage
                    id="pages.staticFlow.includeTransactionCode"
                    defaultMessage="Include Transaction Code"
                  />
                </FieldLabel>
                {isInvalid && <FieldError errors={field.state.meta.errors} />}
              </Field>
            );
          }}
        </form.Field>
      </Card>

      <Card className="flex flex-col gap-4">
        <CardTitle>
          <FormattedMessage
            id="common.attributes"
            defaultMessage="Attributes"
          />
        </CardTitle>
        {schema.attributes.map((attr) => {
          if (attr.isArray) {
            return (
              <form.Field
                key={attr.id}
                name={`attributes.${attr.name}`}
              >
                {(field) => {
                  if (!Array.isArray(field.state.value)) {
                    return null;
                  }
                  return (
                    <AttributeArrayField
                      fieldName={attr.name}
                      type={attr.type}
                      label={
                        attr.displayName[locale] ??
                        Object.values(attr.displayName)[0] ??
                        ""
                      }
                      values={field.state.value}
                      onChange={field.handleChange}
                    />
                  );
                }}
              </form.Field>
            );
          }

          const label =
            attr.displayName[locale] ??
            Object.values(attr.displayName)[0] ??
            attr.name;

          return (
            <form.Field key={attr.name} name={`attributes.${attr.name}`}>
              {(field) => {
                if (Array.isArray(field.state.value)) {
                  return null;
                }
                return (
                  <AttributeField
                    type={attr.type}
                    fieldName={attr.name}
                    label={label}
                    onValueChange={field.handleChange}
                    value={field.state.value}
                  />
                );
              }}
            </form.Field>
          );
        })}
      </Card>

      <Button type="submit" className="self-end">
        <FormattedMessage
          id="pages.staticFlow.submit"
          defaultMessage="submit"
        />
      </Button>
    </form>
  );
}

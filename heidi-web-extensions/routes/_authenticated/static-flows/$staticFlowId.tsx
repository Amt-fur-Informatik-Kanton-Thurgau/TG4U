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

import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { FormattedMessage, useIntl } from "react-intl";
import { z } from "zod";
import { PageHeader } from "@/components/common/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import type { StaticFlow } from "@tg4u/tg4u-web-extension/lib/api/static-flows/api";
import { useUpdateStaticFlowMutation } from "@tg4u/tg4u-web-extension/lib/api/static-flows/mutations";
import { staticFlowOptions } from "@tg4u/tg4u-web-extension/lib/api/static-flows/query-options";
import {
  AttributeArrayField,
  useAttributeArrayForm,
} from "@/components/common/credential-attributes/array-attribute-field";
import { AttributeField } from "@/components/common/credential-attributes/attribute-field";

export const Route = createFileRoute(
  "/_authenticated/static-flows/$staticFlowId",
)({
  component: RouteComponent,
  async loader({ context: { queryClient }, params }) {
    const flow = await queryClient.ensureQueryData(
      staticFlowOptions({ uuid: params.staticFlowId }),
    );
    return {
      crumb: flow.displayName || flow.credentialScheme.credentialIdentifier,
    };
  },
});
function RouteComponent() {
  const { crumb } = Route.useLoaderData();
  const { staticFlowId } = Route.useParams();
  const { data: staticFlow } = useSuspenseQuery(
    staticFlowOptions({ uuid: staticFlowId }),
  );

  return (
    <>
      <PageHeader heading={crumb} />
      <div className="mx-auto my-8 w-full max-w-xl">
        <FormComponent staticFlow={staticFlow} />
      </div>
    </>
  );
}

function FormComponent({ staticFlow }: { staticFlow: StaticFlow }) {
  const { $t } = useIntl();
  const navigate = Route.useNavigate();
  const formSchema = z.object({
    attributes: z.record(
      z.string(),
      z.union([z.string(), z.array(z.string())]),
    ),
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
      displayName: staticFlow.displayName,
      attributes: Object.fromEntries(
        Object.entries(staticFlow.attributes).map(([key, a]) => {
          if (a.isArray) {
            try {
              return [key, JSON.parse(a.value)];
            } catch {
              return [key, [""]];
            }
          }
          return [key, a.value];
        }),
      ) as Record<string, string | string[]>,
    },
    async onSubmit({ value }) {
      updateStaticFlow(
        {
          ...staticFlow,
          displayName: value.displayName,
          attributes: Object.fromEntries(
            Object.entries(value.attributes)
              .map(([key, value]) => {
                const attr = staticFlow.attributes[key];
                if (!attr) {
                  return null;
                }

                return [
                  key,
                  {
                    ...attr,
                    value: attr.isArray
                      ? JSON.stringify(value)
                      : (value as string),
                  },
                ];
              })
              .filter(Boolean),
          ),
        },
        {
          onSuccess: () => {
            navigate({ to: "/static-flows" });
          },
        },
      );
    },
  });
  const { mutateAsync: updateStaticFlow } = useUpdateStaticFlowMutation();

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
      </Card>
      <Card className="flex flex-col gap-4">
        <CardTitle>
          <FormattedMessage
            id="common.attributes"
            defaultMessage="Attributes"
          />
        </CardTitle>
        {Object.entries(staticFlow.attributes).map(
          ([attributeName, attribute], _i) => {
            if (attribute.isArray) {
              return (
                <form.Field
                  key={attributeName}
                  name={`attributes.${attributeName}`}
                >
                  {(field) => {
                    if (!Array.isArray(field.state.value)) {
                      return null;
                    }
                    return (
                      <AttributeArrayField
                        fieldName={attributeName}
                        type={attribute.attributeType}
                        label={attributeName}
                        values={field.state.value}
                        onChange={field.handleChange}
                      />
                    );
                  }}
                </form.Field>
              );
            }

            return (
              <form.Field
                key={attributeName}
                name={`attributes.${attributeName}`}
              >
                {(field) => {
                  if (Array.isArray(field.state.value)) {
                    return null;
                  }
                  return (
                    <AttributeField
                      type={attribute.attributeType}
                      fieldName={attributeName}
                      label={attributeName}
                      onValueChange={field.handleChange}
                      value={field.state.value}
                    />
                  );
                }}
              </form.Field>
            );
          },
        )}
      </Card>
      <Button type="submit">
        <FormattedMessage id="common.save" defaultMessage="Save" />
      </Button>
    </form>
  );
}

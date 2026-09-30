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

import {
  IconArchive,
  IconDots,
  IconFileTypePng,
  IconFileTypeSvg,
  IconPencil,
  IconPlus,
  IconQrcode,
} from "@tabler/icons-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, getRouteApi, Link } from "@tanstack/react-router";
import { useAtomValue } from "jotai";
import { generate } from "lean-qr";
import { toSvgDataURL } from "lean-qr/extras/svg";
import { QRCodeSVG } from "qrcode.react";
import { FormattedMessage, useIntl } from "react-intl";
import { PageHeader } from "@/components/common/page-header";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useDeleteStaticFlowMutation } from "@tg4u/tg4u-web-extension/lib/api/static-flows/mutations";
import { staticFlowOverviewOptions } from "@tg4u/tg4u-web-extension/lib/api/static-flows/query-options";
import { selectedTenantAtom } from "@/lib/atoms";
import { runtimeConfig } from "@tg4u/tg4u-web-extension/runtime-config";

export const Route = createFileRoute("/_authenticated/static-flows/")({
  loader({ context }) {
    context.queryClient.ensureQueryData(staticFlowOverviewOptions());
  },
  component: RouteComponent,
});

const routeApi = getRouteApi("/_authenticated/static-flows");

function RouteComponent() {
  const selectedTenant = useAtomValue(selectedTenantAtom);
  const { data: staticFlows } = useSuspenseQuery(
    staticFlowOverviewOptions(selectedTenant ?? ""),
  );
  const { crumb } = routeApi.useLoaderData();
  const { $t } = useIntl();
  const { mutate: deleteStaticFlow } = useDeleteStaticFlowMutation();
  return (
    <>
      <PageHeader heading={crumb}>
        <Button asChild>
          <Link to="/static-flows/draft">
            <IconPlus />
            <FormattedMessage
              values={{
                pronoun: "masculine",
                value: $t({
                  id: "common.staticFlow",
                  defaultMessage: "Static Flow",
                }),
              }}
              id="common.new.withValue"
              defaultMessage="New {value}"
            />
          </Link>
        </Button>
      </PageHeader>
      <div className="flex flex-col gap-2">
        {staticFlows.length === 0 ? (
          <Card className="mt-4 flex justify-center border-dashed border-muted-foreground/50 py-5 text-muted-foreground">
            <FormattedMessage
              id="pages.staticFlow.empty"
              defaultMessage="No Static Flows found."
            />
          </Card>
        ) : (
          staticFlows.map((flow) => {
            const qrCodeUrl = `openid-credential-offer://?credential_offer_uri=${encodeURIComponent(
              new URL(
                `public/v1/static-flow/credential-offer/${flow.uuid}`,
                runtimeConfig.heidiApiBaseUrl,
              ).toString(),
            )}`;
            const qrCode = generate(qrCodeUrl);
            return (
              <Card key={flow.uuid} className="flex items-center gap-2">
                <div className="ml-1">
                  <h2 className="inline-flex flex-wrap items-center gap-x-2.5 text-lg font-semibold">
                    {flow.displayName ||
                      flow.credentialScheme.credentialIdentifier}
                    {flow.txCode && (
                      <Badge
                        variant="secondary"
                        className="inline-flex gap-2.5 pl-0.5"
                      >
                        <Badge variant="secondary" className="bg-surface">
                          <FormattedMessage
                            id="common.txCode"
                            defaultMessage="TX-Code"
                          />
                        </Badge>
                        <span className="text-base leading-none font-normal tracking-widest">
                          {flow.txCode}
                        </span>
                      </Badge>
                    )}
                  </h2>
                  <p className="text-sm text-muted-foreground">
                    {flow.credentialScheme.credentialIdentifier} &middot;{" "}
                    {flow.credentialScheme.version}
                  </p>
                </div>
                <Button variant="secondary" asChild className="ml-auto">
                  <Link
                    to="/static-flows/$staticFlowId"
                    params={{ staticFlowId: flow.uuid }}
                  >
                    <IconPencil />
                    <FormattedMessage id="common.edit" defaultMessage="Edit" />
                  </Link>
                </Button>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="size-10 shrink-0 p-0">
                      <IconDots />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuLabel>
                      {flow.credentialScheme.credentialIdentifier}
                    </DropdownMenuLabel>
                    <DropdownMenuSeparator />
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <DropdownMenuItem
                          onSelect={(e) => e.preventDefault()}
                          variant="destructive"
                        >
                          <IconArchive />
                          <FormattedMessage
                            id="common.archive"
                            defaultMessage="Archive"
                          />
                        </DropdownMenuItem>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>
                            <FormattedMessage
                              id="common.archive.withValue"
                              defaultMessage="Archive {value}"
                              values={{ value: "" }}
                            />
                          </AlertDialogTitle>
                          <AlertDialogDescription>
                            <FormattedMessage
                              id="pages.staticFlow.archive.description"
                              defaultMessage="Are you sure you want to archive this Static Flow?"
                            />
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel asChild>
                            <Button variant="outline">
                              <FormattedMessage
                                id="common.cancel"
                                defaultMessage="Cancel"
                              />
                            </Button>
                          </AlertDialogCancel>
                          <AlertDialogAction
                            variant="destructive"
                            onClick={() => deleteStaticFlow(flow.uuid)}
                          >
                            <FormattedMessage
                              id="common.archive"
                              defaultMessage="Archive"
                            />
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                    <DropdownMenuSub>
                      <DropdownMenuSubTrigger>
                        <IconQrcode />
                        <FormattedMessage
                          id="pages.staticFlow.displayQrCode"
                          defaultMessage="Display QR-Code"
                        />
                      </DropdownMenuSubTrigger>
                      <DropdownMenuSubContent className="rounded-xl">
                        <QRCodeSVG className="size-48 p-1" value={qrCodeUrl} />
                      </DropdownMenuSubContent>
                    </DropdownMenuSub>

                    <DropdownMenuItem
                      onSelect={() => {
                        const svg = toSvgDataURL(qrCode, {
                          padX: 0,
                          padY: 0,
                          width: 256,
                          height: 256,
                        });
                        const link = document.createElement("a");
                        link.href = svg;
                        // vscode has weird bug when passing literal string "svg" into template string
                        const fileType = "svg";
                        link.download = `${flow.credentialScheme.credentialIdentifier}${flow.txCode ? `_${flow.txCode}` : ""}.${fileType}`;
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                      }}
                    >
                      <IconFileTypeSvg />
                      <FormattedMessage
                        id="common.download.withValue"
                        defaultMessage="Download {value}"
                        values={{
                          value: $t({
                            id: "pages.staticFlow.qrCode.svg",
                            defaultMessage: "QR-Code as SVG",
                          }),
                        }}
                      />
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      onSelect={() => {
                        const png = qrCode.toDataURL({
                          padX: 0,
                          padY: 0,
                          scale: 6,
                        });
                        const link = document.createElement("a");
                        link.href = png;
                        link.download = `${flow.credentialScheme.credentialIdentifier}${flow.txCode ? `_${flow.txCode}` : ""}.png`;
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                      }}
                    >
                      <IconFileTypePng />
                      <FormattedMessage
                        id="common.download.withValue"
                        defaultMessage="Download {value}"
                        values={{
                          value: $t({
                            id: "pages.staticFlow.qrCode.png",
                            defaultMessage: "QR-Code as PNG",
                          }),
                        }}
                      />
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </Card>
            );
          })
        )}
      </div>
    </>
  );
}

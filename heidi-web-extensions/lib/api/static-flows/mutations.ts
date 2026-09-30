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

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useIntl } from "react-intl";
import { toast } from "sonner";
import {
  createStaticFlow,
  deleteStaticFlow,
  updateStaticFlow,
} from "@tg4u/tg4u-web-extension/lib/api/static-flows/api";
import {
  staticFlowOptions,
  staticFlowOverviewOptions,
} from "@tg4u/tg4u-web-extension/lib/api/static-flows/query-options";

export function useCreateStaticFlowMutation() {
  const queryClient = useQueryClient();
  const { $t } = useIntl();
  return useMutation({
    mutationFn: createStaticFlow,
    onSuccess() {
      queryClient.invalidateQueries(staticFlowOverviewOptions());
      toast.success(
        $t({
          id: "pages.staticFlow.toast.create.success",
          defaultMessage: "Static flow created successfully",
        }),
      );
    },
    onError(error) {
      toast.error(
        $t({
          id: "pages.staticFlow.toast.create.error",
          defaultMessage: "Failed to create static flow",
        }),
        {
          description: error.message,
        },
      );
    },
  });
}

export function useUpdateStaticFlowMutation() {
  const queryClient = useQueryClient();
  const { $t } = useIntl();
  return useMutation({
    mutationFn: updateStaticFlow,
    onSuccess(data) {
      queryClient.invalidateQueries(
        staticFlowOptions({
          uuid: data.uuid,
        }),
      );
      toast.success(
        $t({
          id: "pages.staticFlow.toast.update.success",
          defaultMessage: "Static flow updated successfully",
        }),
      );
    },
    onError(error) {
      toast.error(
        $t({
          id: "pages.staticFlow.toast.update.error",
          defaultMessage: "Failed to update static flow",
        }),
        {
          description: error.message,
        },
      );
    },
  });
}

export function useDeleteStaticFlowMutation() {
  const queryClient = useQueryClient();
  const { $t } = useIntl();
  return useMutation({
    mutationFn: deleteStaticFlow,
    onSuccess() {
      queryClient.invalidateQueries(staticFlowOverviewOptions());
      toast.success(
        $t({
          id: "pages.staticFlow.toast.delete.success",
          defaultMessage: "Static flow deleted successfully",
        }),
      );
    },
    onError(error) {
      toast.error(
        $t({
          id: "pages.staticFlow.toast.delete.error",
          defaultMessage: "Failed to delete static flow",
        }),
        {
          description: error.message,
        },
      );
    },
  });
}

// src/pages/OrganizationPage.tsx
import React, { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import OrganizationTable from "../shared/components/OrganizationTable";
import CreateOrganizationModal from "../shared/components/CreateOrganizationModal";
import EditOrganizationModal from "../shared/components/EditOrganizationModal";

import {
  fetchOrganizations,
  createOrganization,
  updateOrganization,
  setOrganizationActive,
  type Organization,
  type OrganizationCreateRequest,
  type OrganizationUpdateRequest,
  type OrganizationType,
  type FetchOrganizationsResponse,
} from "../shared/services/organizationService";

const PAGE_SIZE = 20;

interface Props {
  isSystemAdmin: boolean;
  userOrgId: string;
  myRoleInOrg?: string | null;
}

const OrganizationPage: React.FC<Props> = ({
  isSystemAdmin,
  userOrgId,
  myRoleInOrg,
}) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [filters, setFilters] = useState<{
    search: string;
    active?: boolean;
    orgType: "" | OrganizationType;
  }>({ search: "", active: undefined, orgType: "" });

  const [creating, setCreating] = useState(false);
  const [editingOrganization, setEditingOrganization] =
    useState<Organization | null>(null);

  const canEditOrganizations =
    isSystemAdmin || myRoleInOrg === "ADMIN" || myRoleInOrg === "PARTY_ADMIN";
  const canCreateOrganizations = isSystemAdmin;

  const { data, isLoading, isError, error, refetch } = useQuery<
    FetchOrganizationsResponse,
    Error
  >({
    queryKey: ["organizations", currentPage, filters, isSystemAdmin, userOrgId],
    queryFn: () =>
      fetchOrganizations({
        page: currentPage,
        size: PAGE_SIZE,
        search: filters.search,
        active: filters.active,
        orgType: filters.orgType === "" ? undefined : filters.orgType,
        orgId: isSystemAdmin ? undefined : userOrgId,
      }),
    placeholderData: (prev) => prev,
  });

  const createOrganizationMutation = useMutation<
    unknown,
    Error,
    OrganizationCreateRequest
  >({
    mutationFn: (org) => createOrganization(org),
    onSuccess: () => refetch(),
  });

  const updateOrganizationMutation = useMutation<
    unknown,
    Error,
    OrganizationUpdateRequest
  >({
    mutationFn: (org) => updateOrganization(org),
    onSuccess: () => refetch(),
  });

  const setOrganizationActiveMutation = useMutation<
    void,
    Error,
    { orgId: string; active: boolean }
  >({
    mutationFn: ({ orgId, active }) => setOrganizationActive({ orgId, active }),
    onSuccess: () => refetch(),
  });

  const handleCreate = (newOrg: OrganizationCreateRequest) => {
    createOrganizationMutation.mutate(newOrg);
    setCreating(false);
  };

  const handleUpdate = (updatedOrg: OrganizationUpdateRequest) => {
    updateOrganizationMutation.mutate(updatedOrg);
    setEditingOrganization(null);
  };

  const handleToggleActive = (organization: Organization) => {
    setOrganizationActiveMutation.mutate({
      orgId: organization.orgId,
      active: !organization.active,
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Organization Management</h1>

        {canCreateOrganizations && (
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setCreating(true);
            }}
            className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
          >
            Create New Organization
          </button>
        )}
      </div>

      <OrganizationTable
        organizations={data?.items ?? []}
        totalItems={data?.totalElements ?? 0}
        currentPage={currentPage}
        onPageChange={setCurrentPage}
        onEdit={
          canEditOrganizations
            ? (org) => {
                setEditingOrganization(org);
              }
            : null
        }
        onToggleActive={isSystemAdmin ? handleToggleActive : null}
        isLoading={isLoading}
      />

      {/* ✅ Render modals in a fixed “top layer” container */}
      <div className="fixed inset-0 z-[99999] pointer-events-none">
        <div className="pointer-events-auto">
          {creating && (
            <CreateOrganizationModal
              onClose={() => setCreating(false)}
              onSubmit={handleCreate}
              isLoading={createOrganizationMutation.isPending}
            />
          )}

          {editingOrganization && (
            <EditOrganizationModal
              organization={editingOrganization}
              onClose={() => setEditingOrganization(null)}
              onSubmit={handleUpdate}
              isLoading={updateOrganizationMutation.isPending}
            />
          )}
        </div>
      </div>

      {isError && <p className="text-red-600">Error: {error.message}</p>}
    </div>
  );
};

export default OrganizationPage;

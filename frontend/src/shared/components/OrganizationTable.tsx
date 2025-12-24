// src/shared/components/OrganizationTable.tsx
import React from "react";
import type { Organization } from "../services/organizationService";

interface OrganizationTableProps {
  organizations: Organization[];
  totalItems: number;
  currentPage: number;
  onPageChange: (page: number) => void;
  onEdit: ((organization: Organization) => void) | null;
  onToggleActive: ((organization: Organization) => void) | null;
  isLoading: boolean;
}

const PAGE_SIZE = 20;

const OrganizationTable: React.FC<OrganizationTableProps> = ({
  organizations,
  totalItems,
  currentPage,
  onPageChange,
  onEdit,
  onToggleActive,
  isLoading,
}) => {
  const safeOrgs = Array.isArray(organizations) ? organizations : [];

  const canPrev = currentPage > 0;
  const canNext =
    safeOrgs.length > 0 && (currentPage + 1) * PAGE_SIZE < totalItems;

  return (
    <div className="w-full">
      {/* Loading */}
      {isLoading && (
        <div className="rounded-xl border bg-white p-6 shadow-sm">
          <p className="text-sm text-gray-600">Loading organizations...</p>
        </div>
      )}

      {/* Empty */}
      {!isLoading && safeOrgs.length === 0 && (
        <div className="rounded-xl border bg-white p-6 text-center shadow-sm">
          <p className="text-sm text-gray-600">No organizations found.</p>
        </div>
      )}

      {/* Table */}
      {!isLoading && safeOrgs.length > 0 && (
        <div className="overflow-hidden rounded-xl border bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full table-fixed border-collapse">
              <thead className="bg-gray-50">
                <tr className="text-left text-xs font-semibold uppercase tracking-wide text-gray-600">
                  <th className="w-[32%] px-4 py-3">Name</th>
                  <th className="w-[18%] px-4 py-3">Type</th>
                  <th className="w-[20%] px-4 py-3">Party</th>
                  <th className="w-[15%] px-4 py-3">Status</th>
                  <th className="w-[15%] px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>

              <tbody className="divide-y">
                {safeOrgs.map((org) => (
                  <tr key={org.orgId} className="hover:bg-gray-50">
                    {/* Name */}
                    <td className="px-4 py-3">
                      <div className="truncate font-medium text-gray-900">
                        {org.orgName}
                      </div>
                    </td>

                    <td className="px-4 py-3 text-sm text-gray-700">
                      <span className="truncate">{org.organizationType}</span>
                    </td>

                    <td className="px-4 py-3 text-sm text-gray-700">
                      <span className="truncate">{org.partyName ?? "—"}</span>
                    </td>

                    <td className="px-4 py-3">
                      {org.active ? (
                        <span className="inline-flex items-center rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-700">
                          Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full bg-gray-200 px-3 py-1 text-xs font-semibold text-gray-700">
                          Inactive
                        </span>
                      )}
                    </td>

                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        {onEdit && (
                          <button
                            type="button"
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              onEdit(org);
                            }}
                            className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                          >
                            Edit
                          </button>
                        )}

                        {onToggleActive && (
                          <button
                            type="button"
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              onToggleActive(org);
                            }}
                            className={`rounded-md px-3 py-1.5 text-sm font-semibold text-white ${
                              org.active
                                ? "bg-red-600 hover:bg-red-700"
                                : "bg-blue-600 hover:bg-blue-700"
                            }`}
                          >
                            {org.active ? "Deactivate" : "Activate"}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex flex-col gap-3 border-t bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-gray-600">
              Page <span className="font-medium">{currentPage + 1}</span> •
              Showing{" "}
              <span className="font-medium">{currentPage * PAGE_SIZE + 1}</span>{" "}
              –{" "}
              <span className="font-medium">
                {Math.min((currentPage + 1) * PAGE_SIZE, totalItems)}
              </span>{" "}
              of <span className="font-medium">{totalItems}</span>
            </p>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  onPageChange(currentPage - 1);
                }}
                disabled={!canPrev}
                className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Previous
              </button>

              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  onPageChange(currentPage + 1);
                }}
                disabled={!canNext}
                className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrganizationTable;

// src/shared/components/UserTable.tsx
import React from "react";
import type { UserDto } from "../types/userTypes";

interface UserTableProps {
  users: UserDto[];
  totalItems: number;
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  isLoading: boolean;

  onEditUser: (user: UserDto) => void;
  onDeleteUser: (user: UserDto) => void;

  partyNameById: Record<string, string>;
  countyNameById: Record<string, string>;
}

function fmt(v?: string | null) {
  const s = (v ?? "").trim();
  return s.length ? s : "—";
}

function getDateCreated(u: any): string {
  const raw =
    u.dateCreated ??
    u.createdAt ??
    u.created_at ??
    u.date_created ??
    u.createdOn ??
    u.created_on ??
    null;

  if (!raw) return "—";
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) return String(raw);
  return d.toLocaleString();
}

const UserTable: React.FC<UserTableProps> = ({
  users,
  totalItems,
  currentPage,
  totalPages,
  onPageChange,
  isLoading,
  onEditUser,
  onDeleteUser,
  partyNameById,
  countyNameById,
}) => {
  return (
    <div className="w-full text-left">
      <div className="text-xs text-gray-600 mb-2">
        Total: <span className="font-semibold">{totalItems}</span> • Page{" "}
        <span className="font-semibold">{currentPage + 1}</span> of{" "}
        <span className="font-semibold">{Math.max(totalPages, 1)}</span>
      </div>

      {/* Responsive container */}
      <div className="w-full overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <table className="min-w-[1100px] w-full table-auto border-collapse">
          <thead className="bg-gray-100">
            <tr className="divide-x divide-gray-200">
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Name
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Email
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Phone
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Position
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Role
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Party
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                County
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap">
                Created
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap text-center">
                Active
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap text-center">
                Verified
              </th>
              <th className="px-4 py-3 text-xs font-bold text-gray-700 border-b border-gray-300 whitespace-nowrap text-center">
                Actions
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-gray-200">
            {isLoading ? (
              <tr>
                <td colSpan={11} className="px-4 py-4 text-sm text-gray-500">
                  Loading...
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={11} className="px-4 py-4 text-sm text-gray-500">
                  No users found.
                </td>
              </tr>
            ) : (
              users.map((u) => {
                const partyName = u.partyId
                  ? partyNameById[u.partyId] ?? "Unknown"
                  : "None";

                const countyName = u.assignedCountyId
                  ? countyNameById[u.assignedCountyId] ?? "Unknown"
                  : "None";

                return (
                  <tr
                    key={u.userId}
                    className="odd:bg-white even:bg-gray-50 hover:bg-gray-100 divide-x divide-gray-200"
                  >
                    <td className="px-4 py-3 text-sm whitespace-nowrap">
                      <div className="font-semibold text-gray-900">
                        {fmt(u.firstName)} {fmt(u.lastName)}
                      </div>
                    </td>

                    <td className="px-4 py-3 text-sm whitespace-nowrap">
                      {fmt(u.email)}
                    </td>

                    <td className="px-4 py-3 text-sm whitespace-nowrap">
                      {fmt(u.phoneNumber)}
                    </td>

                    <td className="px-4 py-3 text-sm whitespace-nowrap">
                      {fmt(u.position)}
                    </td>

                    <td className="px-4 py-3 text-sm font-semibold whitespace-nowrap">
                      {fmt(u.roleName)}
                    </td>

                    <td className="px-4 py-3 text-sm whitespace-nowrap">
                      {partyName}
                    </td>

                    <td className="px-4 py-3 text-sm whitespace-nowrap">
                      {countyName}
                    </td>

                    <td className="px-4 py-3 text-xs text-gray-600 whitespace-nowrap">
                      {getDateCreated(u)}
                    </td>

                    <td className="px-4 py-3 text-center whitespace-nowrap">
                      <input
                        type="checkbox"
                        checked={!!u.isActive}
                        readOnly
                        className="h-4 w-4 accent-blue-600"
                      />
                    </td>

                    <td className="px-4 py-3 text-center whitespace-nowrap">
                      <input
                        type="checkbox"
                        checked={!!u.isVerified}
                        readOnly
                        className="h-4 w-4 accent-blue-600"
                      />
                    </td>

                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex justify-center gap-2">
                        <button
                          onClick={() => onEditUser(u)}
                          className="px-3 py-1.5 text-xs rounded-md border border-gray-300 bg-white hover:bg-gray-50"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => onDeleteUser(u)}
                          className="px-3 py-1.5 text-xs rounded-md border border-red-300 bg-white text-red-700 hover:bg-red-50"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center gap-2">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
          className="px-3 py-2 text-sm rounded-md border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Previous
        </button>

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage + 1 >= totalPages}
          className="px-3 py-2 text-sm rounded-md border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Next
        </button>
      </div>
    </div>
  );
};

export default UserTable;

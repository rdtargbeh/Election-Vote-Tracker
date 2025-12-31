// src/shared/components/CreateOrganizationModal.tsx
import React, { useState } from "react";
import type {
  OrganizationCreateRequest,
  OrganizationType,
} from "../services/organizationService";

type Props = {
  onClose: () => void;
  onSubmit: (org: OrganizationCreateRequest) => void;
  isLoading: boolean;
};

const ORG_TYPES: OrganizationType[] = [
  "POLITICAL_PARTY",
  "COALITION",
  "NEC",
  "NGO",
  "MEDIA",
  "OTHER",
];

const inputClass =
  "w-full rounded-xl border border-gray-300 bg-white px-4 py-3.5 text-sm text-gray-900 shadow-sm outline-none " +
  "focus:border-blue-500 focus:ring-4 focus:ring-blue-100";

const labelClass = "block text-sm font-semibold text-gray-800";
const helpClass = "text-xs text-gray-500 leading-relaxed";
const fieldWrap = "space-y-2";

const CreateOrganizationModal: React.FC<Props> = ({
  onClose,
  onSubmit,
  isLoading,
}) => {
  const [newOrg, setNewOrg] = useState<OrganizationCreateRequest>({
    orgName: "",
    subdomain: "",
    organizationType: "NGO",
    logoUrl: "",
    primaryColor: "",
    isActive: true,
    partyId: undefined,
  });

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const el = e.currentTarget;
    const name = el.name as keyof OrganizationCreateRequest;

    if (el instanceof HTMLInputElement && el.type === "checkbox") {
      setNewOrg((prev) => ({ ...prev, [name]: el.checked as any }));
      return;
    }

    setNewOrg((prev) => ({ ...prev, [name]: el.value as any }));
  };

  const handleSubmit = () => {
    onSubmit({
      ...newOrg,
      orgName: newOrg.orgName.trim(),
      subdomain: newOrg.subdomain?.trim() || "",
      logoUrl: newOrg.logoUrl?.trim() || "",
      primaryColor: newOrg.primaryColor?.trim() || "",
    });
  };

  return (
    <div className="fixed inset-0 z-[99999]">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/60"
        onMouseDown={onClose}
        aria-hidden="true"
      />

      {/* Centering wrapper */}
      <div className="relative flex min-h-full items-center justify-center p-4 sm:p-8">
        <div
          className="w-full max-w-3xl overflow-hidden rounded-2xl bg-white shadow-2xl ring-1 ring-black/10"
          onMouseDown={(e) => e.stopPropagation()}
          role="dialog"
          aria-modal="true"
        >
          {/* Header */}
          <div className="flex items-start justify-between gap-4 border-b px-6 py-5 sm:px-8">
            <div>
              <h2 className="text-xl font-bold text-gray-900 sm:text-2xl">
                Create New Organization
              </h2>
              <p className="mt-1.5 text-sm text-gray-600">
                Provide details to register a tenant organization.
              </p>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="rounded-xl p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
              aria-label="Close"
            >
              ✕
            </button>
          </div>

          {/* Body */}
          <div className="max-h-[78vh] overflow-y-auto px-6 py-6 sm:px-8">
            <form className="space-y-8" onSubmit={(e) => e.preventDefault()}>
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                <div className={`md:col-span-2 ${fieldWrap}`}>
                  <label className={labelClass}>Organization Name</label>
                  <input
                    type="text"
                    name="orgName"
                    value={newOrg.orgName}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="Unity Party"
                    required
                  />
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Subdomain</label>
                  <input
                    type="text"
                    name="subdomain"
                    value={newOrg.subdomain ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="e.g., unityparty"
                  />
                  <p className={helpClass}>Used for tenant routing.</p>
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Organization Type</label>
                  <select
                    name="organizationType"
                    value={newOrg.organizationType}
                    onChange={handleChange}
                    className={inputClass}
                    required
                  >
                    {ORG_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Logo URL</label>
                  <input
                    type="text"
                    name="logoUrl"
                    value={newOrg.logoUrl ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="https://..."
                  />
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Primary Color</label>
                  <input
                    type="text"
                    name="primaryColor"
                    value={newOrg.primaryColor ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="#1D4ED8"
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="flex items-start gap-3 rounded-xl border border-gray-200 bg-gray-50 px-5 py-4">
                    <input
                      type="checkbox"
                      name="isActive"
                      checked={!!newOrg.isActive}
                      onChange={handleChange}
                      className="mt-1 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-200"
                    />
                    <div className="space-y-1">
                      <div className="text-sm font-semibold text-gray-900">
                        Active
                      </div>
                      <div className="text-xs text-gray-600">
                        Toggle organization access.
                      </div>
                    </div>
                  </label>
                </div>
              </div>
            </form>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 border-t bg-white px-6 py-4 sm:px-8">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-gray-300 px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>

            <button
              type="button"
              onClick={handleSubmit}
              disabled={isLoading}
              className="rounded-xl bg-blue-600 px-6 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 disabled:opacity-60"
            >
              {isLoading ? "Creating..." : "Create"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CreateOrganizationModal;

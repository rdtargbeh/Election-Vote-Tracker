// src/shared/components/EditOrganizationModal.tsx
import React, { useEffect, useMemo, useState } from "react";
import type {
  Organization,
  OrganizationType,
  OrganizationUpdateRequest,
} from "../services/organizationService";
import { fetchParties, type PartyDto } from "../services/partyService";

type Props = {
  organization: Organization;
  onSubmit: (updated: OrganizationUpdateRequest) => void;
  onClose: () => void;
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

const EditOrganizationModal: React.FC<Props> = ({
  organization,
  onSubmit,
  onClose,
  isLoading,
}) => {
  const [editedOrg, setEditedOrg] = useState<OrganizationUpdateRequest>({
    orgId: organization.orgId,
    orgName: organization.orgName,
    organizationType: organization.organizationType,
    partyId: organization.partyId ?? null,
    subdomain: organization.subdomain ?? null,
    logoUrl: organization.logoUrl ?? null,
    primaryColor: organization.primaryColor ?? null,
    active: organization.active,
  });

  const [parties, setParties] = useState<PartyDto[]>([]);
  const [isLoadingParties, setIsLoadingParties] = useState(false);

  // Prefer org.partyName for display (best UX), fallback to blank.
  const [selectedPartyName, setSelectedPartyName] = useState<string>(
    organization.partyName ?? ""
  );

  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = prev;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [onClose]);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        setIsLoadingParties(true);
        const data = await fetchParties();
        if (mounted) setParties(Array.isArray(data) ? data : []);
      } finally {
        if (mounted) setIsLoadingParties(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  // If org has partyId but no partyName, map after parties load.
  useEffect(() => {
    if (!editedOrg.partyId) return;
    if (selectedPartyName) return;
    if (parties.length === 0) return;

    const match = parties.find((p) => p.partyId === editedOrg.partyId);
    if (match) setSelectedPartyName(match.partyName);
  }, [editedOrg.partyId, parties, selectedPartyName]);

  const resolvedPartyId = useMemo(() => {
    if (!selectedPartyName) return null;
    return (
      parties.find((p) => p.partyName === selectedPartyName)?.partyId ?? null
    );
  }, [selectedPartyName, parties]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const el = e.currentTarget;
    const name = el.name as keyof OrganizationUpdateRequest;

    if (el instanceof HTMLInputElement && el.type === "checkbox") {
      setEditedOrg((prev) => ({ ...prev, [name]: el.checked as any }));
      return;
    }

    setEditedOrg((prev) => ({ ...prev, [name]: el.value as any }));
  };

  const handleSubmit = () => {
    onSubmit({
      ...editedOrg,
      orgName: editedOrg.orgName?.trim() || undefined,
      subdomain: editedOrg.subdomain?.trim()
        ? editedOrg.subdomain.trim()
        : null,
      logoUrl: editedOrg.logoUrl?.trim() ? editedOrg.logoUrl.trim() : null,
      primaryColor: editedOrg.primaryColor?.trim()
        ? editedOrg.primaryColor.trim()
        : null,
      partyId: resolvedPartyId,
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
          className="w-full max-w-5xl overflow-hidden rounded-2xl bg-white shadow-2xl ring-1 ring-black/10"
          onMouseDown={(e) => e.stopPropagation()}
          role="dialog"
          aria-modal="true"
        >
          {/* Header */}
          <div className="flex items-start justify-between gap-4 border-b px-6 py-5 sm:px-8">
            <div>
              <h2 className="text-xl font-bold text-gray-900 sm:text-2xl">
                Edit Organization
              </h2>
              <p className="mt-1.5 text-sm text-gray-600">
                Update organization profile and status.
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
                    value={editedOrg.orgName ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                  />
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Subdomain</label>
                  <input
                    type="text"
                    name="subdomain"
                    value={editedOrg.subdomain ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                  />
                  <p className={helpClass}>Used for tenant routing.</p>
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Organization Type</label>
                  <select
                    name="organizationType"
                    value={editedOrg.organizationType ?? "NGO"}
                    onChange={handleChange}
                    className={inputClass}
                  >
                    {ORG_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Party (optional)</label>
                  <select
                    value={selectedPartyName}
                    onChange={(e) =>
                      setSelectedPartyName(e.currentTarget.value)
                    }
                    className={inputClass}
                  >
                    <option value="">None</option>
                    {parties.map((p) => (
                      <option key={p.partyId} value={p.partyName}>
                        {p.partyName}
                      </option>
                    ))}
                  </select>
                  {isLoadingParties && (
                    <p className={helpClass}>Loading parties…</p>
                  )}
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Logo URL</label>
                  <input
                    type="text"
                    name="logoUrl"
                    value={editedOrg.logoUrl ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                  />
                </div>

                <div className={fieldWrap}>
                  <label className={labelClass}>Primary Color</label>
                  <input
                    type="text"
                    name="primaryColor"
                    value={editedOrg.primaryColor ?? ""}
                    onChange={handleChange}
                    className={inputClass}
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="flex items-start gap-3 rounded-xl border border-gray-200 bg-gray-50 px-5 py-4">
                    <input
                      type="checkbox"
                      name="active"
                      checked={!!editedOrg.active}
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
              {isLoading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditOrganizationModal;

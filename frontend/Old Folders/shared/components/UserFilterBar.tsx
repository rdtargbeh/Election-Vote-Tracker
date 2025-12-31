import React, { useEffect, useState } from "react";

interface UserFilterBarProps {
  filters: {
    search: string;
    active?: boolean;
    roleName: string;
  };
  onChange: (filters: {
    search: string;
    active?: boolean;
    roleName: string;
  }) => void;
}

const UserFilterBar: React.FC<UserFilterBarProps> = ({ filters, onChange }) => {
  const [local, setLocal] = useState(filters);

  useEffect(() => setLocal(filters), [filters]);

  return (
    <div
      style={{ display: "flex", gap: 8, marginBottom: 12, flexWrap: "wrap" }}
    >
      <input
        type="text"
        placeholder="Search name/email/username"
        value={local.search}
        onChange={(e) => setLocal({ ...local, search: e.target.value })}
        style={{ padding: "8px 10px", minWidth: 240 }}
      />

      <select
        value={local.active === undefined ? "" : String(local.active)}
        onChange={(e) => {
          const v = e.target.value;
          setLocal({
            ...local,
            active: v === "" ? undefined : v === "true",
          });
        }}
        style={{ padding: "8px 10px" }}
      >
        <option value="">All Status</option>
        <option value="true">Active</option>
        <option value="false">Inactive</option>
      </select>

      <button onClick={() => onChange(local)} style={{ padding: "8px 12px" }}>
        Apply
      </button>

      <button
        onClick={() =>
          onChange({ search: "", active: undefined, roleName: "" })
        }
        style={{ padding: "8px 12px" }}
      >
        Reset
      </button>
    </div>
  );
};

export default UserFilterBar;

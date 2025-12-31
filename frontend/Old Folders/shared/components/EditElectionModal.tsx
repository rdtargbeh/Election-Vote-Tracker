import React, { useState } from "react";
import type {
  ElectionDto,
  ElectionType,
  ElectionUpdateRequest,
} from "../services/electionService";

interface Props {
  election: ElectionDto;
  onClose: () => void;
  onSubmit: (data: ElectionUpdateRequest) => Promise<void>;
  isLoading: boolean;
}

const ELECTION_TYPES: { value: ElectionType; label: string }[] = [
  { value: "PRESIDENTIAL", label: "Presidential" },
  { value: "LEGISLATIVE", label: "Legislative" },
  { value: "SENATORIAL", label: "Senatorial" },
  { value: "REPRESENTATIVE", label: "Representative" },
  { value: "REFERENDUM", label: "Referendum" },
  { value: "PRESIDENTIAL_GENERAL", label: "Presidential General" },
  { value: "BY_ELECTION", label: "By-election" },
  { value: "LOCAL", label: "Local" },
];

const EditElectionModal: React.FC<Props> = ({
  election,
  onClose,
  onSubmit,
  isLoading,
}) => {
  const [electionName, setElectionName] = useState(election.electionName);
  const [year, setYear] = useState<number>(election.year);
  const [electionType, setElectionType] = useState<ElectionType>(
    election.electionType
  );
  const [isActive, setIsActive] = useState<boolean>(election.isActive);

  const handleSubmit = async () => {
    if (!electionName.trim() || !year || !electionType) {
      alert("Please fill in all required fields.");
      return;
    }

    await onSubmit({
      electionName: electionName.trim(),
      year: Number(year),
      electionType,
      isActive,
    });
  };

  return (
    <div className="fixed inset-0 z-[999999] flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-lg">
        <h2 className="text-lg font-semibold text-gray-900">Edit Election</h2>

        <div className="mt-4 space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700">
              Election Name
            </label>
            <input
              className="mt-1 w-full rounded-lg border px-3 py-2"
              value={electionName}
              onChange={(e) => setElectionName(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">
              Year
            </label>
            <input
              className="mt-1 w-full rounded-lg border px-3 py-2"
              type="number"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">
              Type
            </label>
            <select
              className="mt-1 w-full rounded-lg border px-3 py-2"
              value={electionType}
              onChange={(e) => setElectionType(e.target.value as ElectionType)}
            >
              {ELECTION_TYPES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>

          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
            />
            Active
          </label>
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <button
            onClick={onClose}
            disabled={isLoading}
            className="rounded-lg border px-4 py-2 text-sm"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={isLoading}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {isLoading ? "Saving..." : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default EditElectionModal;

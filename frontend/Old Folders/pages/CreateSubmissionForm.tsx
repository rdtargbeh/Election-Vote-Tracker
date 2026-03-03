// src/pages/CreateSubmissionForm.tsx
import React, { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../shared/lib/store/authStore";
import { captureGeolocation } from "../utility/geo";

/**
 * CreateSubmissionForm — final: per-file types + multipart upload + optional geo
 *
 * Key points:
 * - Files state holds { file, type } so each file can have an associated FileType.
 * - FormData includes:
 *     - "payload" => JSON (VoteSubmissionCreateRequest)
 *     - "files" => one part per file
 *     - "fileTypes" => JSON array of strings (fileType per file, same order)
 * - includeLocation checkbox controls geolocation prompt
 * - Frontend enforces MAX_FILES (VITE_UPLOAD_MAX_FILES || 10)
 */

type Id = string;
type CandidateDto = {
  candidateId: Id;
  displayName: string;
  partyAbbrev?: string | null;
  centerId?: Id | null;
};
type CountyDto = { countyId: Id; countyName: string };
type DistrictDto = { districtId: Id; districtName: string };
type PollingCenterDto = { centerId: Id; centerName: string };
type PollingPlaceDto = { placeId: Id; placeName?: string; label?: string };
type ElectionDto = { electionId: Id; electionName: string; year?: number };
type PartyDto = {
  partyId: Id;
  abbreviation?: string | null;
  abbrev?: string | null;
};

const API_BASE =
  (import.meta.env.VITE_API_BASE_URL as string) ?? "http://localhost:8080/api";
const DEBUG_NETWORK = false;
const UUID_RE =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

// Frontend file limit (defaults to 10)
const MAX_FILES = Number(import.meta.env.VITE_UPLOAD_MAX_FILES ?? 10) || 10;

// FileType values (must match server enum names)
const FILE_TYPE_OPTIONS = [
  "TALLY_SHEET",
  "PHOTO",
  "AUDIO",
  "VIDEO",
  "DOCUMENT",
] as const;
type FileType = (typeof FILE_TYPE_OPTIONS)[number];

function buildUrl(path: string) {
  if (/^https?:\/\//i.test(path)) return path;
  const base = API_BASE.replace(/\/$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${base}${p}`;
}

function tryParseJwtClaims(token?: string) {
  if (!token) return null;
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(payload)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export default function CreateSubmissionForm(): React.ReactElement {
  const qc = useQueryClient();
  const navigate = useNavigate();

  // auth detection (same heuristics as before)
  const storeToken = useAuthStore(
    (s: any) => s.token ?? s.accessToken ?? s.idToken ?? null
  );
  const currentUser = useAuthStore((s: any) => s.currentUser ?? null);
  const storeSnapshot = (useAuthStore as any).getState
    ? (useAuthStore as any).getState()
    : null;

  const detectToken = () => {
    if (storeToken && typeof storeToken === "string")
      return storeToken.replace(/^Bearer\s+/i, "");
    try {
      const w: any = window as any;
      if (w.__evt?.token)
        return String(w.__evt.token).replace(/^Bearer\s+/i, "");
      if (w.evt?.token) return String(w.evt.token).replace(/^Bearer\s+/i, "");
    } catch {}
    const keys = [
      "token",
      "authToken",
      "accessToken",
      "idToken",
      "evt.token",
      "evtToken",
    ];
    for (const k of keys) {
      const v = localStorage.getItem(k);
      if (v) return String(v).replace(/^Bearer\s+/i, "");
    }
    return null;
  };
  const detectedToken = detectToken();
  const tokenClaims = tryParseJwtClaims(detectedToken ?? undefined);

  const detectOrgId = () => {
    try {
      if (storeSnapshot) {
        const c =
          storeSnapshot.currentOrgId ??
          storeSnapshot.orgId ??
          storeSnapshot.defaultOrgId ??
          null;
        if (c) return String(c);
      }
    } catch {}
    try {
      const w: any = window as any;
      if (w.__evt?.currentOrgId) return String(w.__evt.currentOrgId);
      if (w.evt?.currentOrgId) return String(w.evt.currentOrgId);
    } catch {}
    const keys = [
      "currentOrgId",
      "evt.currentOrgId",
      "orgId",
      "current_org_id",
    ];
    for (const k of keys) {
      const v = localStorage.getItem(k);
      if (v) return v;
    }
    return null;
  };
  const detectedOrgId = detectOrgId();

  const detectAgentCandidate = () => {
    try {
      if (
        currentUser &&
        (currentUser.id || currentUser.userId || currentUser.user_id)
      )
        return String(
          currentUser.id ?? currentUser.userId ?? currentUser.user_id
        );
    } catch {}
    if (tokenClaims) {
      const maybe =
        tokenClaims.userId ??
        tokenClaims.userID ??
        tokenClaims.user_id ??
        tokenClaims.id ??
        tokenClaims.sub ??
        null;
      if (maybe) return String(maybe);
    }
    const keys = ["currentUserId", "userId", "user_id", "agentId", "username"];
    for (const k of keys) {
      const v = localStorage.getItem(k);
      if (v) return v;
    }
    return null;
  };

  const [agentId, setAgentId] = useState<string | null>(detectAgentCandidate());
  const [manualAgentUuid, setManualAgentUuid] = useState<string>("");

  // form fields
  const [countyId, setCountyId] = useState<Id | null>(null);
  const [districtId, setDistrictId] = useState<Id | null>(null);
  const [centerId, setCenterId] = useState<Id | null>(null);
  const [placeId, setPlaceId] = useState<Id | null>(null);
  const [electionId, setElectionId] = useState<Id | null>(null);

  const [candidateVotes, setCandidateVotes] = useState<Record<Id, string>>({});
  const [invalidBallots, setInvalidBallots] = useState<string>("0");
  const [blankBallots, setBlankBallots] = useState<string>("0");
  const [rejectedBallots, setRejectedBallots] = useState<string>("0");
  const [spoiledBallots, setSpoiledBallots] = useState<string>("0");
  const [ballotsCastOverride, setBallotsCastOverride] = useState<string | null>(
    null
  );
  const [comments, setComments] = useState<string>("");

  // files: each item holds the File object + chosen FileType
  const [files, setFiles] = useState<Array<{ file: File; type: FileType }>>([]);
  const [includeLocation, setIncludeLocation] = useState<boolean>(false);

  const [profileDebug, setProfileDebug] = useState<any | null>(null);
  const [serverValidationErrors, setServerValidationErrors] = useState<
    any | null
  >(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // number helpers
  const parseNumber = (v: string | number | undefined | null) => {
    if (v === null || v === undefined) return 0;
    if (typeof v === "number") return Number.isFinite(v) ? v : 0;
    const s = String(v).trim();
    if (s === "") return 0;
    const n = Number(s);
    return Number.isFinite(n) ? n : 0;
  };

  const computedBallotsCast = useMemo(() => {
    const candidateSum = Object.values(candidateVotes).reduce(
      (s, v) => s + parseNumber(v),
      0
    );
    return (
      candidateSum +
      parseNumber(invalidBallots) +
      parseNumber(blankBallots) +
      parseNumber(rejectedBallots) +
      parseNumber(spoiledBallots)
    );
  }, [
    candidateVotes,
    invalidBallots,
    blankBallots,
    rejectedBallots,
    spoiledBallots,
  ]);

  const ballotsCast =
    ballotsCastOverride === null
      ? computedBallotsCast
      : parseNumber(ballotsCastOverride);

  // fetch wrapper
  const fetchJson = async (path: string, options?: RequestInit) => {
    const url = buildUrl(path);
    const headers: Record<string, string> = { Accept: "application/json" };
    if (detectedToken) headers["Authorization"] = `Bearer ${detectedToken}`;
    const merged: RequestInit = {
      credentials: "include",
      ...options,
      headers: {
        ...headers,
        ...(options?.headers as Record<string, string> | undefined),
      },
    };
    if (DEBUG_NETWORK)
      console.debug("[fetchJson]", merged.method ?? "GET", url, merged.headers);
    const res = await fetch(url, merged);
    const txt = await res.text().catch(() => "");
    let body: any = txt;
    try {
      body = txt ? JSON.parse(txt) : null;
    } catch {
      body = txt;
    }
    if (!res.ok) {
      const e: any = new Error(`HTTP ${res.status}`);
      e.status = res.status;
      e.body = body;
      throw e;
    }
    if (
      body &&
      typeof body === "object" &&
      "content" in body &&
      Array.isArray(body.content)
    )
      return body.content;
    return body;
  };

  // lookups (same queries as previously used)
  const countiesQuery = useQuery<CountyDto[]>({
    queryKey: ["counties"],
    queryFn: async () => (await fetchJson("/counties")) as CountyDto[],
  });
  const districtsQuery = useQuery<DistrictDto[]>({
    queryKey: ["districts", countyId],
    enabled: !!countyId,
    queryFn: async () => {
      const params = new URLSearchParams();
      if (countyId) params.set("countyId", countyId);
      params.set("page", "0");
      params.set("size", "500");
      return (await fetchJson(
        `/districts?${params.toString()}`
      )) as DistrictDto[];
    },
  });
  const centersQuery = useQuery<PollingCenterDto[]>({
    queryKey: ["centers", countyId, districtId],
    enabled: !!countyId || !!districtId,
    queryFn: async () => {
      const params = new URLSearchParams();
      if (countyId) params.set("countyId", countyId);
      if (districtId) params.set("districtId", districtId);
      params.set("page", "0");
      params.set("size", "500");
      return (await fetchJson(
        `/polling-centers?${params.toString()}`
      )) as PollingCenterDto[];
    },
  });
  const placesQuery = useQuery<PollingPlaceDto[]>({
    queryKey: ["places", centerId],
    enabled: !!centerId,
    queryFn: async () =>
      (await fetchJson(
        `/polling-places/by-center/${centerId}`
      )) as PollingPlaceDto[],
  });
  const electionsQuery = useQuery<ElectionDto[]>({
    queryKey: ["elections", "active"],
    queryFn: async () =>
      (await fetchJson(`/elections?activeOnly=true`)) as ElectionDto[],
  });

  const partiesQuery = useQuery<PartyDto[]>({
    queryKey: ["parties"],
    queryFn: async () =>
      (await fetchJson(`/parties?page=0&size=500`)) as PartyDto[],
    staleTime: 60 * 60 * 1000,
  });
  const partyMap = useMemo(() => {
    const m: Record<string, string> = {};
    for (const p of partiesQuery.data ?? []) {
      if (p.partyId) m[String(p.partyId)] = p.abbreviation ?? p.abbrev ?? "";
    }
    return m;
  }, [partiesQuery.data]);

  const candidatesQuery = useQuery<CandidateDto[]>({
    queryKey: ["candidates", electionId, centerId],
    enabled: !!electionId,
    queryFn: async () => {
      const raw = (await fetchJson(
        `/election-candidates/${electionId}/candidates`
      )) as any[];
      if (!Array.isArray(raw)) return [];
      const mapped = raw.map((r: any) => {
        const pid = r.partyId ?? r.party?.partyId ?? r.party_id ?? null;
        const partyAbbrev =
          r.partyAbbrev ??
          r.party?.abbreviation ??
          r.party?.abbrev ??
          (pid ? partyMap[String(pid)] ?? null : null) ??
          null;
        return {
          candidateId:
            r.candidateId ?? r.candidate_id ?? r.id ?? String(Math.random()),
          displayName:
            r.fullName ?? r.full_name ?? r.displayName ?? r.name ?? "Unknown",
          partyAbbrev,
          centerId: r.centerId ?? r.center_id ?? r.pollingCenterId ?? null,
        } as CandidateDto;
      });
      if (centerId) {
        const filtered = mapped.filter(
          (c) => !c.centerId || c.centerId === centerId
        );
        if (filtered.length > 0) return filtered;
      }
      return mapped;
    },
  });

  useEffect(() => {
    const list = candidatesQuery.data ?? [];
    if (!Array.isArray(list)) return;
    setCandidateVotes((prev) => {
      const next = { ...prev };
      for (const c of list)
        if (!(c.candidateId in next)) next[c.candidateId] = "0";
      for (const k of Object.keys(next))
        if (!list.some((c) => c.candidateId === k)) delete next[k];
      return next;
    });
  }, [candidatesQuery.data]);

  // profile fetch
  const fetchProfileAndFindUuid = async (): Promise<string | null> => {
    setProfileDebug(null);
    const token = detectedToken;
    const orgId = detectedOrgId;
    const url = buildUrl("/users/me");
    try {
      if (DEBUG_NETWORK)
        console.debug("[profile] GET", url, "X-Org-Id:", orgId ?? "none");
      const res = await fetch(url, {
        method: "GET",
        credentials: "include",
        headers: {
          Accept: "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...(orgId ? { "X-Org-Id": orgId } : {}),
        },
      });
      const txt = await res.text().catch(() => "");
      let body: any = txt;
      try {
        if (txt) body = JSON.parse(txt);
        else body = null;
      } catch {
        body = txt;
      }
      setProfileDebug({ endpoint: "/users/me", status: res.status, body });
      if (res.ok) {
        const findUuid = (obj: any): string | null => {
          if (!obj) return null;
          if (typeof obj === "string") return UUID_RE.test(obj) ? obj : null;
          if (Array.isArray(obj)) {
            for (const v of obj) {
              const f = findUuid(v);
              if (f) return f;
            }
            return null;
          }
          if (typeof obj === "object") {
            for (const k of Object.keys(obj)) {
              const v = obj[k];
              if (typeof v === "string" && UUID_RE.test(v)) return v;
              const nested = findUuid(v);
              if (nested) return nested;
            }
          }
          return null;
        };
        const found = findUuid(body);
        if (found) {
          setAgentId(found);
          return found;
        }
      }
    } catch (err: any) {
      setProfileDebug({ endpoint: "/users/me", error: String(err) });
    }
    return null;
  };

  // numeric input handlers
  const onCandidateInputChange = (candidateId: Id, raw: string) =>
    setCandidateVotes((s) => ({
      ...s,
      [candidateId]: raw.replace(/[^\d]/g, ""),
    }));
  const handleNumericFocusClear = (
    setter: (v: string) => void,
    current: string
  ) => {
    if (current === "0") setter("");
  };
  const handleNumericBlurDefaultZero = (
    setter: (v: string) => void,
    current: string
  ) => {
    if (current.trim() === "") setter("0");
  };
  const onInvalidChange = (v: string) =>
    setInvalidBallots(v.replace(/[^\d]/g, ""));
  const onBlankChange = (v: string) => setBlankBallots(v.replace(/[^\d]/g, ""));
  const onRejectedChange = (v: string) =>
    setRejectedBallots(v.replace(/[^\d]/g, ""));
  const onSpoiledChange = (v: string) =>
    setSpoiledBallots(v.replace(/[^\d]/g, ""));

  const ensureAgentUuid = async (): Promise<string | null> => {
    if (manualAgentUuid && UUID_RE.test(manualAgentUuid.trim()))
      return manualAgentUuid.trim();
    if (agentId && UUID_RE.test(agentId)) return agentId;
    if (tokenClaims) {
      const maybe =
        tokenClaims.sub ??
        tokenClaims.id ??
        tokenClaims.userId ??
        tokenClaims.user_id ??
        null;
      if (maybe && typeof maybe === "string" && UUID_RE.test(maybe)) {
        setAgentId(String(maybe));
        return String(maybe);
      }
    }
    const found = await fetchProfileAndFindUuid();
    if (found && UUID_RE.test(found)) return found;
    return null;
  };

  // files selection: add new files (enforce MAX_FILES); default file type is TALLY_SHEET
  const handleFilesSelected = (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    const incoming = Array.from(fileList).map((f) => ({
      file: f as File,
      type: "TALLY_SHEET" as FileType,
    }));
    const combined = [...files, ...incoming];
    if (combined.length > MAX_FILES) {
      alert(
        `You can upload up to ${MAX_FILES} files. Please select fewer files.`
      );
      setFiles(combined.slice(0, MAX_FILES));
    } else {
      setFiles(combined);
    }
  };
  const removeFileAt = (index: number) =>
    setFiles((prev) => prev.filter((_, i) => i !== index));
  const changeFileTypeAt = (index: number, t: FileType) =>
    setFiles((prev) =>
      prev.map((p, i) => (i === index ? { file: p.file, type: t } : p))
    );

  // build multipart with fileTypes array (ordered)
  const buildFormDataAndSend = async (
    payloadObj: any,
    filesArray: Array<{ file: File; type: FileType }>
  ) => {
    if (includeLocation) {
      try {
        const coords = await captureGeolocation();
        if (coords) {
          payloadObj.latitude = coords.latitude;
          payloadObj.longitude = coords.longitude;
        }
      } catch {}
    }

    const fd = new FormData();
    fd.append(
      "payload",
      new Blob([JSON.stringify(payloadObj)], { type: "application/json" })
    );
    // fileTypes array ordered to match appended files
    const fileTypesArray = filesArray.map((f) => f.type);
    fd.append("fileTypes", JSON.stringify(fileTypesArray));
    for (let i = 0; i < filesArray.length; i++)
      fd.append("files", filesArray[i].file, filesArray[i].file.name);

    const token = detectedToken;
    const orgId = detectedOrgId;
    const headers: Record<string, string> = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    if (orgId) headers["X-Org-Id"] = orgId;

    const url = buildUrl(
      `/vote-submissions${orgId ? `?orgId=${encodeURIComponent(orgId)}` : ""}`
    );
    if (DEBUG_NETWORK)
      console.debug(
        "[multipart POST]",
        url,
        "headers:",
        headers,
        "fileTypes:",
        fileTypesArray,
        "formKeys:",
        Array.from((fd as any).keys())
      );
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: fd,
      credentials: "include",
    });
    const txt = await res.text().catch(() => "");
    let body: any = txt;
    try {
      if (txt) body = JSON.parse(txt);
      else body = null;
    } catch {
      body = txt;
    }
    if (!res.ok) {
      const err: any = new Error(
        `Create failed: ${res.status} ${res.statusText}`
      );
      err.status = res.status;
      err.body = body;
      throw err;
    }
    return body;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerValidationErrors(null);

    if (!files || files.length === 0) {
      alert("Please attach at least one tally sheet file before submitting.");
      return;
    }
    if (files.length > MAX_FILES) {
      alert(
        `Maximum ${MAX_FILES} files are allowed. Remove some files before submitting.`
      );
      return;
    }

    const resolvedAgent = await ensureAgentUuid();
    if (!resolvedAgent) {
      alert(
        "Agent UUID not found. Click 'Fetch my profile' or paste agent UUID into the 'Agent UUID (optional)' field."
      );
      return;
    }
    const orgId = detectedOrgId;
    if (!orgId) {
      alert(
        "Organization id not detected. Ensure you're logged in and your organization is selected."
      );
      return;
    }
    if (!electionId || !centerId || !placeId) {
      alert("Please choose election, center and polling place.");
      return;
    }

    const votesPayload: Record<Id, number> = {};
    for (const [cid, raw] of Object.entries(candidateVotes)) {
      const n = parseNumber(raw);
      if (n > 0) votesPayload[cid] = n;
    }
    if (Object.keys(votesPayload).length === 0) {
      alert("Enter at least one candidate vote before submitting.");
      return;
    }

    const payload: any = {
      orgId,
      electionId,
      centerId,
      placeId,
      agentId: resolvedAgent,
      candidateVotes: votesPayload,
      ballotsCast,
      invalidBallots: parseNumber(invalidBallots),
      blankBallots: parseNumber(blankBallots),
      rejectedBallots: parseNumber(rejectedBallots),
      spoiledBallots: parseNumber(spoiledBallots),
      comments,
    };

    setIsSubmitting(true);
    try {
      await buildFormDataAndSend(payload, files);
      qc.invalidateQueries({ queryKey: ["vote-submissions"] });
      navigate("/vote-submissions", { replace: true });
    } catch (err: any) {
      console.error("Submit failed", err);
      if (err && (err.status === 400 || err.status === 401) && err.body) {
        setServerValidationErrors(err.body);
        alert("Submission failed: " + JSON.stringify(err.body, null, 2));
      } else {
        alert("Submit failed: " + (err.message || "unknown"));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const anyQueryError =
    countiesQuery.isError ||
    districtsQuery.isError ||
    centersQuery.isError ||
    placesQuery.isError ||
    electionsQuery.isError ||
    candidatesQuery.isError;

  return (
    <div className="max-w-3xl mx-auto p-4">
      <div className="mb-4 flex gap-4 items-end">
        <div>
          <button
            type="button"
            onClick={fetchProfileAndFindUuid}
            className="text-xs px-3 py-1 rounded border bg-white hover:bg-slate-50"
          >
            Fetch my profile
          </button>
        </div>
        <div className="flex-1">
          <label className="text-xs block mb-1">Agent UUID (optional)</label>
          <input
            value={manualAgentUuid}
            onChange={(e) => setManualAgentUuid(e.target.value)}
            placeholder="00000000-0000-0000-0000-000000000000"
            className="block w-full rounded border px-2 py-1 text-sm"
          />
        </div>
      </div>

      {profileDebug && (
        <pre className="text-xs mb-4 p-2 bg-slate-50 rounded max-h-40 overflow-auto">
          {JSON.stringify(profileDebug, null, 2)}
        </pre>
      )}
      {serverValidationErrors && (
        <pre className="text-xs mb-4 p-2 bg-rose-50 rounded max-h-40 overflow-auto">
          {JSON.stringify(serverValidationErrors, null, 2)}
        </pre>
      )}
      {anyQueryError && (
        <div className="mb-4 text-xs text-rose-700 bg-rose-50 p-2 rounded">
          Some lookup queries failed — check DevTools Network.
        </div>
      )}

      <form
        onSubmit={handleSubmit}
        className="space-y-6 bg-white p-6 rounded shadow"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Election */}
          <div>
            <label className="block text-xs font-medium text-slate-700">
              Election (required)
            </label>
            {electionsQuery.isLoading ? (
              <div className="h-9 bg-slate-100 animate-pulse rounded mt-1" />
            ) : electionsQuery.isError ? (
              <div className="text-xs text-rose-600 mt-1">
                Failed to load elections
              </div>
            ) : (
              <select
                value={electionId ?? ""}
                onChange={(e) => setElectionId(e.target.value || null)}
                className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white"
              >
                <option value="">-- Choose election --</option>
                {electionsQuery.data?.map((el) => (
                  <option key={el.electionId} value={el.electionId}>
                    {el.electionName}
                    {el.year ? ` (${el.year})` : ""}
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* County */}
          <div>
            <label className="block text-xs font-medium text-slate-700">
              County
            </label>
            {countiesQuery.isLoading ? (
              <div className="h-9 bg-slate-100 animate-pulse rounded mt-1" />
            ) : countiesQuery.isError ? (
              <div className="text-xs text-rose-600 mt-1">
                Failed to load counties
              </div>
            ) : (
              <select
                value={countyId ?? ""}
                onChange={(e) => setCountyId(e.target.value || null)}
                className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white"
              >
                <option value="">-- Choose county --</option>
                {countiesQuery.data?.map((c) => (
                  <option key={c.countyId} value={c.countyId}>
                    {c.countyName}
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* District */}
          <div>
            <label className="block text-xs font-medium text-slate-700">
              District
            </label>
            <select
              value={districtId ?? ""}
              onChange={(e) => setDistrictId(e.target.value || null)}
              disabled={!countyId}
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white disabled:opacity-60"
            >
              <option value="">-- Choose district --</option>
              {districtsQuery.data?.map((d) => (
                <option key={d.districtId} value={d.districtId}>
                  {d.districtName}
                </option>
              ))}
            </select>
          </div>

          {/* Polling center */}
          <div>
            <label className="block text-xs font-medium text-slate-700">
              Polling center
            </label>
            <select
              value={centerId ?? ""}
              onChange={(e) => setCenterId(e.target.value || null)}
              disabled={!districtId && !countyId}
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white disabled:opacity-60"
            >
              <option value="">-- Choose polling center --</option>
              {centersQuery.data?.map((c) => (
                <option key={c.centerId} value={c.centerId}>
                  {c.centerName}
                </option>
              ))}
            </select>
          </div>

          {/* Polling place */}
          <div>
            <label className="block text-xs font-medium text-slate-700">
              Polling place
            </label>
            {placesQuery.isLoading ? (
              <div className="h-9 bg-slate-100 animate-pulse rounded mt-1" />
            ) : placesQuery.isError ? (
              <div className="text-xs text-rose-600 mt-1">
                Failed to load places
              </div>
            ) : (placesQuery.data ?? []).length === 0 ? (
              <div className="mt-1 text-xs text-slate-500">
                No polling places for selected center
              </div>
            ) : (
              <select
                value={placeId ?? ""}
                onChange={(e) => setPlaceId(e.target.value || null)}
                disabled={!centerId}
                className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white disabled:opacity-60"
              >
                <option value="">-- Choose polling place --</option>
                {placesQuery.data?.map((p) => (
                  <option key={p.placeId} value={p.placeId}>
                    {p.label ?? p.placeName ?? "Unnamed place"}
                  </option>
                ))}
              </select>
            )}
          </div>
        </div>

        {/* File upload */}
        <div>
          <label className="block text-xs font-medium text-slate-700">
            Tally sheet(s) (required)
          </label>
          <input
            type="file"
            multiple
            onChange={(e) => handleFilesSelected(e.target.files)}
            className="mt-1 block w-full"
          />
          <p className="text-xs text-slate-400 mt-1">
            Attach one or more tally sheet files (PDF or image). Required to
            submit. Max files: {MAX_FILES}
          </p>

          {files.length > 0 && (
            <ul className="mt-2 text-sm">
              {files.map((item, i) => (
                <li
                  key={i}
                  className="flex items-center justify-between gap-2 py-1"
                >
                  <div className="truncate">
                    {item.file.name}{" "}
                    <span className="text-xs text-slate-400">
                      ({Math.round(item.file.size / 1024)} KB)
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <select
                      value={item.type}
                      onChange={(e) =>
                        changeFileTypeAt(i, e.target.value as FileType)
                      }
                      className="text-xs rounded border px-2 py-1"
                    >
                      {FILE_TYPE_OPTIONS.map((opt) => (
                        <option key={opt} value={opt}>
                          {opt}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={() => removeFileAt(i)}
                      className="text-xs px-2 py-1 rounded border"
                    >
                      Remove
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Include location */}
        <div className="flex items-center gap-3">
          <input
            id="include-location"
            type="checkbox"
            checked={includeLocation}
            onChange={(e) => setIncludeLocation(e.target.checked)}
          />
          <label htmlFor="include-location" className="text-xs">
            Include my location (optional)
          </label>
        </div>

        {/* Candidates table */}
        <div>
          <h3 className="text-sm font-medium text-slate-800 mb-2">
            Candidates
          </h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="text-left bg-slate-50">
                  <th className="p-2 border-b">Name</th>
                  <th className="p-2 border-b">Party</th>
                  <th className="p-2 border-b text-right">Votes</th>
                </tr>
              </thead>
              <tbody>
                {candidatesQuery.isLoading ? (
                  <tr>
                    <td colSpan={3} className="p-2">
                      Loading candidates…
                    </td>
                  </tr>
                ) : candidatesQuery.isError ? (
                  <tr>
                    <td colSpan={3} className="p-2 text-rose-600">
                      Failed to load candidates
                    </td>
                  </tr>
                ) : (candidatesQuery.data ?? []).length === 0 ? (
                  <tr>
                    <td colSpan={3} className="p-2 text-slate-500">
                      No candidates for selected election
                    </td>
                  </tr>
                ) : (
                  candidatesQuery.data!.map((c) => {
                    const value = candidateVotes[c.candidateId] ?? "0";
                    return (
                      <tr
                        key={c.candidateId}
                        className="odd:bg-white even:bg-slate-50"
                      >
                        <td className="p-2 align-top">{c.displayName}</td>
                        <td className="p-2 align-top">
                          {c.partyAbbrev ?? "—"}
                        </td>
                        <td className="p-2 align-top text-right">
                          <input
                            inputMode="numeric"
                            pattern="[0-9]*"
                            type="text"
                            className="w-28 text-right rounded border px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-400"
                            value={value}
                            onFocus={() =>
                              handleNumericFocusClear(
                                (v: string) =>
                                  setCandidateVotes((s) => ({
                                    ...s,
                                    [c.candidateId]: v,
                                  })),
                                value
                              )
                            }
                            onBlur={() =>
                              handleNumericBlurDefaultZero(
                                (v: string) =>
                                  setCandidateVotes((s) => ({
                                    ...s,
                                    [c.candidateId]: v,
                                  })),
                                value
                              )
                            }
                            onChange={(e) =>
                              onCandidateInputChange(
                                c.candidateId,
                                e.target.value
                              )
                            }
                          />
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Ballots & counts */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="text-xs text-slate-700">
              Ballots (computed — editable)
            </label>
            <input
              inputMode="numeric"
              pattern="[0-9]*"
              type="text"
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
              value={ballotsCastOverride ?? String(computedBallotsCast)}
              onFocus={() => {
                if (
                  (ballotsCastOverride ?? String(computedBallotsCast)) === "0"
                )
                  setBallotsCastOverride("");
              }}
              onBlur={() => {
                if ((ballotsCastOverride ?? "").trim() === "")
                  setBallotsCastOverride(null);
              }}
              onChange={(e) =>
                setBallotsCastOverride(e.target.value.replace(/[^\d]/g, ""))
              }
            />
            <p className="text-xs text-slate-400 mt-1">
              Sum of candidate votes + invalid/blank/rejected/spoiled
            </p>
          </div>

          <div>
            <label className="text-xs text-slate-700">Invalid ballots</label>
            <input
              inputMode="numeric"
              pattern="[0-9]*"
              type="text"
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
              value={invalidBallots}
              onFocus={() =>
                handleNumericFocusClear(setInvalidBallots, invalidBallots)
              }
              onBlur={() =>
                handleNumericBlurDefaultZero(setInvalidBallots, invalidBallots)
              }
              onChange={(e) => onInvalidChange(e.target.value)}
            />
          </div>

          <div>
            <label className="text-xs text-slate-700">Blank ballots</label>
            <input
              inputMode="numeric"
              pattern="[0-9]*"
              type="text"
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
              value={blankBallots}
              onFocus={() =>
                handleNumericFocusClear(setBlankBallots, blankBallots)
              }
              onBlur={() =>
                handleNumericBlurDefaultZero(setBlankBallots, blankBallots)
              }
              onChange={(e) => onBlankChange(e.target.value)}
            />
          </div>

          <div>
            <label className="text-xs text-slate-700">Rejected</label>
            <input
              inputMode="numeric"
              pattern="[0-9]*"
              type="text"
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
              value={rejectedBallots}
              onFocus={() =>
                handleNumericFocusClear(setRejectedBallots, rejectedBallots)
              }
              onBlur={() =>
                handleNumericBlurDefaultZero(
                  setRejectedBallots,
                  rejectedBallots
                )
              }
              onChange={(e) => onRejectedChange(e.target.value)}
            />
          </div>

          <div>
            <label className="text-xs text-slate-700">Spoiled</label>
            <input
              inputMode="numeric"
              pattern="[0-9]*"
              type="text"
              className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
              value={spoiledBallots}
              onFocus={() =>
                handleNumericFocusClear(setSpoiledBallots, spoiledBallots)
              }
              onBlur={() =>
                handleNumericBlurDefaultZero(setSpoiledBallots, spoiledBallots)
              }
              onChange={(e) => onSpoiledChange(e.target.value)}
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-700">
            Comments (optional)
          </label>
          <textarea
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            rows={3}
            className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white"
            placeholder="Notes for this submission..."
          />
        </div>

        <div className="flex items-center justify-between">
          <div className="text-xs text-slate-500">
            Organization & agent will be derived from your account when
            possible.
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="text-xs px-3 py-2 rounded border"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="text-xs px-3 py-2 rounded bg-sky-600 text-white"
            >
              {isSubmitting ? "Submitting..." : "Submit"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

// // src/pages/CreateSubmissionForm.tsx
// import React, { useEffect, useMemo, useState } from "react";
// import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
// import { useNavigate } from "react-router-dom";
// import { useAuthStore } from "../shared/store/authStore";

// /**
//  * CreateSubmissionForm — UI/CSS improvements and input UX fixes
//  *
//  * - Removed the noisy debug info at the top (API base / token / orgId / agentId).
//  * - Inputs that default to "0" (candidate votes, ballots, invalid/blank/rejected/spoiled)
//  *   will clear the "0" when focused so the user can type without deleting first.
//  * - On blur, empty inputs are treated as "0" for display (and numeric calculations).
//  * - Internally these inputs are strings to preserve the empty state while editing.
//  * - Styling improved: focus ring, compact table, better spacing.
//  *
//  * Behavior notes:
//  * - Computations and submission coerce input strings to numbers (empty => 0).
//  * - The rest of the previous logic (fetch lookups, candidates, submit) remains unchanged.
//  */

// type Id = string;

// type CountyDto = { countyId: Id; countyName: string };
// type DistrictDto = { districtId: Id; districtName: string };
// type PollingCenterDto = { centerId: Id; centerName: string };
// type PollingPlaceDto = { placeId: Id; placeName?: string; label?: string };
// type ElectionDto = { electionId: Id; electionName: string; year?: number };
// type PartyDto = {
//   partyId: Id;
//   partyName?: string;
//   abbreviation?: string | null;
//   abbrev?: string | null;
// };
// type CandidateRaw = any;
// type CandidateDto = {
//   candidateId: Id;
//   displayName: string;
//   partyId?: Id | null;
//   partyAbbrev?: string | null;
//   centerId?: Id | null;
// };

// const API_BASE =
//   (import.meta.env.VITE_API_BASE_URL as string) ?? "http://localhost:8080/api";
// const DEBUG_NETWORK = false;
// const UUID_RE =
//   /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

// function buildUrl(path: string) {
//   if (/^https?:\/\//i.test(path)) return path;
//   const base = API_BASE.replace(/\/$/, "");
//   const p = path.startsWith("/") ? path : `/${path}`;
//   return `${base}${p}`;
// }

// function tryParseJwtClaims(token?: string) {
//   if (!token) return null;
//   try {
//     const parts = token.split(".");
//     if (parts.length < 2) return null;
//     const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
//     const json = decodeURIComponent(
//       atob(payload)
//         .split("")
//         .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
//         .join("")
//     );
//     return JSON.parse(json);
//   } catch {
//     return null;
//   }
// }

// export default function CreateSubmissionForm(): React.ReactElement {
//   const qc = useQueryClient();
//   const navigate = useNavigate();

//   // auth store selectors — adjust if your store uses different keys
//   const storeToken = useAuthStore(
//     (s: any) => s.token ?? s.accessToken ?? s.idToken ?? null
//   );
//   const currentUser = useAuthStore((s: any) => s.currentUser ?? null);
//   const storeSnapshot = (useAuthStore as any).getState
//     ? (useAuthStore as any).getState()
//     : null;

//   const detectToken = () => {
//     if (storeToken && typeof storeToken === "string")
//       return storeToken.replace(/^Bearer\s+/i, "");
//     try {
//       const w: any = window as any;
//       if (w.__evt?.token)
//         return String(w.__evt.token).replace(/^Bearer\s+/i, "");
//       if (w.evt?.token) return String(w.evt.token).replace(/^Bearer\s+/i, "");
//     } catch {}
//     const keys = [
//       "token",
//       "authToken",
//       "accessToken",
//       "idToken",
//       "evt.token",
//       "evtToken",
//     ];
//     for (const k of keys) {
//       const v = localStorage.getItem(k);
//       if (v) return String(v).replace(/^Bearer\s+/i, "");
//     }
//     return null;
//   };
//   const detectedToken = detectToken();
//   const tokenClaims = tryParseJwtClaims(detectedToken ?? undefined);

//   // org detection (X-Org-Id)
//   const detectOrgId = () => {
//     try {
//       if (storeSnapshot) {
//         const c =
//           storeSnapshot.currentOrgId ??
//           storeSnapshot.orgId ??
//           storeSnapshot.defaultOrgId ??
//           null;
//         if (c) return String(c);
//       }
//     } catch {}
//     try {
//       const w: any = window as any;
//       if (w.__evt?.currentOrgId) return String(w.__evt.currentOrgId);
//       if (w.evt?.currentOrgId) return String(w.evt.currentOrgId);
//     } catch {}
//     const keys = [
//       "currentOrgId",
//       "evt.currentOrgId",
//       "orgId",
//       "current_org_id",
//     ];
//     for (const k of keys) {
//       const v = localStorage.getItem(k);
//       if (v) return v;
//     }
//     return null;
//   };
//   const detectedOrgId = detectOrgId();

//   // agent detection candidate (may be username)
//   const detectAgentCandidate = () => {
//     try {
//       if (
//         currentUser &&
//         (currentUser.id || currentUser.userId || currentUser.user_id)
//       ) {
//         return String(
//           currentUser.id ?? currentUser.userId ?? currentUser.user_id
//         );
//       }
//     } catch {}
//     if (tokenClaims) {
//       const maybe =
//         tokenClaims.userId ??
//         tokenClaims.userID ??
//         tokenClaims.user_id ??
//         tokenClaims.id ??
//         tokenClaims.sub ??
//         null;
//       if (maybe) return String(maybe);
//     }
//     const localKeys = [
//       "currentUserId",
//       "userId",
//       "user_id",
//       "agentId",
//       "username",
//     ];
//     for (const k of localKeys) {
//       const v = localStorage.getItem(k);
//       if (v) return v;
//     }
//     return null;
//   };

//   const [agentId, setAgentId] = useState<string | null>(detectAgentCandidate());
//   const [manualAgentUuid, setManualAgentUuid] = useState<string>("");

//   // form fields
//   const [countyId, setCountyId] = useState<Id | null>(null);
//   const [districtId, setDistrictId] = useState<Id | null>(null);
//   const [centerId, setCenterId] = useState<Id | null>(null);
//   const [placeId, setPlaceId] = useState<Id | null>(null);
//   const [electionId, setElectionId] = useState<Id | null>(null);

//   // candidateVotes stored as strings so inputs can be empty while editing
//   const [candidateVotes, setCandidateVotes] = useState<Record<Id, string>>({});
//   const [invalidBallots, setInvalidBallots] = useState<string>("0");
//   const [blankBallots, setBlankBallots] = useState<string>("0");
//   const [rejectedBallots, setRejectedBallots] = useState<string>("0");
//   const [spoiledBallots, setSpoiledBallots] = useState<string>("0");
//   const [ballotsCastOverride, setBallotsCastOverride] = useState<string | null>(
//     null
//   );
//   const [comments, setComments] = useState<string>("");

//   const [profileDebug, setProfileDebug] = useState<any | null>(null);
//   const [serverValidationErrors, setServerValidationErrors] = useState<
//     any | null
//   >(null);
//   const [isSubmitting, setIsSubmitting] = useState(false);

//   // helper: parse numeric string -> number (empty or invalid => 0)
//   const parseNumber = (v: string | number | undefined | null) => {
//     if (v === null || v === undefined) return 0;
//     if (typeof v === "number") return Number.isFinite(v) ? v : 0;
//     const s = String(v).trim();
//     if (s === "") return 0;
//     const n = Number(s);
//     return Number.isFinite(n) ? n : 0;
//   };

//   // computed ballots cast uses parseNumber to interpret empty strings as 0
//   const computedBallotsCast = useMemo(() => {
//     const candidateSum = Object.values(candidateVotes).reduce(
//       (s, v) => s + parseNumber(v),
//       0
//     );
//     return (
//       candidateSum +
//       parseNumber(invalidBallots) +
//       parseNumber(blankBallots) +
//       parseNumber(rejectedBallots) +
//       parseNumber(spoiledBallots)
//     );
//   }, [
//     candidateVotes,
//     invalidBallots,
//     blankBallots,
//     rejectedBallots,
//     spoiledBallots,
//   ]);
//   const ballotsCast =
//     ballotsCastOverride === null
//       ? computedBallotsCast
//       : parseNumber(ballotsCastOverride);

//   // fetch wrapper
//   const fetchJson = async (path: string, options?: RequestInit) => {
//     const url = buildUrl(path);
//     const headers: Record<string, string> = { Accept: "application/json" };
//     if (detectedToken) headers["Authorization"] = `Bearer ${detectedToken}`;
//     const merged: RequestInit = {
//       credentials: "include",
//       ...options,
//       headers: {
//         ...headers,
//         ...(options?.headers as Record<string, string> | undefined),
//       },
//     };
//     if (DEBUG_NETWORK)
//       console.debug("[fetchJson]", merged.method ?? "GET", url, merged.headers);
//     const res = await fetch(url, merged);
//     const txt = await res.text().catch(() => "");
//     let body: any = txt;
//     try {
//       body = txt ? JSON.parse(txt) : null;
//     } catch {
//       body = txt;
//     }
//     if (!res.ok) {
//       const e: any = new Error(`HTTP ${res.status}`);
//       e.status = res.status;
//       e.body = body;
//       throw e;
//     }
//     if (
//       body &&
//       typeof body === "object" &&
//       "content" in body &&
//       Array.isArray(body.content)
//     )
//       return body.content;
//     return body;
//   };

//   // lookups
//   const countiesQuery = useQuery<CountyDto[]>({
//     queryKey: ["counties"],
//     queryFn: async () => (await fetchJson("/counties")) as CountyDto[],
//   });
//   const districtsQuery = useQuery<DistrictDto[]>({
//     queryKey: ["districts", countyId],
//     enabled: !!countyId,
//     queryFn: async () => {
//       const params = new URLSearchParams();
//       if (countyId) params.set("countyId", countyId);
//       params.set("page", "0");
//       params.set("size", "500");
//       return (await fetchJson(
//         `/districts?${params.toString()}`
//       )) as DistrictDto[];
//     },
//   });
//   const centersQuery = useQuery<PollingCenterDto[]>({
//     queryKey: ["centers", countyId, districtId],
//     enabled: !!countyId || !!districtId,
//     queryFn: async () => {
//       const params = new URLSearchParams();
//       if (countyId) params.set("countyId", countyId);
//       if (districtId) params.set("districtId", districtId);
//       params.set("page", "0");
//       params.set("size", "500");
//       return (await fetchJson(
//         `/polling-centers?${params.toString()}`
//       )) as PollingCenterDto[];
//     },
//   });
//   const placesQuery = useQuery<PollingPlaceDto[]>({
//     queryKey: ["places", centerId],
//     enabled: !!centerId,
//     queryFn: async () =>
//       (await fetchJson(
//         `/polling-places/by-center/${centerId}`
//       )) as PollingPlaceDto[],
//   });
//   const electionsQuery = useQuery<ElectionDto[]>({
//     queryKey: ["elections", "active"],
//     queryFn: async () =>
//       (await fetchJson(`/elections?activeOnly=true`)) as ElectionDto[],
//   });

//   const partiesQuery = useQuery<PartyDto[]>({
//     queryKey: ["parties"],
//     queryFn: async () =>
//       (await fetchJson(`/parties?page=0&size=500`)) as PartyDto[],
//     staleTime: 60 * 60 * 1000,
//   });
//   const partyMap = useMemo(() => {
//     const m: Record<string, string> = {};
//     for (const p of partiesQuery.data ?? []) {
//       if (p.partyId) m[String(p.partyId)] = p.abbreviation ?? p.abbrev ?? "";
//     }
//     return m;
//   }, [partiesQuery.data]);

//   const candidatesQuery = useQuery<CandidateDto[]>({
//     queryKey: ["candidates", electionId, centerId],
//     enabled: !!electionId,
//     queryFn: async () => {
//       const raw = (await fetchJson(
//         `/election-candidates/${electionId}/candidates`
//       )) as CandidateRaw[];
//       if (!Array.isArray(raw)) return [];
//       const mapped = raw.map((r: any) => {
//         const pid = r.partyId ?? r.party?.partyId ?? r.party_id ?? null;
//         const partyAbbrev =
//           r.partyAbbrev ??
//           r.party?.abbreviation ??
//           r.party?.abbrev ??
//           (pid ? partyMap[String(pid)] ?? null : null) ??
//           null;
//         return {
//           candidateId:
//             r.candidateId ?? r.candidate_id ?? r.id ?? String(Math.random()),
//           displayName:
//             r.fullName ?? r.full_name ?? r.displayName ?? r.name ?? "Unknown",
//           partyId: pid ?? null,
//           partyAbbrev,
//           centerId: r.centerId ?? r.center_id ?? r.pollingCenterId ?? null,
//         } as CandidateDto;
//       });
//       if (centerId) {
//         const filtered = mapped.filter(
//           (c) => !c.centerId || c.centerId === centerId
//         );
//         if (filtered.length > 0) return filtered;
//       }
//       return mapped;
//     },
//   });

//   // keep candidateVotes keys in sync: initialize to "0" (visible) but editable / focus clears it
//   useEffect(() => {
//     const list = candidatesQuery.data ?? [];
//     if (!Array.isArray(list)) return;
//     setCandidateVotes((prev) => {
//       const next = { ...prev };
//       for (const c of list)
//         if (!(c.candidateId in next)) next[c.candidateId] = "0";
//       for (const k of Object.keys(next))
//         if (!list.some((c) => c.candidateId === k)) delete next[k];
//       return next;
//     });
//     // eslint-disable-next-line react-hooks/exhaustive-deps
//   }, [candidatesQuery.data]);

//   // helper: fetch profile at /users/me (tenant-scoped — send X-Org-Id)
//   const fetchProfileAndFindUuid = async (): Promise<string | null> => {
//     setProfileDebug(null);
//     const token = detectedToken;
//     const orgId = detectedOrgId;
//     const url = buildUrl("/users/me");
//     try {
//       if (DEBUG_NETWORK)
//         console.debug("[profile] GET", url, "X-Org-Id:", orgId ?? "none");
//       const res = await fetch(url, {
//         method: "GET",
//         credentials: "include",
//         headers: {
//           Accept: "application/json",
//           ...(token ? { Authorization: `Bearer ${token}` } : {}),
//           ...(orgId ? { "X-Org-Id": orgId } : {}),
//         },
//       });
//       const txt = await res.text().catch(() => "");
//       let body: any = txt;
//       try {
//         if (txt) body = JSON.parse(txt);
//         else body = null;
//       } catch {
//         body = txt;
//       }
//       setProfileDebug({ endpoint: "/users/me", status: res.status, body });
//       if (res.ok) {
//         const findUuid = (obj: any): string | null => {
//           if (!obj) return null;
//           if (typeof obj === "string") return UUID_RE.test(obj) ? obj : null;
//           if (Array.isArray(obj)) {
//             for (const v of obj) {
//               const f = findUuid(v);
//               if (f) return f;
//             }
//             return null;
//           }
//           if (typeof obj === "object") {
//             for (const k of Object.keys(obj)) {
//               const v = obj[k];
//               if (typeof v === "string" && UUID_RE.test(v)) return v;
//               const nested = findUuid(v);
//               if (nested) return nested;
//             }
//           }
//           return null;
//         };
//         const found = findUuid(body);
//         if (found) {
//           setAgentId(found);
//           return found;
//         }
//       }
//     } catch (err: any) {
//       setProfileDebug({ endpoint: "/users/me", error: String(err) });
//     }
//     return null;
//   };

//   // create mutation
//   const createMutation = useMutation<unknown, Error, any>({
//     mutationFn: async (payload) => {
//       const token = detectedToken;
//       const orgId = detectedOrgId;
//       const headers: Record<string, string> = {
//         "Content-Type": "application/json",
//       };
//       if (token) headers["Authorization"] = `Bearer ${token}`;
//       if (orgId) headers["X-Org-Id"] = orgId;
//       const url = buildUrl(
//         `/vote-submissions${orgId ? `?orgId=${encodeURIComponent(orgId)}` : ""}`
//       );
//       if (DEBUG_NETWORK)
//         console.debug(
//           "[create] POST",
//           url,
//           "headers:",
//           headers,
//           "payload:",
//           payload
//         );
//       const res = await fetch(url, {
//         method: "POST",
//         headers,
//         body: JSON.stringify(payload),
//         credentials: "include",
//       });
//       const txt = await res.text().catch(() => "");
//       let body: any = txt;
//       try {
//         if (txt) body = JSON.parse(txt);
//       } catch {}
//       if (!res.ok) {
//         const err: any = new Error(
//           `Create failed: ${res.status} ${res.statusText}`
//         );
//         err.status = res.status;
//         err.body = body;
//         if (res.status === 400 || res.status === 401)
//           setServerValidationErrors(body);
//         throw err;
//       }
//       return body;
//     },
//     onSuccess: () => qc.invalidateQueries({ queryKey: ["vote-submissions"] }),
//   });

//   const onCandidateInputChange = (candidateId: Id, raw: string) => {
//     // allow empty string, or numeric string only
//     const cleaned = raw.replace(/[^\d]/g, "");
//     setCandidateVotes((s) => ({ ...s, [candidateId]: cleaned }));
//   };

//   // focus handler: if input currently "0" make it empty so user can type
//   const handleNumericFocusClear = (
//     setter: (v: string) => void,
//     current: string
//   ) => {
//     if (current === "0") setter("");
//   };

//   // blur handler: if input left blank set to "0" for visual clarity
//   const handleNumericBlurDefaultZero = (
//     setter: (v: string) => void,
//     current: string
//   ) => {
//     if (current.trim() === "") setter("0");
//   };

//   // ballots inputs handlers
//   const onInvalidChange = (v: string) =>
//     setInvalidBallots(v.replace(/[^\d]/g, ""));
//   const onBlankChange = (v: string) => setBlankBallots(v.replace(/[^\d]/g, ""));
//   const onRejectedChange = (v: string) =>
//     setRejectedBallots(v.replace(/[^\d]/g, ""));
//   const onSpoiledChange = (v: string) =>
//     setSpoiledBallots(v.replace(/[^\d]/g, ""));

//   // ensure agent uuid (manual override allowed)
//   const ensureAgentUuid = async (): Promise<string | null> => {
//     if (manualAgentUuid && UUID_RE.test(manualAgentUuid.trim()))
//       return manualAgentUuid.trim();
//     if (agentId && UUID_RE.test(agentId)) return agentId;
//     if (tokenClaims) {
//       const maybe =
//         tokenClaims.sub ??
//         tokenClaims.id ??
//         tokenClaims.userId ??
//         tokenClaims.user_id ??
//         null;
//       if (maybe && typeof maybe === "string" && UUID_RE.test(maybe)) {
//         setAgentId(String(maybe));
//         return String(maybe);
//       }
//     }
//     const found = await fetchProfileAndFindUuid();
//     if (found && UUID_RE.test(found)) return found;
//     return null;
//   };

//   const handleSubmit = async (e: React.FormEvent) => {
//     e.preventDefault();
//     setServerValidationErrors(null);
//     const resolvedAgent = await ensureAgentUuid();
//     if (!resolvedAgent) {
//       alert(
//         "Agent UUID not found. Click 'Fetch my profile' or paste agent UUID into the 'Agent UUID (optional)' field."
//       );
//       return;
//     }
//     const orgId = detectedOrgId;
//     if (!orgId) {
//       alert(
//         "Organization id not detected. Ensure you're logged in and your organization is selected."
//       );
//       return;
//     }
//     if (!electionId || !centerId || !placeId) {
//       alert("Please choose election, center and polling place.");
//       return;
//     }

//     const votesPayload: Record<Id, number> = {};
//     for (const [cid, raw] of Object.entries(candidateVotes)) {
//       const n = parseNumber(raw);
//       if (n > 0) votesPayload[cid] = n;
//     }
//     if (Object.keys(votesPayload).length === 0) {
//       alert("Enter at least one candidate vote before submitting.");
//       return;
//     }

//     const payload = {
//       electionId,
//       centerId,
//       placeId,
//       orgId,
//       agentId: resolvedAgent,
//       candidateVotes: votesPayload,
//       ballotsCast,
//       invalidBallots: parseNumber(invalidBallots),
//       blankBallots: parseNumber(blankBallots),
//       rejectedBallots: parseNumber(rejectedBallots),
//       spoiledBallots: parseNumber(spoiledBallots),
//       comments,
//     };

//     setIsSubmitting(true);
//     try {
//       await createMutation.mutateAsync(payload);
//       navigate("/vote-submissions", { replace: true });
//     } catch (err: any) {
//       console.error("Submit failed", err);
//       if (serverValidationErrors) {
//         alert(
//           "Validation error: " + JSON.stringify(serverValidationErrors, null, 2)
//         );
//       } else {
//         alert("Submit failed: " + (err.message || "unknown"));
//       }
//     } finally {
//       setIsSubmitting(false);
//     }
//   };

//   const anyQueryError =
//     countiesQuery.isError ||
//     districtsQuery.isError ||
//     centersQuery.isError ||
//     placesQuery.isError ||
//     electionsQuery.isError ||
//     candidatesQuery.isError;

//   return (
//     <div className="max-w-3xl mx-auto p-4">
//       {/* top debug info removed per request */}

//       <div className="mb-4 flex gap-4 items-end">
//         <div>
//           <button
//             type="button"
//             onClick={fetchProfileAndFindUuid}
//             className="text-xs px-3 py-1 rounded border bg-white hover:bg-slate-50"
//           >
//             Fetch my profile
//           </button>
//         </div>

//         <div className="flex-1">
//           <label className="text-xs block mb-1">Agent UUID (optional)</label>
//           <input
//             value={manualAgentUuid}
//             onChange={(e) => setManualAgentUuid(e.target.value)}
//             placeholder="00000000-0000-0000-0000-000000000000"
//             className="block w-full rounded border px-2 py-1 text-sm"
//           />
//         </div>
//       </div>

//       {profileDebug && (
//         <pre className="text-xs mb-4 p-2 bg-slate-50 rounded max-h-40 overflow-auto">
//           {JSON.stringify(profileDebug, null, 2)}
//         </pre>
//       )}
//       {serverValidationErrors && (
//         <pre className="text-xs mb-4 p-2 bg-rose-50 rounded max-h-40 overflow-auto">
//           {JSON.stringify(serverValidationErrors, null, 2)}
//         </pre>
//       )}
//       {anyQueryError && (
//         <div className="mb-4 text-xs text-rose-700 bg-rose-50 p-2 rounded">
//           Some lookup queries failed — check DevTools Network.
//         </div>
//       )}

//       <form
//         onSubmit={handleSubmit}
//         className="space-y-6 bg-white p-6 rounded shadow"
//       >
//         <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
//           {/* Election */}
//           <div>
//             <label className="block text-xs font-medium text-slate-700">
//               Election (required)
//             </label>
//             {electionsQuery.isLoading ? (
//               <div className="h-9 bg-slate-100 animate-pulse rounded mt-1" />
//             ) : electionsQuery.isError ? (
//               <div className="text-xs text-rose-600 mt-1">
//                 Failed to load elections
//               </div>
//             ) : (
//               <select
//                 value={electionId ?? ""}
//                 onChange={(e) => setElectionId(e.target.value || null)}
//                 className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white"
//               >
//                 <option value="">-- Choose election --</option>
//                 {electionsQuery.data?.map((el) => (
//                   <option key={el.electionId} value={el.electionId}>
//                     {el.electionName}
//                     {el.year ? ` (${el.year})` : ""}
//                   </option>
//                 ))}
//               </select>
//             )}
//           </div>

//           {/* County */}
//           <div>
//             <label className="block text-xs font-medium text-slate-700">
//               County
//             </label>
//             {countiesQuery.isLoading ? (
//               <div className="h-9 bg-slate-100 animate-pulse rounded mt-1" />
//             ) : countiesQuery.isError ? (
//               <div className="text-xs text-rose-600 mt-1">
//                 Failed to load counties
//               </div>
//             ) : (
//               <select
//                 value={countyId ?? ""}
//                 onChange={(e) => setCountyId(e.target.value || null)}
//                 className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white"
//               >
//                 <option value="">-- Choose county --</option>
//                 {countiesQuery.data?.map((c) => (
//                   <option key={c.countyId} value={c.countyId}>
//                     {c.countyName}
//                   </option>
//                 ))}
//               </select>
//             )}
//           </div>

//           {/* District */}
//           <div>
//             <label className="block text-xs font-medium text-slate-700">
//               District
//             </label>
//             <select
//               value={districtId ?? ""}
//               onChange={(e) => setDistrictId(e.target.value || null)}
//               disabled={!countyId}
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white disabled:opacity-60"
//             >
//               <option value="">-- Choose district --</option>
//               {districtsQuery.data?.map((d) => (
//                 <option key={d.districtId} value={d.districtId}>
//                   {d.districtName}
//                 </option>
//               ))}
//             </select>
//           </div>

//           {/* Polling center */}
//           <div>
//             <label className="block text-xs font-medium text-slate-700">
//               Polling center
//             </label>
//             <select
//               value={centerId ?? ""}
//               onChange={(e) => setCenterId(e.target.value || null)}
//               disabled={!districtId && !countyId}
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white disabled:opacity-60"
//             >
//               <option value="">-- Choose polling center --</option>
//               {centersQuery.data?.map((c) => (
//                 <option key={c.centerId} value={c.centerId}>
//                   {c.centerName}
//                 </option>
//               ))}
//             </select>
//           </div>

//           {/* Polling place (use label if present) */}
//           <div>
//             <label className="block text-xs font-medium text-slate-700">
//               Polling place
//             </label>
//             {placesQuery.isLoading ? (
//               <div className="h-9 bg-slate-100 animate-pulse rounded mt-1" />
//             ) : placesQuery.isError ? (
//               <div className="text-xs text-rose-600 mt-1">
//                 Failed to load places
//               </div>
//             ) : (placesQuery.data ?? []).length === 0 ? (
//               <div className="mt-1 text-xs text-slate-500">
//                 No polling places for selected center
//               </div>
//             ) : (
//               <select
//                 value={placeId ?? ""}
//                 onChange={(e) => setPlaceId(e.target.value || null)}
//                 disabled={!centerId}
//                 className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white disabled:opacity-60"
//               >
//                 <option value="">-- Choose polling place --</option>
//                 {placesQuery.data?.map((p) => (
//                   <option key={p.placeId} value={p.placeId}>
//                     {p.label ?? p.placeName ?? "Unnamed place"}
//                   </option>
//                 ))}
//               </select>
//             )}
//           </div>
//         </div>

//         {/* Candidates table */}
//         <div>
//           <h3 className="text-sm font-medium text-slate-800 mb-2">
//             Candidates
//           </h3>
//           <div className="overflow-x-auto">
//             <table className="w-full text-sm border-collapse">
//               <thead>
//                 <tr className="text-left bg-slate-50">
//                   <th className="p-2 border-b">Name</th>
//                   <th className="p-2 border-b">Party</th>
//                   <th className="p-2 border-b text-right">Votes</th>
//                 </tr>
//               </thead>
//               <tbody>
//                 {candidatesQuery.isLoading ? (
//                   <tr>
//                     <td colSpan={3} className="p-2">
//                       Loading candidates…
//                     </td>
//                   </tr>
//                 ) : candidatesQuery.isError ? (
//                   <tr>
//                     <td colSpan={3} className="p-2 text-rose-600">
//                       Failed to load candidates
//                     </td>
//                   </tr>
//                 ) : (candidatesQuery.data ?? []).length === 0 ? (
//                   <tr>
//                     <td colSpan={3} className="p-2 text-slate-500">
//                       No candidates for selected election
//                     </td>
//                   </tr>
//                 ) : (
//                   candidatesQuery.data!.map((c) => {
//                     const value = candidateVotes[c.candidateId] ?? "0";
//                     return (
//                       <tr
//                         key={c.candidateId}
//                         className="odd:bg-white even:bg-slate-50"
//                       >
//                         <td className="p-2 align-top">{c.displayName}</td>
//                         <td className="p-2 align-top">
//                           {c.partyAbbrev ?? "—"}
//                         </td>
//                         <td className="p-2 align-top text-right">
//                           <input
//                             inputMode="numeric"
//                             pattern="[0-9]*"
//                             type="text"
//                             className="w-28 text-right rounded border px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-sky-400"
//                             value={value}
//                             onFocus={() =>
//                               handleNumericFocusClear(
//                                 (v: string) =>
//                                   setCandidateVotes((s) => ({
//                                     ...s,
//                                     [c.candidateId]: v,
//                                   })),
//                                 value
//                               )
//                             }
//                             onBlur={() =>
//                               handleNumericBlurDefaultZero(
//                                 (v: string) =>
//                                   setCandidateVotes((s) => ({
//                                     ...s,
//                                     [c.candidateId]: v,
//                                   })),
//                                 value
//                               )
//                             }
//                             onChange={(e) =>
//                               onCandidateInputChange(
//                                 c.candidateId,
//                                 e.target.value
//                               )
//                             }
//                           />
//                         </td>
//                       </tr>
//                     );
//                   })
//                 )}
//               </tbody>
//             </table>
//           </div>
//         </div>

//         {/* Ballots & counts */}
//         <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
//           <div>
//             <label className="text-xs text-slate-700">
//               Ballots (computed — editable)
//             </label>
//             <input
//               inputMode="numeric"
//               pattern="[0-9]*"
//               type="text"
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
//               value={ballotsCastOverride ?? String(computedBallotsCast)}
//               onFocus={() => {
//                 if (
//                   (ballotsCastOverride ?? String(computedBallotsCast)) === "0"
//                 )
//                   setBallotsCastOverride("");
//               }}
//               onBlur={() => {
//                 if ((ballotsCastOverride ?? "").trim() === "")
//                   setBallotsCastOverride(null);
//               }}
//               onChange={(e) =>
//                 setBallotsCastOverride(e.target.value.replace(/[^\d]/g, ""))
//               }
//             />
//             <p className="text-xs text-slate-400 mt-1">
//               Sum of candidate votes + invalid/blank/rejected/spoiled
//             </p>
//           </div>

//           <div>
//             <label className="text-xs text-slate-700">Invalid ballots</label>
//             <input
//               inputMode="numeric"
//               pattern="[0-9]*"
//               type="text"
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
//               value={invalidBallots}
//               onFocus={() =>
//                 handleNumericFocusClear(setInvalidBallots, invalidBallots)
//               }
//               onBlur={() =>
//                 handleNumericBlurDefaultZero(setInvalidBallots, invalidBallots)
//               }
//               onChange={(e) => onInvalidChange(e.target.value)}
//             />
//           </div>

//           <div>
//             <label className="text-xs text-slate-700">Blank ballots</label>
//             <input
//               inputMode="numeric"
//               pattern="[0-9]*"
//               type="text"
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
//               value={blankBallots}
//               onFocus={() =>
//                 handleNumericFocusClear(setBlankBallots, blankBallots)
//               }
//               onBlur={() =>
//                 handleNumericBlurDefaultZero(setBlankBallots, blankBallots)
//               }
//               onChange={(e) => onBlankChange(e.target.value)}
//             />
//           </div>

//           <div>
//             <label className="text-xs text-slate-700">Rejected</label>
//             <input
//               inputMode="numeric"
//               pattern="[0-9]*"
//               type="text"
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
//               value={rejectedBallots}
//               onFocus={() =>
//                 handleNumericFocusClear(setRejectedBallots, rejectedBallots)
//               }
//               onBlur={() =>
//                 handleNumericBlurDefaultZero(
//                   setRejectedBallots,
//                   rejectedBallots
//                 )
//               }
//               onChange={(e) => onRejectedChange(e.target.value)}
//             />
//           </div>

//           <div>
//             <label className="text-xs text-slate-700">Spoiled</label>
//             <input
//               inputMode="numeric"
//               pattern="[0-9]*"
//               type="text"
//               className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-sky-400"
//               value={spoiledBallots}
//               onFocus={() =>
//                 handleNumericFocusClear(setSpoiledBallots, spoiledBallots)
//               }
//               onBlur={() =>
//                 handleNumericBlurDefaultZero(setSpoiledBallots, spoiledBallots)
//               }
//               onChange={(e) => onSpoiledChange(e.target.value)}
//             />
//           </div>
//         </div>

//         <div>
//           <label className="block text-xs font-medium text-slate-700">
//             Comments (optional)
//           </label>
//           <textarea
//             value={comments}
//             onChange={(e) => setComments(e.target.value)}
//             rows={3}
//             className="mt-1 block w-full rounded border px-2 py-1 text-sm bg-white"
//             placeholder="Notes for this submission..."
//           />
//         </div>

//         <div className="flex items-center justify-between">
//           <div className="text-xs text-slate-500">
//             Organization & agent will be derived from your account when
//             possible.
//           </div>
//           <div className="flex items-center gap-3">
//             <button
//               type="button"
//               onClick={() => navigate(-1)}
//               className="text-xs px-3 py-2 rounded border"
//             >
//               Cancel
//             </button>
//             <button
//               type="submit"
//               disabled={isSubmitting}
//               className="text-xs px-3 py-2 rounded bg-sky-600 text-white"
//             >
//               {isSubmitting ? "Submitting..." : "Submit"}
//             </button>
//           </div>
//         </div>
//       </form>
//     </div>
//   );
// }

// /**
//  * Election Vote Tracker — Navigation Spec aligned to ENHANCED_ELECTION_VOTE.sql
//  * - rolesAllowed uses EXACT DB role strings
//  * - icons are Lucide icon names (string) to keep this framework-agnostic
//  * - sidebar should render ONLY the 6 top-level nodes (children are for page sub-nav)
//  */

// export type Role =
//   | "SYSTEM_ADMIN"
//   | "NEC_ADMIN"
//   | "ADMIN"
//   | "PARTY_ADMIN"
//   | "AGENT"
//   | "OBSERVER"
//   | "SUPERVISOR"
//   | "COORDINATOR"
//   | "DATA_ENTRY"
//   | "AUDITOR";

// export type NavItem = {
//   key: string;
//   label: string;
//   route: string; // supports dynamic segments like :electionId
//   icon: string; // Lucide icon name (string)
//   description?: string;
//   rolesAllowed: Role[];
//   children?: NavItem[];
//   badgeType?: "unread" | "status" | "count";
//   meta?: {
//     scope?: "GLOBAL" | "ELECTION";
//     backing?: {
//       tables?: string[];
//       views?: string[];
//       mviews?: string[];
//     };
//   };
// };

// const ALL: Role[] = [
//   "SYSTEM_ADMIN",
//   "NEC_ADMIN",
//   "ADMIN",
//   "PARTY_ADMIN",
//   "AGENT",
//   "OBSERVER",
//   "SUPERVISOR",
//   "COORDINATOR",
//   "DATA_ENTRY",
//   "AUDITOR",
// ];

// export const APP_NAV: NavItem[] = [
//   {
//     key: "dashboard",
//     label: "Dashboard",
//     route: "/dashboard",
//     icon: "LayoutDashboard",
//     description: "Your live overview: status, alerts, and quick actions.",
//     rolesAllowed: ALL,
//     meta: { scope: "GLOBAL" },
//   },

//   {
//     key: "elections",
//     label: "Elections",
//     route: "/elections",
//     icon: "Vote",
//     description: "Select an election to enter its workspace.",
//     rolesAllowed: ALL,
//     meta: {
//       scope: "GLOBAL",
//       backing: { tables: ["election"] },
//     },
//     children: [
//       {
//         key: "elections.workspace",
//         label: "Election Workspace",
//         route: "/elections/:electionId/overview",
//         icon: "Landmark",
//         description:
//           "Ballot, allocation, submissions, results, integrity, and NEC workflow.",
//         rolesAllowed: ALL,
//         meta: { scope: "ELECTION" },
//         children: [
//           {
//             key: "elections.workspace.overview",
//             label: "Overview",
//             route: "/elections/:electionId/overview",
//             icon: "Radar",
//             description:
//               "Setup checklist, health signals, alerts, and quick actions.",
//             rolesAllowed: ALL,
//             meta: { scope: "ELECTION" },
//           },

//           // SETUP (election_party, election_candidate, contest, contest_option)
//           {
//             key: "elections.workspace.setup",
//             label: "Setup",
//             route: "/elections/:electionId/setup/parties",
//             icon: "SlidersHorizontal",
//             description: "Ballot structure: parties, candidates, contests.",
//             rolesAllowed: [
//               "SYSTEM_ADMIN",
//               "NEC_ADMIN",
//               "ADMIN",
//               "PARTY_ADMIN",
//               "SUPERVISOR",
//               "COORDINATOR",
//               "AUDITOR",
//             ],
//             meta: {
//               scope: "ELECTION",
//               backing: {
//                 tables: [
//                   "election_party",
//                   "election_candidate",
//                   "contest",
//                   "contest_option",
//                 ],
//               },
//             },
//             children: [
//               {
//                 key: "elections.workspace.setup.parties",
//                 label: "Election Parties",
//                 route: "/elections/:electionId/setup/parties",
//                 icon: "BadgePlus",
//                 rolesAllowed: [
//                   "SYSTEM_ADMIN",
//                   "NEC_ADMIN",
//                   "ADMIN",
//                   "PARTY_ADMIN",
//                 ],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["election_party"] },
//                 },
//               },
//               {
//                 key: "elections.workspace.setup.candidates",
//                 label: "Election Candidates",
//                 route: "/elections/:electionId/setup/candidates",
//                 icon: "UserPlus",
//                 rolesAllowed: [
//                   "SYSTEM_ADMIN",
//                   "NEC_ADMIN",
//                   "ADMIN",
//                   "PARTY_ADMIN",
//                 ],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["election_candidate"] },
//                 },
//               },
//               {
//                 key: "elections.workspace.setup.contests",
//                 label: "Contests & Options",
//                 route: "/elections/:electionId/setup/contests",
//                 icon: "ListTree",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["contest", "contest_option"] },
//                 },
//               },
//             ],
//           },

//           // ALLOCATION (polling_center_allocation, polling_place_allocation)
//           {
//             key: "elections.workspace.allocation",
//             label: "Allocation",
//             route: "/elections/:electionId/allocation/centers",
//             icon: "Boxes",
//             description:
//               "Voter/ballot allocation by center and place (per election).",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN", "AUDITOR"],
//             meta: {
//               scope: "ELECTION",
//               backing: {
//                 tables: [
//                   "polling_center_allocation",
//                   "polling_place_allocation",
//                 ],
//               },
//             },
//             children: [
//               {
//                 key: "elections.workspace.allocation.centers",
//                 label: "Center Allocation",
//                 route: "/elections/:electionId/allocation/centers",
//                 icon: "Building2",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["polling_center_allocation"] },
//                 },
//               },
//               {
//                 key: "elections.workspace.allocation.places",
//                 label: "Place Allocation",
//                 route: "/elections/:electionId/allocation/places",
//                 icon: "Flag",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["polling_place_allocation"] },
//                 },
//               },
//             ],
//           },

//           // SUBMISSIONS (vote_submission, vote_tally, tally_sheet, vote_submission_contest, vote_submission_ranking)
//           {
//             key: "elections.workspace.submissions",
//             label: "Submissions",
//             route: "/elections/:electionId/submissions",
//             icon: "FileSignature",
//             description:
//               "Tally entry, verification, attachments, and contest votes.",
//             rolesAllowed: ALL,
//             meta: {
//               scope: "ELECTION",
//               backing: {
//                 tables: [
//                   "vote_submission",
//                   "vote_tally",
//                   "tally_sheet",
//                   "vote_submission_contest",
//                   "vote_submission_ranking",
//                   "file_upload",
//                 ],
//               },
//             },
//           },

//           // RESULTS (views + compare)
//           {
//             key: "elections.workspace.results",
//             label: "Results",
//             route: "/elections/:electionId/results",
//             icon: "BarChart3",
//             description:
//               "Party / Official / Compare (same layout, different sources).",
//             rolesAllowed: [
//               "SYSTEM_ADMIN",
//               "NEC_ADMIN",
//               "ADMIN",
//               "PARTY_ADMIN",
//               "SUPERVISOR",
//               "COORDINATOR",
//               "AUDITOR",
//             ],
//             meta: {
//               scope: "ELECTION",
//               backing: {
//                 views: [
//                   "v_election_stats_party",
//                   "v_county_stats_party",
//                   "v_district_stats_party",
//                   "v_center_stats_party",
//                   "v_election_stats_official",
//                   "v_county_stats_official",
//                   "v_district_stats_official",
//                   "v_center_stats_official",
//                   "v_candidate_election_stats_party",
//                   "v_candidate_county_stats_party",
//                   "v_candidate_district_stats_party",
//                   "v_candidate_center_stats_party",
//                   "v_candidate_election_stats_official",
//                   "v_candidate_county_stats_official",
//                   "v_candidate_district_stats_official",
//                   "v_candidate_center_stats_official",
//                   "v_candidate_county_compare",
//                 ],
//               },
//             },
//           },

//           // INTEGRITY (discrepancy, anomaly_event, audit_log)
//           {
//             key: "elections.workspace.integrity",
//             label: "Integrity",
//             route: "/elections/:electionId/integrity/discrepancies",
//             icon: "ShieldAlert",
//             description:
//               "Discrepancies, anomalies, and audit context for this election.",
//             rolesAllowed: [
//               "SYSTEM_ADMIN",
//               "NEC_ADMIN",
//               "ADMIN",
//               "SUPERVISOR",
//               "COORDINATOR",
//               "AUDITOR",
//             ],
//             meta: {
//               scope: "ELECTION",
//               backing: {
//                 tables: ["discrepancy", "anomaly_event", "audit_log"],
//               },
//             },
//             children: [
//               {
//                 key: "elections.workspace.integrity.discrepancies",
//                 label: "Discrepancies",
//                 route: "/elections/:electionId/integrity/discrepancies",
//                 icon: "AlertTriangle",
//                 rolesAllowed: [
//                   "SYSTEM_ADMIN",
//                   "NEC_ADMIN",
//                   "ADMIN",
//                   "SUPERVISOR",
//                   "AUDITOR",
//                 ],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["discrepancy"] },
//                 },
//               },
//               {
//                 key: "elections.workspace.integrity.anomalies",
//                 label: "Anomalies",
//                 route: "/elections/:electionId/integrity/anomalies",
//                 icon: "Siren",
//                 rolesAllowed: [
//                   "SYSTEM_ADMIN",
//                   "NEC_ADMIN",
//                   "ADMIN",
//                   "SUPERVISOR",
//                   "AUDITOR",
//                 ],
//                 meta: {
//                   scope: "ELECTION",
//                   backing: { tables: ["anomaly_event"] },
//                 },
//               },
//             ],
//           },

//           // NEC WORKFLOW (conditional)
//           {
//             key: "elections.workspace.nec",
//             label: "NEC Workflow",
//             route: "/elections/:electionId/nec/staging",
//             icon: "BadgeCheck",
//             description: "Stage → publish → history → geo views.",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//             meta: {
//               scope: "ELECTION",
//               backing: {
//                 tables: [
//                   "nec_result_staging",
//                   "nec_result",
//                   "nec_result_history",
//                   "nec_result_geo",
//                 ],
//                 views: ["public.v_nec_result_geo"],
//                 mviews: ["public.mv_nec_result_geo"],
//               },
//             },
//             children: [
//               {
//                 key: "elections.workspace.nec.staging",
//                 label: "Staging",
//                 route: "/elections/:electionId/nec/staging",
//                 icon: "FlaskConical",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//               },
//               {
//                 key: "elections.workspace.nec.published",
//                 label: "Published",
//                 route: "/elections/:electionId/nec/published",
//                 icon: "BadgeCheck",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//               },
//               {
//                 key: "elections.workspace.nec.history",
//                 label: "History",
//                 route: "/elections/:electionId/nec/history",
//                 icon: "History",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//               },
//               {
//                 key: "elections.workspace.nec.geo",
//                 label: "Geo View",
//                 route: "/elections/:electionId/nec/geo",
//                 icon: "MapPinned",
//                 rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//               },
//             ],
//           },
//         ],
//       },
//     ],
//   },

//   {
//     key: "operations",
//     label: "Operations",
//     route: "/operations/submission-queue",
//     icon: "Workflow",
//     description:
//       "Daily work across elections: queue, submissions, observer reports, notifications.",
//     rolesAllowed: ALL,
//     meta: {
//       scope: "GLOBAL",
//       backing: {
//         tables: ["vote_submission", "observer_report", "notification"],
//       },
//     },
//     children: [
//       {
//         key: "operations.queue",
//         label: "Submission Queue",
//         route: "/operations/submission-queue",
//         icon: "Inbox",
//         rolesAllowed: ALL,
//       },
//       {
//         key: "operations.submissions",
//         label: "All Submissions",
//         route: "/operations/submissions",
//         icon: "Table2",
//         rolesAllowed: [
//           "SYSTEM_ADMIN",
//           "NEC_ADMIN",
//           "ADMIN",
//           "PARTY_ADMIN",
//           "SUPERVISOR",
//           "COORDINATOR",
//           "AUDITOR",
//         ],
//       },
//       {
//         key: "operations.observerReports",
//         label: "Observer Reports",
//         route: "/operations/observer-reports",
//         icon: "Binoculars",
//         rolesAllowed: ALL,
//       },
//       {
//         key: "operations.notifications",
//         label: "Notifications",
//         route: "/operations/notifications",
//         icon: "Bell",
//         badgeType: "unread",
//         rolesAllowed: ALL,
//       },
//     ],
//   },

//   {
//     key: "geoRegistry",
//     label: "Geography & Registry",
//     route: "/geography/counties",
//     icon: "Map",
//     description: "Global master data: locations and voter registry pipeline.",
//     rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN", "AUDITOR"],
//     meta: {
//       scope: "GLOBAL",
//       backing: {
//         tables: [
//           "county",
//           "district",
//           "polling_center",
//           "polling_place",
//           "import_batch",
//           "voter_registration_staging",
//           "voter_registration",
//           "voter_registration_public",
//         ],
//       },
//     },
//     children: [
//       {
//         key: "geo.counties",
//         label: "Counties",
//         route: "/geography/counties",
//         icon: "MapPin",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN", "AUDITOR"],
//       },
//       {
//         key: "geo.districts",
//         label: "Districts",
//         route: "/geography/districts",
//         icon: "Route",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN", "AUDITOR"],
//       },
//       {
//         key: "geo.centers",
//         label: "Polling Centers",
//         route: "/geography/centers",
//         icon: "Building2",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN", "AUDITOR"],
//       },
//       {
//         key: "geo.places",
//         label: "Polling Places",
//         route: "/geography/places",
//         icon: "Flag",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN", "AUDITOR"],
//       },

//       {
//         key: "registry",
//         label: "Voter Registry",
//         route: "/registry/import-batches",
//         icon: "UsersRound",
//         description: "Restricted — NEC/System Admin only.",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//         children: [
//           {
//             key: "registry.importBatches",
//             label: "Import Batches",
//             route: "/registry/import-batches",
//             icon: "UploadCloud",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//           {
//             key: "registry.staging",
//             label: "Staging",
//             route: "/registry/staging",
//             icon: "FlaskConical",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//           {
//             key: "registry.private",
//             label: "Private Registry",
//             route: "/registry/private",
//             icon: "Lock",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//           {
//             key: "registry.public",
//             label: "Public Roll",
//             route: "/registry/public",
//             icon: "BookOpen",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//         ],
//       },
//     ],
//   },

//   {
//     key: "reports",
//     label: "Reports",
//     route: "/reports/requests",
//     icon: "FileBarChart",
//     description: "Generate exports and download report files.",
//     rolesAllowed: [
//       "SYSTEM_ADMIN",
//       "NEC_ADMIN",
//       "ADMIN",
//       "PARTY_ADMIN",
//       "AUDITOR",
//     ],
//     meta: {
//       scope: "GLOBAL",
//       backing: { tables: ["report_snapshot", "report_file"] },
//     },
//     children: [
//       {
//         key: "reports.requests",
//         label: "Report Requests",
//         route: "/reports/requests",
//         icon: "ListChecks",
//         rolesAllowed: [
//           "SYSTEM_ADMIN",
//           "NEC_ADMIN",
//           "ADMIN",
//           "PARTY_ADMIN",
//           "AUDITOR",
//         ],
//       },
//       {
//         key: "reports.files",
//         label: "Generated Files",
//         route: "/reports/files",
//         icon: "FolderDown",
//         rolesAllowed: [
//           "SYSTEM_ADMIN",
//           "NEC_ADMIN",
//           "ADMIN",
//           "PARTY_ADMIN",
//           "AUDITOR",
//         ],
//       },
//     ],
//   },

//   {
//     key: "adminSecurity",
//     label: "Admin & Security",
//     route: "/admin/organizations",
//     icon: "Shield",
//     description: "Organizations, users, roles, audit, and security controls.",
//     rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//     meta: {
//       scope: "GLOBAL",
//       backing: {
//         tables: [
//           "organization",
//           "org_membership",
//           "org_setting",
//           "system_users",
//           "user_role",
//           "user_mfa",
//           "user_session",
//           "user_signing_key",
//           "audit_log",
//           "audit_ledger",
//           "audit_ledger_retry",
//           "file_upload",
//         ],
//       },
//     },
//     children: [
//       {
//         key: "admin.tenant",
//         label: "Tenant Management",
//         route: "/admin/organizations",
//         icon: "Buildings",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//         children: [
//           {
//             key: "admin.organizations",
//             label: "Organizations",
//             route: "/admin/organizations",
//             icon: "Buildings",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//           {
//             key: "admin.memberships",
//             label: "Memberships",
//             route: "/admin/memberships",
//             icon: "Users",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//           {
//             key: "admin.orgSettings",
//             label: "Org Settings",
//             route: "/admin/org-settings",
//             icon: "Settings2",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//         ],
//       },
//       {
//         key: "admin.access",
//         label: "Users & Access",
//         route: "/admin/users",
//         icon: "UserCog",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//         children: [
//           {
//             key: "admin.users",
//             label: "Users",
//             route: "/admin/users",
//             icon: "UserCog",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//           {
//             key: "admin.roles",
//             label: "Roles",
//             route: "/admin/roles",
//             icon: "KeyRound",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//           {
//             key: "admin.mfa",
//             label: "MFA",
//             route: "/admin/mfa",
//             icon: "ScanFace",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//           {
//             key: "admin.sessions",
//             label: "Sessions",
//             route: "/admin/sessions",
//             icon: "Timer",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//           {
//             key: "admin.signingKeys",
//             label: "Signing Keys",
//             route: "/admin/signing-keys",
//             icon: "PenTool",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//         ],
//       },
//       {
//         key: "admin.audit",
//         label: "Audit & Files",
//         route: "/admin/audit-logs",
//         icon: "ScrollText",
//         rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//         children: [
//           {
//             key: "admin.auditLogs",
//             label: "Audit Logs",
//             route: "/admin/audit-logs",
//             icon: "ScrollText",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//           {
//             key: "admin.auditLedger",
//             label: "Audit Ledger",
//             route: "/admin/audit-ledger",
//             icon: "LockKeyhole",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN"],
//           },
//           {
//             key: "admin.fileUploads",
//             label: "File Uploads",
//             route: "/admin/file-uploads",
//             icon: "Paperclip",
//             rolesAllowed: ["SYSTEM_ADMIN", "NEC_ADMIN", "ADMIN"],
//           },
//         ],
//       },
//     ],
//   },
// ];

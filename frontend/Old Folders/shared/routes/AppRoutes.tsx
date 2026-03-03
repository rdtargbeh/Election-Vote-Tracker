// import { Route, Navigate } from "react-router-dom";

// import RequireAuth from "../shared/routes/RequireAuth";
// import { RequireElectionAdmin } from "../shared/routes/RequireElectionAdmin";
// import AppShell from "../shared/layout/AppShell";

// import ElectionsLayout from "../pages/election/ElectionCandidatesPage";
// import ElectionManagementPage from "../pages/ElectionManagementPage";

// // placeholders for now — you’ll build these pages next
// import ElectionCandidatesPage from "../pages/elections/ElectionCandidatesPage";
// import ElectionPartiesPage from "../pages/elections/ElectionPartiesPage";
// import ElectionCentersPage from "../pages/elections/ElectionCentersPage";
// import ElectionPlacesPage from "../pages/elections/ElectionPlacesPage";

// export default function AppRoutes() {
//   return (
//     <>
//       {/* ✅ Elections (single sidebar link) */}
//       <Route
//         path="/admin/elections"
//         element={
//           <RequireAuth>
//             <RequireElectionAdmin>
//               <AppShell>
//                 <ElectionsLayout />
//               </AppShell>
//             </RequireElectionAdmin>
//           </RequireAuth>
//         }
//       >
//         {/* index: list of elections */}
//         <Route index element={<ElectionManagementPage />} />

//         {/* election-specific pages */}
//         <Route path=":electionId">
//           <Route index element={<Navigate to="candidates" replace />} />
//           <Route path="candidates" element={<ElectionCandidatesPage />} />
//           <Route path="parties" element={<ElectionPartiesPage />} />
//           <Route path="centers" element={<ElectionCentersPage />} />
//           <Route path="places" element={<ElectionPlacesPage />} />
//         </Route>
//       </Route>
//     </>
//   );
// }

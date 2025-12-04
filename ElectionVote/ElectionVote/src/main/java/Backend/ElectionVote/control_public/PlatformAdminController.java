package Backend.ElectionVote.control_public;

import Backend.ElectionVote.dto.*;
import Backend.ElectionVote.security.AuthorizationService;
import Backend.ElectionVote.service.OrganizationService;
import Backend.ElectionVote.service.PartyService;
import Backend.ElectionVote.service.SystemUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformAdminController {


    private final AuthorizationService authz;
    private final OrganizationService organizationService;
    private final SystemUserService systemUserService;
    private final PartyService partyService;


    /**
     * Create a new organization (tenant).
     * Only SYSTEM_ADMIN (platform admin) can do this.
     */
    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDto createOrganization(@RequestBody @Valid OrganizationCreateRequest req) {
//        authz.requireNecAdminOrPlatformAdmin(); // NEC_ADMIN or SYSTEM_ADMIN
        authz.requirePlatformAdmin(); // uses TenantContext / token to ensure SYSTEM_ADMIN
        return organizationService.create(req);
    }


    /**
     * Create a global/system user (not bound to a specific org).
     *
     * - If req.roleName = SYSTEM_ADMIN → user.systemAdmin = true (platform owner).
     * - Other roles → global users, systemAdmin = false.
     */
    @PostMapping("/system-users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createSystemUser(@RequestBody @Valid UserCreateRequest req) {
        authz.requireAny("SYSTEM_ADMIN");
        return systemUserService.createPlatformAdmin(req);
    }



    /**
     * Create a new political party (global scope).
     *
     * <p>This endpoint is platform-wide and is NOT tenant-scoped.
     * Only a SYSTEM_ADMIN can create parties, because parties are
     * global metadata used by all organizations and elections.
     *
     * <p>Authorization Rules:
     * <ul>
     *   <li>Caller must be authenticated.</li>
     *   <li>Caller must be a SYSTEM_ADMIN (platform admin).</li>
     *   <li>Tenant context (X-Org-Id) is NOT required or used.</li>
     * </ul>
     *
     * <p>Common Use Cases:
     * <ul>
     *   <li>Populate global party list (UP, CDC, ANC, LP, etc.)</li>
     *   <li>Maintain official NEC party registry</li>
     *   <li>Prepare global party catalog for elections</li>
     * </ul>
     *
     * <p>Response:
     * Returns the created {@link PartyDto} with its assigned UUID.
     *
     * @param req Party creation request payload
     * @return Created Party DTO with HTTP 201 (Created)
     * @throws AccessDeniedException if the caller is not SYSTEM_ADMIN
     * @throws IllegalArgumentException if party name or abbreviation already exists
     */
    @PostMapping("/party")
    public ResponseEntity<PartyDto> create(@Valid @RequestBody PartyCreateRequest req) {

        // --- Authorization ---
        // Only platform SYSTEM_ADMIN can create global parties.
        authz.requirePlatformAdmin();

        // --- Handle creation ---
        PartyDto created = partyService.create(req);

        // Return result with HTTP 201
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }


    /**
     * Activate or deactivate an organization (tenant).
     *
     * <p>This endpoint allows a platform-level SYSTEM_ADMIN to enable or disable
     * a tenant. When an organization is deactivated:
     *
     * <ul>
     *   <li>The organization's <code>is_active</code> flag is set to <code>false</code>.</li>
     *   <li>All org_membership records under this tenant are disabled (recommended safety measure).</li>
     *   <li>User sessions for this tenant may also be revoked (if implemented in the service).</li>
     * </ul>
     *
     * <p>Deactivation does <strong>NOT</strong> delete the organization or its data.
     * It simply freezes the tenant so that:
     *
     * <ul>
     *   <li>No one can log in under that organization.</li>
     *   <li>No votes, reports, submissions, messages, or uploads can be created.</li>
     *   <li>Existing data is preserved for audits and later reactivation.</li>
     * </ul>
     *
     * <h3>Authorization:</h3>
     * Only platform-level <strong>SYSTEM_ADMIN</strong> users may call this endpoint.
     *
     * <h3>Example Request:</h3>
     *
     * <pre>
     * PATCH /api/platform/organizations/00089918-4d00-42f3-9e40-3319c4cbb93c/status?active=false
     * </pre>
     *
     * <h3>Query Parameters:</h3>
     * <ul>
     *   <li><strong>active</strong> — <code>true</code> to activate, <code>false</code> to deactivate.</li>
     * </ul>
     *
     * <h3>Responses:</h3>
     * <ul>
     *   <li><strong>204 NO CONTENT</strong> — Operation successful.</li>
     *   <li><strong>403 FORBIDDEN</strong> — Caller is not SYSTEM_ADMIN.</li>
     *   <li><strong>404 NOT FOUND</strong> — Organization does not exist.</li>
     * </ul>
     */
    @PatchMapping("/organizations/{orgId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setOrganizationActive(
            @PathVariable UUID orgId,
            @RequestParam boolean active) {

        authz.requirePlatformAdmin();
        organizationService.setActive(orgId, active);
    }

    // 8a2503cb-8e62-49f9-9c17-4735661338f0   up
    // 6c12f086-ae4a-464c-82d0-cb76822bcbaf  cdc
    // dde191b0-ed2c-4771-a6be-0ec66849a4b5   anc
    //  1922d313-233d-4191-9c86-c5a6f84fe299  nec

    // header
    // X-Org-Id: 010fde0a-c422-43bf-ba9d-310413c55214

}

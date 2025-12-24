package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.dto.UserUpdateRequest;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.service.SystemUserService;
import Backend.ElectionVote.utility.AssignCountyRoleRequest;
import Backend.ElectionVote.utility.ChangePasswordRequest;
import Backend.ElectionVote.utility.UserSearchRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    @Autowired
    private  SystemUserService systemUserService;


    /* ======================= Core CRUD / Query (Tenant-scoped) ======================= */

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest req) {
        UserDto dto = systemUserService.createInTenant(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{userId}")
    public UserDto update(@PathVariable UUID userId, @Valid @RequestBody UserUpdateRequest req) {
        return systemUserService.updateInTenant(userId, req);
    }


    /**
     * Assign a tenant user to a county and a tenant-scoped role
     * (e.g., "COORDINATOR" for Nimba County).
     *
     * Security:
     *  - Only tenant ADMIN or platform SYSTEM_ADMIN can call this.
     *  - Tenant is derived from X-Org-Id / subdomain (TenantContext).
     */
    @PatchMapping("/{userId}/assign-county-role")
    public ResponseEntity<UserDto> assignUserToCountyAndRole(
            @PathVariable("userId") UUID userId,
            @RequestBody AssignCountyRoleRequest req
    ) {
        UserDto updated = systemUserService.assignUserToCountyAndRole(
                userId,
                req.getCountyId(),
                req.getRoleName()
        );
        return ResponseEntity.ok(updated);
    }


    @GetMapping
    public Page<UserDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "dateCreated") Pageable pageable
    ) {
        UserSearchRequest req = new UserSearchRequest(q, active, null, null, null);
        return systemUserService.searchInTenant(req, pageable);
    }


    /* ======================= Status Flags ======================= */

    @PatchMapping("/{userId}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActive(@PathVariable UUID userId, @RequestBody @Valid SetBooleanRequest body) {
        systemUserService.setActiveInTenant(userId, body.value());
    }

    @PatchMapping("/{id}/verified")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setVerified(@PathVariable UUID id, @RequestBody @Valid SetBooleanRequest body) {
        systemUserService.setVerifiedInTenant(id, body.value());
    }

    /* ======================= Credentials & Security ======================= */

    @PostMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID id, @Valid @RequestBody ChangePasswordRequest req) {
        systemUserService.changePassword(id, req);
    }

    @PostMapping("/{id}/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminResetPassword(@PathVariable UUID userId, @RequestBody @Valid AdminResetPasswordRequest body) {
        systemUserService.adminResetPasswordInTenant(userId, body.newPassword());
    }

    @PatchMapping("/{userId}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLock(@PathVariable UUID userId, @Valid @RequestBody SetLockRequest body) {
        systemUserService.setLockInTenant(userId, body.lock(), body.until());
    }

    @PostMapping("/{id}/login-failure")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordLoginFailure(@PathVariable UUID id) {
        systemUserService.recordLoginFailure(id);
    }

    @PostMapping("/{id}/login-success")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordLoginSuccess(@PathVariable UUID id) {
        systemUserService.recordLoginSuccess(id);
    }

    /* ======================= Role & Affiliations ======================= */

    @PatchMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable UUID userId, @RequestBody @Valid AssignRoleRequest body) {
        systemUserService.assignRoleInTenant(userId, body.roleName());
    }

    @PatchMapping("/{id}/party")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignParty(@PathVariable UUID id, @RequestBody AssignIdRequest body) {
        systemUserService.assignPartyInTenant(id, body == null ? null : body.id());
    }

    @PatchMapping("/{id}/county")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignCounty(@PathVariable UUID id, @RequestBody AssignIdRequest body) {
        systemUserService.assignCountyInTenant(id, body == null ? null : body.id());
    }

    @PatchMapping("/{id}/default-org")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setDefaultOrg(@PathVariable UUID id, @RequestBody AssignIdRequest body) {
        systemUserService.setDefaultOrgInTenant(id, body == null ? null : body.id());
    }


    // ----------------- Add this method -----------------
    /**
     * Return the current authenticated user (tenant-scoped).
     * Client MUST include X-Org-Id header for tenant-scoped requests.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @RequestHeader(name = "X-Org-Id", required = true) UUID orgId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 1) If principal is Jwt and contains a UUID claim -> try load by UUID (tenant-aware)
        Object principal = authentication.getPrincipal();
        UUID userId = null;
        if (principal instanceof Jwt jwt) {
            // prefer UUID claims
            Object claim = jwt.getClaim("userId");
            if (claim == null) claim = jwt.getClaim("user_id");
            if (claim == null) claim = jwt.getClaim("id");
            if (claim == null) claim = jwt.getSubject();
            if (claim instanceof String) {
                try {
                    userId = UUID.fromString((String) claim);
                } catch (IllegalArgumentException ignored) {
                    userId = null;
                }
            }
        }

        // 2) If we obtained a UUID, return user by id (tenant-scoped)
        if (userId != null) {
            Optional<UserDto> dto = systemUserService.getInTenant(userId, orgId);
            return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        }

        // 3) Fallback: use authentication.getName() (username) and lookup by orgId
        String username = authentication.getName();
        if (username != null && !username.isBlank()) {
            Optional<UserDto> dto = systemUserService.getByUsernameInTenant(username, orgId);
            return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        }

        // 4) Could not resolve user
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable UUID id) {
        Optional<UserDto> user = systemUserService.getInTenant(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    /* ======================= Small request bodies ======================= */

    public record SetBooleanRequest(@NotNull Boolean value) {}

    public record AdminResetPasswordRequest(@NotBlank String newPassword) {}

    public record SetLockRequest(
            @NotNull Boolean lock,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime until // optional when lock=false
    ) {}

    public record AssignRoleRequest(@NotNull RoleName roleName) {}

    /** Pass {"id":"<uuid>"} or {} / null to clear (for party/county/default-org). */
    public record AssignIdRequest(UUID id) {}



}

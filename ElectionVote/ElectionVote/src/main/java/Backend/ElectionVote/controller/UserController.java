package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.UserCreateRequest;
import Backend.ElectionVote.dto.UserDto;
import Backend.ElectionVote.dto.UserUpdateRequest;
import Backend.ElectionVote.enums.RoleName;
import Backend.ElectionVote.service.SystemUserService;
import Backend.ElectionVote.utility.ChangePasswordRequest;
import Backend.ElectionVote.utility.UserSearchRequest;
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

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable UUID id) {
        Optional<UserDto> user = systemUserService.getInTenant(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest req) {
        return systemUserService.updateInTenant(id, req);
    }

    /* ======================= Status Flags ======================= */

    @PatchMapping("/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActive(@PathVariable UUID id, @RequestBody @Valid SetBooleanRequest body) {
        systemUserService.setActiveInTenant(id, body.value());
    }

    @PatchMapping("/{id}/verified")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setVerified(@PathVariable UUID id, @RequestBody @Valid SetBooleanRequest body) {
        systemUserService.setVerifiedInTenant(id, body.value());
    }

    /* ======================= Credentials & Security ======================= */

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID id, @Valid @RequestBody ChangePasswordRequest req) {
        systemUserService.changePassword(id, req);
    }

    @PostMapping("/{id}/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminResetPassword(@PathVariable UUID id, @RequestBody @Valid AdminResetPasswordRequest body) {
        systemUserService.adminResetPasswordInTenant(id, body.newPassword());
    }

    @PatchMapping("/{id}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLock(@PathVariable UUID id, @Valid @RequestBody SetLockRequest body) {
        systemUserService.setLockInTenant(id, body.lock(), body.until());
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

    @PatchMapping("/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable UUID id, @RequestBody @Valid AssignRoleRequest body) {
        systemUserService.assignRoleInTenant(id, body.roleName());
    }

    @PatchMapping("/{id}/party")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignParty(@PathVariable UUID id, @RequestBody AssignIdRequest body) {
        // pass null to clear
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

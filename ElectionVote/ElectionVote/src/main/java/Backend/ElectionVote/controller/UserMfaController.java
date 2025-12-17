package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.MfaProvisionResponse;
import Backend.ElectionVote.service.UserMfaService;
import Backend.ElectionVote.utility.MfaStatusResponse;
import Backend.ElectionVote.utility.MfaVerifyRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/mfa")
public class UserMfaController {

    private final UserMfaService service;

    public UserMfaController(UserMfaService service) {
        this.service = service;
    }

    /** Start TOTP setup; return otpauth URL & secret ONCE to be scanned/imported by the app. */
    @PostMapping("/{userId}/totp/provision")
    public MfaProvisionResponse provision(@PathVariable UUID userId,
                                          @RequestParam(defaultValue = "ElectionVote") String issuer) {
        return service.provisionTotp(userId, issuer);
    }

    /** Verify a code and enable MFA */
    @PostMapping("/{userId}/totp/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyAndEnable(@PathVariable UUID userId,
                                @RequestBody @NotNull MfaVerifyRequest body) {
        service.verifyAndEnableTotp(userId, body.getCode());
    }

    /** Disable MFA */
    @PostMapping("/{userId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID userId) {
        service.disable(userId);
    }

    /** Status */
    @GetMapping("/{userId}/status")
    public MfaStatusResponse status(@PathVariable UUID userId) {
        return service.status(userId);
    }
}
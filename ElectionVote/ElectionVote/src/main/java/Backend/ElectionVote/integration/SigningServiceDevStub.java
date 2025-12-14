package Backend.ElectionVote.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;


/**
 * Development signing stub — deterministic pseudo-signature for local/test runs.
 * Use real KMS-backed SigningService in production.
 */
@Component
@ConditionalOnMissingBean(SigningService.class)
public class SigningServiceDevStub implements SigningService {


    @Override
    public SignResult signHex(String hex) {
        if (hex == null) hex = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] sig = md.digest(hex.getBytes(StandardCharsets.UTF_8));
            String signature = java.util.HexFormat.of().formatHex(sig);
            // return a stable dev key id if you want reproducible behavior in tests:
            UUID devKeyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            return new SignResult("dev:" + signature, devKeyId);
        } catch (Exception e) {
            throw new RuntimeException("Dev signing failed", e);
        }
    }



}


package Backend.ElectionVote.integration;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;


/**
 * Development signing stub — deterministic pseudo-signature for local/test runs.
 * Use real KMS-backed SigningService in production.
 */
@Component
public class SigningServiceDevStub implements SigningService {

    @Override
    public SignResult signHex(String hex) {
        if (hex == null) hex = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] sig = md.digest(hex.getBytes(StandardCharsets.UTF_8));
            String signature = java.util.HexFormat.of().formatHex(sig);
            // return a UUID as the key id to match the SignResult signature that expects a UUID
            UUID keyId = UUID.randomUUID();
            return new SignResult("dev:" + signature, keyId);
        } catch (Exception e) {
            throw new RuntimeException("Dev signing failed", e);
        }
    }

}



///**
// * Dev stub for signing. Use profile 'dev' during local runs.
// * NEVER use in production.
// */
//@Service
//@Profile({"dev","test"})
//public class SigningServiceDevStub implements SigningService {
//
//    @Override
//    public SignResult signHex(String hexPayload) {
//        // simple pseudo-signature (base64 of payload + key id); deterministic for tests
//        String keyId = "dev-key";
//        String signature = Base64.getEncoder().encodeToString((hexPayload + "|" + keyId).getBytes(StandardCharsets.UTF_8));
//        return new SignResult(signature, UUID.nameUUIDFromBytes(keyId.getBytes(StandardCharsets.UTF_8)));
//    }
//}

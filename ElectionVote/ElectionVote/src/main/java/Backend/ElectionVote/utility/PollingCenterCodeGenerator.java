package Backend.ElectionVote.utility;

import Backend.ElectionVote.entity.District;

import java.util.UUID;

public final class PollingCenterCodeGenerator {

    private PollingCenterCodeGenerator() { }

    public static String generateCode(District district) {
        // Prefix based on district (first 4 chars of UUID, or later your own district code)
        String districtPart = district.getDistrictId()
                .toString()
                .replace("-", "")
                .substring(0, 4)
                .toUpperCase();

        // Random 4 chars from UUID
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 4)
                .toUpperCase();

        return "PC-" + districtPart + "-" + randomPart;
    }
}

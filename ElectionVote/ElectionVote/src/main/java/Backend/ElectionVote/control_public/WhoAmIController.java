package Backend.ElectionVote.control_public;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class WhoAmIController {

    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication auth) {
        if (auth == null) return Map.of("authenticated", false);
        return Map.of(
                "authenticated", true,
                "name", auth.getName(),
                "authorities", auth.getAuthorities()
                        .stream().map(a -> a.getAuthority()).collect(Collectors.toList())
        );
    }
}
package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.ChatRoomDmDto;
import Backend.ElectionVote.service.ChatRoomDmService;
import Backend.ElectionVote.utility.OpenDmRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/dm")
public class ChatRoomDmController {

    private final ChatRoomDmService service;

    /** Open (or get existing) DM with another user in current tenant */
    @PostMapping("/open")
    public ChatRoomDmDto open(@RequestBody OpenDmRequest req) {
        return service.openOrGet(req.getOtherUserId());
    }

    /** List my DMs in current tenant */
    @GetMapping("/my")
    public List<ChatRoomDmDto> myDms() {
        return service.myDms();
    }
}

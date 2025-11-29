package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.ChatRoomCreateRequest;
import Backend.ElectionVote.dto.ChatRoomDto;
import Backend.ElectionVote.dto.ChatRoomUpdateRequest;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.entity.SystemUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatRoomService {

    ChatRoomDto create(Organization org, SystemUser creator, ChatRoomCreateRequest req);

    ChatRoomDto get(Organization org, UUID roomId);

    Page<ChatRoomDto> search(Organization org, String q, boolean includeArchived, Pageable pageable);

    ChatRoomDto update(Organization org, UUID roomId, ChatRoomUpdateRequest req);

    void archive(Organization org, UUID roomId, boolean archived);
}
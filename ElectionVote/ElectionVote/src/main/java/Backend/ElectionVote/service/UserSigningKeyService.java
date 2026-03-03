package Backend.ElectionVote.service;


import Backend.ElectionVote.dto.UserSigningKeyCreateRequest;
import Backend.ElectionVote.dto.UserSigningKeyDto;

import java.util.List;
import java.util.UUID;

public interface UserSigningKeyService {
    UserSigningKeyDto createForUser(UUID userId, UserSigningKeyCreateRequest req);

    List<UserSigningKeyDto> listForUser(UUID userId);

    UserSigningKeyDto getById(UUID keyId);

    UserSigningKeyDto revoke(UUID keyId);


}
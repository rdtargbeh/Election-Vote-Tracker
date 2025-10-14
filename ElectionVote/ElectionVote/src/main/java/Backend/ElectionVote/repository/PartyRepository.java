package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.OrgMembership;
import Backend.ElectionVote.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartyRepository extends JpaRepository<Party, UUID> {
}

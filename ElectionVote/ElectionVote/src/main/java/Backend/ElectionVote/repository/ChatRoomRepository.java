package Backend.ElectionVote.repository;

import Backend.ElectionVote.entity.ChatRoom;
import Backend.ElectionVote.entity.Organization;
import Backend.ElectionVote.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("""
        select r
        from ChatRoom r
        where r.organization = :org
          and r.isArchived = false
          and (:q is null or lower(coalesce(r.name,'')) like lower(concat('%', :q, '%')))
        order by r.dateCreated desc
    """)
    Page<ChatRoom> searchActive(@Param("org") Organization org,
                                @Param("q") String q,
                                Pageable pageable);

    Optional<ChatRoom> findByOrganizationAndRoomId(Organization org, UUID roomId);

    @Query("""
        select (count(r) > 0)
        from ChatRoom r
        where r.organization = :org and r.roomType = :type and lower(r.name) = lower(:name)
    """)
    boolean existsByOrgTypeAndName(@Param("org") Organization org,
                                   @Param("type") RoomType type,
                                   @Param("name") String name);

    @Query("""
        select r
        from ChatRoom r
        where r.organization.orgId = :orgId
          and (:includeArchived = true or r.isArchived = false)
          and (:q is null or lower(coalesce(r.name, '')) like lower(concat('%', :q, '%')))
        order by r.dateCreated desc
    """)
    Page<ChatRoom> searchInOrg(@Param("orgId") UUID orgId,
                               @Param("q") String q,
                               @Param("includeArchived") boolean includeArchived,
                               Pageable pageable);

    @Query("""
        select r
        from ChatRoom r
        where r.organization.orgId = :orgId
          and r.roomId = :roomId
    """)
    Optional<ChatRoom> findInOrg(@Param("orgId") UUID orgId, @Param("roomId") UUID roomId);

}
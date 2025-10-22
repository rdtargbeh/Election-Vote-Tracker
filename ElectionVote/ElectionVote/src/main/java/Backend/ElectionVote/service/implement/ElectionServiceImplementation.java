package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.service.ElectionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

public class ElectionServiceImplementation implements ElectionService {

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public long countVisibleSubmissions() {
        Object x = em.createNativeQuery("SELECT COUNT(*) FROM public.vote_submission").getSingleResult();
        return ((Number)x).longValue(); // RLS limits this automatically
    }

}

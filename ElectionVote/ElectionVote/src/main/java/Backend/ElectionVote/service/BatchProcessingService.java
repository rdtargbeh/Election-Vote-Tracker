package Backend.ElectionVote.service;

import java.util.UUID;

public interface BatchProcessingService {
    /**
     * Enqueue the batch for asynchronous processing. Returns immediately.
     */
    void enqueueBatch(UUID batchId, UUID actorUserId);
}
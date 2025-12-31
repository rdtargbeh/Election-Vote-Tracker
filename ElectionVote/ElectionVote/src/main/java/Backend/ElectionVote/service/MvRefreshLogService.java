package Backend.ElectionVote.service;


import Backend.ElectionVote.enums.RefreshStatus;

public interface MvRefreshLogService {
    /**
     * Mark a view refresh as started (optional).
     * Some callers prefer to record RUNNING .. then update to SUCCESS/FAILED on completion.
     */
    void markStart(String mvName);

    /**
     * Mark a view refresh as finished with duration and status.
     * status recommended values: SUCCESS, FAILED
     */
    void markFinish(String mvName, long durationMs, RefreshStatus status, String notes);
}
package org.sagebionetworks.repo.model.dbo.curation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;

public interface CurationTaskDao {
    CurationTask createCurationTask(Long userId, CurationTask toCreate);

    CurationTask updateCurationTask(Long userId, CurationTask toUpdate);

    /**
     * Atomically creates a curation task and its initial status in a single row. The status columns
     * (state, dueDate) are written from the bundle's status; the creator stamps the status audit
     * fields. Any executionDetails supplied on the status are ignored (machine-owned).
     *
     * @param userId the principal creating the task
     * @param toCreate the task and status to create; both are written to the same new row
     * @return the created task and status
     */
    TaskBundle createTaskBundle(Long userId, TaskBundle toCreate);

    /**
     * Atomically updates a curation task and its status in a single row, guarded by a single etag.
     * The etag is read from the bundle's task; any executionDetails supplied on the status are
     * ignored (machine-owned).
     *
     * @param userId the principal updating the task
     * @param toUpdate the task and status to update; both are written to the same existing row
     * @return the updated task and status
     */
    TaskBundle updateTaskBundle(Long userId, TaskBundle toUpdate);

    Optional<CurationTask> getCurationTask(Long taskId);

    void deleteCurationTask(Long taskId);

    List<CurationTask> getCurationTasks(Long projectId, long limit, long offset);

    TaskStatus getTaskStatus(Long taskId);

    TaskStatus updateTaskStatus(Long userId, Long taskId, TaskStatus statusUpdate);

    /**
     * Clears the activeSessionId from the execution details of the given task, if execution details exist.
     * Must be called within an existing write transaction.
     */
    void clearActiveSessionId(Long taskId);

    List<TaskBundle> getCurationTaskBundles(List<Long> projectIds, List<Long> assigneeIds,
            List<TaskState> stateFilter, List<Long> taskIds, long limit, long offset);

    Set<Long> getDistinctProjectIds();

}

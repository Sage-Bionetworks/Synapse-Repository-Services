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

    Optional<CurationTask> getCurationTask(Long taskId);

    void deleteCurationTask(Long taskId);

    List<CurationTask> getCurationTasks(Long projectId, long limit, long offset);

    TaskStatus getTaskStatus(Long taskId);

    TaskStatus updateTaskStatus(Long userId, Long taskId, TaskStatus statusUpdate);

    List<TaskBundle> getCurationTaskBundles(List<Long> projectIds, List<Long> assigneeIds,
            List<TaskState> stateFilter, long limit, long offset);

    Set<Long> getDistinctProjectIds();

}

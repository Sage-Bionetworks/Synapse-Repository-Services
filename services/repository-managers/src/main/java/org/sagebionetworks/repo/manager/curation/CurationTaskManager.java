package org.sagebionetworks.repo.manager.curation;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskStatus;


public interface CurationTaskManager {

    void validateUpdateTaskStatus(UserInfo userInfo, CurationTask task);

    CurationTask createCurationTask(UserInfo userInfo, CurationTask toCreate);

    CurationTask getCurationTask(UserInfo userInfo, Long taskId);

    CurationTask updateCurationTask(UserInfo userInfo, CurationTask toUpdate);

    void deleteCurationTask(UserInfo userInfo, Long taskId);

    ListCurationTaskResponse getCurationTasks(UserInfo userInfo, ListCurationTaskRequest request);

    TaskStatus getTaskStatus(UserInfo userInfo, Long taskId);

    TaskStatus updateTaskStatus(UserInfo userInfo, Long taskId, TaskStatus statusUpdate);

    /**
     * Atomically creates a curation task and its initial status in a single call. All task and status
     * fields are validated before anything is persisted, so an invalid field rejects the whole request
     * and no partial task is created. Requires CREATE access on the task's project.
     *
     * @param userInfo the caller
     * @param toCreate the task and status to create
     * @return the created task and status
     */
    TaskBundle createTaskBundle(UserInfo userInfo, TaskBundle toCreate);

    /**
     * Atomically updates a curation task and its status in a single call, guarded by a single etag
     * read from the bundle's task. All task and status fields are validated before anything is
     * persisted. Requires UPDATE access on the task's project.
     *
     * @param userInfo the caller
     * @param toUpdate the task and status to update
     * @return the updated task and status
     */
    TaskBundle updateTaskBundle(UserInfo userInfo, TaskBundle toUpdate);
}
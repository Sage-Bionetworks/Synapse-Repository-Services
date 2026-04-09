package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CurationTaskService {

    CurationTask createCurationTask(Long userId, CurationTask toCreate);

    CurationTask updateCurationTask(Long userId, CurationTask toUpdate)
            throws NotFoundException;

    CurationTask getCurationTask(Long userId, Long taskId);

    void deleteCurationTask(Long userId, Long taskId) throws NotFoundException;

    ListCurationTaskResponse getCurationTasks(Long userId, ListCurationTaskRequest request);

    TaskStatus getTaskStatus(Long userId, Long taskId);

    TaskStatus updateTaskStatus(Long userId, Long taskId, TaskStatus statusUpdate);
}

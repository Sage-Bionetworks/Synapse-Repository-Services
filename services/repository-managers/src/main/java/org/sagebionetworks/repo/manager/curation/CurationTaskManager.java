package org.sagebionetworks.repo.manager.curation;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;


public interface CurationTaskManager {

    CurationTask createCurationTask(UserInfo userInfo, CurationTask toCreate);

    CurationTask getCurationTask(UserInfo userInfo, Long taskId);

    CurationTask updateCurationTask(UserInfo userInfo, CurationTask toUpdate);

    void deleteCurationTask(UserInfo userInfo, Long taskId);

    ListCurationTaskResponse getCurationTasks(UserInfo userInfo, ListCurationTaskRequest request);
}
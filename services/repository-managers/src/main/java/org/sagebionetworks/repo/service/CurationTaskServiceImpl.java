package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CurationTaskServiceImpl implements CurationTaskService {

    private final CurationTaskManager curationTaskManager;
    private final UserManager userManager;

    @Autowired
    public CurationTaskServiceImpl(
            CurationTaskManager curationTaskManager,
            UserManager userManager
    ) {
        this.curationTaskManager = curationTaskManager;
        this.userManager = userManager;
    }

    @Override
    public CurationTask createCurationTask(Long userId, CurationTask toCreate) {
        UserInfo userInfo = userManager.getUserInfo(userId);
        return curationTaskManager.createCurationTask(userInfo, toCreate);
    }

    @Override
    public CurationTask updateCurationTask(Long userId, CurationTask toUpdate) throws NotFoundException {
        UserInfo userInfo = userManager.getUserInfo(userId);
        return curationTaskManager.updateCurationTask(userInfo, toUpdate);
    }

    @Override
    public CurationTask getCurationTask(Long userId, Long taskId) {
        UserInfo userInfo = userManager.getUserInfo(userId);
        return curationTaskManager.getCurationTask(userInfo, taskId);
    }

    @Override
    public void deleteCurationTask(Long userId, Long taskId) throws NotFoundException {
        UserInfo userInfo = userManager.getUserInfo(userId);
        curationTaskManager.deleteCurationTask(userInfo, taskId);
    }

    @Override
    public ListCurationTaskResponse getCurationTasks(Long userId, ListCurationTaskRequest request) {
        UserInfo userInfo = userManager.getUserInfo(userId);
        return curationTaskManager.getCurationTasks(userInfo, request);
    }
}

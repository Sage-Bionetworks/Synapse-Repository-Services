package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CurationTaskServiceImpl implements CurationTaskService {
    @Override
    public CurationTask createCurationTask(Long userId, CurationTask toCreate) {
        // TODO: Stub
        throw new NotFoundException("The service you requested has not yet been implemented");
    }

    @Override
    public CurationTask updateCurationTask(Long userId, CurationTask toUpdate) throws NotFoundException {
        // TODO: Stub
        throw new NotFoundException("The service you requested has not yet been implemented");
    }

    @Override
    public CurationTask getCurationTask(Long userId, String taskId) {
        // TODO: Stub
        throw new NotFoundException("The service you requested has not yet been implemented");
    }

    @Override
    public void deleteCurationTask(Long userId, String taskId) throws NotFoundException {
        // TODO: Stub
        throw new NotFoundException("The service you requested has not yet been implemented");

    }

    @Override
    public ListCurationTaskResponse getCurationTasks(Long userId, ListCurationTaskRequest request) {
        // TODO: Stub
        throw new NotFoundException("The service you requested has not yet been implemented");
    }
}

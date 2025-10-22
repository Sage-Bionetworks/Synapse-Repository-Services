package org.sagebionetworks.repo.model.dbo.curation;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.curation.CurationTask;

public interface CurationTaskDao {
    CurationTask createCurationTask(Long userId, CurationTask toCreate);

    CurationTask updateCurationTask(Long userId, CurationTask toUpdate);

    Optional<CurationTask> getCurationTask(Long taskId);

    void deleteCurationTask(Long taskId);

    List<CurationTask> getCurationTasks(Long projectId, long limit, long offset);

}

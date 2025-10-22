package org.sagebionetworks.repo.manager.curation;

import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dbo.curation.CurationTaskDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CurationTaskManagerImpl implements CurationTaskManager {

    private final CurationTaskDao curationTaskDao;
    private final AuthorizationManager authorizationManager;
    private final EntityManager entityManager;

    @Autowired
    public CurationTaskManagerImpl(CurationTaskDao curationTaskDao, AuthorizationManager authorizationManager, EntityManager entityManager) {
        this.curationTaskDao = curationTaskDao;
        this.authorizationManager = authorizationManager;
        this.entityManager = entityManager;
    }

    @Override
    @WriteTransaction
    public CurationTask createCurationTask(UserInfo userInfo, CurationTask curationTask) {
        validateCurationTask(userInfo, curationTask);

        authorizationManager.canAccess(userInfo, curationTask.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE).checkAuthorizationOrElseThrow();

        return curationTaskDao.createCurationTask(userInfo.getId(), curationTask);
    }

    @Override
    public CurationTask getCurationTask(UserInfo userInfo, Long taskId) {
        CurationTask task = curationTaskDao.getCurationTask(taskId).orElseThrow(() -> new NotFoundException("Task not found: " + taskId));

        authorizationManager.canAccess(userInfo, task.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();

        return task;
    }

    @Override
    @WriteTransaction
    public CurationTask updateCurationTask(UserInfo userInfo, CurationTask toUpdate) {
        validateCurationTask(userInfo, toUpdate);
        ValidateArgument.required(toUpdate.getTaskId(), "taskId");

        CurationTask existing = getCurationTask(userInfo, toUpdate.getTaskId());

        if (!existing.getProjectId().equals(toUpdate.getProjectId())) {
            throw new IllegalArgumentException("The project for a MetadataTask cannot be changed.");
        }

        authorizationManager.canAccess(userInfo, existing.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();

        return curationTaskDao.updateCurationTask(userInfo.getId(), toUpdate);
    }

    @Override
    @WriteTransaction
    public void deleteCurationTask(UserInfo userInfo, Long taskId) {
        CurationTask existing = getCurationTask(userInfo, taskId);

        authorizationManager.canAccess(userInfo, existing.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();

        curationTaskDao.deleteCurationTask(taskId);
    }

    @Override
    public ListCurationTaskResponse getCurationTasks(UserInfo userInfo, ListCurationTaskRequest request) {
        NextPageToken token = new NextPageToken(request.getNextPageToken());
        ListCurationTaskResponse response = new ListCurationTaskResponse();

        authorizationManager.canAccess(userInfo, request.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();

        List<CurationTask> tasks = curationTaskDao.getCurationTasks(KeyFactory.stringToKey(request.getProjectId()), token.getLimitForQuery(), token.getOffset());
        response.setPage(tasks);
        response.setNextPageToken(token.getNextPageTokenForCurrentResults(tasks));
        return response;
    }

    private void validateCurationTask(UserInfo userInfo, CurationTask task) {
        ValidateArgument.required(task, "MetadataTask");
        ValidateArgument.required(task.getProjectId(), "projectId");
        ValidateArgument.required(task.getDataType(), "dataType");
        ValidateArgument.required(task.getTaskProperties(), "taskProperties");

        if (task.getTaskProperties() instanceof FileBasedMetadataTaskProperties) {
            FileBasedMetadataTaskProperties fileBasedMetadataTaskProperties = (FileBasedMetadataTaskProperties) task.getTaskProperties();
            ValidateArgument.required(fileBasedMetadataTaskProperties.getFileViewId(), "fileViewId");
            ValidateArgument.required(fileBasedMetadataTaskProperties.getUploadFolderId(), "uploadFolderId");

            EntityType typeOfSpecifiedView = entityManager.getEntityType(userInfo, fileBasedMetadataTaskProperties.getFileViewId());
            ValidateArgument.requirement(EntityType.entityview.equals(typeOfSpecifiedView), "The fileViewId must be an EntityView.");

            EntityType typeOfSpecifiedFolder = entityManager.getEntityType(userInfo, fileBasedMetadataTaskProperties.getUploadFolderId());
            ValidateArgument.requirement(EntityType.folder.equals(typeOfSpecifiedFolder) || EntityType.project.equals(typeOfSpecifiedFolder),
                    "The uploadFolderId must be a Folder or Project.");
        } else if (task.getTaskProperties() instanceof RecordBasedMetadataTaskProperties) {
            RecordBasedMetadataTaskProperties recordBasedMetadataTaskProperties = (RecordBasedMetadataTaskProperties) task.getTaskProperties();
            ValidateArgument.required(recordBasedMetadataTaskProperties.getRecordSetId(), "recordSetId");

            EntityType typeOfSpecifiedRecordSet = entityManager.getEntityType(userInfo, recordBasedMetadataTaskProperties.getRecordSetId());
            ValidateArgument.requirement(EntityType.recordset.equals(typeOfSpecifiedRecordSet), "The recordSetId must be a RecordSet.");
        } else {
            throw new IllegalArgumentException("Unknown CurationTaskProperties concreteType: " + task.getTaskProperties().getConcreteType());
        }
    }
}

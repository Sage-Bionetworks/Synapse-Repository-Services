package org.sagebionetworks.repo.manager.curation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dbo.curation.CurationTaskDao;
import org.sagebionetworks.repo.model.dbo.curation.CurationTaskPropertiesType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(MockitoExtension.class)
public class CurationTaskManagerImplUnitTest {
    @Mock
    CurationTaskDao mockCurationTaskDao;

    @Mock
    AuthorizationManager mockAuthorizationManager;

    @Mock
    EntityManager mockEntityManager;

    @Mock
    private AuthorizationStatus mockAuthorizationStatus;

    @InjectMocks
    CurationTaskManagerImpl curationTaskManager;

    Long userId = 101L;
    private UserInfo userInfo;

    Long taskId = 987L;
    String projectId = "syn123";
    String fileViewId = "syn456";
    String recordSetId = "syn789";
    String uploadFolderId = "syn1000";

    @BeforeEach
    public void setup() {
        userInfo = new UserInfo(false, userId);
    }

    @ParameterizedTest
    @EnumSource(CurationTaskPropertiesType.class)
    public void testCreateCurationTaskWithSuccess(CurationTaskPropertiesType type) {
        CurationTask toCreate = createCurationTask(type);
        CurationTask createdByDao = new CurationTask().setTaskId(999L);

        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.createCurationTask(eq(userId), eq(toCreate))).thenReturn(createdByDao);
        if (CurationTaskPropertiesType.FILE_BASED.equals(type)) {
            when(mockEntityManager.getEntityType(eq(userInfo), eq(fileViewId))).thenReturn(EntityType.entityview);
            when(mockEntityManager.getEntityType(eq(userInfo), eq(uploadFolderId))).thenReturn(EntityType.folder);
        } else if (CurationTaskPropertiesType.RECORD_BASED.equals(type)) {
            when(mockEntityManager.getEntityType(eq(userInfo), eq(recordSetId))).thenReturn(EntityType.recordset);
        }

        // Call under test
        CurationTask result = curationTaskManager.createCurationTask(userInfo, toCreate);

        assertSame(createdByDao, result);
    }

    @Test
    public void testCreateCurationTaskFailsWithUnknownPropertiesType() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.FILE_BASED).setTaskProperties(new UnknownCurationTaskProperties());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("Unknown CurationTaskProperties concreteType"));
    }

    @Test
    public void testGetCurationTaskWithSuccess() {
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId);
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);

        // Call under test
        CurationTask result = curationTaskManager.getCurationTask(userInfo, taskId);

        assertEquals(task, result);
    }

    @Test
    public void testGetCurationTaskWithNotFound() {
        Long nonexistentTaskId = 999L;
        when(mockCurationTaskDao.getCurationTask(nonexistentTaskId)).thenReturn(Optional.empty());
        NotFoundException ex = assertThrows(NotFoundException.class, () -> curationTaskManager.getCurationTask(userInfo, nonexistentTaskId));
        assertTrue(ex.getMessage().contains("Task not found"));
    }

    @ParameterizedTest
    @EnumSource(CurationTaskPropertiesType.class)
    public void testUpdateCurationTaskWithSuccess(CurationTaskPropertiesType type) {
        CurationTask task = createCurationTask(type).setTaskId(taskId);
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        if (CurationTaskPropertiesType.FILE_BASED.equals(type)) {
            when(mockEntityManager.getEntityType(eq(userInfo), eq(fileViewId))).thenReturn(EntityType.entityview);
            when(mockEntityManager.getEntityType(eq(userInfo), eq(uploadFolderId))).thenReturn(EntityType.folder);
        } else if (CurationTaskPropertiesType.RECORD_BASED.equals(type)) {
            when(mockEntityManager.getEntityType(eq(userInfo), eq(recordSetId))).thenReturn(EntityType.recordset);
        }
        CurationTask updated = new CurationTask().setTaskId(taskId);
        when(mockCurationTaskDao.updateCurationTask(eq(userId), eq(task))).thenReturn(updated);

        // Call under test
        CurationTask result = curationTaskManager.updateCurationTask(userInfo, task);

        assertSame(updated, result);
    }

    @Test
    public void testUpdateCurationTaskWithProjectIdChangeFails() {
        CurationTask toUpdate = createCurationTask(CurationTaskPropertiesType.FILE_BASED).setTaskId(taskId).setProjectId("syn77777");

        CurationTask existing = createCurationTask(CurationTaskPropertiesType.FILE_BASED).setTaskId(taskId).setProjectId("syn88888"); // different project
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq("syn88888"), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(fileViewId))).thenReturn(EntityType.entityview);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(uploadFolderId))).thenReturn(EntityType.folder);
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(existing));

        // Call under test
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.updateCurationTask(userInfo, toUpdate));
        assertTrue(ex.getMessage().contains("The project for a MetadataTask cannot be changed"));
    }

    @Test
    public void testDeleteCurationTaskWithSuccess() {
        CurationTask toUpdate = createCurationTask(CurationTaskPropertiesType.FILE_BASED);

        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DELETE))).thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(toUpdate));
        doNothing().when(mockCurationTaskDao).deleteCurationTask(taskId);

        // Call under test
        curationTaskManager.deleteCurationTask(userInfo, taskId);

        verify(mockCurationTaskDao).deleteCurationTask(taskId);
    }

    @Test
    public void testDeleteCurationTaskWithNotFound() {
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.empty());
        NotFoundException ex = assertThrows(NotFoundException.class, () -> curationTaskManager.deleteCurationTask(userInfo, taskId));
        assertTrue(ex.getMessage().contains("Task not found"));
    }

    @Test
    public void testGetCurationTasksWithSuccess() {
        ListCurationTaskRequest request = new ListCurationTaskRequest().setProjectId(projectId);
        List<CurationTask> tasks = Arrays.asList(createCurationTask(CurationTaskPropertiesType.FILE_BASED), createCurationTask(CurationTaskPropertiesType.RECORD_BASED));
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.getCurationTasks(eq(KeyFactory.stringToKey(projectId)), anyLong(), anyLong())).thenReturn(tasks);

        // Call under test
        ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

        assertEquals(tasks, response.getPage());
    }

    @Test
    public void testCreateCurationTaskFailsWithMissingProjectId() {
        CurationTask task = createCurationTask().setProjectId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("projectId"));
    }

    @Test
    public void testCreateCurationTaskFailsWithMissingDataType() {
        CurationTask task = createCurationTask().setDataType(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("dataType"));
    }

    @Test
    public void testCreateCurationTaskFailsWithMissingFileViewId() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        ((FileBasedMetadataTaskProperties) task.getTaskProperties()).setFileViewId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("fileViewId"));
    }

    @Test
    public void testCreateCurationTaskFailsWithMissingUploadFolderId() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        ((FileBasedMetadataTaskProperties) task.getTaskProperties()).setUploadFolderId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("uploadFolderId"));
    }

    @Test
    public void testCreateCurationTaskFailsWithMissingRecordSetId() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.RECORD_BASED);
        ((RecordBasedMetadataTaskProperties) task.getTaskProperties()).setRecordSetId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("recordSetId"));
    }

    @Test
    public void testUpdateCurationTaskFailsWithMissingTaskId() {
        when(mockEntityManager.getEntityType(eq(userInfo), eq(fileViewId))).thenReturn(EntityType.entityview);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(uploadFolderId))).thenReturn(EntityType.folder);

        CurationTask task = createCurationTask(CurationTaskPropertiesType.FILE_BASED).setTaskId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.updateCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("taskId"));
    }

    @Test
    public void testCreateCurationTaskFailsWithFileViewIdNotEntityView() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(fileViewId))).thenReturn(EntityType.folder); // Not entityview

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("The fileViewId must be an EntityView."));
    }

    @Test
    public void testCreateCurationTaskFailsWithUploadFolderIdNotFolderOrProject() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(fileViewId))).thenReturn(EntityType.entityview);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(uploadFolderId))).thenReturn(EntityType.table); // Not folder or project

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("The uploadFolderId must be a Folder or Project."));
    }

    @Test
    public void testCreateCurationTaskFailsWithRecordSetIdNotRecordSet() {
        CurationTask task = createCurationTask(CurationTaskPropertiesType.RECORD_BASED);
        when(mockEntityManager.getEntityType(eq(userInfo), eq(recordSetId))).thenReturn(EntityType.folder); // Not recordset

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> curationTaskManager.createCurationTask(userInfo, task));
        assertTrue(ex.getMessage().contains("The recordSetId must be a RecordSet."));
    }

    private CurationTask createCurationTask() {
        return createCurationTask(CurationTaskPropertiesType.FILE_BASED);
    }

    private CurationTask createCurationTask(CurationTaskPropertiesType type) {
        return new CurationTask()
                .setProjectId(projectId)
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(type));
    }

    private CurationTaskProperties createTaskProperties(CurationTaskPropertiesType type) {
        switch (type) {
            case FILE_BASED:
                return new FileBasedMetadataTaskProperties()
                    .setUploadFolderId(uploadFolderId)
                    .setFileViewId(fileViewId);
            case RECORD_BASED:
                return new RecordBasedMetadataTaskProperties().setRecordSetId(recordSetId);
            default:
                throw new RuntimeException("Unknown CurationTaskPropertiesType: " + type.name());
        }
    }

    private class UnknownCurationTaskProperties implements CurationTaskProperties {

        @Override
        public String getConcreteType() {
            return "";
        }

        @Override
        public CurationTaskProperties setConcreteType(String concreteType) {
            return null;
        }

        @Override
        public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
            return null;
        }

        @Override
        public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
            return null;
        }
    }

}

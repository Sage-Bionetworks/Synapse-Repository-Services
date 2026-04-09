package org.sagebionetworks.repo.manager.curation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AccessControlListManager;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;
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
    AccessControlListManager mockAclManager;

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
        CurationTask task1 = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        CurationTask task2 = createCurationTask(CurationTaskPropertiesType.RECORD_BASED);
        TaskBundle bundle1 = new TaskBundle().setTask(task1);
        TaskBundle bundle2 = new TaskBundle().setTask(task2);
        List<TaskBundle> bundles = Arrays.asList(bundle1, bundle2);

        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.getCurationTaskBundles(eq(List.of(KeyFactory.stringToKey(projectId))),
                eq(null), eq(null), anyLong(), anyLong())).thenReturn(bundles);

        // Call under test
        ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

        assertEquals(Arrays.asList(task1, task2), response.getPage());
        assertEquals(bundles, response.getBundlePage());
    }
    
	@Test
	public void testGetCurationTasksWithAssigneeIds() {
		ListCurationTaskRequest request = new ListCurationTaskRequest().setProjectId(projectId)
				.setAssigneeIds(List.of("111", "222"));
		CurationTask task1 = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
		CurationTask task2 = createCurationTask(CurationTaskPropertiesType.RECORD_BASED);
		TaskBundle bundle1 = new TaskBundle().setTask(task1);
		TaskBundle bundle2 = new TaskBundle().setTask(task2);
		List<TaskBundle> bundles = Arrays.asList(bundle1, bundle2);

		when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY),
				eq(ACCESS_TYPE.READ))).thenReturn(mockAuthorizationStatus);
		when(mockCurationTaskDao.getCurationTaskBundles(eq(List.of(KeyFactory.stringToKey(projectId))), eq(List.of(111L,222L)),
				eq(null), anyLong(), anyLong())).thenReturn(bundles);

		// Call under test
		ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

		assertEquals(Arrays.asList(task1, task2), response.getPage());
		assertEquals(bundles, response.getBundlePage());
	}
	
	@Test
	public void testGetCurationTasksWithAssigneeIdsNotNumber() {
		ListCurationTaskRequest request = new ListCurationTaskRequest().setProjectId(projectId)
				.setAssigneeIds(List.of("not a number", "222"));

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// Call under test
			curationTaskManager.getCurationTasks(userInfo, request);
		}).getMessage();
		assertEquals("For input string: \"not a number\"", message);
		
        verifyZeroInteractions(mockCurationTaskDao, mockAclManager);
	}

    @Test
    public void testGetCurationTasksWithAssignedToMe() {
        Long teamId = 555L;
        Set<Long> groups = new HashSet<>(Arrays.asList(userId, teamId));
        userInfo.setGroups(groups);

        ListCurationTaskRequest request = new ListCurationTaskRequest()
                .setProjectId(projectId)
                .setAssignedToMe(true);

        CurationTask task1 = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        TaskBundle bundle1 = new TaskBundle().setTask(task1);
        List<TaskBundle> bundles = Arrays.asList(bundle1);

        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ)))
                .thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.getCurationTaskBundles(eq(List.of(KeyFactory.stringToKey(projectId))),
                eq(new ArrayList<>(groups)), eq(null), anyLong(), anyLong())).thenReturn(bundles);

        // Call under test
        ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

        assertEquals(Arrays.asList(task1), response.getPage());
        assertEquals(bundles, response.getBundlePage());
    }

    @Test
    public void testGetCurationTasksWithAssignedToMeAndAssigneeIds() {
        ListCurationTaskRequest request = new ListCurationTaskRequest()
                .setProjectId(projectId)
                .setAssignedToMe(true)
                .setAssigneeIds(List.of("111"));

        // Call under test
        String message = assertThrows(IllegalArgumentException.class, () -> {
            curationTaskManager.getCurationTasks(userInfo, request);
        }).getMessage();
        assertTrue(message.contains("Cannot specify both"));

        verifyZeroInteractions(mockCurationTaskDao, mockAclManager);
    }

    @Test
    public void testGetCurationTasksWithNoProjectId() {
        ListCurationTaskRequest request = new ListCurationTaskRequest();

        Set<Long> allProjectIds = new HashSet<>(Arrays.asList(100L, 200L, 300L));
        Set<Long> accessibleIds = new HashSet<>(Arrays.asList(100L, 200L));

        when(mockCurationTaskDao.getDistinctProjectIds()).thenReturn(allProjectIds);
        when(mockAclManager.getAccessibleBenefactors(eq(userInfo), eq(ObjectType.ENTITY), eq(allProjectIds), eq(ACCESS_TYPE.READ)))
                .thenReturn(accessibleIds);

        CurationTask task1 = createCurationTask(CurationTaskPropertiesType.FILE_BASED);
        TaskBundle bundle1 = new TaskBundle().setTask(task1);
        List<TaskBundle> bundles = Arrays.asList(bundle1);

        when(mockCurationTaskDao.getCurationTaskBundles(any(), eq(null), eq(null), anyLong(), anyLong()))
                .thenReturn(bundles);

        // Call under test
        ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

        assertEquals(Arrays.asList(task1), response.getPage());
        assertEquals(bundles, response.getBundlePage());
    }

    @Test
    public void testGetCurationTasksWithNoProjectsFound() {
        ListCurationTaskRequest request = new ListCurationTaskRequest();

        when(mockCurationTaskDao.getDistinctProjectIds()).thenReturn(Collections.emptySet());

        // Call under test
        ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

        assertNotNull(response);
        assertTrue(response.getPage().isEmpty());
        assertTrue(response.getBundlePage().isEmpty());
    }

    @Test
    public void testGetCurationTasksWithNoAccessibleProjects() {
        ListCurationTaskRequest request = new ListCurationTaskRequest();

        Set<Long> allProjectIds = new HashSet<>(Arrays.asList(100L, 200L));
        when(mockCurationTaskDao.getDistinctProjectIds()).thenReturn(allProjectIds);
        when(mockAclManager.getAccessibleBenefactors(eq(userInfo), eq(ObjectType.ENTITY), eq(allProjectIds), eq(ACCESS_TYPE.READ)))
                .thenReturn(Collections.emptySet());

        // Call under test
        ListCurationTaskResponse response = curationTaskManager.getCurationTasks(userInfo, request);

        assertNotNull(response);
        assertTrue(response.getPage().isEmpty());
        assertTrue(response.getBundlePage().isEmpty());
    }

    @Test
    public void testGetTaskStatus() {
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId);
        TaskStatus expectedStatus = new TaskStatus().setTaskId(taskId).setState(TaskState.NOT_STARTED);

        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ)))
                .thenReturn(mockAuthorizationStatus);
        when(mockCurationTaskDao.getTaskStatus(taskId)).thenReturn(expectedStatus);

        // Call under test
        TaskStatus result = curationTaskManager.getTaskStatus(userInfo, taskId);

        assertSame(expectedStatus, result);
        verify(mockCurationTaskDao).getTaskStatus(taskId);
    }

    @Test
    public void testGetTaskStatusTaskNotFound() {
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.empty());

        // Call under test
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> curationTaskManager.getTaskStatus(userInfo, taskId));
        assertTrue(ex.getMessage().contains("Task not found"));
    }

    @Test
    public void testUpdateTaskStatusWithProjectUpdateAccess() {
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag("etag-1");
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId).setAssigneePrincipalId("999");
        TaskStatus expectedResult = new TaskStatus().setTaskId(taskId).setState(TaskState.IN_PROGRESS);

        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
                .thenReturn(AuthorizationStatus.authorized());
        when(mockCurationTaskDao.updateTaskStatus(eq(userId), eq(taskId), eq(statusUpdate))).thenReturn(expectedResult);

        // Call under test
        TaskStatus result = curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate);

        assertSame(expectedResult, result);
        verify(mockCurationTaskDao).updateTaskStatus(userId, taskId, statusUpdate);
    }

    @Test
    public void testUpdateTaskStatusAsDirectAssignee() {
        // The assignee principal ID matches the user's own ID
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag("etag-1");
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId)
                .setAssigneePrincipalId(userId.toString());
        TaskStatus expectedResult = new TaskStatus().setTaskId(taskId).setState(TaskState.IN_PROGRESS);

        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        // No UPDATE access on project
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
                .thenReturn(AuthorizationStatus.accessDenied("no access"));
        when(mockCurationTaskDao.updateTaskStatus(eq(userId), eq(taskId), eq(statusUpdate))).thenReturn(expectedResult);

        // Call under test
        TaskStatus result = curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate);

        assertSame(expectedResult, result);
    }
    
    @Test
    public void testUpdateTaskStatusWithNullAssignee() {
        // The assignee is null
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag("etag-1");
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId)
                .setAssigneePrincipalId(null);

        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        // No UPDATE access on project
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
                .thenReturn(AuthorizationStatus.accessDenied("no access"));

        // Call under test
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate));
        assertTrue(ex.getMessage().contains("You must have UPDATE access on the project or be an assignee of the task."));
    }

    @Test
    public void testUpdateTaskStatusAsGroupAssignee() {
        Long groupId = 555L;
        // The assignee principal ID is a group the user belongs to
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.COMPLETED).setEtag("etag-2");
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId)
                .setAssigneePrincipalId(groupId.toString());
        TaskStatus expectedResult = new TaskStatus().setTaskId(taskId).setState(TaskState.COMPLETED);

        // Add the group to the user's groups
        Set<Long> groups = new HashSet<>();
        groups.add(groupId);
        userInfo.setGroups(groups);

        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        // No UPDATE access on project
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
                .thenReturn(AuthorizationStatus.accessDenied("no access"));
        when(mockCurationTaskDao.updateTaskStatus(eq(userId), eq(taskId), eq(statusUpdate))).thenReturn(expectedResult);

        // Call under test
        TaskStatus result = curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate);

        assertSame(expectedResult, result);
    }

    @Test
    public void testUpdateTaskStatusUnauthorized() {
        Long otherUserId = 999L;
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag("etag-1");
        CurationTask task = new CurationTask().setTaskId(taskId).setProjectId(projectId)
                .setAssigneePrincipalId(otherUserId.toString());

        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.of(task));
        // No UPDATE access on project
        when(mockAuthorizationManager.canAccess(eq(userInfo), eq(projectId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
                .thenReturn(AuthorizationStatus.accessDenied("no access"));

        // Call under test
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate));
        assertTrue(ex.getMessage().contains("You must have UPDATE access on the project or be an assignee of the task."));
    }

    @Test
    public void testUpdateTaskStatusTaskNotFound() {
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag("etag-1");
        when(mockCurationTaskDao.getCurationTask(taskId)).thenReturn(Optional.empty());

        // Call under test
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate));
        assertTrue(ex.getMessage().contains("Task not found"));
    }

    @Test
    public void testUpdateTaskStatusMissingState() {
        TaskStatus statusUpdate = new TaskStatus().setState(null).setEtag("etag-1");

        // Call under test
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate));
        assertTrue(ex.getMessage().contains("state"));
    }

    @Test
    public void testUpdateTaskStatusMissingEtag() {
        TaskStatus statusUpdate = new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag(null);

        // Call under test
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> curationTaskManager.updateTaskStatus(userInfo, taskId, statusUpdate));
        assertTrue(ex.getMessage().contains("etag"));
    }

    @Test
    public void testUpdateTaskStatusNullRequest() {
        // Call under test
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> curationTaskManager.updateTaskStatus(userInfo, taskId, null));
        assertTrue(ex.getMessage().contains("statusUpdate"));
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

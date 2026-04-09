package org.sagebionetworks.repo.model.dbo.curation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.CurationTaskProperties;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.model.curation.execution.GridExecutionDetails;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
class CurationTaskDaoAutowireTest {

    @Autowired
    NodeDAO nodeDao;

    @Autowired
    UserGroupDAO userGroupDAO;

    @Autowired
    CurationTaskDao dao;

    private Long userId;
    private Long modifiedByUserId;
    private Node project1;
    private Node project2;
    private String recordSetId = "syn12345678";
    private String fileViewId = "syn87654321";
    private String uploadFolderId = "syn11223344";

    List<String> nodesToDelete;

    @BeforeEach
    public void before() {
        nodesToDelete = new ArrayList<>();

        UserGroup user1 = new UserGroup();
        user1.setIsIndividual(true);
        user1.setCreationDate(new Date());
		user1.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
        userId = userGroupDAO.create(user1);
        UserGroup user2 = new UserGroup();
        user2.setIsIndividual(true);
        user2.setCreationDate(new Date());
		user2.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
        modifiedByUserId = userGroupDAO.create(user2);

        Node project = new Node();
        project.setName("project1");
        project.setNodeType(EntityType.project);
        project.setCreatedByPrincipalId(userId);
        project.setCreatedOn(new Date());
        project.setModifiedByPrincipalId(userId);
        project.setModifiedOn(new Date());
        project1 = nodeDao.createNewNode(project);
        nodesToDelete.add(project1.getId());

        project.setName("project2");
        project2 = nodeDao.createNewNode(project);
        nodesToDelete.add(project2.getId());
    }

    @AfterEach
    public void afterEach() throws Exception {
        Collections.reverse(nodesToDelete);
        nodesToDelete.forEach(id -> nodeDao.delete(id));

        if (userId != null) {
            userGroupDAO.delete(userId.toString());
        }
        if (modifiedByUserId != null) {
            userGroupDAO.delete(modifiedByUserId.toString());
        }
    }


    @ParameterizedTest
    @EnumSource(CurationTaskPropertiesType.class)
    public void testCRUD(CurationTaskPropertiesType taskType) {
        CurationTask toCreate = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setInstructions("these are the instructions")
                .setTaskProperties(createTaskProperties(taskType));

        // Create
        CurationTask created = dao.createCurationTask(userId, toCreate);
        assertNotNull(created.getTaskId());
        assertEquals(project1.getId(), created.getProjectId());
        assertEquals("fastq", created.getDataType());
        assertEquals("these are the instructions", created.getInstructions());
        assertEquals(userId.toString(), created.getCreatedBy());
        assertNotNull(created.getCreatedOn());
        assertEquals(userId.toString(), created.getModifiedBy());
        assertNotNull(created.getModifiedOn());
        assertNotNull(created.getEtag());
        assertEquals(created.getTaskProperties(), toCreate.getTaskProperties());

        // Read
        Optional<CurationTask> fetched = dao.getCurationTask(created.getTaskId());
        assertTrue(fetched.isPresent());
        assertEquals(created, fetched.get());

        // Update
        String newInstructions = "new instructions";
        created.setInstructions(newInstructions);
        created.setCreatedBy("123456789012"); // changes to created/modified By/On should be ignored
        created.setCreatedOn(null);
        created.setModifiedBy("1");
        created.setModifiedOn(new Date(0));

        // call under test
        dao.updateCurationTask(modifiedByUserId, created);
        CurationTask updated = dao.getCurationTask(created.getTaskId()).get();

        assertEquals(newInstructions, updated.getInstructions());
        assertNotEquals(fetched.get().getEtag(), updated.getEtag());
        assertNotNull(updated.getModifiedOn());
        assertNotEquals(fetched.get().getModifiedOn(), updated.getModifiedOn());
        assertEquals(modifiedByUserId.toString(), updated.getModifiedBy());
        assertEquals(fetched.get().getCreatedBy(), updated.getCreatedBy());
        assertEquals(fetched.get().getCreatedOn(), updated.getCreatedOn());

        // Delete
        dao.deleteCurationTask(created.getTaskId());
        assertTrue(dao.getCurationTask(created.getTaskId()).isEmpty());
    }
    
    @ParameterizedTest
    @EnumSource(CurationTaskPropertiesType.class)
    public void testCRUDWithAssignee(CurationTaskPropertiesType taskType) {
        CurationTask toCreate = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setInstructions("these are the instructions")
                .setTaskProperties(createTaskProperties(taskType))
                .setAssigneePrincipalId(userId.toString());

        // Create
        CurationTask created = dao.createCurationTask(userId, toCreate);
        assertNotNull(created.getTaskId());
        assertEquals(project1.getId(), created.getProjectId());
        assertEquals("fastq", created.getDataType());
        assertEquals("these are the instructions", created.getInstructions());
        assertEquals(userId.toString(), created.getCreatedBy());
        assertNotNull(created.getCreatedOn());
        assertEquals(userId.toString(), created.getModifiedBy());
        assertNotNull(created.getModifiedOn());
        assertNotNull(created.getEtag());
        assertEquals(created.getTaskProperties(), toCreate.getTaskProperties());
        assertEquals(userId.toString(), toCreate.getAssigneePrincipalId());

        // Read
        Optional<CurationTask> fetched = dao.getCurationTask(created.getTaskId());
        assertTrue(fetched.isPresent());
        assertEquals(created, fetched.get());

        // Update
        String newInstructions = "new instructions";
        created.setInstructions(newInstructions);
        created.setCreatedBy("123456789012"); // changes to created/modified By/On should be ignored
        created.setCreatedOn(null);
        created.setModifiedBy("1");
        created.setModifiedOn(new Date(0));
        created.setAssigneePrincipalId(modifiedByUserId.toString());

        // call under test
        dao.updateCurationTask(modifiedByUserId, created);
        CurationTask updated = dao.getCurationTask(created.getTaskId()).get();

        assertEquals(newInstructions, updated.getInstructions());
        assertNotEquals(fetched.get().getEtag(), updated.getEtag());
        assertNotNull(updated.getModifiedOn());
        assertNotEquals(fetched.get().getModifiedOn(), updated.getModifiedOn());
        assertEquals(modifiedByUserId.toString(), updated.getModifiedBy());
        assertEquals(fetched.get().getCreatedBy(), updated.getCreatedBy());
        assertEquals(fetched.get().getCreatedOn(), updated.getCreatedOn());
        assertEquals(modifiedByUserId.toString(), updated.getAssigneePrincipalId());

        // Delete
        dao.deleteCurationTask(created.getTaskId());
        assertTrue(dao.getCurationTask(created.getTaskId()).isEmpty());
    }

    @Test
    public void testNoDuplicateDataTypeWithinProject_onCreate() {
        CurationTask fastqDataType1 = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED));

        CurationTask fastqDataType2 = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED));

        // Create the first one
        CurationTask created1 = dao.createCurationTask(userId, fastqDataType1);
        assertNotNull(created1);

        // call under test
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> dao.createCurationTask(userId, fastqDataType2));
        assertTrue(e.getMessage().contains("A curation task with the specified data type already exists in this project."));

        // Verify that we can create the same data type in a different project
        fastqDataType2.setProjectId(project2.getId());
        CurationTask created2 = dao.createCurationTask(userId, fastqDataType2);
        assertNotNull(created2);

        // Cleanup
        dao.deleteCurationTask(created1.getTaskId());
        dao.deleteCurationTask(created2.getTaskId());
    }

    @Test
    public void testUpdateWithNotFound() {
        CurationTask task = new CurationTask()
                .setTaskId(9999999L);

        // Call under test
        NotFoundException ex = assertThrows(NotFoundException.class, () -> dao.updateCurationTask(userId, task));

        assertEquals("A curation task with ID 9999999 does not exist.", ex.getMessage());
    }

    @Test
    public void testNoDuplicateDataTypeWithinProject_onUpdate() {
        String dataType1 = "a".repeat(255);
        String dataType2 = "a".repeat(256); // verify varchar column is not truncated in the unique index

        CurationTask task1 = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType(dataType1)
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED));

        CurationTask task2 = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType(dataType2)
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED));

        // call under test - verify varchar column is not truncated in the unique index
        CurationTask created1 = dao.createCurationTask(userId, task1);
        CurationTask created2 = dao.createCurationTask(userId, task2);
        assertNotNull(created1);
        assertNotNull(created2);


        created2.setDataType(dataType1);
        // call under test - should encounter a uniqueness constraint violation
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> dao.updateCurationTask(userId, created2));
        assertTrue(e.getMessage().contains("A curation task with the specified data type already exists in this project."));

        // Cleanup
        dao.deleteCurationTask(created1.getTaskId());
        dao.deleteCurationTask(created2.getTaskId());
    }


    @Test
    public void testGetListForProject() {
        CurationTask taskInProject1 = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED));

        CurationTask anotherTaskInProject1 = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("rnaseq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED));

        CurationTask taskInProject2 = new CurationTask()
                .setProjectId(project2.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED));

        // Create
        CurationTask projectOneTask1 = dao.createCurationTask(userId, taskInProject1);
        CurationTask projectOneTask2 = dao.createCurationTask(userId, anotherTaskInProject1);
        CurationTask projectTwoTask = dao.createCurationTask(userId, taskInProject2);

        // call under test - get all tasks for project 1, ensure project 2 task is not included
        List<CurationTask> tasksForProject1 = dao.getCurationTasks(KeyFactory.stringToKey(project1.getId()), 10, 0);
        assertEquals(2, tasksForProject1.size());
        assertEquals(tasksForProject1.get(0), projectOneTask1);
        assertEquals(tasksForProject1.get(1), projectOneTask2);

        // Test limit and offset
        List<CurationTask> tasks = dao.getCurationTasks(KeyFactory.stringToKey(project1.getId()), 1, 0);
        assertEquals(1, tasks.size());
        assertEquals(projectOneTask1, tasks.get(0));

        tasks = dao.getCurationTasks(KeyFactory.stringToKey(project1.getId()), 1, 1);
        assertEquals(1, tasks.size());
        assertEquals(projectOneTask2, tasks.get(0));


        // Cleanup
        dao.deleteCurationTask(projectOneTask1.getTaskId());
        dao.deleteCurationTask(projectOneTask2.getTaskId());
        dao.deleteCurationTask(projectTwoTask.getTaskId());
    }

    @Test
    public void testConflictingUpdate() {
        CurationTask toCreate = new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setInstructions("these are the instructions")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED));

        CurationTask created = dao.createCurationTask(userId, toCreate);
        assertNotNull(created.getTaskId());

        // Update, but scramble the etag
        String newInstructions = "new instructions";
        String wrongEtag = created.getEtag() + "xxx";
        created.setEtag(wrongEtag);
        created.setInstructions(newInstructions);

        assertThrows(ConflictingUpdateException.class, () -> dao.updateCurationTask(userId, created));
    }

    @Test
    public void testGetTaskStatus() {
        CurationTask created = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        // call under test
        TaskStatus status = dao.getTaskStatus(created.getTaskId());

        assertEquals(created.getTaskId(), status.getTaskId());
        assertEquals(TaskState.NOT_STARTED, status.getState());
        assertEquals(created.getEtag(), status.getEtag());
        assertNull(status.getExecutionDetails());
        assertNull(status.getLastUpdatedBy());
        assertNull(status.getLastUpdatedOn());

        dao.deleteCurationTask(created.getTaskId());
    }

    @Test
    public void testGetTaskStatusNotFound() {
        // call under test
        assertThrows(NotFoundException.class, () -> dao.getTaskStatus(9999999L));
    }

    @Test
    public void testUpdateTaskStatus() {
        CurationTask created = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        TaskStatus initialStatus = dao.getTaskStatus(created.getTaskId());
        assertEquals(TaskState.NOT_STARTED, initialStatus.getState());
        assertEquals(created.getEtag(), initialStatus.getEtag());

        TaskStatus statusUpdate = new TaskStatus()
                .setState(TaskState.IN_PROGRESS)
                .setEtag(initialStatus.getEtag());

        // call under test
        TaskStatus updated = dao.updateTaskStatus(userId, created.getTaskId(), statusUpdate);

        assertEquals(TaskState.IN_PROGRESS, updated.getState());
        assertNotEquals(initialStatus.getEtag(), updated.getEtag());
        assertEquals(userId.toString(), updated.getLastUpdatedBy());
        assertNotNull(updated.getLastUpdatedOn());
        assertNull(updated.getExecutionDetails());

        dao.deleteCurationTask(created.getTaskId());
    }

    @Test
    public void testUpdateTaskStatusWithExecutionDetails() {
        CurationTask created = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        TaskStatus initialStatus = dao.getTaskStatus(created.getTaskId());

        GridExecutionDetails executionDetails = new GridExecutionDetails();
        executionDetails.setActiveSessionId("session-123");

        TaskStatus statusUpdate = new TaskStatus()
                .setState(TaskState.IN_PROGRESS)
                .setEtag(initialStatus.getEtag())
                .setExecutionDetails(executionDetails);

        // call under test
        TaskStatus updated = dao.updateTaskStatus(userId, created.getTaskId(), statusUpdate);

        assertEquals(TaskState.IN_PROGRESS, updated.getState());
        assertNotNull(updated.getExecutionDetails());
        assertTrue(updated.getExecutionDetails() instanceof GridExecutionDetails);
        assertEquals("session-123", ((GridExecutionDetails) updated.getExecutionDetails()).getActiveSessionId());

        dao.deleteCurationTask(created.getTaskId());
    }

    @Test
    public void testUpdateTaskStatusConflicting() {
        CurationTask created = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        TaskStatus statusUpdate = new TaskStatus()
                .setState(TaskState.IN_PROGRESS)
                .setEtag("wrong-etag");

        // call under test
        assertThrows(ConflictingUpdateException.class,
                () -> dao.updateTaskStatus(userId, created.getTaskId(), statusUpdate));

        dao.deleteCurationTask(created.getTaskId());
    }

    @Test
    public void testUpdateTaskStatusNotFound() {
        TaskStatus statusUpdate = new TaskStatus()
                .setState(TaskState.IN_PROGRESS)
                .setEtag("0");

        // call under test
        assertThrows(NotFoundException.class,
                () -> dao.updateTaskStatus(userId, 9999999L, statusUpdate));
    }

    @Test
    public void testGetCurationTaskBundles() {
        CurationTask created1 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        CurationTask created2 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("rnaseq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED)));

        // call under test
        List<TaskBundle> bundles = dao.getCurationTaskBundles(
                List.of(KeyFactory.stringToKey(project1.getId())),
                null, null, 10, 0);

        assertEquals(2, bundles.size());
        assertEquals(created1.getTaskId(), bundles.get(0).getTask().getTaskId());
        assertEquals(TaskState.NOT_STARTED, bundles.get(0).getStatus().getState());
        assertEquals(created2.getTaskId(), bundles.get(1).getTask().getTaskId());
        assertEquals(TaskState.NOT_STARTED, bundles.get(1).getStatus().getState());

        dao.deleteCurationTask(created1.getTaskId());
        dao.deleteCurationTask(created2.getTaskId());
    }

    @Test
    public void testGetCurationTaskBundlesWithAssigneeFilter() {
        CurationTask created1 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setAssigneePrincipalId(userId.toString())
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        CurationTask created2 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("rnaseq")
                .setAssigneePrincipalId(modifiedByUserId.toString())
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED)));

        // call under test - filter by userId
        List<TaskBundle> bundles = dao.getCurationTaskBundles(
                List.of(KeyFactory.stringToKey(project1.getId())),
                List.of(userId), null, 10, 0);

        assertEquals(1, bundles.size());
        assertEquals(created1.getTaskId(), bundles.get(0).getTask().getTaskId());

        dao.deleteCurationTask(created1.getTaskId());
        dao.deleteCurationTask(created2.getTaskId());
    }

    @Test
    public void testGetCurationTaskBundlesWithStateFilter() {
        CurationTask created1 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        CurationTask created2 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("rnaseq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED)));

        // Move created1 to IN_PROGRESS
        TaskStatus status = dao.getTaskStatus(created1.getTaskId());
        dao.updateTaskStatus(userId, created1.getTaskId(),
                new TaskStatus().setState(TaskState.IN_PROGRESS).setEtag(status.getEtag()));

        // call under test - filter by IN_PROGRESS
        List<TaskBundle> bundles = dao.getCurationTaskBundles(
                List.of(KeyFactory.stringToKey(project1.getId())),
                null, List.of(TaskState.IN_PROGRESS), 10, 0);

        assertEquals(1, bundles.size());
        assertEquals(created1.getTaskId(), bundles.get(0).getTask().getTaskId());
        assertEquals(TaskState.IN_PROGRESS, bundles.get(0).getStatus().getState());

        dao.deleteCurationTask(created1.getTaskId());
        dao.deleteCurationTask(created2.getTaskId());
    }

    @Test
    public void testGetDistinctProjectIds() {
        CurationTask created1 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project1.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.FILE_BASED)));

        CurationTask created2 = dao.createCurationTask(userId, new CurationTask()
                .setProjectId(project2.getId())
                .setDataType("fastq")
                .setTaskProperties(createTaskProperties(CurationTaskPropertiesType.RECORD_BASED)));

        // call under test
        Set<Long> projectIds = dao.getDistinctProjectIds();

        assertTrue(projectIds.contains(KeyFactory.stringToKey(project1.getId())));
        assertTrue(projectIds.contains(KeyFactory.stringToKey(project2.getId())));

        dao.deleteCurationTask(created1.getTaskId());
        dao.deleteCurationTask(created2.getTaskId());
    }

    private CurationTaskProperties createTaskProperties(CurationTaskPropertiesType taskType) {
        switch (taskType) {
            case FILE_BASED:
                return new FileBasedMetadataTaskProperties().setFileViewId(fileViewId).setUploadFolderId(uploadFolderId);
            case RECORD_BASED:
                return new RecordBasedMetadataTaskProperties().setRecordSetId(recordSetId);
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }
}
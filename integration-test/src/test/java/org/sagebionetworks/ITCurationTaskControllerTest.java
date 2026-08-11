package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.ListCurationTaskRequest;
import org.sagebionetworks.repo.model.curation.ListCurationTaskResponse;
import org.sagebionetworks.repo.model.curation.TaskBundle;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.table.EntityView;

@ExtendWith(ITTestExtension.class)
public class ITCurationTaskControllerTest {
    private List<Entity> entitiesToDelete;

    private final SynapseClient synapse;

    public ITCurationTaskControllerTest(SynapseClient synapse) {
        this.synapse = synapse;
    }

    Project project;
    Folder folder;
    EntityView view;
    RecordSet recordSet;
    File csvFile;

    @BeforeEach
    public void before() throws SynapseException, FileNotFoundException, IOException {
        entitiesToDelete = new LinkedList<Entity>();

        // Set up project, folder, file view, and record set
        project = synapse.createEntity(new Project().setName("ITGridControllerTest" + UUID.randomUUID()));
        entitiesToDelete.add(project);

        folder = synapse.createEntity(new Folder().setName("folder").setParentId(project.getId()));
        entitiesToDelete.add(folder);

        view = synapse.createEntity(new EntityView().setName("view").setViewTypeMask(1L).setParentId(project.getId()));
        entitiesToDelete.add(view);

        csvFile = new File(ITCurationTaskControllerTest.class.getClassLoader().getResource("docs/test.csv").getFile().replaceAll("%20", " "));
        FileHandle csvFileHandle = synapse.multipartUpload(csvFile, null, false, true);
        recordSet = synapse.createEntity(new RecordSet().setName("record set").setParentId(folder.getId()).setUpsertKey(Collections.singletonList("id")).setDataFileHandleId(csvFileHandle.getId()));
        entitiesToDelete.add(recordSet);
    }

    @AfterEach
    public void after() throws Exception {
        for (Entity entity : entitiesToDelete) {
            synapse.deleteEntity(entity);
        }
    }

    @Test
    public void testCurationTaskCRUD() throws AssertionError, SynapseException, URISyntaxException, InterruptedException {

        CurationTask fbTask = new CurationTask()
                .setProjectId(project.getId())
                .setDataType("fastq: file-based")
                .setInstructions("upload to the folder and annotate with the view")
                .setTaskProperties(
                        new FileBasedMetadataTaskProperties()
                                .setFileViewId(view.getId())
                                .setUploadFolderId(folder.getId())
                );

        CurationTask rbTask = new CurationTask()
                .setProjectId(project.getId())
                .setDataType("fastq: record-based")
                .setInstructions("add data to the recordset")
                .setTaskProperties(
                        new RecordBasedMetadataTaskProperties().setRecordSetId(recordSet.getId())
                );

        // call under test - create
        fbTask = synapse.createCurationTask(fbTask);
        rbTask = synapse.createCurationTask(rbTask);

        assertNotNull(fbTask.getTaskId());
        assertNotNull(rbTask.getTaskId());

        // call under test - get
        assertEquals(fbTask, synapse.getMetadataTask(fbTask.getTaskId()));
        assertEquals(rbTask, synapse.getMetadataTask(rbTask.getTaskId()));

        // call under test - get page
        ListCurationTaskResponse response = synapse.listMetadataTasks(new ListCurationTaskRequest().setProjectId(project.getId()));
        assertEquals(2, response.getPage().size());
        assertNull(response.getNextPageToken());
        assertEquals(fbTask, response.getPage().get(0));
        assertEquals(rbTask, response.getPage().get(1));

        fbTask.setInstructions("new instructions for file-based task");
        rbTask.setInstructions("new instructions for record-based task");

        // call under test - update
        fbTask = synapse.updateMetadataTask(fbTask);
        rbTask = synapse.updateMetadataTask(rbTask);

        assertEquals("new instructions for file-based task", fbTask.getInstructions());
        assertEquals("new instructions for record-based task", rbTask.getInstructions());

        // call under test - delete
        synapse.deleteMetadataTask(fbTask.getTaskId());
        synapse.deleteMetadataTask(rbTask.getTaskId());

        response = synapse.listMetadataTasks(new ListCurationTaskRequest().setProjectId(project.getId()));
        assertEquals(0, response.getPage().size());
        assertNull(response.getNextPageToken());
    }

    @Test
    public void testGetTaskStatus() throws SynapseException {
        CurationTask task = new CurationTask()
                .setProjectId(project.getId())
                .setDataType("fastq: file-based")
                .setInstructions("upload files")
                .setTaskProperties(
                        new FileBasedMetadataTaskProperties()
                                .setFileViewId(view.getId())
                                .setUploadFolderId(folder.getId())
                );

        task = synapse.createCurationTask(task);

        try {
            // call under test
            TaskStatus status = synapse.getTaskStatus(task.getTaskId());

            assertEquals(task.getTaskId(), status.getTaskId());
            assertEquals(TaskState.NOT_STARTED, status.getState());
            assertEquals(task.getEtag(), status.getEtag());
            assertNull(status.getExecutionDetails());
            assertNull(status.getLastUpdatedBy());
            assertNull(status.getLastUpdatedOn());
        } finally {
            synapse.deleteMetadataTask(task.getTaskId());
        }
    }

    @Test
    public void testUpdateTaskStatus() throws SynapseException {
        CurationTask task = new CurationTask()
                .setProjectId(project.getId())
                .setDataType("fastq: file-based")
                .setInstructions("upload files")
                .setTaskProperties(
                        new FileBasedMetadataTaskProperties()
                                .setFileViewId(view.getId())
                                .setUploadFolderId(folder.getId())
                );

        task = synapse.createCurationTask(task);

        try {
            // Verify the initial status via list with bundlePage
            ListCurationTaskResponse listResponse = synapse.listMetadataTasks(
                    new ListCurationTaskRequest().setProjectId(project.getId())
            );

            assertNotNull(listResponse.getBundlePage());
            assertEquals(1, listResponse.getBundlePage().size());

            TaskBundle bundle = listResponse.getBundlePage().get(0);
            assertEquals(task, bundle.getTask());
            assertNotNull(bundle.getStatus());
            assertEquals(TaskState.NOT_STARTED, bundle.getStatus().getState());
            assertEquals(task.getEtag(), bundle.getStatus().getEtag());

            // Set due date via task update
            Date dueDate = new Date(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli());
            task.setDueDate(dueDate);
            task = synapse.updateMetadataTask(task);
            assertEquals(dueDate, task.getDueDate());

            // Update status to IN_PROGRESS using the updated task's etag
            TaskStatus statusUpdate = new TaskStatus()
                    .setState(TaskState.IN_PROGRESS)
                    .setEtag(task.getEtag());

            TaskStatus updatedStatus = synapse.updateTaskStatus(task.getTaskId(), statusUpdate);

            assertEquals(TaskState.IN_PROGRESS, updatedStatus.getState());
            assertNotNull(updatedStatus.getEtag());
            assertNotEquals(task.getEtag(), updatedStatus.getEtag());
            assertNotNull(updatedStatus.getLastUpdatedBy());
            assertNotNull(updatedStatus.getLastUpdatedOn());

            String inProgressEtag = updatedStatus.getEtag();

            // Update status to COMPLETED using the new etag
            statusUpdate = new TaskStatus()
                    .setState(TaskState.COMPLETED)
                    .setEtag(inProgressEtag);

            updatedStatus = synapse.updateTaskStatus(task.getTaskId(), statusUpdate);

            assertEquals(TaskState.COMPLETED, updatedStatus.getState());
            assertNotNull(updatedStatus.getEtag());
            assertNotEquals(inProgressEtag, updatedStatus.getEtag());
        } finally {
            synapse.deleteMetadataTask(task.getTaskId());
        }
    }

    @Test
    public void testListCurationTasksWithFilters() throws SynapseException {
        CurationTask fbTask = new CurationTask()
                .setProjectId(project.getId())
                .setDataType("fastq: file-based")
                .setInstructions("upload files")
                .setTaskProperties(
                        new FileBasedMetadataTaskProperties()
                                .setFileViewId(view.getId())
                                .setUploadFolderId(folder.getId())
                );

        CurationTask rbTask = new CurationTask()
                .setProjectId(project.getId())
                .setDataType("fastq: record-based")
                .setInstructions("add records")
                .setTaskProperties(
                        new RecordBasedMetadataTaskProperties().setRecordSetId(recordSet.getId())
                );

        fbTask = synapse.createCurationTask(fbTask);
        rbTask = synapse.createCurationTask(rbTask);

        try {
            // Update the file-based task to IN_PROGRESS
            TaskStatus statusUpdate = new TaskStatus()
                    .setState(TaskState.IN_PROGRESS)
                    .setEtag(fbTask.getEtag());

            synapse.updateTaskStatus(fbTask.getTaskId(), statusUpdate);

            // List with stateFilter=[IN_PROGRESS], should only return the file-based task
            ListCurationTaskResponse response = synapse.listMetadataTasks(
                    new ListCurationTaskRequest()
                            .setProjectId(project.getId())
                            .setStateFilter(Arrays.asList(TaskState.IN_PROGRESS))
            );

            assertNotNull(response.getPage());
            assertEquals(1, response.getPage().size());
            assertEquals(fbTask.getTaskId(), response.getPage().get(0).getTaskId());

            assertNotNull(response.getBundlePage());
            assertEquals(1, response.getBundlePage().size());
            assertEquals(fbTask.getTaskId(), response.getBundlePage().get(0).getTask().getTaskId());
            assertEquals(TaskState.IN_PROGRESS, response.getBundlePage().get(0).getStatus().getState());
        } finally {
            synapse.deleteMetadataTask(fbTask.getTaskId());
            synapse.deleteMetadataTask(rbTask.getTaskId());
        }
    }

}

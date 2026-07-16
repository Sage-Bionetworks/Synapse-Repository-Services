package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionRequest;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionResponse;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.model.curation.execution.SampleSheetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Full end-to-end test of sample sheet generation through the executable-task framework: it submits
 * a COMPUTE_TASK_EXECUTION async job for a CurationTask whose execution details are
 * {@link SampleSheetGenerationExecutionDetails}. The job is picked up by the ComputeTaskExecutionWorker,
 * dispatched to the SampleSheetGenerationSubWorker, which runs the AI supervisor (delegating to the
 * specialists over a shared code interpreter session) and then persists a RecordSet + review task.
 * Requires live Bedrock + code interpreter access.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SampleSheetGenerationIntegrationTest {

	private static final long MAX_WAIT_MS = 1000 * 60 * 4;

	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private CurationTaskManager curationTaskManager;

	@Autowired
	private ColumnModelManager columnModelManager;

	@Autowired
	private TableManagerSupport tableManagerSupport;

	@Autowired
	private org.sagebionetworks.repo.manager.schema.JsonSchemaManager jsonSchemaManager;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;

	private UserInfo adminUser;
	private String projectId;
	private String viewId;
	private String outputFolderId;
	private String targetSchemaId;

	@BeforeEach
	public void setup() throws Exception {
		jsonSchemaManager.truncateAll();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		Project project = new Project();
		project.setName("SampleSheetIT-" + UUID.randomUUID());
		projectId = entityManager.createEntity(adminUser, project, null);

		Folder folder = new Folder();
		folder.setName("output");
		folder.setParentId(projectId);
		outputFolderId = entityManager.createEntity(adminUser, folder, null);

		// Two source files annotated as sequencing samples. These annotations become the columns/rows
		// of the EntityView the supervisor reads from.
		createAnnotatedFile("SAMPLE_1", "sample_1_R1.fastq.gz", "forward");
		createAnnotatedFile("SAMPLE_2", "sample_2_R1.fastq.gz", "reverse");

		// The view schema is the default system columns (id, name, etag, etc.) plus columns for the
		// sample annotations. Without the annotation columns, SELECT * returns only the system columns
		// and the supervisor cannot see the source data.
		List<ColumnModel> schema = tableManagerSupport.getDefaultTableViewColumns(
				ViewEntityType.entityview, ViewTypeMask.File.getMask());
		schema.addAll(columnModelManager.createColumnModels(adminUser, List.of(
				new ColumnModel().setName("sample").setColumnType(ColumnType.STRING).setMaximumSize(100L),
				new ColumnModel().setName("fastq_1").setColumnType(ColumnType.STRING).setMaximumSize(256L),
				new ColumnModel().setName("strandedness").setColumnType(ColumnType.STRING).setMaximumSize(50L))));
		List<String> columnIds = schema.stream().map(ColumnModel::getId).collect(Collectors.toList());

		viewId = asyncHelper.createEntityView(adminUser, "SampleSheetInputView", projectId,
				columnIds, List.of(projectId), ViewTypeMask.File.getMask(), false).getId();

		asyncHelper.assertQueryResult(adminUser, "SELECT * FROM " + viewId, (QueryResultBundle result) -> {
			assertNotNull(result.getQueryResult());
			assertEquals(2, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);

		targetSchemaId = registerSchema(getSchemaFromClasspath("sampleSheet/rnaseq-schema-input.json"));
	}

	@AfterEach
	public void cleanup() {
		if (projectId != null) {
			try {
				entityManager.deleteEntity(adminUser, projectId);
			} catch (Exception e) { }
		}
		try {
			jsonSchemaManager.truncateAll();
		} catch (Exception e) { }
	}

	@Test
	public void testGenerateSampleSheetViaComputeTask() throws Exception {
		// Create the curation task. A sample-sheet generation task reads a file view and writes to a
		// folder, so it uses FileBasedMetadataTaskProperties.
		CurationTask task = curationTaskManager.createCurationTask(adminUser, new CurationTask()
				.setProjectId(projectId)
				.setDataType("rnaseq sample sheet")
				.setInstructions("Generate an nf-core/rnaseq sample sheet from the input view.")
				.setTaskProperties(new FileBasedMetadataTaskProperties()
						.setFileViewId(viewId)
						.setUploadFolderId(outputFolderId)));

		// Attach the executable sample-sheet generation details, leaving the task in NOT_STARTED.
		TaskStatus currentStatus = curationTaskManager.getTaskStatus(adminUser, task.getTaskId());
		currentStatus.setState(TaskState.NOT_STARTED);
		currentStatus.setExecutionDetails(new SampleSheetGenerationExecutionDetails()
				.setInputFileViewId(viewId)
				.setOutputFolderId(outputFolderId)
				.setTargetSchemaId(targetSchemaId));
		curationTaskManager.updateTaskStatus(adminUser, task.getTaskId(), currentStatus);

		// call under test — submit and monitor the real COMPUTE_TASK_EXECUTION job end to end.
		ComputeTaskExecutionRequest request = new ComputeTaskExecutionRequest().setTaskId(task.getTaskId());
		ComputeTaskExecutionResponse response = asyncHelper.assertJobResponse(adminUser, request,
				(ComputeTaskExecutionResponse r) -> {
					assertNotNull(r.getExecutionDetails());
					SampleSheetGenerationExecutionDetails details =
							(SampleSheetGenerationExecutionDetails) r.getExecutionDetails();
					assertNotNull(details.getOutputRecordSetId(), "outputRecordSetId should be populated");
					assertNotNull(details.getReviewTaskId(), "reviewTaskId should be populated");
				}, MAX_WAIT_MS).getResponse();

		SampleSheetGenerationExecutionDetails details =
				(SampleSheetGenerationExecutionDetails) response.getExecutionDetails();

		// The output RecordSet is a real recordset entity in the output folder, backed by a CSV.
		assertEquals(EntityType.recordset, entityManager.getEntityType(adminUser, details.getOutputRecordSetId()));
		RecordSet recordSet = entityManager.getEntity(adminUser, details.getOutputRecordSetId(), RecordSet.class);
		assertEquals(outputFolderId, recordSet.getParentId());
		assertNotNull(recordSet.getDataFileHandleId(), "RecordSet should be backed by a CSV file handle");

		// The task advanced to IN_REVIEW.
		assertEquals(TaskState.IN_REVIEW, curationTaskManager.getTaskStatus(adminUser, task.getTaskId()).getState());
	}

	private void createAnnotatedFile(String sample, String fastq1, String strandedness) throws Exception {
		String fileId = entityManager.createEntity(adminUser, new FileEntity()
				.setName(UUID.randomUUID().toString())
				.setParentId(projectId)
				.setDataFileHandleId(fileHandleDaoHelper.create((f) -> {
					f.setCreatedBy(adminUser.getId().toString());
					f.setFileName(UUID.randomUUID().toString());
					f.setContentSize(1024L);
				}).getId()), null);

		FileEntity file = entityManager.getEntity(adminUser, fileId, FileEntity.class);

		Annotations annotations = new Annotations().setId(file.getId()).setEtag(file.getEtag());
		AnnotationsV2TestUtils.putAnnotations(annotations, "sample", sample, AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "fastq_1", fastq1, AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "strandedness", strandedness, AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUser, file.getId(), annotations);

		file = entityManager.getEntity(adminUser, fileId, FileEntity.class);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY,
				KeyFactory.stringToKey(file.getId()), file.getEtag(), MAX_WAIT_MS);
	}

	private JsonSchema getSchemaFromClasspath(String name) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			return EntityFactory.createEntityFromJSONString(IOUtils.toString(in, "UTF-8"), JsonSchema.class);
		}
	}

	private String registerSchema(JsonSchema schema) throws Exception {
		asyncHelper.getOrCreateOrganization(adminUser.getId(), "my.samplesheet.org");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);
		CreateSchemaResponse response = asyncHelper.assertJobResponse(adminUser, request,
				(CreateSchemaResponse r) -> assertNotNull(r.getNewVersionInfo()), MAX_WAIT_MS).getResponse();
		return response.getNewVersionInfo().get$id();
	}
}

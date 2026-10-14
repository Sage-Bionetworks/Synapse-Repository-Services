package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionRequest;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionResponse;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.TaskState;
import org.sagebionetworks.repo.model.curation.TaskStatus;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Full end-to-end test of RecordSet generation through the executable-task framework: it submits a
 * COMPUTE_TASK_EXECUTION async job for a CurationTask whose execution details are
 * {@link RecordSetGenerationExecutionDetails}. The job is picked up by the ComputeTaskExecutionWorker,
 * dispatched to the RecordSetGenerationSubWorker, which runs the AI supervisor. The supervisor must
 * read the actual contents of the input Folder's files off the shared code interpreter session: a data
 * CSV of source rows and a JSON synonym dictionary. It maps the abbreviated source values through the
 * dictionary to the canonical values the target schema's enumeration requires, writes the conformed
 * CSV, and this sub-worker stores it as a new version of the pre-created destination RecordSet and
 * binds the target JSON Schema. Requires live Bedrock + code interpreter access.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RecordSetGenerationIntegrationTest {

	private static final long MAX_WAIT_MS = 1000 * 60 * 4;

	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private CurationTaskManager curationTaskManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private org.sagebionetworks.repo.manager.schema.JsonSchemaManager jsonSchemaManager;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;

	private UserInfo adminUser;
	private String projectId;
	private String inputFolderId;
	private String outputFolderId;
	private String targetSchemaId;

	@BeforeEach
	public void setup() throws Exception {
		jsonSchemaManager.truncateAll();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		Project project = new Project().setName("RecordSetGenIT-" + UUID.randomUUID());
		projectId = entityManager.createEntity(adminUser, project, null);

		Folder outputFolder = new Folder().setName("output").setParentId(projectId);
		outputFolderId = entityManager.createEntity(adminUser, outputFolder, null);

		// The input Folder holds the source FileEntities the supervisor reads and transforms.
		Folder inputFolder = new Folder().setName("input").setParentId(projectId);
		inputFolderId = entityManager.createEntity(adminUser, inputFolder, null);

		// A data CSV of source rows. The 'strand' column uses abbreviations ('fwd'/'rev') that are NOT
		// valid values for the target schema's 'strandedness' enum. The supervisor must map them via the
		// dictionary below to succeed.
		createInputFile("samples.csv",
				"sample_name,read1_file,strand\n"
						+ "SAMPLE_1,sample_1_R1.fastq.gz,fwd\n"
						+ "SAMPLE_2,sample_2_R1.fastq.gz,rev\n",
				ContentType.create("text/csv"));

		// A JSON synonym dictionary mapping the source abbreviations to the schema's allowed values. The
		// supervisor must read this file's contents to conform the data.
		createInputFile("strand-dictionary.json",
				"{\n"
						+ "  \"strandedness\": {\n"
						+ "    \"fwd\": \"forward\",\n"
						+ "    \"rev\": \"reverse\",\n"
						+ "    \"unstr\": \"unstranded\"\n"
						+ "  }\n"
						+ "}\n",
				ContentType.APPLICATION_JSON);

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
	public void testGenerateRecordSetViaComputeTask() throws Exception {
		// The data manager pre-creates the record-based destination task and the RecordSet it targets,
		// with the target JSON Schema already bound to the RecordSet. The sub-worker writes the generated
		// CSV into that RecordSet as a new version.
		String destinationRecordSetId = createDestinationRecordSet();
		String originalDataFileHandleId = entityManager.getEntity(adminUser, destinationRecordSetId, RecordSet.class)
				.getDataFileHandleId();
		CurationTask destinationTask = curationTaskManager.createCurationTask(adminUser, new CurationTask()
				.setProjectId(projectId)
				.setDataType("rnaseq sample sheet review")
				.setInstructions("Review the generated sample sheet.")
				.setTaskProperties(new RecordBasedMetadataTaskProperties().setRecordSetId(destinationRecordSetId)));

		// The generation task carries its input parameters in its RecordSetGenerationExecutionProperties:
		// the input Folder, the transformation instructions, and the destination task. The target schema
		// is the one bound to the destination RecordSet.
		CurationTask task = curationTaskManager.createCurationTask(adminUser, new CurationTask()
				.setProjectId(projectId)
				.setDataType("rnaseq sample sheet")
				.setInstructions("Generate an nf-core/rnaseq sample sheet from the input folder files.")
				.setTaskProperties(new RecordSetGenerationExecutionProperties()
						.setFolderId(inputFolderId)
						.setInstructions("The file samples.csv holds one source row per sample. The file "
								+ "strand-dictionary.json maps abbreviated strand codes to their full names. Produce one "
								+ "output row per source row: map 'sample_name' to 'sample', 'read1_file' to 'fastq_1', "
								+ "and map the 'strand' abbreviation through strand-dictionary.json to the full value for "
								+ "'strandedness'. Leave 'fastq_2' empty.")
						.setDestinationTaskId(destinationTask.getTaskId())));

		// Attach the executable RecordSet generation details (a routing marker), leaving the task in
		// NOT_STARTED.
		TaskStatus currentStatus = curationTaskManager.getTaskStatus(adminUser, task.getTaskId());
		currentStatus.setState(TaskState.NOT_STARTED);
		currentStatus.setExecutionDetails(new RecordSetGenerationExecutionDetails());
		curationTaskManager.updateTaskStatus(adminUser, task.getTaskId(), currentStatus);

		// call under test — submit and monitor the real COMPUTE_TASK_EXECUTION job end to end.
		ComputeTaskExecutionRequest request = new ComputeTaskExecutionRequest().setTaskId(task.getTaskId());
		asyncHelper.assertJobResponse(adminUser, request,
				(ComputeTaskExecutionResponse r) -> assertNotNull(r.getExecutionDetails()), MAX_WAIT_MS).getResponse();

		// The generated CSV was written into the destination RecordSet as a new version, replacing the
		// original CSV file handle.
		RecordSet recordSet = entityManager.getEntity(adminUser, destinationRecordSetId, RecordSet.class);
		assertEquals(EntityType.recordset, entityManager.getEntityType(adminUser, destinationRecordSetId));
		assertNotNull(recordSet.getDataFileHandleId(), "RecordSet should be backed by a CSV file handle");
		assertNotEquals(originalDataFileHandleId, recordSet.getDataFileHandleId(),
				"The generated CSV should replace the original file handle");

		// The generated CSV must contain the dictionary-mapped values, not the source abbreviations,
		// proving the supervisor read the dictionary file's contents and applied the synonym mapping.
		String generatedCsv = fileHandleManager.downloadFileToString(recordSet.getDataFileHandleId());
		assertTrue(generatedCsv.contains("forward"), "Expected mapped value 'forward' in: " + generatedCsv);
		assertTrue(generatedCsv.contains("reverse"), "Expected mapped value 'reverse' in: " + generatedCsv);
		// The source abbreviations must have been mapped away. ('reverse' contains 'rev', so only 'fwd'
		// is a clean signal that the raw source values did not survive.)
		assertFalse(generatedCsv.contains("fwd"), "Source abbreviation 'fwd' should have been mapped away: " + generatedCsv);

		// The generation task advanced to IN_REVIEW.
		assertEquals(TaskState.IN_REVIEW, curationTaskManager.getTaskStatus(adminUser, task.getTaskId()).getState());
	}

	private String createDestinationRecordSet() {
		String dataFileHandleId = fileHandleDaoHelper.create((f) -> {
			f.setCreatedBy(adminUser.getId().toString());
			f.setFileName("destination.csv");
			f.setContentSize(1L);
		}).getId();
		String recordSetId = entityManager.createEntity(adminUser, new RecordSet()
				.setName("Destination RecordSet " + UUID.randomUUID())
				.setParentId(outputFolderId)
				.setDataFileHandleId(dataFileHandleId)
				.setUpsertKey(List.of("sample")), null);
		// The target schema is bound to the destination RecordSet by the data manager ahead of execution.
		entityManager.bindSchemaToEntity(adminUser, new BindSchemaToEntityRequest()
				.setEntityId(recordSetId)
				.setSchema$id(targetSchemaId));
		return recordSetId;
	}

	private void createInputFile(String fileName, String content, ContentType contentType) throws Exception {
		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(adminUser.getId().toString(), new Date(),
				content.getBytes(StandardCharsets.UTF_8), fileName, contentType, null);
		entityManager.createEntity(adminUser, new FileEntity().setName(fileName)
				.setParentId(inputFolderId).setDataFileHandleId(fileHandle.getId()), null);
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

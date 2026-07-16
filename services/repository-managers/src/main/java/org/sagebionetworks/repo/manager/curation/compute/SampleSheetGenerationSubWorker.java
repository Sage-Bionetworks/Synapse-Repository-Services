package org.sagebionetworks.repo.manager.curation.compute;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.agent.supervisor.SampleSheetSupervisorFactory;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.execution.SampleSheetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.util.ValidateArgument;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Executes a sample sheet generation task via the multi-agent supervisor.
 * <p>
 * The AI supervisor is responsible only for producing a conformed CSV on the shared code
 * interpreter session (source annotations from the input EntityView, transformed to match the
 * target JSON Schema). This sub-worker performs the deterministic persistence around that:
 * <ol>
 * <li>Start a code interpreter session and run the supervisor, asking it to write the sample sheet
 * CSV to a known session path.</li>
 * <li>Pull that CSV off the session into a Synapse file handle.</li>
 * <li>Create a {@link RecordSet} entity in the output folder backed by that file handle, and bind
 * the target JSON Schema to it.</li>
 * <li>Create a record-based review {@link CurationTask} referencing the new RecordSet.</li>
 * </ol>
 * The updated {@link SampleSheetGenerationExecutionDetails} (with {@code outputRecordSetId} and
 * {@code reviewTaskId} populated) is returned; the dispatcher then transitions the task to
 * IN_REVIEW.
 */
@Service
public class SampleSheetGenerationSubWorker implements ComputeTaskSubWorker<SampleSheetGenerationExecutionDetails> {

	private static final Logger LOG = LogManager.getLogger(SampleSheetGenerationSubWorker.class);

	static final String OUTPUT_CSV_PATH = "sample_sheet/output.csv";
	static final String SUCCESS_MARKER = "RESULT: SUCCESS";
	static final String ERROR_MARKER = "RESULT: ERROR";

	private final SampleSheetSupervisorFactory supervisorFactory;
	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final CodeInterpreterTools codeInterpreterTools;
	private final EntityManager entityManager;
	private final CurationTaskManager curationTaskManager;

	public SampleSheetGenerationSubWorker(SampleSheetSupervisorFactory supervisorFactory,
			AgentCoreCodeInterpreterClient codeInterpreterClient, CodeInterpreterTools codeInterpreterTools,
			EntityManager entityManager, CurationTaskManager curationTaskManager) {
		this.supervisorFactory = supervisorFactory;
		this.codeInterpreterClient = codeInterpreterClient;
		this.codeInterpreterTools = codeInterpreterTools;
		this.entityManager = entityManager;
		this.curationTaskManager = curationTaskManager;
	}

	@Override
	public Class<SampleSheetGenerationExecutionDetails> getExecutionDetailsType() {
		return SampleSheetGenerationExecutionDetails.class;
	}

	@Override
	public SampleSheetGenerationExecutionDetails execute(UserInfo user, CurationTask task,
			SampleSheetGenerationExecutionDetails details, AsyncJobProgressCallback callback) throws Exception {
		ValidateArgument.required(details.getInputFileViewId(), "inputFileViewId");
		ValidateArgument.required(details.getOutputFolderId(), "outputFolderId");
		ValidateArgument.required(details.getTargetSchemaId(), "targetSchemaId");

		String sessionId = codeInterpreterClient.startSession("sampleSheetGen-" + task.getTaskId());
		try {
			callback.updateProgress("Running the sample sheet supervisor", 0L, 100L);
			String supervisorResponse = supervisorFactory.create().chat(buildSupervisorMessage(details), user, sessionId);

			if (supervisorResponse == null || !supervisorResponse.contains(SUCCESS_MARKER)) {
				throw new IllegalStateException("Sample sheet generation did not succeed: "
						+ extractErrorMessage(supervisorResponse));
			}

			callback.updateProgress("Creating the RecordSet from the generated sample sheet", 50L, 100L);
			List<String> columns = readCsvHeader(sessionId);

			// Pull the generated CSV off the session and into a Synapse file handle. getFileFromSession
			// reads userInfo/sessionId from the tool context, so provide them directly.
			ToolContext toolContext = new ToolContext(Map.of("userInfo", user, "sessionId", sessionId));
			String dataFileHandleId = codeInterpreterTools.getFileFromSession(OUTPUT_CSV_PATH, "text/csv", toolContext);

			RecordSet recordSet = new RecordSet();
			recordSet.setName("Sample Sheet (task " + task.getTaskId() + ")");
			recordSet.setParentId(details.getOutputFolderId());
			recordSet.setDataFileHandleId(dataFileHandleId);
			// upsertKey must be non-empty; use the first column of the generated sample sheet as the key.
			recordSet.setUpsertKey(List.of(columns.get(0)));

			String recordSetId = entityManager.createEntity(user, recordSet, null);

			// Bind the target JSON Schema to the RecordSet so it is validated and indexed against it.
			entityManager.bindSchemaToEntity(user, new BindSchemaToEntityRequest()
					.setEntityId(recordSetId)
					.setSchema$id(details.getTargetSchemaId()));

			callback.updateProgress("Creating the review task", 80L, 100L);
			CurationTask reviewTask = curationTaskManager.createCurationTask(user, new CurationTask()
					.setProjectId(task.getProjectId())
					.setDataType(task.getDataType() + " (sample sheet review)")
					.setInstructions("Review the generated sample sheet.")
					.setTaskProperties(new RecordBasedMetadataTaskProperties().setRecordSetId(recordSetId)));

			details.setOutputRecordSetId(recordSetId);
			details.setReviewTaskId(reviewTask.getTaskId());

			callback.updateProgress("Sample sheet generation complete", 100L, 100L);
			return details;
		} finally {
			try {
				codeInterpreterClient.stopSession(sessionId);
			} catch (Exception e) {
				LOG.warn("Failed to stop code interpreter session {}", sessionId, e);
			}
		}
	}

	String buildSupervisorMessage(SampleSheetGenerationExecutionDetails details) {
		return "Generate a sample sheet.\n"
				+ "inputFileViewId: " + details.getInputFileViewId() + "\n"
				+ "targetSchemaId: " + details.getTargetSchemaId() + "\n"
				+ "outputCsvPath: " + OUTPUT_CSV_PATH + "\n"
				+ "Write the final, conformed sample sheet CSV to outputCsvPath and report the result.";
	}

	/**
	 * Reads the header line of the generated CSV directly from the session to determine the column
	 * names deterministically (rather than parsing them out of the supervisor's prose).
	 */
	List<String> readCsvHeader(String sessionId) {
		CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python",
				"with open('" + OUTPUT_CSV_PATH + "') as f:\n    print(f.readline().strip())");
		if (result.isError()) {
			throw new IllegalStateException("Could not read the generated sample sheet header: " + result.textOutput());
		}
		String header = result.textOutput() == null ? "" : result.textOutput().trim();
		if (header.isEmpty()) {
			throw new IllegalStateException("The generated sample sheet has no header row");
		}
		String[] parts = header.split(",");
		return List.of(parts).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	private String extractErrorMessage(String supervisorResponse) {
		if (supervisorResponse == null) {
			return "the supervisor returned no response";
		}
		int idx = supervisorResponse.indexOf(ERROR_MARKER);
		if (idx >= 0) {
			return supervisorResponse.substring(idx);
		}
		return supervisorResponse;
	}
}

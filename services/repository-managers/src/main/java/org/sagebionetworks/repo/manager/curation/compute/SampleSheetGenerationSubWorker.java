package org.sagebionetworks.repo.manager.curation.compute;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.supervisor.SampleSheetSupervisorFactory;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.execution.SampleSheetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.execution.SampleSheetGenerationExecutionProperties;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.util.ValidateArgument;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Executes a sample sheet generation task via the multi-agent supervisor.
 * <p>
 * The generation task's input parameters are carried by its
 * {@link SampleSheetGenerationExecutionProperties} (task properties): the input file-based task
 * providing the source FileView, and the destination record-based task. The target JSON Schema is
 * the schema bound to the destination RecordSet. The AI supervisor is responsible only for producing
 * a conformed CSV on the shared code interpreter session (source annotations from the input FileView,
 * transformed to match the target JSON Schema). This sub-worker performs the deterministic
 * persistence around that:
 * <ol>
 * <li>Resolve the source FileView from the input file-based task, the destination {@link RecordSet}
 * from the destination record-based task, and the target JSON Schema from the RecordSet's binding.</li>
 * <li>Start a code interpreter session and run the supervisor, asking it to write the sample sheet
 * CSV to a known session path.</li>
 * <li>Pull the generated CSV off the session into a Synapse file handle and store it as a new
 * version of the destination RecordSet.</li>
 * </ol>
 * The input and destination tasks are created and configured by the data manager ahead of
 * execution, which gives them full control over the workflow. The dispatcher transitions the task
 * to IN_REVIEW once this returns.
 */
@Service
public class SampleSheetGenerationSubWorker implements ComputeTaskSubWorker<SampleSheetGenerationExecutionDetails> {

	private static final Logger LOG = LogManager.getLogger(SampleSheetGenerationSubWorker.class);

	static final String OUTPUT_CSV_PATH = "sample_sheet/output.csv";

	private final SampleSheetSupervisorFactory supervisorFactory;
	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final CodeInterpreterFileManager codeInterpreterFileManager;
	private final RecordSetOutputWriter recordSetOutputWriter;
	private final CurationTaskManager curationTaskManager;

	public SampleSheetGenerationSubWorker(SampleSheetSupervisorFactory supervisorFactory,
			AgentCoreCodeInterpreterClient codeInterpreterClient, CodeInterpreterFileManager codeInterpreterFileManager,
			RecordSetOutputWriter recordSetOutputWriter, CurationTaskManager curationTaskManager) {
		this.supervisorFactory = supervisorFactory;
		this.codeInterpreterClient = codeInterpreterClient;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
		this.recordSetOutputWriter = recordSetOutputWriter;
		this.curationTaskManager = curationTaskManager;
	}

	@Override
	public Class<SampleSheetGenerationExecutionDetails> getExecutionDetailsType() {
		return SampleSheetGenerationExecutionDetails.class;
	}

	@Override
	public SampleSheetGenerationExecutionDetails execute(UserInfo user, CurationTask task,
			SampleSheetGenerationExecutionDetails details, AsyncJobProgressCallback callback) throws Exception {
		if (!(task.getTaskProperties() instanceof SampleSheetGenerationExecutionProperties properties)) {
			throw new IllegalArgumentException(
					"Task " + task.getTaskId() + " must have SampleSheetGenerationExecutionProperties.");
		}
		ValidateArgument.required(properties.getInputTaskId(), "inputTaskId");
		ValidateArgument.required(properties.getDestinationTaskId(), "destinationTaskId");

		// Resolve the source FileView, destination RecordSet, and the target schema bound to that
		// RecordSet up front, so a misconfigured task fails before the (expensive) supervisor run.
		String fileViewId = getInputFileViewId(user, properties.getInputTaskId());
		String recordSetId = getDestinationRecordSetId(user, properties.getDestinationTaskId());
		String targetSchemaId = recordSetOutputWriter.getBoundSchemaId(user, recordSetId);

		String sessionId = codeInterpreterClient.startSession("sampleSheetGen-" + task.getTaskId());
		try {
			callback.updateProgress("Running the sample sheet supervisor", 0L, 100L);
			// The batch path runs against an already-started session, so the id is placed directly under
			// CODE_SESSION_ID (no lazy supplier) for the supervisor's specialists to resolve.
			ToolContext toolContext = new ToolContext(Map.of(
					AgentToolContextKey.USER_INFO.getKey(), user,
					AgentToolContextKey.CODE_SESSION_ID.getKey(), sessionId));
			String supervisorResponse = supervisorFactory.create()
					.chat(buildSupervisorMessage(fileViewId, targetSchemaId), toolContext);

			SupervisorResult.requireSuccess(supervisorResponse, "Sample sheet generation did not succeed: ");

			callback.updateProgress("Storing the generated sample sheet in the RecordSet", 50L, 100L);

			// Pull the generated CSV off the session and into a Synapse file handle.
			String dataFileHandleId = codeInterpreterFileManager.getFileFromSession(user, sessionId, OUTPUT_CSV_PATH,
					"text/csv");

			recordSetOutputWriter.storeCsvAsNewRecordSetVersion(user, recordSetId, dataFileHandleId);

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

	/**
	 * Resolves the source FileView from the input task. The input task must be a file-based task with
	 * a FileView; the data manager references the curation task used to collect the source data.
	 */
	String getInputFileViewId(UserInfo user, Long inputTaskId) {
		CurationTask inputTask = curationTaskManager.getCurationTask(user, inputTaskId);
		if (!(inputTask.getTaskProperties() instanceof FileBasedMetadataTaskProperties fileProperties)) {
			throw new IllegalArgumentException(
					"Input task " + inputTaskId + " must be a file-based metadata task.");
		}
		ValidateArgument.required(fileProperties.getFileViewId(), "input task " + inputTaskId + " fileViewId");
		return fileProperties.getFileViewId();
	}

	/**
	 * Resolves the RecordSet that the pre-created destination task references. The destination task
	 * must be a record-based task with a RecordSet; the data manager is responsible for configuring it
	 * before execution.
	 */
	String getDestinationRecordSetId(UserInfo user, Long destinationTaskId) {
		CurationTask destinationTask = curationTaskManager.getCurationTask(user, destinationTaskId);
		if (!(destinationTask.getTaskProperties() instanceof RecordBasedMetadataTaskProperties recordProperties)) {
			throw new IllegalArgumentException(
					"Destination task " + destinationTaskId + " must be a record-based metadata task.");
		}
		ValidateArgument.required(recordProperties.getRecordSetId(),
				"destination task " + destinationTaskId + " recordSetId");
		return recordProperties.getRecordSetId();
	}

	String buildSupervisorMessage(String fileViewId, String targetSchemaId) {
		return "Generate a sample sheet.\n"
				+ "inputFileViewId: " + fileViewId + "\n"
				+ "targetSchemaId: " + targetSchemaId + "\n"
				+ "outputCsvPath: " + OUTPUT_CSV_PATH + "\n"
				+ "Write the final, conformed sample sheet CSV to outputCsvPath and report the result.";
	}

}

package org.sagebionetworks.repo.manager.curation.compute;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.manager.agent.supervisor.RecordSetGenerationSupervisorFactory;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.util.ValidateArgument;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Executes a RecordSet generation task via the multi-agent supervisor.
 * <p>
 * The generation task's input parameters are carried by its
 * {@link RecordSetGenerationExecutionProperties} (task properties): the input Folder of source
 * FileEntities, the data manager's transformation instructions, and the destination record-based
 * task. The target JSON Schema is the schema bound to the destination RecordSet. The AI supervisor is
 * responsible only for producing a conformed CSV on the shared code interpreter session (source files
 * from the input Folder, transformed per the instructions to match the target JSON Schema). This
 * sub-worker performs the deterministic persistence around that:
 * <ol>
 * <li>Resolve the destination {@link RecordSet} from the destination record-based task and the target
 * JSON Schema from the RecordSet's binding, and verify the input Folder does not hold more than
 * {@link #MAX_INPUT_FILES} FileEntities.</li>
 * <li>Start a code interpreter session and run the supervisor, asking it to read the input files and
 * write the conformed CSV to a known session path.</li>
 * <li>Pull the generated CSV off the session into a Synapse file handle and store it as a new version
 * of the destination RecordSet.</li>
 * </ol>
 * The destination task is created and configured by the data manager ahead of execution. The
 * dispatcher transitions the task to IN_REVIEW once this returns.
 */
@Service
public class RecordSetGenerationSubWorker implements ComputeTaskSubWorker<RecordSetGenerationExecutionDetails> {

	private static final Logger LOG = LogManager.getLogger(RecordSetGenerationSubWorker.class);

	static final String OUTPUT_CSV_PATH = "recordset/output.csv";
	static final String MESSAGE_TEMPLATE = "prompts/recordset-generation-message.vtp";

	/**
	 * The maximum number of FileEntities the input Folder may contain. The supervisor reads every
	 * input file's contents into the shared session, so an unbounded folder would push an unbounded
	 * amount of data (and cost) at the agent; the task fails fast when the folder exceeds this.
	 */
	static final int MAX_INPUT_FILES = 20;

	private final RecordSetGenerationSupervisorFactory supervisorFactory;
	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final CodeInterpreterFileManager codeInterpreterFileManager;
	private final RecordSetOutputWriter recordSetOutputWriter;
	private final CurationTaskManager curationTaskManager;
	private final EntityManager entityManager;
	private final VelocityEngine velocityEngine;

	public RecordSetGenerationSubWorker(RecordSetGenerationSupervisorFactory supervisorFactory,
			AgentCoreCodeInterpreterClient codeInterpreterClient, CodeInterpreterFileManager codeInterpreterFileManager,
			RecordSetOutputWriter recordSetOutputWriter, CurationTaskManager curationTaskManager,
			EntityManager entityManager, VelocityEngine velocityEngine) {
		this.supervisorFactory = supervisorFactory;
		this.codeInterpreterClient = codeInterpreterClient;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
		this.recordSetOutputWriter = recordSetOutputWriter;
		this.curationTaskManager = curationTaskManager;
		this.entityManager = entityManager;
		this.velocityEngine = velocityEngine;
	}

	@Override
	public Class<RecordSetGenerationExecutionDetails> getExecutionDetailsType() {
		return RecordSetGenerationExecutionDetails.class;
	}

	@Override
	public RecordSetGenerationExecutionDetails execute(UserInfo user, CurationTask task,
			RecordSetGenerationExecutionDetails details, AsyncJobProgressCallback callback) throws Exception {
		if (!(task.getTaskProperties() instanceof RecordSetGenerationExecutionProperties properties)) {
			throw new IllegalArgumentException(
					"Task " + task.getTaskId() + " must have RecordSetGenerationExecutionProperties.");
		}
		ValidateArgument.required(properties.getFolderId(), "folderId");
		ValidateArgument.required(properties.getInstructions(), "instructions");
		ValidateArgument.required(properties.getDestinationTaskId(), "destinationTaskId");

		// Resolve the destination RecordSet, the target schema bound to it, and bound the input file
		// count up front, so a misconfigured or oversized task fails before the (expensive) supervisor run.
		String recordSetId = getDestinationRecordSetId(user, properties.getDestinationTaskId());
		String targetSchemaId = recordSetOutputWriter.getBoundSchemaId(user, recordSetId);
		validateInputFileCount(user, properties.getFolderId());

		long startNanos = System.nanoTime();
		String sessionId = codeInterpreterClient.startSession("recordSetGen-" + task.getTaskId());
		try {
			LOG.info("Starting RecordSet generation for task {}: inputFolderId={}, recordSetId={}, targetSchemaId={}, "
					+ "sessionId={}", task.getTaskId(), properties.getFolderId(), recordSetId, targetSchemaId, sessionId);
			callback.updateProgress("Running the RecordSet generation supervisor", 0L, 100L);
			// The batch path runs against an already-started session, so a constant supplier over that
			// id is installed for the supervisor's specialists to resolve.
			ToolContext toolContext = new ToolContext(Map.of(
					AgentToolContextKey.USER_INFO.getKey(), user,
					AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of(sessionId)));
			String supervisorResponse = supervisorFactory.create().chat(buildSupervisorMessage(properties.getFolderId(),
					targetSchemaId, properties.getInstructions()), toolContext);
			LOG.info("RecordSet generation supervisor for task {} (session {}) returned: {}", task.getTaskId(),
					sessionId, supervisorResponse);

			SupervisorResult.requireSuccess(supervisorResponse, "RecordSet generation did not succeed: ");

			callback.updateProgress("Storing the generated CSV in the RecordSet", 50L, 100L);

			// Pull the generated CSV off the session and into a Synapse file handle.
			String dataFileHandleId = codeInterpreterFileManager.getFileFromSession(user, sessionId, OUTPUT_CSV_PATH,
					"text/csv");

			recordSetOutputWriter.storeCsvAsNewRecordSetVersion(user, recordSetId, dataFileHandleId);

			callback.updateProgress("RecordSet generation complete", 100L, 100L);
			LOG.info("Completed RecordSet generation for task {} in {} ms: stored file handle {} on RecordSet {}",
					task.getTaskId(), (System.nanoTime() - startNanos) / 1_000_000L, dataFileHandleId, recordSetId);
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

	/**
	 * Fails fast if the input Folder holds more than {@link #MAX_INPUT_FILES} FileEntities. The count
	 * is the total number of direct FileEntity children the user can access, so the supervisor is never
	 * asked to read an unbounded number of files into the session.
	 */
	void validateInputFileCount(UserInfo user, String folderId) {
		long fileCount = entityManager.getChildren(user, new EntityChildrenRequest()
				.setParentId(folderId)
				.setIncludeTypes(List.of(EntityType.file))
				.setIncludeTotalChildCount(true)).getTotalChildCount();
		ValidateArgument.requirement(fileCount <= MAX_INPUT_FILES,
				"The input folder " + folderId + " has " + fileCount + " files, which exceeds the maximum of "
						+ MAX_INPUT_FILES + " allowed for RecordSet generation.");
	}

	/**
	 * Renders the supervisor's task message. The data manager's instructions are supplied as a
	 * Velocity context value (not part of the template source), so they are substituted verbatim and
	 * cannot inject Velocity directives.
	 */
	String buildSupervisorMessage(String folderId, String targetSchemaId, String instructions) {
		VelocityContext context = new VelocityContext();
		context.put("inputFolderId", folderId);
		context.put("targetSchemaId", targetSchemaId);
		context.put("outputCsvPath", OUTPUT_CSV_PATH);
		context.put("instructions", instructions);

		Template template = velocityEngine.getTemplate(MESSAGE_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}
}

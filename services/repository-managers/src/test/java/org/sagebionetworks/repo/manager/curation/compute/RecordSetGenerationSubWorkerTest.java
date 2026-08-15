package org.sagebionetworks.repo.manager.curation.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.supervisor.RecordSetGenerationSupervisorFactory;
import org.sagebionetworks.repo.manager.config.ManagerConfiguration;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.execution.RecordSetGenerationExecutionProperties;
import org.sagebionetworks.repo.model.curation.metadata.FileBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class RecordSetGenerationSubWorkerTest {

	@Mock
	private RecordSetGenerationSupervisorFactory supervisorFactory;
	@Mock
	private Agent supervisor;
	@Mock
	private AgentCoreCodeInterpreterClient codeInterpreterClient;
	@Mock
	private CodeInterpreterFileManager codeInterpreterFileManager;
	@Mock
	private RecordSetOutputWriter recordSetOutputWriter;
	@Mock
	private CurationTaskManager curationTaskManager;
	@Mock
	private EntityManager entityManager;
	@Mock
	private AsyncJobProgressCallback callback;

	private RecordSetGenerationSubWorker subWorker;

	private UserInfo user;
	private CurationTask task;
	private RecordSetGenerationExecutionDetails details;

	@BeforeEach
	public void setup() {
		subWorker = new RecordSetGenerationSubWorker(supervisorFactory, codeInterpreterClient, codeInterpreterFileManager,
				recordSetOutputWriter, curationTaskManager, entityManager, new ManagerConfiguration().velocityEngine());
		user = new UserInfo(false, 101L, AuthorizationConstants.DEFAULT_REALM_ID);
		// The generation task carries its input parameters in its RecordSetGenerationExecutionProperties.
		task = new CurationTask().setTaskId(555L).setProjectId("syn1").setDataType("fastq")
				.setTaskProperties(new RecordSetGenerationExecutionProperties()
						.setFolderId("syn100")
						.setInstructions("One row per file; sample = file name without extension.")
						.setDestinationTaskId(888L));
		details = new RecordSetGenerationExecutionDetails();
	}

	/**
	 * Stubs the record-based destination task (888) referenced by the generation task's properties.
	 */
	private void stubDestinationTask(String recordSetId) {
		when(curationTaskManager.getCurationTask(user, 888L)).thenReturn(new CurationTask().setTaskId(888L)
				.setProjectId("syn1").setTaskProperties(new RecordBasedMetadataTaskProperties().setRecordSetId(recordSetId)));
	}

	/**
	 * Stubs the target schema $id bound to the destination RecordSet.
	 */
	private void stubBoundSchema(String recordSetId, String schemaId) {
		when(recordSetOutputWriter.getBoundSchemaId(user, recordSetId)).thenReturn(schemaId);
	}

	/**
	 * Stubs the input folder's direct FileEntity count returned by the up-front limit check.
	 */
	private void stubInputFileCount(long fileCount) {
		when(entityManager.getChildren(eq(user), any(EntityChildrenRequest.class)))
				.thenReturn(new EntityChildrenResponse().setTotalChildCount(fileCount));
	}

	@Test
	public void testGetExecutionDetailsType() {
		// call under test
		assertEquals(RecordSetGenerationExecutionDetails.class, subWorker.getExecutionDetailsType());
	}

	@Test
	public void testExecuteHappyPath() throws Exception {
		stubDestinationTask("syn300");
		stubBoundSchema("syn300", "my.org-Sheet-1.0.0");
		stubInputFileCount(2);
		when(codeInterpreterClient.startSession(any())).thenReturn("session-1");
		when(supervisorFactory.create()).thenReturn(supervisor);
		when(supervisor.chat(any(), any()))
				.thenReturn("Done. RESULT: SUCCESS - " + RecordSetGenerationSubWorker.OUTPUT_CSV_PATH);
		when(codeInterpreterFileManager.getFileFromSession(eq(user), eq("session-1"),
				eq(RecordSetGenerationSubWorker.OUTPUT_CSV_PATH), eq("text/csv"))).thenReturn("999");

		// call under test
		RecordSetGenerationExecutionDetails result = subWorker.execute(user, task, details, callback);

		assertEquals(details, result);

		// The input folder, the RecordSet's bound schema, and the untrusted instructions (inside
		// delimiters) are passed to the supervisor, along with a tool context carrying the acting user
		// and the already-started code session.
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ToolContext> contextCaptor = ArgumentCaptor.forClass(ToolContext.class);
		verify(supervisor).chat(messageCaptor.capture(), contextCaptor.capture());
		String message = messageCaptor.getValue();
		assertTrue(message.contains("inputFolderId: syn100"), "Got: " + message);
		assertTrue(message.contains("targetSchemaId: my.org-Sheet-1.0.0"), "Got: " + message);
		assertTrue(message.contains("<instructions>"), "Got: " + message);
		assertTrue(message.contains("sample = file name without extension"), "Got: " + message);
		assertEquals(user, AgentToolContextKey.USER_INFO.get(contextCaptor.getValue()));
		assertEquals("session-1", AgentToolContextKey.CODE_SESSION_ID.get(contextCaptor.getValue()));

		// The generated CSV is handed to the shared writer to store as a new RecordSet version.
		verify(recordSetOutputWriter).storeCsvAsNewRecordSetVersion(user, "syn300", "999");

		// The referenced task is left untouched.
		verify(curationTaskManager, never()).updateCurationTask(any(), any());

		// Session is always stopped.
		verify(codeInterpreterClient).stopSession("session-1");
	}

	@Test
	public void testExecuteWithMissingExecutionProperties() {
		task.setTaskProperties(new FileBasedMetadataTaskProperties());

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> subWorker.execute(user, task, details, callback));

		assertTrue(ex.getMessage().contains("RecordSetGenerationExecutionProperties"));
		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testExecuteWithNonRecordBasedDestinationTask() {
		// The destination task is not a record-based task; this fails before the supervisor runs.
		when(curationTaskManager.getCurationTask(user, 888L)).thenReturn(new CurationTask().setTaskId(888L)
				.setProjectId("syn1").setTaskProperties(new FileBasedMetadataTaskProperties()));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> subWorker.execute(user, task, details, callback));

		assertTrue(ex.getMessage().contains("must be a record-based metadata task"));
		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testExecuteWithDestinationTaskMissingRecordSet() {
		// The destination task is record-based but has no RecordSet configured.
		when(curationTaskManager.getCurationTask(user, 888L)).thenReturn(new CurationTask().setTaskId(888L)
				.setProjectId("syn1").setTaskProperties(new RecordBasedMetadataTaskProperties()));

		// call under test
		assertThrows(IllegalArgumentException.class, () -> subWorker.execute(user, task, details, callback));

		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testExecuteWithSupervisorError() throws Exception {
		stubDestinationTask("syn300");
		stubBoundSchema("syn300", "my.org-Sheet-1.0.0");
		stubInputFileCount(2);
		when(codeInterpreterClient.startSession(any())).thenReturn("session-1");
		when(supervisorFactory.create()).thenReturn(supervisor);
		when(supervisor.chat(any(), any()))
				.thenReturn("RESULT: ERROR - the input folder syn100 is empty");

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> subWorker.execute(user, task, details, callback));

		assertTrue(ex.getMessage().contains("input folder syn100 is empty"));
		// No persistence should occur when the supervisor did not succeed.
		verify(recordSetOutputWriter, never()).storeCsvAsNewRecordSetVersion(any(), any(), any());
		// Session is still stopped.
		verify(codeInterpreterClient).stopSession("session-1");
	}

	@Test
	public void testExecuteWithMissingFolderId() {
		((RecordSetGenerationExecutionProperties) task.getTaskProperties()).setFolderId(null);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> subWorker.execute(user, task, details, callback));

		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testExecuteWithMissingInstructions() {
		((RecordSetGenerationExecutionProperties) task.getTaskProperties()).setInstructions(null);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> subWorker.execute(user, task, details, callback));

		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testExecuteWithMissingDestinationTaskId() {
		((RecordSetGenerationExecutionProperties) task.getTaskProperties()).setDestinationTaskId(null);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> subWorker.execute(user, task, details, callback));

		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testExecuteWithInputFileCountAtLimit() throws Exception {
		stubDestinationTask("syn300");
		stubBoundSchema("syn300", "my.org-Sheet-1.0.0");
		// Exactly at the limit is allowed; the supervisor runs.
		stubInputFileCount(RecordSetGenerationSubWorker.MAX_INPUT_FILES);
		when(codeInterpreterClient.startSession(any())).thenReturn("session-1");
		when(supervisorFactory.create()).thenReturn(supervisor);
		when(supervisor.chat(any(), any()))
				.thenReturn("RESULT: SUCCESS - " + RecordSetGenerationSubWorker.OUTPUT_CSV_PATH);
		when(codeInterpreterFileManager.getFileFromSession(eq(user), eq("session-1"),
				eq(RecordSetGenerationSubWorker.OUTPUT_CSV_PATH), eq("text/csv"))).thenReturn("999");

		// call under test
		subWorker.execute(user, task, details, callback);

		verify(recordSetOutputWriter).storeCsvAsNewRecordSetVersion(user, "syn300", "999");
	}

	@Test
	public void testExecuteWithTooManyInputFiles() {
		stubDestinationTask("syn300");
		stubBoundSchema("syn300", "my.org-Sheet-1.0.0");
		// One over the limit fails fast, before the (expensive) supervisor run.
		stubInputFileCount(RecordSetGenerationSubWorker.MAX_INPUT_FILES + 1);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> subWorker.execute(user, task, details, callback));

		assertTrue(ex.getMessage().contains("exceeds the maximum"), "Got: " + ex.getMessage());
		verify(codeInterpreterClient, never()).startSession(any());
		verify(supervisorFactory, never()).create();
		verify(recordSetOutputWriter, never()).storeCsvAsNewRecordSetVersion(any(), any(), any());
	}
}

package org.sagebionetworks.repo.manager.curation.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.sagebionetworks.repo.manager.agent.supervisor.SampleSheetSupervisor;
import org.sagebionetworks.repo.manager.agent.supervisor.SampleSheetSupervisorFactory;
import org.sagebionetworks.repo.manager.curation.CurationTaskManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.CurationTask;
import org.sagebionetworks.repo.model.curation.execution.SampleSheetGenerationExecutionDetails;
import org.sagebionetworks.repo.model.curation.metadata.RecordBasedMetadataTaskProperties;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class SampleSheetGenerationSubWorkerTest {

	@Mock
	private SampleSheetSupervisorFactory supervisorFactory;
	@Mock
	private SampleSheetSupervisor supervisor;
	@Mock
	private AgentCoreCodeInterpreterClient codeInterpreterClient;
	@Mock
	private CodeInterpreterTools codeInterpreterTools;
	@Mock
	private EntityManager entityManager;
	@Mock
	private CurationTaskManager curationTaskManager;
	@Mock
	private AsyncJobProgressCallback callback;

	private SampleSheetGenerationSubWorker subWorker;

	private UserInfo user;
	private CurationTask task;
	private SampleSheetGenerationExecutionDetails details;

	@BeforeEach
	public void setup() {
		subWorker = new SampleSheetGenerationSubWorker(supervisorFactory, codeInterpreterClient, codeInterpreterTools,
				entityManager, curationTaskManager);
		user = new UserInfo(false, 101L);
		task = new CurationTask().setTaskId(555L).setProjectId("syn1").setDataType("fastq");
		details = new SampleSheetGenerationExecutionDetails()
				.setInputFileViewId("syn100")
				.setOutputFolderId("syn200")
				.setTargetSchemaId("my.org-Sheet-1.0.0");
	}

	@Test
	public void testGetExecutionDetailsType() {
		// call under test
		assertEquals(SampleSheetGenerationExecutionDetails.class, subWorker.getExecutionDetailsType());
	}

	@Test
	public void testExecuteHappyPath() throws Exception {
		when(codeInterpreterClient.startSession(any())).thenReturn("session-1");
		when(supervisorFactory.create()).thenReturn(supervisor);
		when(supervisor.chat(any(), eq(user), eq("session-1")))
				.thenReturn("Done. RESULT: SUCCESS - " + SampleSheetGenerationSubWorker.OUTPUT_CSV_PATH);
		// Header read from the session.
		when(codeInterpreterClient.executeCode(eq("session-1"), eq("python"), any()))
				.thenReturn(new CodeExecutionResult("sampleId,assay,platform\n", false, List.of()));
		when(codeInterpreterTools.getFileFromSession(eq(SampleSheetGenerationSubWorker.OUTPUT_CSV_PATH), eq("text/csv"),
				any(ToolContext.class))).thenReturn("999");
		when(entityManager.createEntity(eq(user), any(RecordSet.class), isNull())).thenReturn("syn300");
		when(curationTaskManager.createCurationTask(eq(user), any(CurationTask.class)))
				.thenReturn(new CurationTask().setTaskId(777L));

		// call under test
		SampleSheetGenerationExecutionDetails result = subWorker.execute(user, task, details, callback);

		assertEquals("syn300", result.getOutputRecordSetId());
		assertEquals(777L, result.getReviewTaskId());

		// RecordSet created in the output folder, backed by the pulled file handle, keyed by the first column.
		ArgumentCaptor<RecordSet> recordSetCaptor = ArgumentCaptor.forClass(RecordSet.class);
		verify(entityManager).createEntity(eq(user), recordSetCaptor.capture(), isNull());
		RecordSet created = recordSetCaptor.getValue();
		assertEquals("syn200", created.getParentId());
		assertEquals("999", created.getDataFileHandleId());
		assertEquals(List.of("sampleId"), created.getUpsertKey());

		// Target schema bound to the new RecordSet.
		ArgumentCaptor<BindSchemaToEntityRequest> bindCaptor = ArgumentCaptor.forClass(BindSchemaToEntityRequest.class);
		verify(entityManager).bindSchemaToEntity(eq(user), bindCaptor.capture());
		assertEquals("syn300", bindCaptor.getValue().getEntityId());
		assertEquals("my.org-Sheet-1.0.0", bindCaptor.getValue().getSchema$id());

		// Review task references the new RecordSet.
		ArgumentCaptor<CurationTask> taskCaptor = ArgumentCaptor.forClass(CurationTask.class);
		verify(curationTaskManager).createCurationTask(eq(user), taskCaptor.capture());
		CurationTask reviewTask = taskCaptor.getValue();
		assertEquals("syn1", reviewTask.getProjectId());
		assertTrue(reviewTask.getTaskProperties() instanceof RecordBasedMetadataTaskProperties);
		assertEquals("syn300", ((RecordBasedMetadataTaskProperties) reviewTask.getTaskProperties()).getRecordSetId());

		// Session is always stopped.
		verify(codeInterpreterClient).stopSession("session-1");
	}

	@Test
	public void testExecuteWithSupervisorError() throws Exception {
		when(codeInterpreterClient.startSession(any())).thenReturn("session-1");
		when(supervisorFactory.create()).thenReturn(supervisor);
		when(supervisor.chat(any(), eq(user), eq("session-1")))
				.thenReturn("RESULT: ERROR - the input view syn100 is empty");

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> subWorker.execute(user, task, details, callback));

		assertTrue(ex.getMessage().contains("input view syn100 is empty"));
		// No persistence should occur when the supervisor did not succeed.
		verify(entityManager, never()).createEntity(any(), any(), any());
		verify(curationTaskManager, never()).createCurationTask(any(), any());
		// Session is still stopped.
		verify(codeInterpreterClient).stopSession("session-1");
	}

	@Test
	public void testExecuteWithMissingInputFileViewId() {
		details.setInputFileViewId(null);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> subWorker.execute(user, task, details, callback));
	}
}

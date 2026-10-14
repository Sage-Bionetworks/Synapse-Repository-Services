package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFailureCode;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileRequest;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileResult;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentFailureCode;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentState;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;

@ExtendWith(MockitoExtension.class)
public class AgentChatAttachmentStagerTest {

	@Mock
	private CodeInterpreterFileManager mockCodeInterpreterFileManager;
	@Mock
	private CodeInterpreterSessionProvider mockSessionProvider;

	@InjectMocks
	private AgentChatAttachmentStager stager;

	private final UserInfo user = new UserInfo(false, 101L, AuthorizationConstants.DEFAULT_REALM_ID);
	private final String agentSessionId = "agent-session-1";
	private final String codeSessionId = "code-session-1";

	private FileHandleAssociation association(String fileHandleId, String associateObjectId) {
		return new FileHandleAssociation().setFileHandleId(fileHandleId)
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId(associateObjectId);
	}

	private PushFileResult stagedResult(FileHandleAssociation association, String sessionPath, String fileName,
			String contentType, Long contentSizeBytes) {
		return new PushFileResult(new PushFileRequest(association, null), sessionPath, null, null, null, fileName,
				contentType, contentSizeBytes);
	}

	private PushFileResult failedResult(FileHandleAssociation association, String error, PushFailureCode failureCode) {
		return new PushFileResult(new PushFileRequest(association, null), null, null, error, failureCode, null, null,
				null);
	}

	@Test
	public void testStageAttachmentsWithNullAttachments() {
		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId, null);

		assertEquals(List.of(), statuses);
		// A conversational turn with no attachments must not resolve or touch a code session.
		verifyNoInteractions(mockSessionProvider);
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testStageAttachmentsWithEmptyAttachments() {
		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId, List.of());

		assertEquals(List.of(), statuses);
		verifyNoInteractions(mockSessionProvider);
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testStageAttachmentsWithTooManyAttachments() {
		List<FileHandleAssociation> attachments = new ArrayList<>();
		for (int i = 0; i <= AgentChatAttachmentStager.MAX_AGENT_CHAT_ATTACHMENTS; i++) {
			attachments.add(association(String.valueOf(i), "syn" + i));
		}

		// call under test
		String message = assertThrows(IllegalArgumentException.class,
				() -> stager.stageAttachments(user, agentSessionId, attachments)).getMessage();

		assertTrue(message.contains("at most " + AgentChatAttachmentStager.MAX_AGENT_CHAT_ATTACHMENTS), "Got: " + message);
		// Over-count is rejected before any session is resolved or any file is staged.
		verifyNoInteractions(mockSessionProvider);
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testStageAttachmentsWithCumulativeLimitExceeded() {
		// The session already holds all-but-one of the allowed files, so attaching two more would push the
		// cumulative total past the limit even though this single turn is well under it.
		List<FileHandleAssociation> attachments = List.of(association("1", "syn1"), association("2", "syn2"));

		when(mockSessionProvider.getOrCreateSession(agentSessionId)).thenReturn(codeSessionId);
		when(mockCodeInterpreterFileManager.countFilesInSessionDirectory(codeSessionId,
				CodeInterpreterFileManager.ATTACHMENTS_DIRECTORY))
						.thenReturn(AgentChatAttachmentStager.MAX_AGENT_CHAT_ATTACHMENTS - 1);

		// call under test
		String message = assertThrows(IllegalArgumentException.class,
				() -> stager.stageAttachments(user, agentSessionId, attachments)).getMessage();

		assertTrue(message.contains("maximum of " + AgentChatAttachmentStager.MAX_AGENT_CHAT_ATTACHMENTS),
				"Got: " + message);
		// The cumulative cap is enforced before any file is staged.
		verify(mockCodeInterpreterFileManager, never()).pushFileHandlesToSession(any(), any(), any());
	}

	@Test
	public void testStageAttachmentsAtCumulativeLimit() {
		// The session holds one fewer than the limit and this turn attaches exactly one, hitting the limit
		// but not exceeding it, so the file is staged.
		FileHandleAssociation association = association("222", "syn123");

		when(mockSessionProvider.getOrCreateSession(agentSessionId)).thenReturn(codeSessionId);
		when(mockCodeInterpreterFileManager.countFilesInSessionDirectory(codeSessionId,
				CodeInterpreterFileManager.ATTACHMENTS_DIRECTORY))
						.thenReturn(AgentChatAttachmentStager.MAX_AGENT_CHAT_ATTACHMENTS - 1);
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(user), any(), eq(codeSessionId)))
				.thenReturn(List.of(stagedResult(association, "attachments/data.csv", "data.csv", "text/csv", 4096L)));

		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId, List.of(association));

		assertEquals(1, statuses.size());
		assertEquals(AgentChatAttachmentState.STAGED, statuses.get(0).getStatus());
	}

	@Test
	public void testStageAttachmentsWithStagedFile() {
		FileHandleAssociation association = association("222", "syn123");

		when(mockSessionProvider.getOrCreateSession(agentSessionId)).thenReturn(codeSessionId);
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(user), any(), eq(codeSessionId)))
				.thenReturn(List.of(stagedResult(association, "attachments/data.csv", "data.csv", "text/csv", 4096L)));

		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId, List.of(association));

		AgentChatAttachmentStatus expected = new AgentChatAttachmentStatus()
				.setFileHandleId("222")
				.setAssociateObjectId("syn123")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setStatus(AgentChatAttachmentState.STAGED)
				.setFileName("data.csv")
				.setSessionPath("attachments/data.csv")
				.setContentType("text/csv")
				.setContentSizeBytes(4096L);
		assertEquals(List.of(expected), statuses);

		// The session is resolved eagerly (the same one Curie's lazy supplier reuses), and each attachment
		// is pushed with a null session path so the manager derives it from the file name.
		verify(mockSessionProvider).getOrCreateSession(agentSessionId);
		ArgumentCaptor<List<PushFileRequest>> pushCaptor = ArgumentCaptor.forClass(List.class);
		verify(mockCodeInterpreterFileManager).pushFileHandlesToSession(eq(user), pushCaptor.capture(),
				eq(codeSessionId));
		List<PushFileRequest> pushRequests = pushCaptor.getValue();
		assertEquals(1, pushRequests.size());
		assertEquals(association, pushRequests.get(0).association());
		assertEquals(null, pushRequests.get(0).sessionPath());
	}

	@Test
	public void testStageAttachmentsWithFailedFile() {
		FileHandleAssociation association = association("222", "syn123");

		when(mockSessionProvider.getOrCreateSession(agentSessionId)).thenReturn(codeSessionId);
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(user), any(), eq(codeSessionId)))
				.thenReturn(List.of(failedResult(association, "You do not have permission to download this file.",
						PushFailureCode.UNAUTHORIZED)));

		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId, List.of(association));

		AgentChatAttachmentStatus expected = new AgentChatAttachmentStatus()
				.setFileHandleId("222")
				.setAssociateObjectId("syn123")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setStatus(AgentChatAttachmentState.FAILED)
				.setFailureMessage("You do not have permission to download this file.")
				.setFailureCode(AgentChatAttachmentFailureCode.UNAUTHORIZED);
		assertEquals(List.of(expected), statuses);
	}

	@Test
	public void testStageAttachmentsMapsEveryFailureCode() {
		FileHandleAssociation notFound = association("1", "syn1");
		FileHandleAssociation unauthorized = association("2", "syn2");
		FileHandleAssociation unsupported = association("3", "syn3");
		FileHandleAssociation tooLarge = association("4", "syn4");
		FileHandleAssociation notS3 = association("5", "syn5");
		FileHandleAssociation executionError = association("6", "syn6");
		List<FileHandleAssociation> attachments = List.of(notFound, unauthorized, unsupported, tooLarge, notS3,
				executionError);

		when(mockSessionProvider.getOrCreateSession(agentSessionId)).thenReturn(codeSessionId);
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(user), any(), eq(codeSessionId)))
				.thenReturn(List.of(
						failedResult(notFound, "not found", PushFailureCode.NOT_FOUND),
						failedResult(unauthorized, "unauthorized", PushFailureCode.UNAUTHORIZED),
						failedResult(unsupported, "unsupported", PushFailureCode.UNSUPPORTED_TYPE),
						failedResult(tooLarge, "too large", PushFailureCode.EXCEEDS_SIZE_LIMIT),
						failedResult(notS3, "not s3", PushFailureCode.NOT_S3),
						failedResult(executionError, "execution error", PushFailureCode.EXECUTION_ERROR)));

		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId, attachments);

		// Codes a client can act on map through directly; the rest collapse to UNKNOWN_ERROR. Order is
		// preserved, so each status lines up with its attachment.
		assertEquals(6, statuses.size());
		assertEquals(AgentChatAttachmentFailureCode.NOT_FOUND, statuses.get(0).getFailureCode());
		assertEquals(AgentChatAttachmentFailureCode.UNAUTHORIZED, statuses.get(1).getFailureCode());
		assertEquals(AgentChatAttachmentFailureCode.UNSUPPORTED_TYPE, statuses.get(2).getFailureCode());
		assertEquals(AgentChatAttachmentFailureCode.EXCEEDS_SIZE_LIMIT, statuses.get(3).getFailureCode());
		assertEquals(AgentChatAttachmentFailureCode.UNKNOWN_ERROR, statuses.get(4).getFailureCode());
		assertEquals(AgentChatAttachmentFailureCode.UNKNOWN_ERROR, statuses.get(5).getFailureCode());
		assertEquals("syn1", statuses.get(0).getAssociateObjectId());
		assertEquals("syn6", statuses.get(5).getAssociateObjectId());
	}

	@Test
	public void testStageAttachmentsPreservesRequestOrder() {
		FileHandleAssociation first = association("1", "syn1");
		FileHandleAssociation second = association("2", "syn2");
		FileHandleAssociation third = association("3", "syn3");

		when(mockSessionProvider.getOrCreateSession(agentSessionId)).thenReturn(codeSessionId);
		when(mockCodeInterpreterFileManager.pushFileHandlesToSession(eq(user), any(), eq(codeSessionId)))
				.thenReturn(List.of(
						stagedResult(first, "attachments/a.csv", "a.csv", "text/csv", 1L),
						failedResult(second, "too large", PushFailureCode.EXCEEDS_SIZE_LIMIT),
						stagedResult(third, "attachments/c.csv", "c.csv", "text/csv", 3L)));

		// call under test
		List<AgentChatAttachmentStatus> statuses = stager.stageAttachments(user, agentSessionId,
				List.of(first, second, third));

		assertEquals(3, statuses.size());
		assertEquals("syn1", statuses.get(0).getAssociateObjectId());
		assertEquals(AgentChatAttachmentState.STAGED, statuses.get(0).getStatus());
		assertEquals("syn2", statuses.get(1).getAssociateObjectId());
		assertEquals(AgentChatAttachmentState.FAILED, statuses.get(1).getStatus());
		assertEquals("syn3", statuses.get(2).getAssociateObjectId());
		assertEquals(AgentChatAttachmentState.STAGED, statuses.get(2).getStatus());
	}
}

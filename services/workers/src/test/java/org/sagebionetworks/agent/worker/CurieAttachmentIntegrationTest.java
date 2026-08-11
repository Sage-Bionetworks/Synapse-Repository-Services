package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterSessionProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentState;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * End-to-end test that a user can attach existing Synapse files to a Curie chat turn and Curie can
 * read them. Synapse exposes two flavors of file that a user may attach:
 * <ol>
 * <li>A <b>raw file handle</b> — an uploaded file that is not associated with any Synapse object and
 * is therefore downloadable only by the user who created it.</li>
 * <li>A file backed by a Synapse object, here a {@link FileEntity}.</li>
 * </ol>
 * Both must be attachable. Each attachment is a {@link FileHandleAssociation} the user is authorized to
 * download; the staging path copies it into the turn's code-interpreter session so Curie can operate on
 * it by path. We upload one file of each flavor, each carrying a distinct text "code", attach both to a
 * single chat turn, and assert that (1) Curie reports both codes back — proving it actually read the
 * staged files — and (2) {@link AgentChatResponse#getAttachmentStatuses()} reports both as STAGED with a
 * session path. A later turn attaches a third file and reads it, and we then assert the authoritative
 * in-session file count grows from two to three — proving the code interpreter session is reused across
 * chat requests so attachments accumulate rather than reset. Requires live Bedrock + code interpreter +
 * AgentCore Memory access.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@TestInstance(Lifecycle.PER_CLASS)
public class CurieAttachmentIntegrationTest {

	private static final long MAX_WAIT_MS = 1000L * 60 * 3;
	private static final String RAW_HANDLE_CODE = "RAW-HANDLE-CODE-88421";
	private static final String FILE_ENTITY_CODE = "FILE-ENTITY-CODE-13795";
	private static final String SECOND_TURN_CODE = "SECOND-TURN-CODE-52063";

	@Autowired
	private AgentService agentService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityService entityService;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridManager gridManager;
	@Autowired
	private CodeInterpreterSessionProvider codeInterpreterSessionProvider;
	@Autowired
	private CodeInterpreterFileManager codeInterpreterFileManager;

	private UserInfo admin;
	private GridSession session;
	private Long usersReplicaId;
	private FileHandleAssociation rawHandleAttachment;
	private FileHandleAssociation fileEntityAttachment;
	private FileHandleAssociation secondTurnAttachment;

	@BeforeAll
	public void beforeAll() throws Exception {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityService.truncateAll();

		Project project = entityService.createEntity(admin.getId(), new Project().setName("CurieAttachmentTest"), null);

		// A raw file handle: uploaded but never attached to any Synapse object. It is authorized for
		// download solely because admin created it (AuthorizationManagerImpl.canDownloadFile authorizes the
		// creator unconditionally, before and independent of any association). A raw handle has no associated
		// object, so we self-reference it: associateObjectId = fileHandleId with an arbitrary non-null type.
		// The association's object is only consulted in the non-creator authorization branch, which a raw
		// handle — downloadable only by its creator — can never reach; the fields exist only to satisfy the
		// non-null validation of FileHandleAssociation.
		S3FileHandle rawHandle = uploadTextFile("raw-handle", "The secret code is " + RAW_HANDLE_CODE + ".");
		rawHandleAttachment = new FileHandleAssociation().setFileHandleId(rawHandle.getId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId(rawHandle.getId());

		// A file backed by a Synapse object: a FileEntity. The attachment associates the handle with the
		// entity, and download authorization resolves through the entity's ACL in the usual way.
		S3FileHandle entityHandle = uploadTextFile("file-entity", "The secret code is " + FILE_ENTITY_CODE + ".");
		FileEntity fileEntity = entityService.createEntity(admin.getId(), new FileEntity().setName("curie-attachment")
				.setParentId(project.getId()).setDataFileHandleId(entityHandle.getId()), null);
		fileEntityAttachment = new FileHandleAssociation().setFileHandleId(entityHandle.getId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId(fileEntity.getId());

		// A third raw handle, attached on a later turn to prove the code session is reused: it should land in
		// the same session that already holds the first turn's files.
		S3FileHandle secondTurnHandle = uploadTextFile("second-turn", "The secret code is " + SECOND_TURN_CODE + ".");
		secondTurnAttachment = new FileHandleAssociation().setFileHandleId(secondTurnHandle.getId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId(secondTurnHandle.getId());

		// A minimal empty grid is enough: this test exercises attachment staging, not grid data. The
		// experimental grid context still needs a real session and user replica so the agent session validates
		// and routes through the Curie supervisor.
		session = asynchronousJobWorkerHelper.assertJobResponse(admin, new CreateGridRequest(),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		usersReplicaId = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica()
				.getReplicaId();
	}

	@Test
	public void testCurieReadsRawHandleAndFileEntityAttachments() throws Exception {
		// One durable agent session across both turns. The code interpreter session is resolved
		// deterministically from this agent session, so both turns must share the same session.
		AgentSession agentSession = createCurieSession();

		// Turn 1 — attach both files and ask Curie to read each and report its code. Curie learns the staged
		// files (and their session paths) from the per-turn preamble, then reads them via its code interpreter
		// — so recovering both codes proves both flavors were staged and readable.
		AgentChatResponse firstResponse = chat(agentSession, List.of(rawHandleAttachment, fileEntityAttachment),
				"I've attached two text files. Read both of them and report the exact code string contained in "
						+ "each file.");

		String firstText = firstResponse.getResponseText();
		assertTrue(firstText.contains(RAW_HANDLE_CODE),
				"Curie should report the raw file handle's code. Got: " + firstText);
		assertTrue(firstText.contains(FILE_ENTITY_CODE),
				"Curie should report the FileEntity's code. Got: " + firstText);

		// Both attachments must be reported STAGED, in request order, each with a session path.
		List<AgentChatAttachmentStatus> firstStatuses = firstResponse.getAttachmentStatuses();
		assertNotNull(firstStatuses);
		assertEquals(2, firstStatuses.size());

		AgentChatAttachmentStatus rawStatus = firstStatuses.get(0);
		assertEquals(rawHandleAttachment.getFileHandleId(), rawStatus.getFileHandleId());
		assertEquals(AgentChatAttachmentState.STAGED, rawStatus.getStatus());
		assertNotNull(rawStatus.getSessionPath());

		AgentChatAttachmentStatus entityStatus = firstStatuses.get(1);
		assertEquals(fileEntityAttachment.getFileHandleId(), entityStatus.getFileHandleId());
		assertEquals(AgentChatAttachmentState.STAGED, entityStatus.getStatus());
		assertNotNull(entityStatus.getSessionPath());

		// The two files staged on turn 1 are the only files in the session's attachments directory. This
		// authoritative count (read straight from the session, not from Curie's prose) is the baseline the
		// next turn's file must add to.
		assertEquals(2, attachmentsFileCount(agentSession));

		// Turn 2 — attach a third file and ask only for its code, confirming a later turn can stage and read a
		// new attachment.
		AgentChatResponse secondResponse = chat(agentSession, List.of(secondTurnAttachment),
				"I've now attached one more file. Read it and report the exact code string it contains.");

		String secondText = secondResponse.getResponseText();
		assertTrue(secondText.contains(SECOND_TURN_CODE),
				"Curie should report the newly attached file's code. Got: " + secondText);

		// attachmentStatuses is per-turn: this turn reports only the file it attached, staged into the session.
		List<AgentChatAttachmentStatus> secondStatuses = secondResponse.getAttachmentStatuses();
		assertNotNull(secondStatuses);
		assertEquals(1, secondStatuses.size());
		assertEquals(secondTurnAttachment.getFileHandleId(), secondStatuses.get(0).getFileHandleId());
		assertEquals(AgentChatAttachmentState.STAGED, secondStatuses.get(0).getStatus());
		assertNotNull(secondStatuses.get(0).getSessionPath());

		// The attachments directory now holds all three files from both turns. Because every turn's
		// attachments stage under the same "attachments/" directory, a total of three proves the second
		// turn added to the first turn's files rather than replacing them — i.e. the same code session is
		// reused across chat requests. A fresh-per-turn session would hold only the single turn-2 file.
		assertEquals(3, attachmentsFileCount(agentSession));
	}

	/**
	 * The authoritative count of files currently in the session's attachments directory, resolved the same
	 * way production does: the code interpreter session is derived deterministically from the agent session,
	 * then counted in-session. This is the cumulative total across all of the conversation's turns.
	 */
	private int attachmentsFileCount(AgentSession agentSession) {
		String codeSessionId = codeInterpreterSessionProvider.getOrCreateSession(agentSession.getSessionId());
		return codeInterpreterFileManager.countFilesInSessionDirectory(codeSessionId,
				CodeInterpreterFileManager.ATTACHMENTS_DIRECTORY);
	}

	/**
	 * Create an experimental grid agent session bound to the shared grid session, so chat turns route
	 * through the Curie supervisor rather than the default Bedrock agent.
	 */
	private AgentSession createCurieSession() {
		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(session.getSessionId())
				.setUsersReplicaId(usersReplicaId).setExperimental(true);
		AgentSession agentSession = agentService.createSession(admin.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		return agentSession;
	}

	/**
	 * Send one chat turn (with attachments) through the agent chat async job and return the full response.
	 */
	private AgentChatResponse chat(AgentSession agentSession, List<FileHandleAssociation> attachments, String chatText)
			throws Exception {
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(agentSession.getSessionId()).setChatText(chatText)
						.setAttachments(attachments).setEnableTrace(true),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(agentSession.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					System.out.println(response.getResponseText());
				}, MAX_WAIT_MS).getResponse();
	}

	private S3FileHandle uploadTextFile(String namePrefix, String contents) throws Exception {
		File temp = File.createTempFile(namePrefix, ".txt");
		try (FileWriter writer = new FileWriter(temp)) {
			writer.write(contents);
		}
		S3FileHandle handle = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/plain").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();
		return handle;
	}
}

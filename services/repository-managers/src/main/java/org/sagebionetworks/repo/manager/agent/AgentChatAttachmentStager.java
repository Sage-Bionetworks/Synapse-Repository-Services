package org.sagebionetworks.repo.manager.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFailureCode;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileRequest;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager.PushFileResult;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentFailureCode;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentState;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Stages the attachments of a single chat turn into the turn's code interpreter session. Each
 * attachment is a reference to an existing Synapse file (a {@link FileHandleAssociation}); staging
 * copies the authorized, eligible files into the shared session so the agent and its specialists can
 * read them by path for the rest of the conversation. Per-attachment authorization, the approved-type
 * whitelist, and the per-file size cap are all enforced by {@link CodeInterpreterFileManager}; this
 * class adds only the attachment limit and the mapping of each staging outcome onto the
 * {@link AgentChatAttachmentStatus} returned to the client.
 */
@Service
public class AgentChatAttachmentStager {

	/**
	 * The maximum number of attached files a single Curie chat session may hold. Because the code
	 * interpreter session is reused across chat turns, this is a cumulative cap over the whole
	 * conversation, not a per-turn cap: a turn is rejected when the files already staged in the session
	 * plus the turn's own attachments would exceed this limit.
	 */
	static final int MAX_AGENT_CHAT_ATTACHMENTS = 20;

	private final CodeInterpreterFileManager codeInterpreterFileManager;
	private final CodeInterpreterSessionProvider sessionProvider;

	public AgentChatAttachmentStager(CodeInterpreterFileManager codeInterpreterFileManager,
			CodeInterpreterSessionProvider sessionProvider) {
		this.codeInterpreterFileManager = codeInterpreterFileManager;
		this.sessionProvider = sessionProvider;
	}

	/**
	 * Stage the given attachments into the code interpreter session for the given Synapse chat session,
	 * returning one status per attachment in request order. A null or empty attachment list stages
	 * nothing and resolves no session, preserving the invariant that a purely conversational turn never
	 * provisions a code session. The session is reused across turns, so the {@link #MAX_AGENT_CHAT_ATTACHMENTS}
	 * limit is enforced cumulatively: a request is rejected with {@link IllegalArgumentException} — before
	 * any file is staged — when the files already in the session plus this turn's attachments would exceed
	 * the limit. Otherwise each attachment is staged best-effort: files that cannot be authorized, resolved,
	 * or staged are reported as {@link AgentChatAttachmentState#FAILED} with a cause and the turn proceeds
	 * with whatever staged successfully.
	 *
	 * @param user           The user on whose behalf the files are staged; used for download authorization
	 * @param agentSessionId The Synapse chat session id whose code interpreter session receives the files
	 * @param attachments    The files to stage; may be null or empty
	 * @return One {@link AgentChatAttachmentStatus} per attachment, in request order; empty when there
	 *         were no attachments
	 */
	public List<AgentChatAttachmentStatus> stageAttachments(UserInfo user, String agentSessionId,
			List<FileHandleAssociation> attachments) {
		ValidateArgument.required(user, "user");
		ValidateArgument.requiredNotBlank(agentSessionId, "agentSessionId");

		if (attachments == null || attachments.isEmpty()) {
			return Collections.emptyList();
		}
		// Fast fail before resolving a session when this turn alone already exceeds the limit; the
		// cumulative check below cannot pass in that case regardless of what the session holds.
		if (attachments.size() > MAX_AGENT_CHAT_ATTACHMENTS) {
			throw new IllegalArgumentException("A Curie chat session may hold at most " + MAX_AGENT_CHAT_ATTACHMENTS
					+ " attached files, but this message alone provided " + attachments.size() + ".");
		}

		String sessionId = sessionProvider.getOrCreateSession(agentSessionId);

		// The session is reused across turns, so files staged on earlier turns still count against the
		// limit. Count what the session already holds and reject the turn if adding these attachments
		// would push the cumulative total past the limit, before staging any of them.
		int existingCount = codeInterpreterFileManager.countFilesInSessionDirectory(sessionId,
				CodeInterpreterFileManager.ATTACHMENTS_DIRECTORY);
		if (existingCount + attachments.size() > MAX_AGENT_CHAT_ATTACHMENTS) {
			throw new IllegalArgumentException("This chat session already holds " + existingCount
					+ " attached files; attaching " + attachments.size()
					+ " more would exceed the maximum of " + MAX_AGENT_CHAT_ATTACHMENTS + " attached files per session.");
		}

		// Session paths are derived from each file's own name, so the requests carry a null path.
		List<PushFileRequest> pushRequests = new ArrayList<>(attachments.size());
		for (FileHandleAssociation association : attachments) {
			pushRequests.add(new PushFileRequest(association, null));
		}

		List<PushFileResult> pushResults = codeInterpreterFileManager.pushFileHandlesToSession(user, pushRequests,
				sessionId);

		List<AgentChatAttachmentStatus> statuses = new ArrayList<>(pushResults.size());
		for (int i = 0; i < pushResults.size(); i++) {
			statuses.add(toStatus(attachments.get(i), pushResults.get(i)));
		}
		return statuses;
	}

	/**
	 * Maps a single staging outcome onto the client-facing status, echoing the request's association. A
	 * staged file carries its resolved name, session path, content type, and size; a failed file carries
	 * the human-readable reason and the mapped failure code instead.
	 */
	private static AgentChatAttachmentStatus toStatus(FileHandleAssociation association, PushFileResult result) {
		AgentChatAttachmentStatus status = new AgentChatAttachmentStatus()
				.setFileHandleId(association.getFileHandleId())
				.setAssociateObjectId(association.getAssociateObjectId())
				.setAssociateObjectType(association.getAssociateObjectType());

		if (result.isError()) {
			return status.setStatus(AgentChatAttachmentState.FAILED)
					.setFailureMessage(result.error())
					.setFailureCode(toFailureCode(result.failureCode()));
		}
		return status.setStatus(AgentChatAttachmentState.STAGED)
				.setFileName(result.fileName())
				.setSessionPath(result.sessionPath())
				.setContentType(result.contentType())
				.setContentSizeBytes(result.contentSizeBytes());
	}

	/**
	 * Translates the internal staging failure cause into the client-facing failure code. Causes that a
	 * chat client cannot act on individually (a non-S3 handle or an in-session download error) collapse to
	 * {@link AgentChatAttachmentFailureCode#UNKNOWN_ERROR}.
	 */
	private static AgentChatAttachmentFailureCode toFailureCode(PushFailureCode failureCode) {
		if (failureCode == null) {
			return AgentChatAttachmentFailureCode.UNKNOWN_ERROR;
		}
		return switch (failureCode) {
			case NOT_FOUND -> AgentChatAttachmentFailureCode.NOT_FOUND;
			case UNAUTHORIZED -> AgentChatAttachmentFailureCode.UNAUTHORIZED;
			case UNSUPPORTED_TYPE -> AgentChatAttachmentFailureCode.UNSUPPORTED_TYPE;
			case EXCEEDS_SIZE_LIMIT -> AgentChatAttachmentFailureCode.EXCEEDS_SIZE_LIMIT;
			case NOT_S3, EXECUTION_ERROR -> AgentChatAttachmentFailureCode.UNKNOWN_ERROR;
		};
	}
}

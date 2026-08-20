package org.sagebionetworks.repo.manager.agent;

import org.sagebionetworks.util.ValidateArgument;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.model.CodeInterpreterSessionStatus;
import software.amazon.awssdk.services.bedrockagentcore.model.CodeInterpreterSessionSummary;
import software.amazon.awssdk.services.bedrockagentcore.model.ListCodeInterpreterSessionsRequest;
import software.amazon.awssdk.services.bedrockagentcore.model.ListCodeInterpreterSessionsResponse;

/**
 * Provisions a durable AWS Bedrock AgentCore code interpreter session for an interactive Curie chat
 * session, without persisting the AWS session id in our database. A deterministic, sanitized session
 * name is derived from the Synapse {@code AgentSession.sessionId}; the same name resolves to the same
 * session across workers and chat turns.
 * <p>
 * Reuse works by listing sessions filtered to {@code READY}: a timed-out session (AgentCore's own idle
 * timeout, default 900s) has left {@code READY}, so it is skipped and a fresh one is created — expiry
 * handling is automatic, with no reaper worker and no explicit terminate call.
 */
@Service
public class CodeInterpreterSessionProvider {

	static final String SESSION_NAME_PREFIX = "curie_";

	private final BedrockAgentCoreClient bedrockAgentCoreClient;
	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final String codeInterpreterIdentifier;

	public CodeInterpreterSessionProvider(BedrockAgentCoreClient bedrockAgentCoreClient,
			AgentCoreCodeInterpreterClient codeInterpreterClient,
			@Qualifier("codeInterpreterIdentifier") String codeInterpreterIdentifier) {
		this.bedrockAgentCoreClient = bedrockAgentCoreClient;
		this.codeInterpreterClient = codeInterpreterClient;
		this.codeInterpreterIdentifier = codeInterpreterIdentifier;
	}

	/**
	 * A per-turn, lazily-resolving, memoizing supplier of the code interpreter session for a Synapse
	 * chat session. The session is not resolved (and not created) until the supplier is first invoked —
	 * the first code activity of the turn — and the resolved id is cached for the remainder of the turn
	 * so a turn running several tools makes at most one {@code list} call.
	 */
	public CodeSessionSupplier lazySupplier(String agentSessionId) {
		ValidateArgument.requiredNotBlank(agentSessionId, "agentSessionId");
		return new CodeSessionSupplier() {

			private String resolvedSessionId;

			@Override
			public String getSessionId() {
				if (resolvedSessionId == null) {
					resolvedSessionId = getOrCreateSession(agentSessionId);
				}
				return resolvedSessionId;
			}

			@Override
			public String resolvedSessionIdOrNull() {
				return resolvedSessionId;
			}
		};
	}

	/**
	 * Return the id of a {@code READY} code interpreter session for the given Synapse chat session,
	 * creating one if none exists. Reuse is keyed by the deterministic name derived from
	 * {@code agentSessionId}, so the same session is shared across workers and turns.
	 */
	public String getOrCreateSession(String agentSessionId) {
		ValidateArgument.requiredNotBlank(agentSessionId, "agentSessionId");
		String name = codeSessionName(agentSessionId);
		String existing = findReadySessionByName(name);
		if (existing != null) {
			return existing;
		}
		return codeInterpreterClient.startSession(name);
	}

	/**
	 * Page through the {@code READY} sessions of this stack's code interpreter and return the id of the
	 * first whose name matches, or {@code null} if none match.
	 */
	String findReadySessionByName(String name) {
		String nextToken = null;
		do {
			ListCodeInterpreterSessionsResponse response = bedrockAgentCoreClient
					.listCodeInterpreterSessions(ListCodeInterpreterSessionsRequest.builder()
							.codeInterpreterIdentifier(codeInterpreterIdentifier)
							.status(CodeInterpreterSessionStatus.READY)
							.nextToken(nextToken)
							.build());
			for (CodeInterpreterSessionSummary summary : response.items()) {
				if (name.equals(summary.name())) {
					return summary.sessionId();
				}
			}
			nextToken = response.nextToken();
		} while (nextToken != null);
		return null;
	}

	/**
	 * Derive a deterministic, AgentCore-safe session name from the Synapse chat session id: a fixed
	 * {@code curie_} prefix followed by the id with any character outside {@code [A-Za-z0-9_-]} replaced
	 * by {@code _}. The prefix keeps interactive Curie sessions visually distinct from the batch
	 * sub-workers' sessions listed under the same code interpreter.
	 */
	String codeSessionName(String agentSessionId) {
		return SESSION_NAME_PREFIX + agentSessionId.replaceAll("[^A-Za-z0-9_-]", "_");
	}
}

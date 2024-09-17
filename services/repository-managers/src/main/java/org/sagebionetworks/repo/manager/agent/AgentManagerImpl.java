package org.sagebionetworks.repo.manager.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlHandlerProvider;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.sagebionetworks.repo.model.dbo.agent.AgentDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler.Visitor;
import software.amazon.awssdk.services.bedrockagentruntime.model.ReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.SessionState;

@Service
public class AgentManagerImpl implements AgentManager {

	public static final String TSTALIASID = "TSTALIASID";

	private final AgentDao agentDao;
	private final BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;
	private final String stackBedrockAgentId;
	private final ReturnControlHandlerProvider handlerProvider;
	private Logger logger;

	@Autowired
	public AgentManagerImpl(AgentDao agentDao, BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient,
			String stackBedrockAgentId, ReturnControlHandlerProvider handlerProvider) {
		super();
		this.agentDao = agentDao;
		this.bedrockAgentRuntimeAsyncClient = bedrockAgentRuntimeAsyncClient;
		this.stackBedrockAgentId = stackBedrockAgentId;
		this.handlerProvider = handlerProvider;
	}
	
	@Autowired
	public void setLoggerProvider(LoggerProvider provider) {
		this.logger = provider.getLogger(AgentManagerImpl.class.getName());
	}

	@WriteTransaction
	@Override
	public AgentSession createSession(UserInfo userInfo, CreateAgentSessionRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAgentAccessLevel(), "request.agentAccessLevel");
		// only authenticated users can start a chat session.
		AuthorizationUtils.disallowAnonymous(userInfo);

		if (request.getAgentId() != null && !request.getAgentId().isBlank()) {
			if (!AuthorizationUtils.isSageEmployeeOrAdmin(userInfo)) {
				throw new UnauthorizedException("Currently, only internal users can override the agentId.");
			}
		}
		String agentId = (request.getAgentId() == null || request.getAgentId().isBlank()) ? stackBedrockAgentId
				: request.getAgentId();
		return agentDao.createSession(userInfo.getId(), request.getAgentAccessLevel(), agentId);
	}

	@WriteTransaction
	@Override
	public AgentSession updateSession(UserInfo userInfo, UpdateAgentSessionRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		ValidateArgument.required(request.getAgentAccessLevel(), "request.agentAccessLevel");
		AgentSession s = getAndValidateAgentSession(userInfo, request.getSessionId());
		if (request.getAgentAccessLevel().equals(s.getAgentAccessLevel())) {
			return s;
		}
		return agentDao.updateSession(request.getSessionId(), request.getAgentAccessLevel());
	}

	@WriteTransaction
	@Override
	public AgentChatResponse invokeAgent(UserInfo userInfo, AgentChatRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		AgentSession session = getAndValidateAgentSession(userInfo, request.getSessionId());
		// do nothing with an empty of blank input.
		if (request.getChatText() == null || request.getChatText().isBlank()) {
			return new AgentChatResponse().setResponseText("").setSessionId(request.getSessionId());
		}
		String responseText = invokeAgentWithText(session, request.getChatText());
		return new AgentChatResponse().setResponseText(responseText).setSessionId(request.getSessionId());
	}

	/**
	 * Helper to get and validate the session for the provided sessionId.
	 * 
	 * @param userInfo
	 * @param sessionId
	 * @return
	 */
	AgentSession getAndValidateAgentSession(UserInfo userInfo, String sessionId) {
		AgentSession s = agentDao.getAgentSession(sessionId).orElseThrow(() -> {
			return new NotFoundException("Agent session does not exist");
		});
		if (!userInfo.getId().equals(s.getStartedBy())) {
			throw new UnauthorizedException("Only the user that started a session may access it");
		}
		return s;
	}

	@Override
	public AgentSession getSession(UserInfo userInfo, String sessionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(sessionId, "sessionId");
		return getAndValidateAgentSession(userInfo, sessionId);
	}

	/**
	 * Send the user's text directly to the agent via an invoke_agent call.
	 * 
	 * @param sessionId
	 * @param inputText
	 * @return
	 */
	String invokeAgentWithText(AgentSession session, String inputText) {
		InvokeAgentRequest startRequest = InvokeAgentRequest.builder().agentId(session.getAgentId())
				.agentAliasId(TSTALIASID).sessionId(session.getSessionId()).enableTrace(false).inputText(inputText)
				.build();
		AgentResponse res = invokeAgentAsync(session, startRequest);
		int count = 0;
		// When the invocation ID is not null, the agent has requested more information
		// with a return_control response.
		while (res.getInvocationId() != null) {
			if(count > 10) {
				throw new IllegalStateException("Max number of 10 return_control agent response exceeded.");
			}
			int thisCount = count;
			res.getReturnControlEvents().forEach(e -> {
				logger.info(
						"return_control sessionId: '{}', count: '{}', actionGroup: '{}', function: '{}', params: '{}'",
						session.getAgentId(), thisCount, e.getActionGroup(), e.getFunction(),
						e.getParameters().toString());
			});
			// Each time the agent responds with return_control we need to get the requested
			// data and send it with another invoke_agent call.
			List<InvocationResultMember> eventResults = executeEvents(session.getAgentAccessLevel(),
					res.getReturnControlEvents());
			InvokeAgentRequest returnRequest = InvokeAgentRequest.builder().agentId(session.getAgentId())
					.agentAliasId(TSTALIASID).sessionId(session.getSessionId()).sessionState(SessionState.builder()
							.invocationId(res.getInvocationId()).returnControlInvocationResults(eventResults).build())
					.enableTrace(false).build();
			res = invokeAgentAsync(session, returnRequest);
			count++;
		}
		return res.getBuilder().toString();
	}

	/**
	 * The main invoke_agent call.
	 * 
	 * @param session
	 * @param invokeAgentRequest
	 * @return
	 */
	AgentResponse invokeAgentAsync(AgentSession session, InvokeAgentRequest invokeAgentRequest) {
		try {
			// This object will capture the response data pushed to the handler.
			AgentResponse response = new AgentResponse();
			var responseStreamHandler = InvokeAgentResponseHandler.builder()
					.subscriber(Visitor.builder().onReturnControl(payload -> {
						/*
						 * The agent has requested more information by providing a return_control
						 * response..
						 */
						Long runAsUser = getRunAsUser(session);
						List<ReturnControlEvent> events = extractEvents(runAsUser, payload);
						response.setReturnControl(payload.invocationId(), events);
					}).onChunk(chunk -> {
						// The agent will return results in chunks that must be concatenated.
						response.appendText(chunk.bytes().asUtf8String());
					}).build()).onResponse(resp -> {
						logger.info("onResponse() sessionId: '{}'", session.getSessionId());
					}).onError(t -> {
						logger.error("onError() sessionId: '{}' errorMessage:'{}'", session.getSessionId(),
								t.getMessage());
					}).build();

			CompletableFuture<Void> future = bedrockAgentRuntimeAsyncClient.invokeAgent(invokeAgentRequest,
					responseStreamHandler);
			future.get();
			return response;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the ID of the user that should be used for return_control event handlers
	 * based on the session's access level.
	 * 
	 * @param session
	 * @return
	 */
	Long getRunAsUser(AgentSession session) {
		switch (session.getAgentAccessLevel()) {
		case PUBLICLY_ACCESSIBLE:
			return AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		case READ_YOUR_PRIVATE_DATA:
		case WRITE_YOUR_PRIVATE_DATA:
			return session.getStartedBy();
		default:
			throw new IllegalStateException("Unknown agentAccessLevel:" + session.getAgentAccessLevel());
		}
	}

	/**
	 * Execute each of the passed events and return each result in a list.
	 * 
	 * @param events
	 * @return
	 * @throws Exception
	 */
	List<InvocationResultMember> executeEvents(AgentAccessLevel accessLevel, List<ReturnControlEvent> events) {
		List<InvocationResultMember> results = new ArrayList<>();
		for (ReturnControlEvent e : events) {
			ReturnControlHandler handler = handlerProvider.getHandler(e.getActionGroup(), e.getFunction())
					.orElseThrow(() -> new UnsupportedOperationException(
							String.format("No handler for actionGroup: '%s' and function: '%s'", e.getActionGroup(),
									e.getFunction())));
			String responseBody = handleEvent(accessLevel, handler, e);
			results.add(InvocationResultMember.builder()
					.functionResult(FunctionResult.builder().actionGroup(e.getActionGroup()).function(e.getFunction())
							.responseBody(Map.of("TEXT", ContentBody.builder().body(responseBody).build())).build())
					.build());
		}
		return results;
	}

	/**
	 * Handle the provided event bases on provided access level. When write access
	 * is needed but not provided, the resulting message
	 * 
	 * @param accessLevel
	 * @param handler
	 * @param event
	 * @return
	 * @throws Exception
	 */
	String handleEvent(AgentAccessLevel accessLevel, ReturnControlHandler handler, ReturnControlEvent event) {
		try {
			if (handler.needsWriteAccess() && !AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA.equals(accessLevel)) {
				throw new UnauthorizedException(String.format(
						"Calling actionGroup: '%s' function: '%s' requires an access level of '%s'. The current session has an access level of '%s'. Please inform the user that they will need to need to change the access level of this session to be '%s' before this function may be called.",
						event.getActionGroup(), event.getFunction(), AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA,
						accessLevel, AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));

			} else {
				return handler.handleEvent(event);
			}
		} catch (Exception e) {
			logger.error(
					"Return_control event execution failed. Will send the following message to the agent: '{}'",
					e.getMessage());
			// on failure provide the error message to the agent in JSON.
			JSONObject error = new JSONObject();
			error.put("errorMessage", e.getMessage());
			return error.toString();
		}
	}

	/**
	 * Helper to extract the events from the payload.
	 * 
	 * @param payload
	 * @return
	 */
	List<ReturnControlEvent> extractEvents(Long userId, ReturnControlPayload payload) {
		List<ReturnControlEvent> events = new ArrayList<>();
		payload.invocationInputs().forEach(iim -> {
			var input = iim.functionInvocationInput();
			List<Parameter> params = new ArrayList<>();
			input.parameters().forEach(p -> {

				params.add(new Parameter(p.name(), p.type(), p.value()));
			});
			events.add(new ReturnControlEvent(userId, input.actionGroup(), input.function(), params));
		});
		return events;
	}

	public static class AgentResponse {

		private final StringBuilder builder;
		private List<ReturnControlEvent> returnControlEvents;
		private String invocationId;

		public AgentResponse() {
			builder = new StringBuilder();
			returnControlEvents = new ArrayList<>(2);
		}

		/**
		 * Called for a normal response.
		 * @param text
		 */
		AgentResponse appendText(String text) {
			builder.append(text);
			return this;
		}
		
		/**
		 * Called for a return_control response.
		 * @param invocationId
		 * @param returnControlEvents
		 */
		AgentResponse setReturnControl(String invocationId, List<ReturnControlEvent> returnControlEvents) {
			this.invocationId = invocationId;
			this.returnControlEvents = returnControlEvents;
			return this;
		}

		public StringBuilder getBuilder() {
			return builder;
		}

		public List<ReturnControlEvent> getReturnControlEvents() {
			return returnControlEvents;
		}

		public String getInvocationId() {
			return invocationId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(builder.toString(), invocationId, returnControlEvents);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AgentResponse other = (AgentResponse) obj;
			return Objects.equals(builder.toString(), other.builder.toString()) && Objects.equals(invocationId, other.invocationId)
					&& Objects.equals(returnControlEvents, other.returnControlEvents);
		}

		@Override
		public String toString() {
			return "AgentResponse [builder=" + builder + ", returnControlEvents=" + returnControlEvents
					+ ", invocationId=" + invocationId + "]";
		}

	}

}

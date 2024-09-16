package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.service.AsynchronousJobServices;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The Synapse chat services allow the user to chat with a 'bot' (agent) that has
 * access to Synapse data. By default, Synapse provides a 'baseline' agent that
 * has basic access to all of the supported Synapse action group functions and
 * knowledge bases. Sage authorized collaborators can create their own agents
 * that can also utilize the same action groups and knowledge bases used by the
 * baseline agent. The collaborator's agent can then be used to override the
 * baseline agent for chat sessions.
 * </p>
 * To get started, the first step is to create a new session by calling:
 * <a href="${POST.agent.session}">POST /agent/session</a>. You will need the
 * sessionId from the resulting <a href=
 * "${org.sagebionetworks.repo.model.agent.AgentSession}">AgentSession</a>. The
 * sessionId uniquely identifies a single conversation between the user and the
 * agent. Specifically, the session contains user's chat requests, agent
 * responses and all of the data gathered by the agent to meet user requests.
 * The session data is the context provided to the Large Language Model (LLM)
 * used by the agent. Only the user that create a session will have access to
 * its data and be able to use it for an agent conversation.
 * </p>
 * The user's conversation with an agent is a series of asynchronous jobs under
 * the provided sessionId, where each job is started by providing the user's
 * prompt to the agent. The job's response will then include the agent's
 * response to the user's prompt. Use:
 * <a href="POST.agent.chat.async.start">POST /agent/chat/async/start<a/> to
 * start a chat job and <a href="GET.agent.chat.async.get.asyncToken">GET
 * /agent/chat/async/get/{asyncToken}</a> to get the job's results.
 * 
 */
@Controller
@ControllerInfo(displayName = "Agent Chat Services", path = "repo/v1")
@RequestMapping(UrlHelpers.REPO_PATH)
public class AgentController {

	@Autowired
	private AgentService agentService;
	@Autowired
	private AsynchronousJobServices asynchronousJobServices;

	/**
	 * Start a new chat session. Each chat session request must include an <a href=
	 * "org.sagebionetworks.repo.model.agent.AgentAccessLevel">AgentAccessLevel</a>
	 * that defines what level of data access the agent will given during this
	 * session.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.AGENT_SESSION }, method = RequestMethod.POST)
	public @ResponseBody AgentSession createSession(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateAgentSessionRequest request) {
		return agentService.createSession(userId, request);
	}

	/**
	 * Get the agent session using its session id.
	 * 
	 * @param userId
	 * @param sessionId
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.AGENT_SESSION_ID }, method = RequestMethod.GET)
	public @ResponseBody AgentSession getAgentSession(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String sessionId) {
		return agentService.getSession(userId, sessionId);
	}
	/**
	 * Update the access level of an existing agent session.
	 * </p>
	 * Only the user that started the session can change its access level.
	 * 
	 * @param userId
	 * @param sessionId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.AGENT_SESSION_ID }, method = RequestMethod.PUT)
	public @ResponseBody AgentSession updateSession(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String sessionId,
			@RequestBody UpdateAgentSessionRequest request) {
		ValidateArgument.required(request, "request");
		request.setSessionId(sessionId);
		return agentService.updateSession(userId, request);
	}

	/**
	 * Start an asynchronous job to exchange a single prompt from the user in the
	 * request. The agent's response to this prompt can be retrieved by calling:
	 * <a href="GET.agent.chat.async.get.asyncToken">GET
	 * /agent/chat/async/get/{asyncToken}</a>.
	 * </p>
	 * The request must include the sessionId that uniquely identifies a single
	 * conversation between the user and the agent (see:
	 * <a href="${POST.agent.session}">POST /agent/session</a>).
	 * </p>
	 * Only the user that started a session may use its sessionId.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AGENT_CHAT_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId chatStart(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AgentChatRequest request) {
		AsynchronousJobStatus job = asynchronousJobServices.startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the agent's response to a user's prompt started by calling:
	 * <a href="POST.agent.chat.async.start">POST /agent/chat/async/start<a/>
	 * </p>
	 * Only the user that started the job may get the job's results.
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.AGENT_CHAT_GET, method = RequestMethod.GET)
	public @ResponseBody AgentChatResponse chatGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken)
			throws Throwable {
		AsynchronousJobStatus jobStatus = asynchronousJobServices.getJobStatusAndThrow(userId,
				asyncToken);
		return (AgentChatResponse) jobStatus.getResponseBody();
	}
}

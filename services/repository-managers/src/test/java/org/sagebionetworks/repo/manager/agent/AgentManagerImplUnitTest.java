package org.sagebionetworks.repo.manager.agent;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.core.Logger;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscription;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.AgentManagerImpl.AgentResponse;
import org.sagebionetworks.repo.manager.agent.context.AgentContextValidator;
import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.OpenApiReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlHandlerProvider;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.manager.agent.supervisor.CurieSupervisorFactory;
import org.sagebionetworks.repo.manager.agent.tool.AgentTraceCallback;
import org.sagebionetworks.repo.manager.config.AgentSuffix;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentFailureCode;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentState;
import org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationRequest;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.AgentType;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.agent.SessionContext;
import org.sagebionetworks.repo.model.agent.TraceEvent;
import org.sagebionetworks.repo.model.agent.TraceEventsRequest;
import org.sagebionetworks.repo.model.agent.TraceEventsResponse;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.agent.AgentDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.springframework.ai.chat.model.ToolContext;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.ActionGroupInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.ApiInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.ApiParameter;
import software.amazon.awssdk.services.bedrockagentruntime.model.ApiRequestBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.ApiResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInputMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.OrchestrationModelInvocationOutput;
import software.amazon.awssdk.services.bedrockagentruntime.model.OrchestrationTrace;
import software.amazon.awssdk.services.bedrockagentruntime.model.PropertyParameters;
import software.amazon.awssdk.services.bedrockagentruntime.model.RawResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.ReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.SessionState;
import software.amazon.awssdk.services.bedrockagentruntime.model.Trace;
import software.amazon.awssdk.services.bedrockagentruntime.model.TracePart;
import software.amazon.awssdk.services.bedrockagentruntime.model.responsestream.DefaultChunk;
import software.amazon.awssdk.services.bedrockagentruntime.model.responsestream.DefaultReturnControl;
import software.amazon.awssdk.services.bedrockagentruntime.model.responsestream.DefaultTrace;

@ExtendWith(MockitoExtension.class)
public class AgentManagerImplUnitTest {

	@Mock
	private AgentDao mockAgentDao;

	@Mock
	private BedrockAgentRuntimeAsyncClient mockAgentRuntime;

	@Mock
	private AgentClientProvider mockAgentClientProvider;

	@Mock
	private ReturnControlHandlerProvider mockReturnControlHandlerProvider;

	@Mock
	private CompletableFuture<Void> mockCompletableFuture;

	@Mock
	private Subscription mockSubscription;

	@Mock
	private LoggerProvider mockLoggerProvider;

	@Mock
	private Logger mockLogger;

	@Mock
	private ReturnControlHandler mockReturnControlHandlerOne;

	@Mock
	private ReturnControlHandler mockReturnControlHandlerTwo;

	@Mock
	private OpenApiReturnControlHandler mockOpenApiReturnControlHandler;

	@Mock
	private Clock mockClock;

	@Mock
	private AsynchronousJobStatusDAO mockStatusDao;

	@Mock
	private FeatureManager mockFeatureManager;
	
	@Mock
	private AgentContextValidator mockContextValidator;

	@Mock
	private Consumer mockCloudwatchConsumer;
	
	@Mock
	private UserManager mockUserManager;

	@Mock
	private CurieSupervisorFactory mockCurieSupervisorFactory;

	@Mock
	private Agent mockCurieSupervisor;

	@Mock
	private AgentChatAttachmentStager mockAttachmentStager;

	private AgentManagerImpl manager;

	private String stackBedrockAgentId;
	private String stackBedrockGridAgentId;
	private Long adminId;
	private Long sageTeamId;
	private Long anonymousUserId;

	private UserInfo sageUser;
	private UserInfo admin;
	private UserInfo anonymous;
	private UserInfo nonSageNonAdmin;

	private AgentRegistration agentRegistration;
	private AgentRegistrationRequest agentRegistrationRequest;
	private CreateAgentSessionRequest createRequest;
	private AgentSession session;
	private String sessionId;
	private String inputText;

	private String actionGroup;
	private String functionOne;
	private String functionTwo;

	private String apiFunction;
	private String apiPath;
	private HttpMethod apiHttpMethod;
	private HttpCode apiHttpCode;

	private Parameter parameter;
	private ReturnControlEvent returnControlEventOne;
	private ReturnControlEvent returnControlEventTwo;
	private ReturnControlEvent returnControlEventApi;
	private JSONObject requestBody;
	private List<ReturnControlEvent> returnControlEvents;
	private String invocationId;

	private String returnControlResponseBody;
	private InvocationResultMember invocationResultMember;
	private List<InvocationResultMember> invocationResultMembers;

	private InvokeAgentRequest invokeAgentRequest;
	private InvokeAgentRequest invokeAgentReturnRequest;

	private InvokeAgentRequest invokeAgentRequestWithTrace;
	private InvokeAgentRequest invokeAgentReturnRequestWithTrace;

	private UpdateAgentSessionRequest updateRequest;

	private AgentChatRequest chatRequest;
	private String jobId;

	private TracePart traceInput;
	private TracePart traceOutput;
	private String traceOutText;
	private TraceEventsRequest traceRequest;
	private SessionContext sessionContext;

	@BeforeEach
	public void before() {
		stackBedrockAgentId = "stackAgentId";
		stackBedrockGridAgentId = "stackGridAgentId";

		Map<AgentSuffix, String> idMap = Map.of(AgentSuffix.basic, stackBedrockAgentId, AgentSuffix.grid,
				stackBedrockGridAgentId);

		manager = Mockito.spy(new AgentManagerImpl(mockAgentDao, mockAgentClientProvider, idMap,
				mockReturnControlHandlerProvider, mockClock, mockStatusDao, mockFeatureManager, mockContextValidator, mockCloudwatchConsumer, mockUserManager, mockCurieSupervisorFactory, mockAttachmentStager));

		when(mockLoggerProvider.getLogger(AgentManagerImpl.class.getName())).thenReturn(mockLogger);
		manager.setLoggerProvider(mockLoggerProvider);

		adminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		sageTeamId = BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId();
		anonymousUserId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();

		boolean isAdmin = false;
		sageUser = new UserInfo(isAdmin, 444L, AuthorizationConstants.DEFAULT_REALM_ID, Set.of(sageTeamId));

		anonymous = new UserInfo(false, anonymousUserId, AuthorizationConstants.DEFAULT_REALM_ID);
		anonymous.setRealmAnonymousUserId(anonymousUserId);

		admin = new UserInfo(true, adminId, AuthorizationConstants.DEFAULT_REALM_ID);

		nonSageNonAdmin = new UserInfo(false, 555L, AuthorizationConstants.DEFAULT_REALM_ID);

		invocationId = "someInvocationId";

		agentRegistrationRequest = new AgentRegistrationRequest().setAwsAgentId(stackBedrockAgentId)
				.setAwsAliasId(AgentManagerImpl.TSTALIASID);

		agentRegistration = new AgentRegistration().setAgentRegistrationId("reg111").setAwsAgentId("awsAgentId222")
				.setAwsAliasId("awaAgentAlias222");
		sessionId = "sessionId111";
		createRequest = new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE)
				.setAgentRegistrationId(agentRegistration.getAgentRegistrationId());

		session = new AgentSession().setSessionId(sessionId).setStartedBy(nonSageNonAdmin.getId())
				.setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE);

		updateRequest = new UpdateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA)
				.setSessionId(sessionId);

		inputText = "what is the meaning of life?";
		chatRequest = new AgentChatRequest().setChatText(inputText).setSessionId(sessionId);

		actionGroup = "someActionGroup";
		functionOne = "functionOne";
		parameter = new Parameter("paramOne", "string", "valueOne");
		returnControlEventOne = new ReturnControlEvent(session.getStartedBy(), actionGroup, functionOne,
				List.of(parameter));

		actionGroup = "someActionGroup";
		functionTwo = "functionTwo";
		returnControlEventTwo = new ReturnControlEvent(session.getStartedBy(), actionGroup, functionTwo,
				List.of(new Parameter("paramTwo", "string", "valueTwo")));

		returnControlEvents = List.of(returnControlEventOne, returnControlEventTwo);

		// Open API function
		apiFunction = "PUT /foo/{id}";
		apiPath = "/foo/{id}";
		apiHttpMethod = HttpMethod.put;
		apiHttpCode = HttpCode.created;

		requestBody = new JSONObject();
		requestBody.put("someKey", "someValue");
		List<Parameter> requestBodyParams = List.of(new Parameter("someKey", "string", "someValue"));
		sessionContext = new GridAgentSessionContext().setGridSessionId("98765");
		returnControlEventApi = new ReturnControlEvent(session.getStartedBy(), actionGroup, apiFunction,
				List.of(new Parameter("id", "string", "987")), requestBodyParams, null);

		JSONObject response = new JSONObject();
		response.put("someKey", "someValue");
		returnControlResponseBody = response.toString();

		invocationResultMember = InvocationResultMember.builder()
				.functionResult(FunctionResult.builder().actionGroup(actionGroup).function(functionOne)
						.responseBody(Map.of("TEXT", ContentBody.builder().body(returnControlResponseBody).build()))
						.build())
				.build();
		invocationResultMembers = List.of(invocationResultMember);

		var builder = InvokeAgentRequest.builder().agentId(agentRegistration.getAwsAgentId())
				.agentAliasId(agentRegistration.getAwsAliasId()).sessionId(session.getSessionId()).enableTrace(false)
				.inputText(inputText).sessionState(sessionState -> sessionState.promptSessionAttributes(
						Map.of("access_level", AgentAccessLevel.PUBLICLY_ACCESSIBLE.toString())));

		invokeAgentRequest = builder.build();

		builder.enableTrace(true);
		invokeAgentRequestWithTrace = builder.build();

		var returnBuilder = InvokeAgentRequest.builder().agentId(agentRegistration.getAwsAgentId())
				.agentAliasId(agentRegistration.getAwsAliasId()).sessionId(session.getSessionId())
				.sessionState(SessionState.builder().invocationId(invocationId)
						.returnControlInvocationResults(invocationResultMembers)
						.promptSessionAttributes(
								Map.of("access_level", AgentAccessLevel.PUBLICLY_ACCESSIBLE.toString()))
						.build())
				.enableTrace(false);

		invokeAgentReturnRequest = returnBuilder.build();
		returnBuilder.enableTrace(true);
		invokeAgentReturnRequestWithTrace = returnBuilder.build();

		jobId = "987321";

		traceInput = DefaultTrace.builder().sessionId(sessionId)
				.trace(Trace.builder().orchestrationTrace(OrchestrationTrace.builder()
						.invocationInput(InvocationInput.builder()
								.actionGroupInvocationInput(ActionGroupInvocationInput.builder()
										.actionGroupName(actionGroup).function(functionOne).build())
								.build())
						.build()).build())
				.build();
		traceOutText = "thinking about stuff";
		traceOutput = DefaultTrace.builder().sessionId(sessionId)
				.trace(Trace.builder().orchestrationTrace(OrchestrationTrace.builder()
						.modelInvocationOutput(OrchestrationModelInvocationOutput.builder()
								.rawResponse(RawResponse.builder().content(traceOutText).build()).build())
						.build()).build())
				.build();

		traceRequest = new TraceEventsRequest().setJobId(jobId).setNewerThanTimestamp(123L);
	}

	@Test
	public void testCreateSessionWithUnknownRegistrationId() {
		createRequest.setAgentRegistrationId("unknown");
		when(mockAgentDao.getRegeistration("unknown")).thenReturn(Optional.empty());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createSession(sageUser, createRequest);
		}).getMessage();
		assertEquals("AgentRegistrationId='unknown' does not exist", message);
	}

	@Test
	public void testCreateSessionWithSageUser() {
		when(mockAgentDao.getRegeistration(agentRegistration.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		when(mockAgentDao.createSession(sageUser.getId(), createRequest.getAgentAccessLevel(),
				createRequest.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(sageUser, createRequest);
		assertEquals(session, result);
	}

	@Test
	public void testCreateSessionWithAdmin() {
		when(mockAgentDao.getRegeistration(agentRegistration.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		when(mockAgentDao.createSession(admin.getId(), createRequest.getAgentAccessLevel(),
				createRequest.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(admin, createRequest);
		assertEquals(session, result);
		verifyNoMoreInteractions(mockContextValidator);
	}

	@Test
	public void testCreateSessionWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createSession(anonymous, createRequest);
		}).getMessage();
		assertEquals("Must login to perform this action", message);
		verifyNoMoreInteractions(mockAgentDao);
		verifyNoMoreInteractions(mockContextValidator);
	}

	@Test
	public void testCreateSessionWithNonSageNonAdminWithAgentId() {
		when(mockAgentDao.getRegeistration(agentRegistration.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));

		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				agentRegistration.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(nonSageNonAdmin, createRequest);
		assertEquals(session, result);
		verifyNoMoreInteractions(mockContextValidator);
	}

	@Test
	public void testCreateSessionWithNonSageNonAdminWithNullRegistrationId() {
		when(mockAgentDao.createOrGetRegistration(AgentType.BASELINE, agentRegistrationRequest))
				.thenReturn(agentRegistration);
		createRequest.setAgentRegistrationId(null);
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				agentRegistration.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(nonSageNonAdmin, createRequest);
		assertEquals(session, result);
		verifyNoMoreInteractions(mockContextValidator);
	}

	@Test
	public void testCreateSessionWithNonSageNonAdminWithBlankRegistrationId() {
		when(mockAgentDao.createOrGetRegistration(AgentType.BASELINE, agentRegistrationRequest))
				.thenReturn(agentRegistration);
		createRequest.setAgentRegistrationId("");
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				agentRegistration.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(nonSageNonAdmin, createRequest);
		assertEquals(session, result);
		verifyNoMoreInteractions(mockContextValidator);
	}

	@Test
	public void testCreateSessionWithGridContextAndNullRegistrationId() {
		createRequest.setSessionContext(new GridAgentSessionContext().setGridSessionId("grid123"));
		createRequest.setAgentRegistrationId(null);
		agentRegistration.setAwsAgentId(stackBedrockGridAgentId);
		when(mockAgentDao.createOrGetRegistration(AgentType.BASELINE, new AgentRegistrationRequest()
				.setAwsAgentId(stackBedrockGridAgentId).setAwsAliasId(AgentManagerImpl.TSTALIASID)))
				.thenReturn(agentRegistration);
		when(mockContextValidator.validate(nonSageNonAdmin, createRequest.getSessionContext())).thenReturn(createRequest.getSessionContext());
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				agentRegistration.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);
		
		// call under test
		AgentSession result = manager.createSession(nonSageNonAdmin, createRequest);
		assertEquals(session, result);
		verify(mockContextValidator).validate(nonSageNonAdmin, createRequest.getSessionContext());
	}
	
	@Test
	public void testCreateSessionWithGridContextAndRegistrationId() {
		createRequest.setSessionContext(new GridAgentSessionContext().setGridSessionId("grid123"));
		agentRegistration.setAwsAgentId(stackBedrockGridAgentId);
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				agentRegistration.getAgentRegistrationId(), createRequest.getSessionContext())).thenReturn(session);
		when(mockAgentDao.getRegeistration(agentRegistration.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		when(mockContextValidator.validate(nonSageNonAdmin, createRequest.getSessionContext())).thenReturn(createRequest.getSessionContext());
		// call under test
		AgentSession result = manager.createSession(nonSageNonAdmin, createRequest);
		assertEquals(session, result);
	}

	@Test
	public void testCreateSessionWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createSession(null, createRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
		verifyNoMoreInteractions(mockAgentDao);
	}

	@Test
	public void testCreateSessionWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createSession(sageUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyNoMoreInteractions(mockAgentDao);
	}

	@Test
	public void testCreateSessionWithNullAccessLevel() {
		createRequest.setAgentAccessLevel(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createSession(sageUser, createRequest);
		}).getMessage();
		assertEquals("request.agentAccessLevel is required.", message);
		verifyNoMoreInteractions(mockAgentDao);
	}

	@Test
	public void testGetAndValidateAgentSession() {
		when(mockAgentDao.getAgentSession(sessionId)).thenReturn(Optional.of(session));

		// call under test
		AgentSession result = manager.getAndValidateAgentSession(nonSageNonAdmin, sessionId);
		assertEquals(session, result);

	}

	@Test
	public void testGetAndValidateSessionWithEmptySession() {

		when(mockAgentDao.getAgentSession(sessionId)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.getAndValidateAgentSession(nonSageNonAdmin, sessionId);
		}).getMessage();
		assertEquals("Agent session does not exist", message);
	}

	@Test
	public void testGetAndValidateSessionWithOtherStarter() {

		when(mockAgentDao.getAgentSession(sessionId)).thenReturn(Optional.of(session));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getAndValidateAgentSession(sageUser, sessionId);
		}).getMessage();
		assertEquals("Only the user that started a session may access it", message);
	}

	@Test
	public void testUpdateSession() {
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		when(mockAgentDao.updateSession(updateRequest.getSessionId(), updateRequest.getAgentAccessLevel()))
				.thenReturn(session);

		// call under test
		AgentSession result = manager.updateSession(sageUser, updateRequest);
		assertEquals(result, session);
	}

	@Test
	public void testUpdateSessionWithNoChange() {
		updateRequest.setAgentAccessLevel(AgentAccessLevel.valueOf(session.getAgentAccessLevel().name()));
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		// call under test
		AgentSession result = manager.updateSession(sageUser, updateRequest);
		assertEquals(result, session);
		verifyNoMoreInteractions(mockAgentDao);
	}

	@Test
	public void testUpdateSessionWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateSession(null, updateRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testUpdateSessionWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateSession(sageUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testUpdateSessionWithNullSessionId() {
		updateRequest.setSessionId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateSession(sageUser, updateRequest);
		}).getMessage();
		assertEquals("request.sessionId is required.", message);
	}

	@Test
	public void testUpdateSessionWithNullAccessLevel() {
		updateRequest.setAgentAccessLevel(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateSession(sageUser, updateRequest);
		}).getMessage();
		assertEquals("request.agentAccessLevel is required.", message);
	}

	@Test
	public void testInvokeAgent() {
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		String responseText = "hi";
		doReturn(responseText).when(manager).invokeAgentWithText(jobId, session, chatRequest);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		AgentChatResponse expected = new AgentChatResponse().setSessionId(sessionId).setResponseText(responseText);
		assertEquals(response, expected);

	}

	@Test
	public void testInvokeAgentWithExperimentalGridContext() {
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid123")
				.setExperimental(true);
		session.setSessionContext(gridContext);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		when(mockAttachmentStager.stageAttachments(eq(sageUser), eq(sessionId), any())).thenReturn(List.of());
		when(mockCurieSupervisorFactory.create()).thenReturn(mockCurieSupervisor);
		ArgumentCaptor<ToolContext> contextCaptor = ArgumentCaptor.forClass(ToolContext.class);
		when(mockCurieSupervisor.chat(eq(inputText), contextCaptor.capture())).thenReturn("curie-answer");

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		// A turn without attachments carries no attachmentStatuses in the response.
		assertEquals(new AgentChatResponse().setSessionId(sessionId).setResponseText("curie-answer"), response);
		verify(manager, never()).invokeAgentWithText(any(), any(), any());
		// The supervisor receives a tool context carrying the acting user, chat session, grid context, and
		// (here empty) staged attachments.
		ToolContext toolContext = contextCaptor.getValue();
		assertEquals(sageUser, AgentToolContextKey.USER_INFO.get(toolContext));
		assertEquals(sessionId, AgentToolContextKey.CHAT_SESSION_ID.get(toolContext));
		assertEquals(gridContext, AgentToolContextKey.GRID_SESSION_CONTEXT.get(toolContext));
		assertEquals(List.of(), AgentToolContextKey.STAGED_ATTACHMENTS.get(toolContext));
	}

	@Test
	public void testInvokeAgentWithExperimentalGridContextAndTraceEnabled() {
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid123")
				.setExperimental(true);
		session.setSessionContext(gridContext);
		chatRequest.setEnableTrace(true);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		when(mockAttachmentStager.stageAttachments(eq(sageUser), eq(sessionId), any())).thenReturn(List.of());
		when(mockCurieSupervisorFactory.create()).thenReturn(mockCurieSupervisor);
		ArgumentCaptor<ToolContext> contextCaptor = ArgumentCaptor.forClass(ToolContext.class);
		when(mockCurieSupervisor.chat(eq(inputText), contextCaptor.capture())).thenReturn("curie-answer");
		when(mockClock.currentTimeMillis()).thenReturn(123L);

		// call under test
		manager.invokeAgent(sageUser, jobId, chatRequest);

		// With trace enabled, the tool context carries a callback that writes trace against this job using
		// the manager's clock.
		AgentTraceCallback callback = (AgentTraceCallback) AgentToolContextKey.TRACE_CALLBACK
				.get(contextCaptor.getValue());
		assertNotNull(callback);
		callback.addTraceToJob("a message");
		verify(mockAgentDao).addTraceToJob(jobId, 123L, "a message");
	}

	@Test
	public void testInvokeAgentWithExperimentalGridContextAndTraceDisabled() {
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid123")
				.setExperimental(true);
		session.setSessionContext(gridContext);
		chatRequest.setEnableTrace(false);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		when(mockAttachmentStager.stageAttachments(eq(sageUser), eq(sessionId), any())).thenReturn(List.of());
		when(mockCurieSupervisorFactory.create()).thenReturn(mockCurieSupervisor);
		ArgumentCaptor<ToolContext> contextCaptor = ArgumentCaptor.forClass(ToolContext.class);
		when(mockCurieSupervisor.chat(eq(inputText), contextCaptor.capture())).thenReturn("curie-answer");

		// call under test
		manager.invokeAgent(sageUser, jobId, chatRequest);

		// With trace disabled no callback is placed in the tool context, so nothing is recorded.
		assertNull(AgentToolContextKey.TRACE_CALLBACK.get(contextCaptor.getValue()));
	}

	@Test
	public void testInvokeAgentWithExperimentalGridContextAndAttachments() {
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid123")
				.setExperimental(true);
		session.setSessionContext(gridContext);
		FileHandleAssociation stagedAssociation = new FileHandleAssociation().setFileHandleId("1")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn1");
		FileHandleAssociation failedAssociation = new FileHandleAssociation().setFileHandleId("2")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn2");
		List<FileHandleAssociation> attachments = List.of(stagedAssociation, failedAssociation);
		chatRequest.setAttachments(attachments);

		AgentChatAttachmentStatus stagedStatus = new AgentChatAttachmentStatus().setFileHandleId("1")
				.setAssociateObjectId("syn1").setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setStatus(AgentChatAttachmentState.STAGED).setFileName("data.csv")
				.setSessionPath("attachments/data.csv").setContentType("text/csv").setContentSizeBytes(4096L);
		AgentChatAttachmentStatus failedStatus = new AgentChatAttachmentStatus().setFileHandleId("2")
				.setAssociateObjectId("syn2").setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setStatus(AgentChatAttachmentState.FAILED).setFailureMessage("too large")
				.setFailureCode(AgentChatAttachmentFailureCode.EXCEEDS_SIZE_LIMIT);
		List<AgentChatAttachmentStatus> statuses = List.of(stagedStatus, failedStatus);

		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		when(mockAttachmentStager.stageAttachments(sageUser, sessionId, attachments)).thenReturn(statuses);
		when(mockCurieSupervisorFactory.create()).thenReturn(mockCurieSupervisor);
		ArgumentCaptor<ToolContext> contextCaptor = ArgumentCaptor.forClass(ToolContext.class);
		when(mockCurieSupervisor.chat(eq(inputText), contextCaptor.capture())).thenReturn("curie-answer");

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		// The full per-attachment status list, in request order, is returned to the client.
		assertEquals(new AgentChatResponse().setSessionId(sessionId).setResponseText("curie-answer")
				.setAttachmentStatuses(statuses), response);
		verify(mockAttachmentStager).stageAttachments(sageUser, sessionId, attachments);
		verify(manager, never()).invokeAgentWithText(any(), any(), any());
		// Only the successfully staged files are forwarded to the supervisor; failures reach the client only.
		assertEquals(List.of(stagedStatus),
				AgentToolContextKey.STAGED_ATTACHMENTS.get(contextCaptor.getValue()));
	}

	@Test
	public void testInvokeAgentWithAttachmentsOnNonCurieSession() {
		// A default (non-grid) session cannot accept attachments; the request is rejected outright.
		FileHandleAssociation association = new FileHandleAssociation().setFileHandleId("1")
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId("syn1");
		chatRequest.setAttachments(List.of(association));
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(sageUser, jobId, chatRequest);
		}).getMessage();

		assertEquals("Attachments are only supported for experimental grid (Curie) chat sessions.", message);
		// Rejected before any staging or model invocation.
		verifyNoInteractions(mockAttachmentStager);
		verifyNoInteractions(mockCurieSupervisorFactory);
		verify(manager, never()).invokeAgentWithText(any(), any(), any());
	}

	@Test
	public void testInvokeAgentWithNonExperimentalGridContext() {
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid123")
				.setExperimental(false);
		session.setSessionContext(gridContext);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		String responseText = "hi";
		doReturn(responseText).when(manager).invokeAgentWithText(jobId, session, chatRequest);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		assertEquals(new AgentChatResponse().setSessionId(sessionId).setResponseText(responseText), response);
		verifyNoMoreInteractions(mockCurieSupervisorFactory);
	}

	@Test
	public void testInvokeAgentWithGridContextAndNullExperimental() {
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid123");
		session.setSessionContext(gridContext);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);
		String responseText = "hi";
		doReturn(responseText).when(manager).invokeAgentWithText(jobId, session, chatRequest);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		assertEquals(new AgentChatResponse().setSessionId(sessionId).setResponseText(responseText), response);
		verifyNoMoreInteractions(mockCurieSupervisorFactory);
	}

	@Test
	public void testInvokeAgentWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(null, jobId, chatRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullJobId() {
		jobId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(sageUser, jobId, chatRequest);
		}).getMessage();
		assertEquals("jobId is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(sageUser, jobId, null);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullSessionId() {
		chatRequest.setSessionId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(sageUser, jobId, chatRequest);
		}).getMessage();
		assertEquals("request.sessionId is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullText() {
		chatRequest.setChatText(null);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		AgentChatResponse expected = new AgentChatResponse().setSessionId(sessionId).setResponseText("");
		assertEquals(response, expected);

	}

	@Test
	public void testInvokeAgentWithBlankText() {
		chatRequest.setChatText(" \n\t ");
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, jobId, chatRequest);

		AgentChatResponse expected = new AgentChatResponse().setSessionId(sessionId).setResponseText("");
		assertEquals(response, expected);
	}

	@Test
	public void testGetSession() {
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		// call under test
		AgentSession result = manager.getSession(sageUser, sessionId);
		assertEquals(session, result);
	}

	@Test
	public void testGetSessionWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getSession(null, sessionId);
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testGetSessionWithNullSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getSession(sageUser, null);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testInvokeAgentWithText() {
		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		doReturn(new AgentResponse().appendText("foo")).when(manager).invokeAgentAsync(jobId,
				agentRegistration.getType(), session, invokeAgentRequest);

		// call under test
		String results = manager.invokeAgentWithText(jobId, session, chatRequest);
		assertEquals("foo", results);
	}
	
	@Test
	public void testInvokeAgentWithTextAndSessionContext() {
		session.setSessionContext(sessionContext);
		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		doReturn(new AgentResponse().appendText("foo")).when(manager).invokeAgentAsync(jobId,
				agentRegistration.getType(), session, invokeAgentRequest);

		// call under test
		String results = manager.invokeAgentWithText(jobId, session, chatRequest);
		assertEquals("foo", results);
	}

	@Test
	public void testInvokeAgentWithTextWithReturnControl() {

		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		doReturn(new AgentResponse().setReturnControl(invocationId, returnControlEvents)).when(manager)
				.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentRequest);
		doReturn(invocationResultMembers).when(manager).executeEvents(session.getAgentAccessLevel(),
				returnControlEvents);
		doReturn(new AgentResponse().appendText("bar")).when(manager).invokeAgentAsync(jobId,
				agentRegistration.getType(), session, invokeAgentReturnRequest);

		// call under test
		String results = manager.invokeAgentWithText(jobId, session, chatRequest);
		assertEquals("bar", results);
	}

	@Test
	public void testInvokeAgentWithTextWithReturnControlAndTraceFalse() {
		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		chatRequest.setEnableTrace(false);
		doReturn(new AgentResponse().setReturnControl(invocationId, returnControlEvents)).when(manager)
				.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentRequest);
		doReturn(invocationResultMembers).when(manager).executeEvents(session.getAgentAccessLevel(),
				returnControlEvents);
		doReturn(new AgentResponse().appendText("bar")).when(manager).invokeAgentAsync(jobId,
				agentRegistration.getType(), session, invokeAgentReturnRequest);

		// call under test
		String results = manager.invokeAgentWithText(jobId, session, chatRequest);
		assertEquals("bar", results);
	}

	@Test
	public void testInvokeAgentWithTextWithReturnControlAndTraceNull() {
		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		chatRequest.setEnableTrace(null);
		doReturn(new AgentResponse().setReturnControl(invocationId, returnControlEvents)).when(manager)
				.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentRequest);
		doReturn(invocationResultMembers).when(manager).executeEvents(session.getAgentAccessLevel(),
				returnControlEvents);
		doReturn(new AgentResponse().appendText("bar")).when(manager).invokeAgentAsync(jobId,
				agentRegistration.getType(), session, invokeAgentReturnRequest);

		// call under test
		String results = manager.invokeAgentWithText(jobId, session, chatRequest);
		assertEquals("bar", results);
	}

	@Test
	public void testInvokeAgentWithTextWithReturnControlAndTraceTrue() {
		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		chatRequest.setEnableTrace(true);
		doReturn(new AgentResponse().setReturnControl(invocationId, returnControlEvents)).when(manager)
				.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentRequestWithTrace);
		doReturn(invocationResultMembers).when(manager).executeEvents(session.getAgentAccessLevel(),
				returnControlEvents);
		doReturn(new AgentResponse().appendText("bar")).when(manager).invokeAgentAsync(jobId,
				agentRegistration.getType(), session, invokeAgentReturnRequestWithTrace);

		// call under test
		String results = manager.invokeAgentWithText(jobId, session, chatRequest);
		assertEquals("bar", results);
	}

	@Test
	public void testInvokeAgentWithTextWithReturnControlInfiniteLoop() {

		when(mockAgentDao.getRegeistration(session.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		doReturn(new AgentResponse().setReturnControl(invocationId, returnControlEvents)).when(manager)
				.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentRequest);
		doReturn(invocationResultMembers).when(manager).executeEvents(session.getAgentAccessLevel(),
				returnControlEvents);
		// return control triggers another return control infinite loop.
		doReturn(new AgentResponse().setReturnControl(invocationId, returnControlEvents)).when(manager)
				.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentReturnRequest);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.invokeAgentWithText(jobId, session, chatRequest);
		}).getMessage();
		assertEquals("Max number of 1000 return_control agent response exceeded.", message);
		verify(mockLogger, times(2002)).info(any(String.class), any(), any(), any(), any(), any());
	}

	@Test
	public void testInvokeAgentAsnchMultipleOnChunk() throws InterruptedException, ExecutionException {
		when(mockCompletableFuture.get()).thenReturn(null);
		when(mockAgentClientProvider.getBedrockAgentRuntimeAsyncClient(agentRegistration.getType()))
				.thenReturn(mockAgentRuntime);
		// mock two onChunk()
		doAnswer((InvocationOnMock invocation) -> {
			InvokeAgentResponseHandler asyncResponseHandler = invocation.getArgument(1);
			asyncResponseHandler.onEventStream((s) -> {
				s.onSubscribe(mockSubscription);
				s.onNext(DefaultChunk.builder().bytes(SdkBytes.fromUtf8String("one")).build());
				s.onNext(DefaultChunk.builder().bytes(SdkBytes.fromUtf8String("two")).build());
			});
			asyncResponseHandler.responseReceived(InvokeAgentResponse.builder().build());
			return mockCompletableFuture;
		}).when(mockAgentRuntime).invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class));

		// call under test
		AgentResponse response = manager.invokeAgentAsync(jobId, agentRegistration.getType(), session,
				invokeAgentRequest);
		assertEquals("onetwo", response.getBuilder().toString());
		verify(mockLogger).info("onResponse() sessionId: '{}'", session.getSessionId());
	}

	@Test
	public void testInvokeAgentAsnchWithOnTrace() throws InterruptedException, ExecutionException {
		when(mockCompletableFuture.get()).thenReturn(null);
		doNothing().when(manager).onTrace(any(), any());
		when(mockAgentClientProvider.getBedrockAgentRuntimeAsyncClient(agentRegistration.getType()))
				.thenReturn(mockAgentRuntime);
		// mock two onChunk()
		doAnswer((InvocationOnMock invocation) -> {
			InvokeAgentResponseHandler asyncResponseHandler = invocation.getArgument(1);
			asyncResponseHandler.onEventStream((s) -> {
				s.onSubscribe(mockSubscription);
				s.onNext(traceInput);
				s.onNext(traceOutput);
				s.onNext(DefaultChunk.builder().bytes(SdkBytes.fromUtf8String("one")).build());
				s.onNext(DefaultChunk.builder().bytes(SdkBytes.fromUtf8String("two")).build());
			});
			asyncResponseHandler.responseReceived(InvokeAgentResponse.builder().build());
			return mockCompletableFuture;
		}).when(mockAgentRuntime).invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class));

		// call under test
		AgentResponse response = manager.invokeAgentAsync(jobId, agentRegistration.getType(), session,
				invokeAgentRequest);
		assertEquals("onetwo", response.getBuilder().toString());
		verify(mockLogger).info("onResponse() sessionId: '{}'", session.getSessionId());
		verify(manager).onTrace(jobId, traceInput);
		verify(manager).onTrace(jobId, traceOutput);
	}

	@Test
	public void testInvokeAgentWithTextWithOnReturnControl() throws InterruptedException, ExecutionException {
		ReturnControlPayload payload = DefaultReturnControl.builder().invocationId(invocationId).build();
		doReturn(session.getStartedBy()).when(manager).getRunAsUser(session);
		doReturn(returnControlEvents).when(manager).extractEvents(session.getStartedBy(), null, payload);

		when(mockCompletableFuture.get()).thenReturn(null);

		when(mockAgentClientProvider.getBedrockAgentRuntimeAsyncClient(agentRegistration.getType()))
				.thenReturn(mockAgentRuntime);
		// mock mock a return control call
		doAnswer((InvocationOnMock invocation) -> {
			InvokeAgentResponseHandler asyncResponseHandler = invocation.getArgument(1);
			asyncResponseHandler.onEventStream((s) -> {
				s.onSubscribe(mockSubscription);
				s.onNext(payload);
			});
			return mockCompletableFuture;
		}).when(mockAgentRuntime).invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class));

		// call under test
		AgentResponse response = manager.invokeAgentAsync(jobId, agentRegistration.getType(), session,
				invokeAgentRequest);
		assertEquals(new AgentResponse().setReturnControl(invocationId, returnControlEvents), response);
	}
	
	@Test
	public void testInvokeAgentWithTextWithOnReturnControlAndContext() throws InterruptedException, ExecutionException {
		session.setSessionContext(sessionContext);
		ReturnControlPayload payload = DefaultReturnControl.builder().invocationId(invocationId).build();
		doReturn(session.getStartedBy()).when(manager).getRunAsUser(session);
		doReturn(returnControlEvents).when(manager).extractEvents(session.getStartedBy(), sessionContext, payload);

		when(mockCompletableFuture.get()).thenReturn(null);

		when(mockAgentClientProvider.getBedrockAgentRuntimeAsyncClient(agentRegistration.getType()))
				.thenReturn(mockAgentRuntime);
		// mock mock a return control call
		doAnswer((InvocationOnMock invocation) -> {
			InvokeAgentResponseHandler asyncResponseHandler = invocation.getArgument(1);
			asyncResponseHandler.onEventStream((s) -> {
				s.onSubscribe(mockSubscription);
				s.onNext(payload);
			});
			return mockCompletableFuture;
		}).when(mockAgentRuntime).invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class));

		// call under test
		AgentResponse response = manager.invokeAgentAsync(jobId, agentRegistration.getType(), session,
				invokeAgentRequest);
		assertEquals(new AgentResponse().setReturnControl(invocationId, returnControlEvents), response);
	}

	@Test
	public void testInvokeAgentWithTextWithError() throws InterruptedException, ExecutionException {

		when(mockAgentClientProvider.getBedrockAgentRuntimeAsyncClient(agentRegistration.getType()))
				.thenReturn(mockAgentRuntime);
		InterruptedException e = new InterruptedException("something");
		when(mockCompletableFuture.get()).thenThrow(e);
		// mock two onChunk()
		doAnswer((InvocationOnMock invocation) -> {
			InvokeAgentResponseHandler asyncResponseHandler = invocation.getArgument(1);
			asyncResponseHandler.onEventStream((s) -> {
				s.onSubscribe(mockSubscription);
				s.onNext(DefaultChunk.builder().bytes(SdkBytes.fromUtf8String("one")).build());
				s.onNext(DefaultChunk.builder().bytes(SdkBytes.fromUtf8String("two")).build());
			});
			asyncResponseHandler.exceptionOccurred(e);
			return mockCompletableFuture;
		}).when(mockAgentRuntime).invokeAgent(any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class));

		String message = assertThrows(RuntimeException.class, () -> {
			// call under test
			manager.invokeAgentAsync(jobId, agentRegistration.getType(), session, invokeAgentRequest);
		}).getMessage();
		assertEquals("java.lang.InterruptedException: something", message);

		verify(mockLogger).error("onError() sessionId: '{}' errorMessage:'{}'", sessionId, e.getMessage());
	}

	@ParameterizedTest
	@EnumSource(value = AgentAccessLevel.class, names = { "READ_YOUR_PRIVATE_DATA", "WRITE_YOUR_PRIVATE_DATA" })
	public void testGetRunAsUserWithNonPublic(AgentAccessLevel level) {
		session.setAgentAccessLevel(level);
		// call under test
		assertEquals(session.getStartedBy(), manager.getRunAsUser(session));
	}

	@ParameterizedTest
	@EnumSource(value = AgentAccessLevel.class, names = { "PUBLICLY_ACCESSIBLE" })
	public void testGetRunAsUserWithPublic(AgentAccessLevel level) {
		session.setAgentAccessLevel(level);
		when(mockUserManager.getUserInfo(session.getStartedBy())).thenReturn(anonymous);
		// call under test
		assertEquals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(),
				manager.getRunAsUser(session));
	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testExecuteEventsWithEachType(AgentAccessLevel level) throws Exception {
		when(mockReturnControlHandlerProvider.getHandler(actionGroup, functionOne))
				.thenReturn(Optional.of(mockReturnControlHandlerOne));
		doReturn("one").when(manager).handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		when(mockReturnControlHandlerProvider.getHandler(actionGroup, functionTwo))
				.thenReturn(Optional.of(mockReturnControlHandlerTwo));
		doReturn("two").when(manager).handleEvent(level, mockReturnControlHandlerTwo, returnControlEventTwo);

		List<InvocationResultMember> expected = List.of(createInvocationResultMember(actionGroup, functionOne, "one"),
				createInvocationResultMember(actionGroup, functionTwo, "two"));

		// call under test
		List<InvocationResultMember> results = manager.executeEvents(level, returnControlEvents);
		assertEquals(expected, results);

	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testExecuteEventsWithOpenApiHandler(AgentAccessLevel level) throws Exception {
		when(mockReturnControlHandlerProvider.getHandler(actionGroup, apiFunction))
				.thenReturn(Optional.of(mockOpenApiReturnControlHandler));
		doReturn("one").when(manager).handleEvent(level, mockOpenApiReturnControlHandler, returnControlEventApi);

		when(mockOpenApiReturnControlHandler.getActionGroup()).thenReturn(actionGroup);
		when(mockOpenApiReturnControlHandler.getPath()).thenReturn(apiPath);
		when(mockOpenApiReturnControlHandler.getHttpMethod()).thenReturn(apiHttpMethod);
		when(mockOpenApiReturnControlHandler.getSuccessHttpCode()).thenReturn(apiHttpCode);

		List<InvocationResultMember> expected = List.of(InvocationResultMember.builder()
				.apiResult(ApiResult.builder().actionGroup(actionGroup).apiPath(apiPath)
						.httpMethod(apiHttpMethod.name()).httpStatusCode(apiHttpCode.getCode())
						.responseBody(Map.of("TEXT", ContentBody.builder().body("one").build())).build())
				.build());

		// call under test
		List<InvocationResultMember> results = manager.executeEvents(level, List.of(returnControlEventApi));
		assertEquals(expected, results);

	}

	private InvocationResultMember createInvocationResultMember(String actionGroup, String function, String response) {
		return InvocationResultMember.builder().functionResult(FunctionResult.builder().actionGroup(actionGroup)
				.function(function).responseBody(Map.of("TEXT", ContentBody.builder().body(response).build())).build())
				.build();
	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testExecuteEventsWithEachTypeAndNoHandler(AgentAccessLevel level) throws Exception {
		when(mockReturnControlHandlerProvider.getHandler(actionGroup, functionOne))
				.thenReturn(Optional.of(mockReturnControlHandlerOne));
		doReturn("one").when(manager).handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		when(mockReturnControlHandlerProvider.getHandler(actionGroup, functionTwo)).thenReturn(Optional.empty());

		String message = assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			manager.executeEvents(level, returnControlEvents);
		}).getMessage();
		assertEquals("No handler for actionGroup: 'someActionGroup' and function: 'functionTwo'", message);
	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testHandleEventWithNeedWriteAccessFalse(AgentAccessLevel level) throws Exception {

		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(false);
		when(mockReturnControlHandlerOne.handleEvent(returnControlEventOne)).thenReturn("one");

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		assertEquals("one", result);
		verify(manager).logInvocationEventToCloudWatch(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, null, returnControlEventOne, null);
		verify(manager, never()).logInvocationEventToCloudWatch(eq(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInputFailure), any(), any(), any());
	}

	@ParameterizedTest
	@EnumSource(value = AgentAccessLevel.class, names = { "WRITE_YOUR_PRIVATE_DATA" })
	public void testHandleEventWithNeedWriteAccessTrueWithWrite(AgentAccessLevel level) throws Exception {

		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(true);
		when(mockFeatureManager.isFeatureEnabled(Feature.ALLOW_AGENT_WRITES)).thenReturn(true);
		when(mockReturnControlHandlerOne.handleEvent(returnControlEventOne)).thenReturn("one");

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		assertEquals("one", result);

		verify(manager).logInvocationEventToCloudWatch(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, null, returnControlEventOne, null);
		verify(manager, never()).logInvocationEventToCloudWatch(eq(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInputFailure), any(), any(), any());
	}

	@ParameterizedTest
	@EnumSource(value = AgentAccessLevel.class, names = { "PUBLICLY_ACCESSIBLE", "READ_YOUR_PRIVATE_DATA" })
	public void testHandleEventWithNeedWriteAccessTrue(AgentAccessLevel level) throws Exception {
		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(true);
		when(mockFeatureManager.isFeatureEnabled(Feature.ALLOW_AGENT_WRITES)).thenReturn(true);

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		String expectedMessage = "Calling actionGroup: 'someActionGroup' function: 'functionOne' requires an access level of 'WRITE_YOUR_PRIVATE_DATA'. The current session has an access level of '"
				+ level.name()
				+ "'. Please inform the user that they will need to need to change the access level of this session to be 'WRITE_YOUR_PRIVATE_DATA' before this function may be called.";
		assertEquals("{\"errorMessage\":\"" + expectedMessage + "\"}", result);
		verify(mockLogger).error(
				"Return_control event execution failed. Will send the following message to the agent: '{}'",
				expectedMessage);

		verify(manager).logInvocationEventToCloudWatch(eq(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInputFailure), eq(null), eq(returnControlEventOne), any(UnauthorizedException.class));
	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testHandleEventWithFeatrueDisabled(AgentAccessLevel level) throws Exception {
		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(true);
		when(mockFeatureManager.isFeatureEnabled(Feature.ALLOW_AGENT_WRITES)).thenReturn(false);

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		assertEquals("{\"errorMessage\":\"The feature to allow agents to write to Synapse is currently disabled.\"}",
				result);
		verify(manager).logInvocationEventToCloudWatch(eq(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInputFailure), eq(null), eq(returnControlEventOne), any(UnsupportedOperationException.class));
	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testHandleEventWithException(AgentAccessLevel level) throws Exception {
		IllegalArgumentException e = new IllegalArgumentException("need this");
		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(false);
		when(mockReturnControlHandlerOne.handleEvent(returnControlEventOne)).thenThrow(e);

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		// the error message is sent to the agent in a JSON body.
		assertEquals("{\"errorMessage\":\"need this\"}", result);
		verify(mockLogger).error(
				"Return_control event execution failed. Will send the following message to the agent: '{}'",
				e.getMessage());

		verify(manager).logInvocationEventToCloudWatch(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, null, returnControlEventOne, null);
		verify(manager).logInvocationEventToCloudWatch(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInputFailure, null, returnControlEventOne, e);
	}

	@Test
	public void testExtractEvents() {
		String invocationId = "invocationId";

		ReturnControlPayload payload = ReturnControlPayload.builder().invocationId(invocationId)
				.invocationInputs(List.of(createInvocationInputMember(actionGroup, functionOne),
						createInvocationInputMember(actionGroup, functionTwo)))
				.build();

		List<Parameter> params = List.of(new Parameter("oneKey", "string", "oneValue"),
				new Parameter("twoKey", "string", "twoValue"));
		List<ReturnControlEvent> expected = List.of(
				new ReturnControlEvent(anonymousUserId, actionGroup, functionOne, params),
				new ReturnControlEvent(anonymousUserId, actionGroup, functionTwo, params));

		// call under test
		List<ReturnControlEvent> results = manager.extractEvents(anonymousUserId,null, payload);
		assertEquals(expected, results);
	}

	@Test
	public void testExtractEventsApiFunction() {
		String invocationId = "invocationId";
		String apiPathOne = "/foo/one/{id}";
		String apiPathTwo = "/foo/two/{another}";

		ReturnControlPayload payload = ReturnControlPayload.builder().invocationId(invocationId)
				.invocationInputs(List.of(createInvocationInputMemberApi(actionGroup, apiPathOne, "get"),
						createInvocationInputMemberApi(actionGroup, apiPathTwo, "PUT")))
				.build();

		List<Parameter> params = List.of(new Parameter("oneKey", "string", "oneValue"),
				new Parameter("twoKey", "string", "twoValue"));
		List<ReturnControlEvent> expected = List.of(
				new ReturnControlEvent(anonymousUserId, actionGroup, "GET /foo/one/{id}", params),
				new ReturnControlEvent(anonymousUserId, actionGroup, "PUT /foo/two/{another}", params));

		// call under test
		List<ReturnControlEvent> results = manager.extractEvents(anonymousUserId,null, payload);
		assertEquals(expected, results);
	}

	@Test
	public void testExtractEventsWithBothTypes() {
		String invocationId = "invocationId";
		String apiPathOne = "/foo/one/{id}";

		ReturnControlPayload payload = ReturnControlPayload.builder().invocationId(invocationId)
				.invocationInputs(List.of(createInvocationInputMemberApi(actionGroup, apiPathOne, "get"),
						createInvocationInputMember(actionGroup, functionTwo)))
				.build();

		List<Parameter> params = List.of(new Parameter("oneKey", "string", "oneValue"),
				new Parameter("twoKey", "string", "twoValue"));
		List<ReturnControlEvent> expected = List.of(
				new ReturnControlEvent(anonymousUserId, actionGroup, "GET /foo/one/{id}", params),
				new ReturnControlEvent(anonymousUserId, actionGroup, functionTwo, params));

		// call under test
		List<ReturnControlEvent> results = manager.extractEvents(anonymousUserId,null, payload);
		assertEquals(expected, results);
	}

	@Test
	public void testGetChatTrace() {
		when(mockStatusDao.getJobStatus(jobId))
				.thenReturn(new AsynchronousJobStatus().setJobId(jobId).setStartedByUserId(sageUser.getId()));
		List<TraceEvent> events = List.of(new TraceEvent().setMessage("one").setTimestamp(123L));
		when(mockAgentDao.listTraceEvents(jobId, traceRequest.getNewerThanTimestamp())).thenReturn(events);
		// call under test
		TraceEventsResponse res = manager.getChatTrace(sageUser, traceRequest);
		assertEquals(new TraceEventsResponse().setJobId(jobId).setPage(events), res);
	}

	@Test
	public void testGetChatTraceWithNullTimestamp() {
		traceRequest.setNewerThanTimestamp(null);
		when(mockStatusDao.getJobStatus(jobId))
				.thenReturn(new AsynchronousJobStatus().setJobId(jobId).setStartedByUserId(sageUser.getId()));
		List<TraceEvent> events = List.of(new TraceEvent().setMessage("one").setTimestamp(123L));
		when(mockAgentDao.listTraceEvents(jobId, null)).thenReturn(events);
		// call under test
		TraceEventsResponse res = manager.getChatTrace(sageUser, traceRequest);
		assertEquals(new TraceEventsResponse().setJobId(jobId).setPage(events), res);
	}

	@Test
	public void testGetChatTraceWithUnauthorized() {
		when(mockStatusDao.getJobStatus(jobId))
				.thenReturn(new AsynchronousJobStatus().setJobId(jobId).setStartedByUserId(admin.getId()));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getChatTrace(sageUser, traceRequest);
		}).getMessage();
		assertEquals("Only the user that started the job may access the job's trace", message);

		verifyNoMoreInteractions(mockAgentDao);
	}

	@Test
	public void testGetChatTraceWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getChatTrace(null, traceRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testGetChatTraceWithRequest() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getChatTrace(sageUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testGetChatTraceWithNullJobId() {
		traceRequest.setJobId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getChatTrace(sageUser, traceRequest);
		}).getMessage();
		assertEquals("request.jobId is required.", message);
	}

	private InvocationInputMember createInvocationInputMember(String actionGroup, String function) {
		FunctionParameter paramOne = FunctionParameter.builder().name("oneKey").type("string").value("oneValue")
				.build();
		FunctionParameter paramTwo = FunctionParameter.builder().name("twoKey").type("string").value("twoValue")
				.build();

		return InvocationInputMember.builder().functionInvocationInput(FunctionInvocationInput.builder()
				.actionGroup(actionGroup).function(function).parameters(List.of(paramOne, paramTwo)).build()).build();
	}

	private InvocationInputMember createInvocationInputMemberApi(String actionGroup, String apiPath, String method) {
		ApiParameter paramOne = ApiParameter.builder().name("oneKey").type("string").value("oneValue").build();
		ApiParameter paramTwo = ApiParameter.builder().name("twoKey").type("string").value("twoValue").build();
		return InvocationInputMember.builder().apiInvocationInput(ApiInvocationInput.builder().actionGroup(actionGroup)
				.apiPath(apiPath).httpMethod(method).parameters(paramOne, paramTwo).build()).build();
	}

	@Test
	public void testOnTraceWithInput() {
		// call under test
		manager.onTrace(jobId, traceInput);
		verifyNoMoreInteractions(mockAgentDao);
	}

	@Test
	public void testOnTraceWithOutput() {
		when(mockClock.currentTimeMillis()).thenReturn(1L);
		// call under test
		manager.onTrace(jobId, traceOutput);
		verify(mockAgentDao).addTraceToJob(jobId, 1L, traceOutText);
	}

	@Test
	public void testRegisterAgentWithAdmin() {
		when(mockAgentDao.createOrGetRegistration(AgentType.CUSTOM, agentRegistrationRequest))
				.thenReturn(agentRegistration);
		// call under test
		var r = manager.createOrGetAgentRegistration(admin, agentRegistrationRequest);
		assertEquals(agentRegistration, r);
	}

	@Test
	public void testRegisterAgentWithSager() {
		when(mockAgentDao.createOrGetRegistration(AgentType.CUSTOM, agentRegistrationRequest))
				.thenReturn(agentRegistration);
		// call under test
		var r = manager.createOrGetAgentRegistration(sageUser, agentRegistrationRequest);
		assertEquals(agentRegistration, r);
	}

	@Test
	public void testRegisterAgentWithSagerNullAlias() {
		agentRegistrationRequest.setAwsAliasId(null);
		when(mockAgentDao.createOrGetRegistration(AgentType.CUSTOM, new AgentRegistrationRequest()
				.setAwsAgentId(agentRegistrationRequest.getAwsAgentId()).setAwsAliasId(AgentManagerImpl.TSTALIASID)))
				.thenReturn(agentRegistration);
		// call under test
		var r = manager.createOrGetAgentRegistration(sageUser, agentRegistrationRequest);
		assertEquals(agentRegistration, r);
	}

	@Test
	public void testRegisterAgentWithSagerBlankAlias() {
		agentRegistrationRequest.setAwsAliasId(" \n");
		when(mockAgentDao.createOrGetRegistration(AgentType.CUSTOM, new AgentRegistrationRequest()
				.setAwsAgentId(agentRegistrationRequest.getAwsAgentId()).setAwsAliasId(AgentManagerImpl.TSTALIASID)))
				.thenReturn(agentRegistration);
		// call under test
		var r = manager.createOrGetAgentRegistration(sageUser, agentRegistrationRequest);
		assertEquals(agentRegistration, r);
	}

	@Test
	public void testRegisterAgentWithNonSager() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createOrGetAgentRegistration(nonSageNonAdmin, agentRegistrationRequest);
		}).getMessage();
		assertEquals("Currently, only internal users can register agents.", message);
	}

	@Test
	public void testRegisterAgentWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createOrGetAgentRegistration(anonymous, agentRegistrationRequest);
		}).getMessage();
		assertEquals("Currently, only internal users can register agents.", message);
	}

	@Test
	public void testRegisterAgentWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrGetAgentRegistration(null, agentRegistrationRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testRegisterAgentWithRequestUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createOrGetAgentRegistration(sageUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testGetAgentRegistration() {
		when(mockAgentDao.getRegeistration(agentRegistration.getAgentRegistrationId()))
				.thenReturn(Optional.of(agentRegistration));
		// call under test
		var r = manager.getAgentRegistration(nonSageNonAdmin, agentRegistration.getAgentRegistrationId());
		assertEquals(agentRegistration, r);
	}

	@Test
	public void testGetAgentRegistrationWithNotFound() {
		when(mockAgentDao.getRegeistration(agentRegistration.getAgentRegistrationId())).thenReturn(Optional.empty());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getAgentRegistration(nonSageNonAdmin, agentRegistration.getAgentRegistrationId());
		}).getMessage();
		assertEquals("AgentRegistrationId='reg111' does not exist", message);
	}

	@Test
	public void testGetAgentRegistrationWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getAgentRegistration(anonymous, agentRegistration.getAgentRegistrationId());
		}).getMessage();
		assertEquals("Must login to perform this action", message);
	}

	@Test
	public void testGetAgentRegistrationWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getAgentRegistration(null, agentRegistration.getAgentRegistrationId());
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testGetAgentRegistrationWithNullId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getAgentRegistration(nonSageNonAdmin, null);
		}).getMessage();
		assertEquals("agentRegistrationId is required.", message);
	}

	@Test
	public void testFromInvocationInputMemberWithFunction() {

		List<Parameter> params = List.of(new Parameter("oneKey", "string", "oneValue"),
				new Parameter("twoKey", "string", "twoValue"));
		InvocationInputMember member = createInvocationInputMember(actionGroup, functionOne);
		// call under test
		ReturnControlEvent event = manager.fromInvocationInputMember(adminId,null, member);
		ReturnControlEvent expected = new ReturnControlEvent(adminId, actionGroup, functionOne, params);
		assertEquals(expected, event);
	}

	@Test
	public void testFromInvocationInputMemberWithApi() {
		String apiPathOne = "/foo/one/{id}";

		List<Parameter> params = List.of(new Parameter("oneKey", "string", "oneValue"),
				new Parameter("twoKey", "string", "twoValue"));
		InvocationInputMember member = createInvocationInputMemberApi(actionGroup, apiPathOne, "get");
		// call under test
		ReturnControlEvent event = manager.fromInvocationInputMember(adminId,null, member);
		ReturnControlEvent expected = new ReturnControlEvent(adminId, actionGroup, "GET /foo/one/{id}", params);
		assertEquals(expected, event);
	}

	@Test
	public void testFromInvocationInputMemberWithUnknownType() {
		InvocationInputMember member = InvocationInputMember.builder().build();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.fromInvocationInputMember(adminId,null, member);
		});
		assertEquals("Expected either function or api invocation", e.getMessage());
	}

	@Test
	public void testLogInvocationEventToCloudWatch() {
		GridAgentSessionContext ctx = new GridAgentSessionContext();
		// call under test
		manager.logInvocationEventToCloudWatch(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, ctx, returnControlEventApi, null);
		verify(mockCloudwatchConsumer).addProfileData(any(ProfileData.class));
	}

	@Test
	public void testGetRequestBody() {
		JSONObject object = new JSONObject();
		object.put("keyOne", "someString");
		object.put("keyTwo", false);
		ApiRequestBody body = ApiRequestBody.builder()
				.content(Map.of("application/json", PropertyParameters.builder()
						.properties(
								software.amazon.awssdk.services.bedrockagentruntime.model.Parameter.builder()
										.name("baseOne").type("string").value("foo").build(),
								software.amazon.awssdk.services.bedrockagentruntime.model.Parameter.builder()
										.name("baseTwo").type("object").value(object.toString()).build())
						.build()))
				.build();

		// call under test
		List<Parameter> result = manager.getRequestBody(body);
		List<Parameter> expected = List.of(new Parameter("baseOne", "string", "foo"),
				new Parameter("baseTwo", "object", "{\"keyOne\":\"someString\",\"keyTwo\":false}"));
		assertEquals(expected, result);
	}

	@Test
	public void testGetRequestBodyWithNullBody() {
		ApiRequestBody body = null;

		// call under test
		List<Parameter> result = manager.getRequestBody(body);
		assertNull(result);
	}

}

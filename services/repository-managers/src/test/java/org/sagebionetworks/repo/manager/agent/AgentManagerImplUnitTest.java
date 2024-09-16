package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscription;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlHandlerProvider;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.sagebionetworks.repo.model.dbo.agent.AgentDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInputMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.ReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.SessionState;
import software.amazon.awssdk.services.bedrockagentruntime.model.responsestream.DefaultChunk;
import software.amazon.awssdk.services.bedrockagentruntime.model.responsestream.DefaultReturnControl;

@ExtendWith(MockitoExtension.class)
public class AgentManagerImplUnitTest {

	@Mock
	private AgentDao mockAgentDao;

	@Mock
	private BedrockAgentRuntimeAsyncClient mockAgentRuntime;

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

	@Spy
	@InjectMocks
	private AgentManagerImpl manager;

	private String stackBedrockAgentId;
	private Long adminId;
	private Long sageTeamId;
	private Long anonymousUserId;

	private UserInfo sageUser;
	private UserInfo admin;
	private UserInfo anonymous;
	private UserInfo nonSageNonAdmin;

	private CreateAgentSessionRequest createRequest;
	private AgentSession session;
	private String sessionId;
	private String inputText;

	private String actionGroup;
	private String functionOne;
	private String functionTwo;
	private Parameter parameter;
	private ReturnControlEvent returnControlEventOne;
	private ReturnControlEvent returnControlEventTwo;
	private List<ReturnControlEvent> returnControlEvents;

	private String returnControlResponseBody;
	private InvocationResultMember invocationResultMember;
	private List<InvocationResultMember> invocationResultMembers;

	private UpdateAgentSessionRequest updateRequest;

	private AgentChatRequest chatRequest;

	@BeforeEach
	public void before() {
		stackBedrockAgentId = "stackAgentId";
		ReflectionTestUtils.setField(manager, "stackBedrockAgentId", stackBedrockAgentId);

		adminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		sageTeamId = BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId();
		anonymousUserId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();

		boolean isAdmin = false;
		sageUser = new UserInfo(isAdmin);
		sageUser.setGroups(Set.of(sageTeamId));
		sageUser.setId(444L);

		anonymous = new UserInfo(false);
		anonymous.setId(anonymousUserId);

		admin = new UserInfo(true);
		admin.setId(adminId);

		nonSageNonAdmin = new UserInfo(false);
		nonSageNonAdmin.setId(555L);

		sessionId = "sessionId111";
		createRequest = new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE)
				.setAgentId("onetwothree");
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

		JSONObject response = new JSONObject();
		response.put("someKey", "someValue");
		returnControlResponseBody = response.toString();

		invocationResultMember = InvocationResultMember.builder()
				.functionResult(FunctionResult.builder().actionGroup(actionGroup).function(functionOne)
						.responseBody(Map.of("TEXT", ContentBody.builder().body(returnControlResponseBody).build()))
						.build())
				.build();
		invocationResultMembers = List.of(invocationResultMember);
	}

	@Test
	public void testCreateSessionWithSageUser() {
		when(mockAgentDao.createSession(sageUser.getId(), createRequest.getAgentAccessLevel(),
				createRequest.getAgentId())).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(sageUser, createRequest);
		assertEquals(session, result);
	}

	@Test
	public void testCreateSessionWithAdmin() {
		when(mockAgentDao.createSession(admin.getId(), createRequest.getAgentAccessLevel(), createRequest.getAgentId()))
				.thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(admin, createRequest);
		assertEquals(session, result);
	}

	@Test
	public void testCreateSessionWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createSession(anonymous, createRequest);
		}).getMessage();
		assertEquals("Must login to perform this action", message);
		verifyZeroInteractions(mockAgentDao);
	}

	@Test
	public void testCreateSessionWithNonSageNonAdminWithAgentId() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createSession(nonSageNonAdmin, createRequest);
		}).getMessage();
		assertEquals("Currently, only internal users can override the agentId.", message);
		verifyZeroInteractions(mockAgentDao);
	}

	@Test
	public void testCreateSessionWithNonSageNonAdminWithNullAgentId() {
		createRequest.setAgentId(null);
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				stackBedrockAgentId)).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(nonSageNonAdmin, createRequest);
		assertEquals(session, result);
	}

	@Test
	public void testCreateSessionWithNonSageNonAdminWithBlankAgentId() {
		createRequest.setAgentId("");
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest.getAgentAccessLevel(),
				stackBedrockAgentId)).thenReturn(session);

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
		verifyZeroInteractions(mockAgentDao);
	}

	@Test
	public void testCreateSessionWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createSession(sageUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockAgentDao);
	}

	@Test
	public void testCreateSessionWithNullAccessLevel() {
		createRequest.setAgentAccessLevel(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createSession(sageUser, createRequest);
		}).getMessage();
		assertEquals("request.agentAccessLevel is required.", message);
		verifyZeroInteractions(mockAgentDao);
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
		verifyZeroInteractions(mockAgentDao);
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
		doReturn(responseText).when(manager).invokeAgentWithText(session, chatRequest.getChatText());

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, chatRequest);

		AgentChatResponse expected = new AgentChatResponse().setSessionId(sessionId).setResponseText(responseText);
		assertEquals(response, expected);

	}

	@Test
	public void testInvokeAgentWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(null, chatRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(sageUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullSessionId() {
		chatRequest.setSessionId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invokeAgent(sageUser, chatRequest);
		}).getMessage();
		assertEquals("request.sessionId is required.", message);
	}

	@Test
	public void testInvokeAgentWithNullText() {
		chatRequest.setChatText(null);
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, chatRequest);

		AgentChatResponse expected = new AgentChatResponse().setSessionId(sessionId).setResponseText("");
		assertEquals(response, expected);

	}

	@Test
	public void testInvokeAgentWithBlankText() {
		chatRequest.setChatText(" \n\t ");
		doReturn(session).when(manager).getAndValidateAgentSession(sageUser, sessionId);

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, chatRequest);

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
		InvokeAgentRequest expected = InvokeAgentRequest.builder().agentId(session.getAgentId())
				.agentAliasId(AgentManagerImpl.TSTALIASID).sessionId(session.getSessionId()).enableTrace(false)
				.inputText(inputText).build();

		doReturn("foo").when(manager).invokeAgent(session, expected);

		// call under test
		String results = manager.invokeAgentWithText(session, inputText);
		assertEquals("foo", results);
	}

	@Test
	public void testInvokeAgentWithTextWithMultipleOnChunk() throws InterruptedException, ExecutionException {
		when(mockLoggerProvider.getLogger(AgentManagerImpl.class.getName())).thenReturn(mockLogger);
		when(mockCompletableFuture.get()).thenReturn(null);
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
		String response = manager.invokeAgentWithText(session, inputText);
		assertEquals("onetwo", response);
		verify(mockLogger).info("onResponse() sessionId: '{}'", session.getSessionId());
	}

	@Test
	public void testInvokeAgentWithTextWithOnReturnControl() throws InterruptedException, ExecutionException {
		ReturnControlPayload payload = DefaultReturnControl.builder().invocationId("someActionGroup").build();
		String returnControlResults = "response after return control";
		doReturn(returnControlResults).when(manager).invokeAgentWithReturnControlResults(session, payload);

		when(mockCompletableFuture.get()).thenReturn(null);
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
		String response = manager.invokeAgentWithText(session, inputText);
		assertEquals(returnControlResults, response);
	}

	@Test
	public void testInvokeAgentWithTextWithError() throws InterruptedException, ExecutionException {
		when(mockLoggerProvider.getLogger(AgentManagerImpl.class.getName())).thenReturn(mockLogger);
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
			manager.invokeAgentWithText(session, inputText);
		}).getMessage();
		assertEquals("java.lang.InterruptedException: something", message);

		verify(mockLogger).error("onError() sessionId: '{}' errorMessage:'{}'", sessionId,  e.getMessage());
	}

	@Test
	public void testInvokeAgentWithReturnControlResults() {
		String invokationId = "invocationId";
		ReturnControlPayload payload = ReturnControlPayload.builder().invocationId(invokationId).build();
		doReturn(session.getStartedBy()).when(manager).getRunAsUser(session);
		doReturn(returnControlEvents).when(manager).extractEvents(session.getStartedBy(), payload);
		doReturn(invocationResultMembers).when(manager).executeEvents(session.getAgentAccessLevel(),
				returnControlEvents);

		InvokeAgentRequest newRequest = InvokeAgentRequest.builder().agentId(session.getAgentId())
				.agentAliasId(AgentManagerImpl.TSTALIASID).sessionId(session.getSessionId())
				.sessionState(SessionState.builder().invocationId(invokationId)
						.returnControlInvocationResults(invocationResultMembers).build())
				.enableTrace(false).build();
		String responseString = "The answer is 42";
		doReturn(responseString).when(manager).invokeAgent(session, newRequest);

		// call under test
		String results = manager.invokeAgentWithReturnControlResults(session, payload);
		assertEquals(responseString, results);
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
	}

	@ParameterizedTest
	@EnumSource(value = AgentAccessLevel.class, names = { "WRITE_YOUR_PRIVATE_DATA" })
	public void testHandleEventWithNeedWriteAccessTrueWithWrite(AgentAccessLevel level) throws Exception {

		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(true);
		when(mockReturnControlHandlerOne.handleEvent(returnControlEventOne)).thenReturn("one");

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		assertEquals("one", result);
	}

	@ParameterizedTest
	@EnumSource(value = AgentAccessLevel.class, names = { "PUBLICLY_ACCESSIBLE", "READ_YOUR_PRIVATE_DATA" })
	public void testHandleEventWithNeedWriteAccessTrue(AgentAccessLevel level) throws Exception {
		when(mockLoggerProvider.getLogger(AgentManagerImpl.class.getName())).thenReturn(mockLogger);
		when(mockReturnControlHandlerOne.needsWriteAccess()).thenReturn(true);

		// call under test
		String result = manager.handleEvent(level, mockReturnControlHandlerOne, returnControlEventOne);
		String expectedMessage = "Calling actionGroup: 'someActionGroup' function: 'functionOne' requires an access level of 'WRITE_YOUR_PRIVATE_DATA'. The current session has an access level of '"
				+ level.name()
				+ "'. Please inform the user that they will need to need to change the access level of this session to be 'WRITE_YOUR_PRIVATE_DATA' before this function may be called.";
		assertEquals("{\"errorMessage\":\"" + expectedMessage + "\"}", result);
		verify(mockLogger).error(
				"Return_control event execution failed. Will send the following message to the agent: '{}'",
				expectedMessage);
	}

	@ParameterizedTest
	@EnumSource(AgentAccessLevel.class)
	public void testHandleEventWithException(AgentAccessLevel level) throws Exception {
		when(mockLoggerProvider.getLogger(AgentManagerImpl.class.getName())).thenReturn(mockLogger);
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
		List<ReturnControlEvent> results = manager.extractEvents(anonymousUserId, payload);
		assertEquals(expected, results);
	}

	private InvocationInputMember createInvocationInputMember(String actionGroup, String function) {
		FunctionParameter paramOne = FunctionParameter.builder().name("oneKey").type("string").value("oneValue")
				.build();
		FunctionParameter paramTwo = FunctionParameter.builder().name("twoKey").type("string").value("twoValue")
				.build();

		return InvocationInputMember.builder().functionInvocationInput(FunctionInvocationInput.builder()
				.actionGroup(actionGroup).function(function).parameters(List.of(paramOne, paramTwo)).build()).build();
	}
}

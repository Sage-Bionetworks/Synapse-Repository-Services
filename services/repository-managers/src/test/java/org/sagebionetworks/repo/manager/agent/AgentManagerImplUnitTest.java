package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
public class AgentManagerImplUnitTest {

	@Mock
	private AgentDao mockAgentDao;

	@Spy
	@InjectMocks
	private AgentManagerImpl manager;

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

	private UpdateAgentSessionRequest updateRequest;

	private AgentChatRequest chatRequest;

	@BeforeEach
	public void before() {
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

		chatRequest = new AgentChatRequest().setChatText("hi").setSessionId(sessionId);

	}

	@Test
	public void testCreateSessionWithSageUser() {
		when(mockAgentDao.createSession(sageUser.getId(), createRequest)).thenReturn(session);

		// call under test
		AgentSession result = manager.createSession(sageUser, createRequest);
		assertEquals(session, result);
	}

	@Test
	public void testCreateSessionWithAdmin() {
		when(mockAgentDao.createSession(admin.getId(), createRequest)).thenReturn(session);

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
		when(mockAgentDao.createSession(nonSageNonAdmin.getId(), createRequest)).thenReturn(session);

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
		when(mockAgentDao.updateSession(updateRequest)).thenReturn(session);

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

		// call under test
		AgentChatResponse response = manager.invokeAgent(sageUser, chatRequest);

		AgentChatResponse expected = new AgentChatResponse().setSessionId(sessionId).setResponseText("hello");
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
}

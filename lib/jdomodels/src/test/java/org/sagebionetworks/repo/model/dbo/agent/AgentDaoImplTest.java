package org.sagebionetworks.repo.model.dbo.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.UpdateAgentSessionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AgentDaoImplTest {

	@Autowired
	private AgentDao agentDao;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	private CreateAgentSessionRequest createRequest;
	private UpdateAgentSessionRequest updateRequest;

	@BeforeEach
	public void before() {
		this.createRequest = new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE)
				.setAgentId("123");
		this.updateRequest = new UpdateAgentSessionRequest()
				.setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA).setSessionId("sessionId");
	}

	@AfterEach
	public void after() {
		agentDao.truncateAll();
	}

	@Test
	public void testCreateSession() {
		CreateAgentSessionRequest request = new CreateAgentSessionRequest()
				.setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE).setAgentId("agent123");

		// call under test
		AgentSession session = agentDao.createSession(adminUserId, request);
		assertNotNull(session);
		assertNotNull(session.getSessionId());
		assertNotNull(session.getEtag());
		assertNotEquals(session.getEtag(), session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getModifiedOn());
		assertEquals(session.getStartedOn(), session.getModifiedOn());
		assertEquals(adminUserId, session.getStartedBy());
		assertEquals(AgentAccessLevel.PUBLICLY_ACCESSIBLE, session.getAgentAccessLevel());

		assertEquals(Optional.of(session), agentDao.getAgentSession(session.getSessionId()));

	}

	@Test
	public void testGetSessionWithNotFound() {
		// call under test
		assertEquals(Optional.empty(), agentDao.getAgentSession("does not exist"));
	}

	@Test
	public void testUpdateSession() throws InterruptedException {
		CreateAgentSessionRequest request = new CreateAgentSessionRequest()
				.setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA).setAgentId("agent123");

		AgentSession session = agentDao.createSession(adminUserId, request);
		assertNotNull(session);

		Thread.sleep(1001L);

		// call under test
		AgentSession updated = agentDao.updateSession(new UpdateAgentSessionRequest()
				.setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA).setSessionId(session.getSessionId()));
		assertNotNull(updated);

		assertEquals(session.getSessionId(), updated.getSessionId());
		assertEquals(session.getStartedBy(), updated.getStartedBy());
		assertEquals(session.getAgentId(), updated.getAgentId());
		assertEquals(session.getStartedOn(), updated.getStartedOn());
		assertTrue(updated.getModifiedOn().getTime() > session.getStartedOn().getTime());
		assertNotEquals(session.getEtag(), updated.getEtag());
		assertEquals(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA, updated.getAgentAccessLevel());

		assertEquals(Optional.of(updated), agentDao.getAgentSession(session.getSessionId()));
	}

	@Test
	public void testMultipleSessions() {

		AgentSession one = agentDao.createSession(adminUserId, new CreateAgentSessionRequest().setAgentId("one")
				.setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE));
		AgentSession two = agentDao.createSession(adminUserId, new CreateAgentSessionRequest().setAgentId("two")
				.setAgentAccessLevel(AgentAccessLevel.PUBLICLY_ACCESSIBLE));

		assertNotEquals(one.getSessionId(), two.getSessionId());

		AgentSession oneUp = agentDao.updateSession(new UpdateAgentSessionRequest().setSessionId(one.getSessionId())
				.setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));
		AgentSession twoUp = agentDao.updateSession(new UpdateAgentSessionRequest().setSessionId(two.getSessionId())
				.setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));

		assertEquals(AgentAccessLevel.READ_YOUR_PRIVATE_DATA, oneUp.getAgentAccessLevel());
		assertEquals(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA, twoUp.getAgentAccessLevel());

	}

	@Test
	public void testCreateSessionWithNullUserId() {
		adminUserId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, createRequest);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testCreateSessionWithNullRequest() {
		createRequest = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, createRequest);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testCreateSessionWithNullAccessLevel() {
		createRequest.setAgentAccessLevel(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, createRequest);
		}).getMessage();
		assertEquals("request.accessLevel is required.", message);
	}

	@Test
	public void testCreateSessionWithNullAgentId() {
		createRequest.setAgentId(null);
		AgentSession one = agentDao.createSession(adminUserId, createRequest);
		assertNotNull(one);
		assertNotNull(one.getSessionId());
		assertNull(one.getAgentId());
	}

	@Test
	public void testUpdateSessionWithNullRequest() {
		updateRequest = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.updateSession(updateRequest);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testUpdateSessionWithNullSessionId() {
		updateRequest.setSessionId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.updateSession(updateRequest);
		}).getMessage();
		assertEquals("request.sessionId is required.", message);
	}
	
	@Test
	public void testUpdateSessionWithNullAccessLevel() {
		updateRequest.setAgentAccessLevel(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.updateSession(updateRequest);
		}).getMessage();
		assertEquals("request.accessLevel is required.", message);
	}

}

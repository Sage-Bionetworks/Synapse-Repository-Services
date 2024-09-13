package org.sagebionetworks.repo.model.dbo.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AgentDaoImplTest {

	@Autowired
	private AgentDao agentDao;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	private String sessionId;
	private AgentAccessLevel accessLevel;
	private String agentId;

	@BeforeEach
	public void before() {
		this.sessionId = "sessionId";
		this.accessLevel = AgentAccessLevel.PUBLICLY_ACCESSIBLE;
		this.agentId = "123";
	}

	@AfterEach
	public void after() {
		agentDao.truncateAll();
	}

	@Test
	public void testCreateSession() {
		// call under test
		AgentSession session = agentDao.createSession(adminUserId, accessLevel, agentId);
		assertNotNull(session);
		assertNotNull(session.getSessionId());
		assertNotNull(session.getEtag());
		assertNotEquals(session.getEtag(), session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getModifiedOn());
		assertEquals(session.getStartedOn(), session.getModifiedOn());
		assertEquals(adminUserId, session.getStartedBy());
		assertEquals(AgentAccessLevel.PUBLICLY_ACCESSIBLE, session.getAgentAccessLevel());
		assertEquals(agentId, session.getAgentId());

		assertEquals(Optional.of(session), agentDao.getAgentSession(session.getSessionId()));

	}

	@Test
	public void testGetSessionWithNotFound() {
		// call under test
		assertEquals(Optional.empty(), agentDao.getAgentSession("does not exist"));
	}

	@Test
	public void testUpdateSession() throws InterruptedException {

		AgentSession session = agentDao.createSession(adminUserId, accessLevel, agentId);
		assertNotNull(session);

		Thread.sleep(1001L);

		// call under test
		AgentSession updated = agentDao.updateSession(session.getSessionId(), AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA);
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

		AgentSession one = agentDao.createSession(adminUserId, AgentAccessLevel.PUBLICLY_ACCESSIBLE, "one");
		AgentSession two = agentDao.createSession(adminUserId, AgentAccessLevel.PUBLICLY_ACCESSIBLE, "two");

		assertNotEquals(one.getSessionId(), two.getSessionId());

		AgentSession oneUp = agentDao.updateSession(one.getSessionId(), AgentAccessLevel.READ_YOUR_PRIVATE_DATA);
		AgentSession twoUp = agentDao.updateSession(two.getSessionId(), AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA);

		assertEquals(AgentAccessLevel.READ_YOUR_PRIVATE_DATA, oneUp.getAgentAccessLevel());
		assertEquals(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA, twoUp.getAgentAccessLevel());

	}

	@Test
	public void testCreateSessionWithNullUserId() {
		adminUserId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, accessLevel, agentId);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testCreateSessionWithNullAccessLevel() {
		accessLevel = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, accessLevel, agentId);
		}).getMessage();
		assertEquals("accessLevel is required.", message);
	}

	@Test
	public void testCreateSessionWithNullAgentId() {
		agentId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, accessLevel, agentId);
		}).getMessage();
		assertEquals("agentId is required.", message);;
	}



	@Test
	public void testUpdateSessionWithNullSessionId() {
		sessionId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.updateSession(sessionId, accessLevel);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}
	
	@Test
	public void testUpdateSessionWithNullAccessLevel() {
		accessLevel = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.updateSession(sessionId, accessLevel);
		}).getMessage();
		assertEquals("accessLevel is required.", message);
	}

}

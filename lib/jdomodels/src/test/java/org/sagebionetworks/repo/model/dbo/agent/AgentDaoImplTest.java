package org.sagebionetworks.repo.model.dbo.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentRegistration;
import org.sagebionetworks.repo.model.agent.AgentRegistrationRequest;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.AgentType;
import org.sagebionetworks.repo.model.agent.TraceEvent;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AgentDaoImplTest {

	@Autowired
	private AgentDao agentDao;

	@Autowired
	private AsynchronousJobStatusDAO asyncDao;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	private UserInfo admin;

	private String sessionId;
	private AgentAccessLevel accessLevel;
	private AgentRegistrationRequest registrationRequest;

	private AgentChatRequest request;
	private String registrationId;

	@BeforeEach
	public void before() {
		this.sessionId = "sessionId";
		this.accessLevel = AgentAccessLevel.PUBLICLY_ACCESSIBLE;
		this.registrationId = "123";
		this.admin = new UserInfo(true, adminUserId);
		this.admin.setContext(new CallersContext().setSessionId("abc"));
		this.request = new AgentChatRequest().setSessionId(sessionId).setChatText("hello");
		this.registrationRequest = new AgentRegistrationRequest().setAwsAgentId("awsId").setAwsAliasId("awsAlias");
	}

	@AfterEach
	public void after() {
		agentDao.truncateAll();
	}

	@Test
	public void testCreateSession() {

		AgentRegistration registration = agentDao.createOrGetRegistration(AgentType.BASELINE, registrationRequest);
		// call under test
		AgentSession session = agentDao.createSession(adminUserId, accessLevel, registration.getAgentRegistrationId());
		assertNotNull(session);
		assertNotNull(session.getSessionId());
		assertNotNull(session.getEtag());
		assertNotEquals(session.getEtag(), session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getModifiedOn());
		assertEquals(session.getStartedOn(), session.getModifiedOn());
		assertEquals(adminUserId, session.getStartedBy());
		assertEquals(AgentAccessLevel.PUBLICLY_ACCESSIBLE, session.getAgentAccessLevel());
		assertEquals(registration.getAgentRegistrationId(), session.getAgentRegistrationId());

		assertEquals(Optional.of(session), agentDao.getAgentSession(session.getSessionId()));

	}

	@Test
	public void testGetSessionWithNotFound() {
		// call under test
		assertEquals(Optional.empty(), agentDao.getAgentSession("does not exist"));
	}

	@Test
	public void testUpdateSession() throws InterruptedException {
		AgentRegistration registration = agentDao.createOrGetRegistration(AgentType.BASELINE, registrationRequest);
		AgentSession session = agentDao.createSession(adminUserId, accessLevel, registration.getAgentRegistrationId());
		assertNotNull(session);

		Thread.sleep(1001L);

		// call under test
		AgentSession updated = agentDao.updateSession(session.getSessionId(), AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA);
		assertNotNull(updated);

		assertEquals(session.getSessionId(), updated.getSessionId());
		assertEquals(session.getStartedBy(), updated.getStartedBy());
		assertEquals(session.getAgentRegistrationId(), registration.getAgentRegistrationId());
		assertEquals(session.getStartedOn(), updated.getStartedOn());
		assertTrue(updated.getModifiedOn().getTime() > session.getStartedOn().getTime());
		assertNotEquals(session.getEtag(), updated.getEtag());
		assertEquals(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA, updated.getAgentAccessLevel());

		assertEquals(Optional.of(updated), agentDao.getAgentSession(session.getSessionId()));
	}

	@Test
	public void testMultipleSessions() {
		AgentRegistration reg = agentDao.createOrGetRegistration(AgentType.BASELINE, registrationRequest);
		AgentSession one = agentDao.createSession(adminUserId, AgentAccessLevel.PUBLICLY_ACCESSIBLE,
				reg.getAgentRegistrationId());
		AgentSession two = agentDao.createSession(adminUserId, AgentAccessLevel.PUBLICLY_ACCESSIBLE,
				reg.getAgentRegistrationId());

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
			agentDao.createSession(adminUserId, accessLevel, registrationId);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testCreateSessionWithNullAccessLevel() {
		accessLevel = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, accessLevel, registrationId);
		}).getMessage();
		assertEquals("accessLevel is required.", message);
	}

	@Test
	public void testCreateSessionWithNullRegistrationId() {
		registrationId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createSession(adminUserId, accessLevel, registrationId);
		}).getMessage();
		assertEquals("registrationId is required.", message);
		;
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

	@Test
	public void testAddMessageWithMultipleJobs() {
		String one = "one";
		String two = "two";
		String three = "three";
		String four = "four";
		AsynchronousJobStatus jobOne = asyncDao.startJob(admin, request);
		AsynchronousJobStatus jobTwo = asyncDao.startJob(admin, request);

		long timeOne = 1;
		long timeTwo = 2;

		// call under test
		agentDao.addTraceToJob(jobOne.getJobId(), timeOne, one);
		agentDao.addTraceToJob(jobTwo.getJobId(), timeOne, three);

		agentDao.addTraceToJob(jobOne.getJobId(), timeTwo, two);
		agentDao.addTraceToJob(jobTwo.getJobId(), timeTwo, four);

		// job one with null filter
		assertEquals(
				List.of(new TraceEvent().setMessage(one).setTimestamp(timeOne),
						new TraceEvent().setMessage(two).setTimestamp(timeTwo)),
				agentDao.listTraceEvents(jobOne.getJobId(), null));

		// job one with non-null filter
		assertEquals(List.of(new TraceEvent().setMessage(two).setTimestamp(timeTwo)),
				agentDao.listTraceEvents(jobOne.getJobId(), timeOne));

		// job two with null filter
		assertEquals(
				List.of(new TraceEvent().setMessage(three).setTimestamp(timeOne),
						new TraceEvent().setMessage(four).setTimestamp(timeTwo)),
				agentDao.listTraceEvents(jobTwo.getJobId(), null));

		// job two with non-null filter
		assertEquals(List.of(new TraceEvent().setMessage(four).setTimestamp(timeTwo)),
				agentDao.listTraceEvents(jobTwo.getJobId(), timeOne));

	}

	@Test
	public void testAddMessageWithDuplicate() {
		String one = "one";
		String two = "two";
		AsynchronousJobStatus jobOne = asyncDao.startJob(admin, request);

		long timeOne = 1;

		// call under test
		agentDao.addTraceToJob(jobOne.getJobId(), timeOne, one);
		agentDao.addTraceToJob(jobOne.getJobId(), timeOne, two);

		// null filter
		assertEquals(List.of(new TraceEvent().setMessage(two).setTimestamp(timeOne)),
				agentDao.listTraceEvents(jobOne.getJobId(), null));

		// job one with non-null filter
		assertEquals(List.of(), agentDao.listTraceEvents(jobOne.getJobId(), timeOne));

	}

	@Test
	public void testAddTraceWithNullJobId() {
		String job = null;
		long now = 1L;
		String eventMessage = "hi";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.addTraceToJob(job, now, eventMessage);
		}).getMessage();
		assertEquals("jobId is required.", message);
	}

	@Test
	public void testAddTraceWithJobNotNumber() {
		String job = "abc";
		long now = 1L;
		String eventMessage = "hi";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.addTraceToJob(job, now, eventMessage);
		}).getMessage();
		assertEquals("For input string: \"abc\"", message);
	}

	@Test
	public void testAddTraceWithNullMessage() {
		AsynchronousJobStatus job = asyncDao.startJob(admin, request);
		long now = 1L;
		String eventMessage = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.addTraceToJob(job.getJobId(), now, eventMessage);
		}).getMessage();
		assertEquals("message is required.", message);
	}

	@Test
	public void testListTraceEventWithNullJobId() {
		String job = null;
		long now = 1L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.listTraceEvents(job, now);
		}).getMessage();
		assertEquals("jobId is required.", message);
	}

	@Test
	public void testListTraceEventWithJobNotNumber() {
		String job = "abc";
		long now = 1L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.listTraceEvents(job, now);
		}).getMessage();
		assertEquals("For input string: \"abc\"", message);
	}

	@Test
	public void testDBOAgentSessionMigration() {
		DBOAgentSession dbo = new DBOAgentSession();
		dbo.setAgentId("123");
		dbo.setRegistrationId(null);
		// call under test
		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(DBOAgentSession.BOOTSTRAP_REGISTRATION_ID, dbo.getRegistrationId());
	}

	@Test
	public void testDBOAgentSessionMigrationWithRegistrationId() {
		DBOAgentSession dbo = new DBOAgentSession();
		dbo.setAgentId("123");
		dbo.setRegistrationId(99L);

		// call under test
		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(99L, dbo.getRegistrationId());
	}

	@Test
	public void testDBOAgentSessionMigrationWithNullAgentId() {
		DBOAgentSession dbo = new DBOAgentSession();
		dbo.setAgentId(null);
		dbo.setRegistrationId(99L);

		// call under test
		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertEquals(99L, dbo.getRegistrationId());
	}

	@ParameterizedTest
	@EnumSource(AgentType.class)
	public void testCreateOrGetRegistration(AgentType type) {
		registrationRequest.setAwsAgentId("agentId" + type.ordinal());

		// call under test
		AgentRegistration resp = agentDao.createOrGetRegistration(type, registrationRequest);
		assertNotNull(resp);
		assertNotNull(resp.getAgentRegistrationId());
		assertEquals(registrationRequest.getAwsAgentId(), resp.getAwsAgentId());
		assertEquals(registrationRequest.getAwsAliasId(), resp.getAwsAliasId());
		assertNotNull(resp.getRegisteredOn());
		assertEquals(type, resp.getType());

		// additional call should return the same object
		assertEquals(resp, agentDao.createOrGetRegistration(type, registrationRequest));
		assertEquals(resp, agentDao.createOrGetRegistration(type, registrationRequest));

		// call under test
		assertEquals(Optional.of(resp), agentDao.getRegeistration(resp.getAgentRegistrationId()));
	}

	@Test
	public void testGetRegistrationDoesNotExit() {
		// call under test
		assertEquals(Optional.empty(), agentDao.getRegeistration("0"));
	}

	@Test
	public void testGetRegistrationDoesWithNull() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.getRegeistration(null);
		}).getMessage();
		assertEquals("registrationId is required.", message);
	}

	@Test
	public void testCreateOrGetRegistrationWithNullType() {
		AgentType type = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createOrGetRegistration(type, registrationRequest);
		}).getMessage();
		assertEquals("type is required.", message);
	}

	@Test
	public void testCreateOrGetRegistrationWithNullRequest() {
		AgentType type = AgentType.BASELINE;
		registrationRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createOrGetRegistration(type, registrationRequest);
		}).getMessage();
		assertEquals("request is required.", message);
	}

	@Test
	public void testCreateOrGetRegistrationWithNullRequestAgentId() {
		AgentType type = AgentType.BASELINE;
		registrationRequest.setAwsAgentId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createOrGetRegistration(type, registrationRequest);
		}).getMessage();
		assertEquals("request.awsAgentId is required.", message);
	}

	@Test
	public void testCreateOrGetRegistrationWithNullRequestAliasId() {
		AgentType type = AgentType.BASELINE;
		registrationRequest.setAwsAliasId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			agentDao.createOrGetRegistration(type, registrationRequest);
		}).getMessage();
		assertEquals("request.awsAliasId is required.", message);
	}
}

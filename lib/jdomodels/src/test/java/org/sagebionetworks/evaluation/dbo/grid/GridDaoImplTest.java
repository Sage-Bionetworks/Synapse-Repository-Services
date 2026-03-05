package org.sagebionetworks.evaluation.dbo.grid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.RequestOrigin;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class GridDaoImplTest {

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	private Long otherUser = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
	private Long teamId = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();

	@Autowired
	private GridDao dao;
	@Autowired
	private NodeDAO nodeDao;

	private boolean isAgent;
	private EventSource eventSource;
	private Long limit;
	private Long offset;

	@BeforeEach
	public void before() {
		isAgent = false;
		eventSource = EventSource.WEBSOCKET;
		limit = 100L;
		offset = 0L;
	}

	@AfterEach
	public void after() {
		dao.truncateAll();
		nodeDao.truncateAll();
	}

	@Test
	public void testCreateGridSession() {

		// call under test
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		assertNotNull(session);
		assertEquals(adminUserId.toString(), session.getStartedBy());
		assertNotNull(session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getEtag());
		assertEquals(GridConstants.START_REPLICA_ID_CLIENT, session.getLastReplicaIdClient());
		assertEquals(GridConstants.START_REPLICA_ID_SERVICE, session.getLastReplicaIdService());
		assertNull(session.getSourceEntityId());
		assertNull(session.getGridJsonSchema$Id());
		assertEquals(adminUserId.toString(), session.getOwnerPrincipalId());

		// call under test
		GridSession back = dao.getGridSession(session.getSessionId()).get();
		assertEquals(session, back);
	}
	
	@Test
	public void testCreateGridSessionWithOwner() {

		// call under test
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId).setOwner(teamId));
		assertNotNull(session);
		assertEquals(adminUserId.toString(), session.getStartedBy());
		assertNotNull(session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getEtag());
		assertEquals(GridConstants.START_REPLICA_ID_CLIENT, session.getLastReplicaIdClient());
		assertEquals(GridConstants.START_REPLICA_ID_SERVICE, session.getLastReplicaIdService());
		assertNull(session.getSourceEntityId());
		assertNull(session.getGridJsonSchema$Id());
		assertEquals(teamId.toString(), session.getOwnerPrincipalId());

		// call under test
		GridSession back = dao.getGridSession(session.getSessionId()).get();
		assertEquals(session, back);
		assertEquals(Optional.empty(), dao.getSessionSource(session.getSessionId()));
	}

	@Test
	public void testCreateGridSessionWithTableIdAndSchema() {
		Node node = nodeDao.createNewNode(NodeTestUtils.createNew("source", adminUserId));
		GridSource expectedSource = new GridSource(KeyFactory.stringToKey(node.getId()), EntityType.project);
		// call under test
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId)
				.setSourceId(node.getId()).setSchemaId("someorg-someschema"));
		assertNotNull(session);
		assertEquals(adminUserId.toString(), session.getStartedBy());
		assertNotNull(session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getEtag());
		assertEquals(GridConstants.START_REPLICA_ID_CLIENT, session.getLastReplicaIdClient());
		assertEquals(GridConstants.START_REPLICA_ID_SERVICE, session.getLastReplicaIdService());
		assertEquals(node.getId(), session.getSourceEntityId());
		assertEquals("someorg-someschema", session.getGridJsonSchema$Id());

		// call under test
		GridSession back = dao.getGridSession(session.getSessionId()).get();
		assertEquals(session, back);
		assertEquals(session, back);
		
		assertEquals(Optional.of(expectedSource), dao.getSessionSource(session.getSessionId()));
	}

	@Test
	public void getGridSessionDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getGridSession("doesnotexist"));
	}

	@Test
	public void testGetGridSessionOwner() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		// call under test
		assertEquals(Optional.of(adminUserId), dao.getGridSessionOwner(session.getSessionId()));
	}
	
	@Test
	public void testGetGridSessionOwnerWithTeam() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId).setOwner(teamId));
		// call under test
		assertEquals(Optional.of(teamId), dao.getGridSessionOwner(session.getSessionId()));
	}

	@Test
	public void testGetGridSessionStartedByDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getGridSessionOwner("doesnotexist"));
	}

	@Test
	public void testCreateGridReplicaWithWebsocket() throws InterruptedException {
		eventSource = EventSource.WEBSOCKET;
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridSession other = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		Thread.sleep(1001L);

		// call under test
		GridReplica replica = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		assertNotNull(replica);
		assertNotNull(replica.getCreatedOn());
		assertEquals(adminUserId.toString(), replica.getCreatedBy());
		assertEquals(false, replica.getIsAgentReplica());
		assertEquals(session.getSessionId(), replica.getGridSessionId());
		assertEquals(session.getLastReplicaIdClient() + 1L, replica.getReplicaId());

		// session's etag, modified and last repId should have changed.
		GridSession updated = dao.getGridSession(session.getSessionId()).get();
		assertNotEquals(updated.getEtag(), session.getEtag());
		assertTrue(updated.getModifiedOn().getTime() > session.getModifiedOn().getTime());
		assertEquals(updated.getLastReplicaIdClient(), session.getLastReplicaIdClient() + 1L);
		// service should not have changed.
		assertEquals(updated.getLastReplicaIdService(), session.getLastReplicaIdService());

		// the other session should not have changed.
		assertEquals(other, dao.getGridSession(other.getSessionId()).get());
	}

	@Test
	public void testCreateGridReplicaWithInternal() throws InterruptedException {
		eventSource = EventSource.INTERNAL;
		isAgent = true;
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		Thread.sleep(1001L);

		// call under test
		GridReplica replica = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		assertNotNull(replica);
		assertNotNull(replica.getCreatedOn());
		assertEquals(adminUserId.toString(), replica.getCreatedBy());
		assertEquals(true, replica.getIsAgentReplica());
		assertEquals(session.getSessionId(), replica.getGridSessionId());
		assertEquals(session.getLastReplicaIdService() - 1L, replica.getReplicaId());

		// session's etag, modified and last repId should have changed.
		GridSession updated = dao.getGridSession(session.getSessionId()).get();
		assertNotEquals(updated.getEtag(), session.getEtag());
		assertTrue(updated.getModifiedOn().getTime() > session.getModifiedOn().getTime());
		assertEquals(updated.getLastReplicaIdService(), session.getLastReplicaIdService() - 1L);
		// client should not have changed.
		assertEquals(updated.getLastReplicaIdClient(), session.getLastReplicaIdClient());
	}

	@Test
	public void testGetReplica() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridReplica replica = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		// call under test
		assertEquals(replica, dao.getGridReplica(session.getSessionId(), replica.getReplicaId()).get());
	}

	@Test
	public void testGetReplicatWithDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getGridReplica("doesnotexist", 1L));
	}

	@Test
	public void testGetReplicaCreatedBy() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridReplica replica = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		// call under test
		assertEquals(Optional.of(adminUserId),
				dao.getReplicaCreatedBy(session.getSessionId(), replica.getReplicaId()));
	}

	@Test
	public void testGetReplicaCreatedByWithDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getReplicaCreatedBy("doesnotexist", 0L));
	}

	@ParameterizedTest
	@EnumSource(EventSource.class)
	public void testConnectionCRUD(EventSource source) throws InterruptedException {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridReplica r1 = dao.createReplica(adminUserId, session.getSessionId(), isAgent, source);
		GridReplica r2 = dao.createReplica(adminUserId, session.getSessionId(), isAgent, source);

		GridConnectionInfo info1 = new GridConnectionInfo().setConnectionId(UUID.randomUUID().toString())
				.setCreatedBy(adminUserId).setReplicaId(r1.getReplicaId()).setSessionId(session.getSessionId())
				.setSource(source);
		GridConnectionInfo info2 = new GridConnectionInfo().setConnectionId(UUID.randomUUID().toString())
				.setCreatedBy(adminUserId).setReplicaId(r2.getReplicaId()).setSessionId(session.getSessionId())
				.setSource(source);
		// call under test
		dao.createConnection(info1);
		// call under test
		GridConnectionInfo f1 = dao.getConnection(info1.getConnectionId()).get();
		assertNotNull(f1.getCreatedOn());
		long startingCreatedOn = f1.getCreatedOn().getTime();
		assertEquals(session.getSessionId(), f1.getSessionId());
		assertEquals(r1.getReplicaId(), f1.getReplicaId());
		assertEquals(adminUserId, f1.getCreatedBy());
		assertEquals(info1.getConnectionId(), f1.getConnectionId());

		GridConnectionInfo con = dao.getConnection(f1.getSessionId(), f1.getReplicaId()).get();
		assertEquals(f1, con);

		// call under test
		Optional<GridConnectionInfo> defaultInternalConnection = dao.getSingletonConnection(info1.getSessionId(),
				source);
		if (source.isSingleton()) {
			assertEquals(f1, defaultInternalConnection.get());
		} else {
			assertTrue(defaultInternalConnection.isEmpty());
		}

		Optional<GridConnectionInfo> userDefaultConnection = dao.getSingletonUserConnection(info1.getSessionId(),
				adminUserId, source);

		if (source.isSingleton()) {
			assertEquals(f1, userDefaultConnection.get());
		} else {
			assertTrue(userDefaultConnection.isEmpty());
		}

		// call under test
		dao.createConnection(info2);
		// call under test
		GridConnectionInfo f2 = dao.getConnection(info2.getConnectionId()).get();
		assertNotNull(f2.getCreatedOn());
		assertEquals(session.getSessionId(), f2.getSessionId());
		assertEquals(r2.getReplicaId(), f2.getReplicaId());
		assertEquals(adminUserId, f2.getCreatedBy());
		assertEquals(info2.getConnectionId(), f2.getConnectionId());
		// call under test
		defaultInternalConnection = dao.getSingletonConnection(info1.getSessionId(), source);
		if (source.isSingleton()) {
			assertEquals(f1, defaultInternalConnection.get());
		} else {
			assertTrue(defaultInternalConnection.isEmpty());
		}

		// Wait for the new createdOn to be larger
		Thread.sleep(1001L);

		// replace an existing connection with a new ID
		dao.createConnection(info1.setConnectionId(UUID.randomUUID().toString()));
		f1 = dao.getConnection(info1.getConnectionId()).get();
		assertTrue(f1.getCreatedOn().getTime() > startingCreatedOn);
		assertEquals(session.getSessionId(), f1.getSessionId());
		assertEquals(r1.getReplicaId(), f1.getReplicaId());
		assertEquals(adminUserId, f1.getCreatedBy());
		assertEquals(info1.getConnectionId(), f1.getConnectionId());

		// call under test
		List<GridConnectionInfo> listed = dao.listConnections(session.getSessionId());
		List<GridConnectionInfo> expected = source.getRequestOrigin() == RequestOrigin.USER ? List.of(f1, f2) : List.of(f2, f1);
		assertEquals(expected, listed);

		// call under test
		dao.removeConnection(info1.getConnectionId());
		// double delete is allowed
		dao.removeConnection(info1.getConnectionId());
		assertEquals(Optional.empty(), dao.getConnection(info1.getConnectionId()));
		assertEquals(Optional.empty(), dao.getConnection(info1.getSessionId(), info1.getReplicaId()));

		// should still be able to get the second
		assertEquals(f2, dao.getConnection(info2.getConnectionId()).get());
		// call under test - if internal, default connection should now be f2
		defaultInternalConnection = dao.getSingletonConnection(info1.getSessionId(), source);
		if (source.isSingleton()) {
			assertEquals(f2, defaultInternalConnection.get());
		} else {
			assertTrue(defaultInternalConnection.isEmpty());
		}
	}

	@Test
	public void testSavePatch() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L);
		String s3Key = "thekey";
		Duration expires = Duration.ofSeconds(100L);
		// call under test
		assertTrue(dao.savePatch(session.getSessionId(), patchId, s3Key, expires, 100L));
		assertFalse(dao.savePatch(session.getSessionId(), patchId, s3Key, expires, 100L));

		PatchInfo patch = dao.getPatchInfo(session.getSessionId(), patchId).get();
		assertNotNull(patch);
		assertEquals(session.getSessionId(), patch.getSessionId());
		assertEquals(patchId, patch.getPatchId());
		assertNotNull(patch.getCreatedOn());
		assertNotNull(patch.getExpiresOn());
		assertTrue(patch.getCreatedOn().getTime() < patch.getExpiresOn().getTime());
		assertEquals(s3Key, patch.getS3Key());
		assertEquals(100L, patch.getSizeBytes());

	}

	@Test
	public void testSavePatchWithMultipleSession() {
		GridSession sessionOne = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridSession sessionTwo = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L);
		String s3Key = "thekey";
		Duration expires = Duration.ofSeconds(100L);
		// call under test
		assertTrue(dao.savePatch(sessionOne.getSessionId(), patchId, s3Key, expires, 100L));
		assertFalse(dao.savePatch(sessionOne.getSessionId(), patchId, s3Key, expires, 100L));
		assertTrue(dao.savePatch(sessionTwo.getSessionId(), patchId, s3Key, expires, 100L));
		assertFalse(dao.savePatch(sessionTwo.getSessionId(), patchId, s3Key, expires, 100L));

		PatchInfo patchOne = dao.getPatchInfo(sessionOne.getSessionId(), patchId).get();
		assertNotNull(patchOne);
		assertEquals(sessionOne.getSessionId(), patchOne.getSessionId());
		assertEquals(patchId, patchOne.getPatchId());

		PatchInfo patchTwo = dao.getPatchInfo(sessionTwo.getSessionId(), patchId).get();
		assertNotNull(patchTwo);
		assertEquals(sessionTwo.getSessionId(), patchTwo.getSessionId());
		assertEquals(patchId, patchTwo.getPatchId());
	}

	@Test
	public void testGetPatchWithNotFound() {
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L);
		assertEquals(Optional.empty(), dao.getPatchInfo("notfound", patchId));
	}

	@Test
	public void testListMissingPatches() {
		GridSession sessionOne = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridSession sessionTwo = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		Duration expires = Duration.ofSeconds(100L);

		List<LogicalTimestamp> patchIds = createTestPatchIds(3, 4);
		patchIds.stream().forEach(p -> {
			String s3Key = p.toString();
			assertTrue(dao.savePatch(sessionOne.getSessionId(), p, s3Key, expires, 100L));
			assertTrue(dao.savePatch(sessionTwo.getSessionId(), p, s3Key, expires, 100L));
		});

		List<LogicalTimestamp> patchIdsSortedBySeq = patchIds.stream().sorted((p1, p2) -> {
			if (!p1.getSequenceNumber().equals(p2.getSequenceNumber())) {
				return p1.getSequenceNumber().compareTo(p2.getSequenceNumber());
			} else {
				return p1.getReplicaId().compareTo(p2.getReplicaId());
			}
		}).collect(Collectors.toList());

		// call under test
		List<PatchInfo> list = dao.listMissingPatchInfoForClock(sessionOne.getSessionId(), List.of(), 100);
		// empty clock should return all patches in order of sequence number
		assertEquals(patchIdsSortedBySeq, list.stream().map(PatchInfo::getPatchId).collect(Collectors.toList()));
		// also verify that the returned patch info includes other fields (testing the row mapper)
		for (int i = 0; i < patchIds.size(); i++) {
			PatchInfo info = list.get(i);
			assertEquals(sessionOne.getSessionId(), info.getSessionId());
			assertEquals(patchIdsSortedBySeq.get(i), info.getPatchId());
			assertNotNull(info.getCreatedOn());
			assertNotNull(info.getExpiresOn());
			assertTrue(info.getCreatedOn().getTime() < info.getExpiresOn().getTime());
			assertNotNull(info.getS3Key());
			assertEquals(100L, info.getSizeBytes());
		}

		// call under test
		list = dao.listMissingPatchInfoForClock(sessionOne.getSessionId(),
				List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(9L),
						new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(9L),
						new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(9L)),
				100);
		// up-to-date should be empty patches
		assertEquals(Collections.emptyList(), list);

		// call under test
		list = dao.listMissingPatchInfoForClock(sessionOne.getSessionId(),
				List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(9L),
						new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(7L),
						new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(5L)),
				100);

		List<LogicalTimestamp> expectedPatchIds = List.of(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(8L));

		assertEquals(expectedPatchIds, list.stream().map(PatchInfo::getPatchId).collect(Collectors.toList()));

		// call under test
		list = dao.listMissingPatchInfoForClock(sessionOne.getSessionId(),
				List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(8L),
						new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(6L),
						new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(4L)),
				100);

		expectedPatchIds = List.of(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(4L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(8L));

		assertEquals(expectedPatchIds, list.stream().map(PatchInfo::getPatchId).collect(Collectors.toList()));
	}

	@Test
	public void testListActiveSession() throws InterruptedException {
		assertEquals(Collections.emptyList(), dao.listActiveGridSession(adminUserId, limit, offset));

		List<GridSession> adminSessions = createSessions(3, new CreateGridSession().setUserId(adminUserId));
		List<GridSession> otherSessions = createSessions(3, new CreateGridSession().setUserId(otherUser));

		// call under test
		List<GridSession> list = dao.listActiveGridSession(adminUserId, limit, offset);
		List<GridSession> expected = List.of(adminSessions.get(2), adminSessions.get(1), adminSessions.get(0));
		assertEquals(expected, list);
		// call under test
		dao.deleteGridSession(adminSessions.get(1).getSessionId());
		// call under test
		list = dao.listActiveGridSession(adminUserId, limit, offset);
		expected = List.of(adminSessions.get(2), adminSessions.get(0));
		assertEquals(expected, list);

		list = dao.listActiveGridSession(otherUser, limit, offset);
		expected = List.of(otherSessions.get(2), otherSessions.get(1), otherSessions.get(0));
	}

	@Test
	public void testListActiveSessionWithPagination() throws InterruptedException {
		List<GridSession> adminSessions = createSessions(3, new CreateGridSession().setUserId(adminUserId));
		limit = 1L;
		offset = 0L;
		// call under test
		List<GridSession> list = dao.listActiveGridSession(adminUserId, limit, offset);
		List<GridSession> expected = List.of(adminSessions.get(2));
		assertEquals(expected, list);

		limit = 2L;
		offset = 1L;
		// call under test
		list = dao.listActiveGridSession(adminUserId, limit, offset);
		expected = List.of(adminSessions.get(1), adminSessions.get(0));
		assertEquals(expected, list);
	}

	@Test
	public void testListActiveSessionsWithNullUser() {
		adminUserId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, limit, offset);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testListActiveSessionsWithNullLimit() {
		limit = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, limit, offset);
		}).getMessage();
		assertEquals("limit is required.", message);
	}

	@Test
	public void testListActiveSessionsWithNullOffset() {
		offset = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, limit, offset);
		}).getMessage();
		assertEquals("offset is required.", message);
	}

	@Test
	public void testListActiveSessionWithSource() throws InterruptedException {
		assertEquals(Collections.emptyList(), dao.listActiveGridSession(adminUserId, "syn1", limit, offset));
		Node n1 = nodeDao.createNewNode(NodeTestUtils.createNew("source", adminUserId));
		Node n2 = nodeDao.createNewNode(NodeTestUtils.createNew("source", adminUserId));
		List<GridSession> adminSessions1 = createSessions(1,
				new CreateGridSession().setUserId(adminUserId).setSourceId(n1.getId()));
		List<GridSession> otherSessions = createSessions(3,
				new CreateGridSession().setUserId(otherUser).setSchemaId(n1.getId()));
		List<GridSession> adminSessions2 = createSessions(2,
				new CreateGridSession().setUserId(adminUserId).setSourceId(n2.getId()));

		// call under test
		List<GridSession> list = dao.listActiveGridSession(adminUserId, n1.getId(), limit, offset);
		List<GridSession> expected = List.of(adminSessions1.get(0));
		assertEquals(expected, list);

		// call under test
		list = dao.listActiveGridSession(adminUserId, n2.getId(), limit, offset);
		expected = List.of(adminSessions2.get(1), adminSessions2.get(0));
		assertEquals(expected, list);

		// call under test
		dao.deleteGridSession(adminSessions2.get(1).getSessionId());
		// call under test
		list = dao.listActiveGridSession(adminUserId, n2.getId(), limit, offset);
		expected = List.of(adminSessions2.get(0));
		assertEquals(expected, list);

		list = dao.listActiveGridSession(otherUser, n1.getId(), limit, offset);
		expected = List.of(otherSessions.get(2), otherSessions.get(1), otherSessions.get(0));
	}

	@Test
	public void testListActiveSessionWihtSourceAndPagination() throws InterruptedException {
		Node n1 = nodeDao.createNewNode(NodeTestUtils.createNew("source", adminUserId));
		List<GridSession> adminSessions1 = createSessions(3,
				new CreateGridSession().setUserId(adminUserId).setSourceId(n1.getId()));

		limit = 1L;
		offset = 0L;
		// call under test
		List<GridSession> list = dao.listActiveGridSession(adminUserId, n1.getId(), limit, offset);
		List<GridSession> expected = List.of(adminSessions1.get(2));
		assertEquals(expected, list);

		limit = 2L;
		offset = 1L;
		// call under test
		list = dao.listActiveGridSession(adminUserId, n1.getId(), limit, offset);
		expected = List.of(adminSessions1.get(1), adminSessions1.get(0));
		assertEquals(expected, list);

	}

	@Test
	public void testListActiveSessionsWithSourceAndNullUser() {
		adminUserId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, "syn123", limit, offset);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testListActiveSessionsWithNullSource() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, null, limit, offset);
		}).getMessage();
		assertEquals("sourceId is required.", message);
	}

	@Test
	public void testListActiveSessionsWithSourceNullLimit() {
		limit = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, "syn123", limit, offset);
		}).getMessage();
		assertEquals("limit is required.", message);
	}

	@Test
	public void testListActiveSessionsWithSourceAndNullOffset() {
		offset = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.listActiveGridSession(adminUserId, "syn123", limit, offset);
		}).getMessage();
		assertEquals("offset is required.", message);
	}

	@Test
	public void testDeleteGridSessionWithNullId() {
		offset = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.deleteGridSession(null);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	/**
	 * Helper to create n grid sessions.
	 * 
	 * @param count
	 * @param create
	 * @return
	 * @throws InterruptedException
	 */
	public List<GridSession> createSessions(int count, CreateGridSession create) throws InterruptedException {
		List<GridSession> sessions = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			sessions.add(dao.createGridSession(create));
			Thread.sleep(1001);
		}
		return sessions;
	}

	List<LogicalTimestamp> createTestPatchIds(int replicaCount, int sequenceCount) {
		List<LogicalTimestamp> ids = new ArrayList<>(replicaCount * sequenceCount);
		List<Integer> replicaIds = IntStream.range(1, replicaCount + 1).boxed().sorted((num1, num2) -> {
			// Put even replicas before odd replicas to verify that the patch list is sorted
			// by sequence number before sorting by replica ID
			boolean num1Even = num1 % 2 == 0;
			boolean num2Even = num2 % 2 == 0;
			if (num1Even && !num2Even) {
				return -1;
			} else if (!num1Even && num2Even) {
				return 1;
			}
			return Integer.compare(num1, num2);
		}).collect(Collectors.toList());
		replicaIds.forEach(rep -> {
			for (long seq = 1; seq < sequenceCount + 1; seq++) {
				ids.add(new LogicalTimestamp().setReplicaId(rep.longValue()).setSequenceNumber(seq * 2));
			}
		});
		return ids;
	}

	@Test
	public void testSaveSnapshot() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		ClockTable clockTable = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(20L)));
		String s3Key = "snapshot-key";
		// call under test
		assertTrue(dao.saveSnapshot(session.getSessionId(), clockTable, s3Key, adminUserId));

		// call under test
		GridSnapshot snapshot = dao.getLatestSnapshot(session.getSessionId()).get();
		assertNotNull(snapshot);
		assertEquals(session.getSessionId(), snapshot.getSessionId());
		assertEquals(clockTable, snapshot.getClockTable());
		assertNotNull(snapshot.getCreatedOn());
		assertEquals(adminUserId, snapshot.getCreatedBy());
		assertEquals(s3Key, snapshot.getS3Key());
		assertNotNull(snapshot.getId());
	}

	@Test
	public void testSaveSnapshotWithMultipleSessions() {
		GridSession sessionOne = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		GridSession sessionTwo = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		ClockTable clockTable = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L)));
		String s3Key = "snapshot-key";
		// call under test
		assertTrue(dao.saveSnapshot(sessionOne.getSessionId(), clockTable, s3Key, adminUserId));
		assertTrue(dao.saveSnapshot(sessionTwo.getSessionId(), clockTable, s3Key, otherUser));

		GridSnapshot snapshotOne = dao.getLatestSnapshot(sessionOne.getSessionId()).get();
		assertNotNull(snapshotOne);
		assertEquals(sessionOne.getSessionId(), snapshotOne.getSessionId());
		assertEquals(clockTable, snapshotOne.getClockTable());
		assertEquals(adminUserId, snapshotOne.getCreatedBy());

		GridSnapshot snapshotTwo = dao.getLatestSnapshot(sessionTwo.getSessionId()).get();
		assertNotNull(snapshotTwo);
		assertEquals(sessionTwo.getSessionId(), snapshotTwo.getSessionId());
		assertEquals(clockTable, snapshotTwo.getClockTable());
		assertEquals(otherUser, snapshotTwo.getCreatedBy());
	}

	@Test
	public void testGetLatestSnapshotDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getLatestSnapshot("doesnotexist"));
	}

	@Test
	public void testGetLatestSnapshotWithMultiple() throws InterruptedException {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		ClockTable clockTable1 = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L)));
		ClockTable clockTable2 = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(20L)));

		// Save snapshots with delays to ensure different created times
		dao.saveSnapshot(session.getSessionId(), clockTable1, "key1", adminUserId);
		Thread.sleep(1001L);
		dao.saveSnapshot(session.getSessionId(), clockTable2, "key2", adminUserId);

		// call under test - should get the latest snapshot
		GridSnapshot latest = dao.getLatestSnapshot(session.getSessionId()).get();
		assertNotNull(latest);
		assertEquals(clockTable2, latest.getClockTable());
		assertEquals("key2", latest.getS3Key());
	}

	@Test
	public void testSaveSnapshotWithNullSessionId() {
		ClockTable clockTable = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L)));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.saveSnapshot(null, clockTable, "key", adminUserId);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testSaveSnapshotWithNullClockTable() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.saveSnapshot(session.getSessionId(), null, "key", adminUserId);
		}).getMessage();
		assertEquals("clockTable is required.", message);
	}

	@Test
	public void testSaveSnapshotWithNullS3Key() {
		GridSession session = dao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		ClockTable clockTable = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L)));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.saveSnapshot(session.getSessionId(), clockTable, null, adminUserId);
		}).getMessage();
		assertEquals("s3Key is required.", message);
	}

	@Test
	public void testGetLatestSnapshotWithNullSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getLatestSnapshot(null);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

}

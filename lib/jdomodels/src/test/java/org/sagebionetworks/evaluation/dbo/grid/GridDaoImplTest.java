package org.sagebionetworks.evaluation.dbo.grid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class GridDaoImplTest {

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	@Autowired
	private GridDao dao;

	private boolean isAgent;
	private EventSource eventSource;

	@BeforeEach
	public void before() {
		isAgent = false;
		eventSource = EventSource.WEBSOCKET;
	}

	@AfterEach
	public void after() {
		dao.truncateAll();
	}

	@Test
	public void testCreateGridSession() {

		// call under test
		GridSession session = dao.createGridSession(adminUserId);
		assertNotNull(session);
		assertEquals(adminUserId.toString(), session.getStartedBy());
		assertNotNull(session.getSessionId());
		assertNotNull(session.getStartedOn());
		assertNotNull(session.getEtag());
		assertEquals(GridConstants.START_REPLICA_ID_CLIENT, session.getLastReplicaIdClient());
		assertEquals(GridConstants.START_REPLICA_ID_SERVICE, session.getLastReplicaIdService());

		// call under test
		GridSession back = dao.geGridSession(session.getSessionId()).get();
		assertEquals(session, back);
	}

	@Test
	public void getGridSessionDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.geGridSession("doesnotexist"));
	}

	@Test
	public void testGetGridSessionStartedBy() {
		GridSession session = dao.createGridSession(adminUserId);
		// call under test
		assertEquals(Optional.of(adminUserId), dao.getGridSessionStartedBy(session.getSessionId()));
	}

	@Test
	public void testGetGridSessionStartedByDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getGridSessionStartedBy("doesnotexist"));
	}

	@Test
	public void testCreateGridReplicaWithWebsocket() throws InterruptedException {
		eventSource = EventSource.WEBSOCKET;
		GridSession session = dao.createGridSession(adminUserId);
		GridSession other = dao.createGridSession(adminUserId);
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
		GridSession updated = dao.geGridSession(session.getSessionId()).get();
		assertNotEquals(updated.getEtag(), session.getEtag());
		assertTrue(updated.getModifiedOn().getTime() > session.getModifiedOn().getTime());
		assertEquals(updated.getLastReplicaIdClient(), session.getLastReplicaIdClient() + 1L);
		// service should not have changed.
		assertEquals(updated.getLastReplicaIdService(), session.getLastReplicaIdService());

		// the other session should not have changed.
		assertEquals(other, dao.geGridSession(other.getSessionId()).get());
	}

	@Test
	public void testCreateGridReplicaWithInternal() throws InterruptedException {
		eventSource = EventSource.INTERNAL;
		isAgent = true;
		GridSession session = dao.createGridSession(adminUserId);
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
		GridSession updated = dao.geGridSession(session.getSessionId()).get();
		assertNotEquals(updated.getEtag(), session.getEtag());
		assertTrue(updated.getModifiedOn().getTime() > session.getModifiedOn().getTime());
		assertEquals(updated.getLastReplicaIdService(), session.getLastReplicaIdService() - 1L);
		// client should not have changed.
		assertEquals(updated.getLastReplicaIdClient(), session.getLastReplicaIdClient());
	}

	@Test
	public void testGetReplica() {
		GridSession session = dao.createGridSession(adminUserId);
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
		GridSession session = dao.createGridSession(adminUserId);
		GridReplica replica = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		// call under test
		assertEquals(Optional.of(adminUserId),
				dao.getReplicaCreatedBy(session.getSessionId(), replica.getReplicaId(), isAgent));
	}

	@Test
	public void testGetReplicaCreatedByWithAgentNoMatch() {
		isAgent = true;
		GridSession session = dao.createGridSession(adminUserId);
		GridReplica replica = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		// call under test
		assertEquals(Optional.empty(), dao.getReplicaCreatedBy(session.getSessionId(), replica.getReplicaId(), false));
	}

	@Test
	public void testGetReplicaCreatedByWithDoesNotExist() {
		// call under test
		assertEquals(Optional.empty(), dao.getReplicaCreatedBy("doesnotexist", 0L, isAgent));
	}

	@ParameterizedTest
	@EnumSource(EventSource.class)
	public void testConnectionCRUD(EventSource source) throws InterruptedException {
		GridSession session = dao.createGridSession(adminUserId);
		GridReplica r1 = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);
		GridReplica r2 = dao.createReplica(adminUserId, session.getSessionId(), isAgent, eventSource);

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

		// call under test
		dao.createConnection(info2);
		// call under test
		GridConnectionInfo f2 = dao.getConnection(info2.getConnectionId()).get();
		assertNotNull(f2.getCreatedOn());
		assertEquals(session.getSessionId(), f2.getSessionId());
		assertEquals(r2.getReplicaId(), f2.getReplicaId());
		assertEquals(adminUserId, f2.getCreatedBy());
		assertEquals(info2.getConnectionId(), f2.getConnectionId());

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
		List<GridConnectionInfo> expected = List.of(f1, f2);
		assertEquals(expected, listed);

		// call under test
		dao.removeConnection(info1.getConnectionId());
		// double delete is allowed
		dao.removeConnection(info1.getConnectionId());
		assertEquals(Optional.empty(), dao.getConnection(info1.getConnectionId()));

		// should still be able to get the second
		assertEquals(f2, dao.getConnection(info2.getConnectionId()).get());
	}

	@Test
	public void testSavePatch() {
		GridSession session = dao.createGridSession(adminUserId);
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L);
		String s3Key = "thekey";
		Duration expires = Duration.ofSeconds(100L);
		// call under test
		assertTrue(dao.savePatch(session.getSessionId(), patchId, s3Key, expires));
		assertFalse(dao.savePatch(session.getSessionId(), patchId, s3Key, expires));

		PatchInfo patch = dao.getPatchInfo(session.getSessionId(), patchId).get();
		assertNotNull(patch);
		assertEquals(session.getSessionId(), patch.getSesisonId());
		assertEquals(patchId, patch.getPatchId());
		assertNotNull(patch.getCreatedOn());
		assertNotNull(patch.getExpiresOn());
		assertTrue(patch.getCreatedOn().getTime() < patch.getExpiresOn().getTime());
		assertEquals(s3Key, patch.getS3Key());

	}

	@Test
	public void testSavePatchWithMultipleSession() {
		GridSession sessionOne = dao.createGridSession(adminUserId);
		GridSession sessionTwo = dao.createGridSession(adminUserId);
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L);
		String s3Key = "thekey";
		Duration expires = Duration.ofSeconds(100L);
		// call under test
		assertTrue(dao.savePatch(sessionOne.getSessionId(), patchId, s3Key, expires));
		assertFalse(dao.savePatch(sessionOne.getSessionId(), patchId, s3Key, expires));
		assertTrue(dao.savePatch(sessionTwo.getSessionId(), patchId, s3Key, expires));
		assertFalse(dao.savePatch(sessionTwo.getSessionId(), patchId, s3Key, expires));

		PatchInfo patchOne = dao.getPatchInfo(sessionOne.getSessionId(), patchId).get();
		assertNotNull(patchOne);
		assertEquals(sessionOne.getSessionId(), patchOne.getSesisonId());
		assertEquals(patchId, patchOne.getPatchId());

		PatchInfo patchTwo = dao.getPatchInfo(sessionTwo.getSessionId(), patchId).get();
		assertNotNull(patchTwo);
		assertEquals(sessionTwo.getSessionId(), patchTwo.getSesisonId());
		assertEquals(patchId, patchTwo.getPatchId());
	}

	@Test
	public void testGetPatchWithNotFound() {
		LogicalTimestamp patchId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L);
		assertEquals(Optional.empty(), dao.getPatchInfo("notfound", patchId));
	}

	@Test
	public void testListMissingPatchs() {
		GridSession sessionOne = dao.createGridSession(adminUserId);
		GridSession sessionTwo = dao.createGridSession(adminUserId);
		Duration expires = Duration.ofSeconds(100L);

		List<LogicalTimestamp> patchIds = createTestPatchIds(3, 4);
		patchIds.stream().forEach(p -> {
			String s3Key = p.toString();
			assertTrue(dao.savePatch(sessionOne.getSessionId(), p, s3Key, expires));
			assertTrue(dao.savePatch(sessionTwo.getSessionId(), p, s3Key, expires));
		});

		// call under test
		List<LogicalTimestamp> list = dao.listMissingPatchIdsForClock(sessionOne.getSessionId(), List.of(), 100);
		// empty clock should return all patches
		assertEquals(patchIds, list);

		// call under test
		list = dao.listMissingPatchIdsForClock(sessionOne.getSessionId(),
				List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(8L),
						new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(8L),
						new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(8L)),
				100);
		// up-to-date should be empty patches
		assertEquals(Collections.emptyList(), list);

		// call under test
		list = dao.listMissingPatchIdsForClock(sessionOne.getSessionId(),
				List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(8L),
						new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(6L),
						new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(4L)),
				100);

		List<LogicalTimestamp> expected = List.of(
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(8L));

		assertEquals(expected, list);

	}

	List<LogicalTimestamp> createTestPatchIds(int replicaCount, int sequenceCount) {
		List<LogicalTimestamp> ids = new ArrayList<>(replicaCount * sequenceCount);
		for (long rep = 1; rep < replicaCount + 1; rep++) {
			for (long seq = 1; seq < sequenceCount + 1; seq++) {
				ids.add(new LogicalTimestamp().setReplicaId(rep).setSequenceNumber(seq * 2));
			}
		}
		return ids;
	}

}

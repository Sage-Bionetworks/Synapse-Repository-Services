package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridReplicaPatchBuilderManagerImplAutowireTest {

	@Autowired
	private GridReplicaPatchBuilderManager patchBuilderManager;
	@Autowired
	private GridManager gridManager;
	@Autowired
	private GridDao gridDao;
	@Autowired
	private GridIndexDao gridIndexDao;
	@Autowired
	private GridIndexManager gridIndexManager;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	private String sessionId;
	private Long replicaId;
	private String connectionId;

	@BeforeEach
	public void before() {
		// an empty index database is what a validation replica sees on a newly released stack
		gridIndexManager.truncateAll();
		GridSession session = gridDao.createGridSession(new CreateGridSession().setUserId(adminUserId));
		sessionId = session.getSessionId();
		GridReplica replica = gridDao.createReplica(adminUserId, sessionId, false, EventSource.VALIDATION);
		replicaId = replica.getReplicaId();
		connectionId = UUID.randomUUID().toString();
		gridDao.createConnection(new GridConnectionInfo().setConnectionId(connectionId).setCreatedBy(adminUserId)
				.setReplicaId(replicaId).setSessionId(sessionId).setSource(EventSource.VALIDATION));
	}

	@AfterEach
	public void after() {
		gridIndexManager.truncateAll();
		if (sessionId != null) {
			gridDao.deleteGridSession(sessionId);
		}
	}

	@Test
	public void testBuildPatchWithEmptyIndexAndStoredPatches() throws IOException {
		// written by this replica on the previous stack: a new constant (seq 10) and a
		// two element array insert (seq 11-12).
		LogicalTimestamp storedPatchId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(10L);
		String storedBody = "[[[" + replicaId + ",10]],[0],[14,10,10,[10,10]]]";
		gridManager.savePatch(sessionId, storedPatchId, storedBody);

		// the source replica's index is also behind the stored patches
		IntendedChangeSet changeSet = new IntendedChangeSet().setSessionId(sessionId).setReplicaId(replicaId)
				.setConnectionId(connectionId).setChanges(List.of()).setClockSequenceMaximum(1L);

		// call under test
		patchBuilderManager.buildPatch(changeSet);

		// the next patch starts after the last stored sequence (12), never reusing one
		assertEquals(Optional.of(12L), gridIndexDao.getClockSequenceNumber(sessionId, replicaId, replicaId));
	}
}

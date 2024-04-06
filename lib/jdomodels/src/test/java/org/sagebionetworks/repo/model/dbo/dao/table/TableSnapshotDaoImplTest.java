package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableSnapshotDaoImplTest {

	@Autowired
	private TableSnapshotDao viewSnapshotDao;

	@Autowired
	private TableRowTruthDAO tableTruthDao;
	
	@Autowired
	private TableTransactionDao tableTransactionDao;
	
	@Autowired
	private TransactionTemplate readCommitedTransactionTemplate;
	
	@Autowired
	private NodeDAO nodeDao;
	
	TableSnapshot tableSnapshot;
	IdAndVersion idAndVersion;
	long adminUserId;

	@BeforeEach
	public void beforeEach() {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		tableTruthDao.truncateAllRowData();
		viewSnapshotDao.truncateAll();
		tableTransactionDao.truncateAll();
		nodeDao.truncateAll();
		
		Node newTable = new Node();
		newTable.setName("a table");
		newTable.setCreatedByPrincipalId(adminUserId);
		newTable.setCreatedOn(new Date());
		newTable.setModifiedByPrincipalId(newTable.getCreatedByPrincipalId());
		newTable.setModifiedOn(newTable.getCreatedOn());
		newTable.setNodeType(EntityType.table);
		
		newTable = nodeDao.createNewNode(newTable);
		
		idAndVersion = IdAndVersion.parse(newTable.getId() + "." + newTable.getVersionNumber());
		
		tableSnapshot = new TableSnapshot()
			.withSnapshotId(111L)
			.withTableId(idAndVersion.getId())
			.withVersion(idAndVersion.getVersion().get())
			.withCreatedBy(adminUserId).withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key");
	}

	@AfterEach
	public void afterEach() {
		tableTruthDao.truncateAllRowData();
		viewSnapshotDao.truncateAll();
		tableTransactionDao.truncateAll();
		nodeDao.truncateAll();
	}

	@Test
	public void testTranslate() {
		// call under test
		DBOTableSnapshot dbo = TableSnapshotDaoImpl.translate(tableSnapshot);
		assertNotNull(dbo);
		// call under test
		TableSnapshot clone = TableSnapshotDaoImpl.translate(dbo);
		assertEquals(tableSnapshot, clone);
	}

	@Test
	public void testCreateSnapshot() {
		tableSnapshot.withSnapshotId(null);
		// call under test
		TableSnapshot result = viewSnapshotDao.createSnapshot(tableSnapshot);
		assertNotNull(result);
		assertNotNull(result.getSnapshotId());
		assertEquals(tableSnapshot.getTableId(), result.getTableId());
		assertEquals(tableSnapshot.getVersion(), result.getVersion());
		assertEquals(tableSnapshot.getCreatedBy(), result.getCreatedBy());
		assertEquals(tableSnapshot.getCreatedOn(), result.getCreatedOn());
		assertEquals(tableSnapshot.getBucket(), result.getBucket());
		assertEquals(tableSnapshot.getKey(), result.getKey());
	}

	@Test
	public void testCreateSnapshotDuplicate() {
		tableSnapshot.withSnapshotId(null);
		TableSnapshot result = viewSnapshotDao.createSnapshot(tableSnapshot);
		assertNotNull(result);
		assertNotNull(result.getSnapshotId());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		}).getMessage();
		assertEquals("Snapshot already exists for: " + idAndVersion.toString(), message);
	}

	@Test
	public void testCreateSnapshotNullViewId() {
		tableSnapshot.withTableId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullVersion() {
		tableSnapshot.withVersion(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullCreatedBy() {
		tableSnapshot.withCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullCreatedOn() {
		tableSnapshot.withCreatedOn(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullBucket() {
		tableSnapshot.withBucket(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullKey() {
		tableSnapshot.withKey(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(tableSnapshot);
		});
	}

	@Test
	public void testGetSnapshot() {
		tableSnapshot.withSnapshotId(null);
		TableSnapshot created = viewSnapshotDao.createSnapshot(tableSnapshot);
		assertNotNull(created);
		// call under test
		TableSnapshot result = viewSnapshotDao.getSnapshot(idAndVersion).get();
		assertEquals(created, result);
	}
	
	@Test
	public void testGetSnapshotNotFound() {
		// call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getSnapshot(idAndVersion);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void testGetSnapshotId() {
		tableSnapshot.withSnapshotId(null);
		TableSnapshot created = viewSnapshotDao.createSnapshot(tableSnapshot);
		assertNotNull(created);
		// call under test
		long id = viewSnapshotDao.getSnapshotId(idAndVersion);
		assertEquals(created.getSnapshotId(), id);
	}
	
	@Test
	public void testGetSnapshotIdNotFound() {
		assertThrows(NotFoundException.class, ()->{
			// call under test
			viewSnapshotDao.getSnapshotId(idAndVersion);
		});
	}
	
	@Test
	public void testGetSnapshotIdNullId() {
		idAndVersion = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			viewSnapshotDao.getSnapshotId(idAndVersion);
		});
	}
	
	@Test
	public void testGetSnapshotIdNullVersion() {
		idAndVersion = IdAndVersion.parse("syn123");
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			viewSnapshotDao.getSnapshotId(idAndVersion);
		});
	}
		
	@Test
	public void testGetMostRecentTableSnapshotWithLatestVersion() {

		String tableId = idAndVersion.getId().toString();
		
		Long snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, false);
		snapshotVersion = addTableChanges(tableId, true);
				
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
		
		addTableChanges(tableId, false);
		
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).build();
		
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotTwo, result.get());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithLatestSnapshot() {
				
		String tableId = idAndVersion.getId().toString();
		
		Long snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
		
		addTableChanges(tableId, false);
		
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).setVersion(snapshotTwo.getVersion()).build();
				
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotTwo, result.get());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithPreviousSnapshot() {
		
		String tableId = idAndVersion.getId().toString();
		
		Long snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, false);
		snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
		
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).setVersion(snapshotOne.getVersion()).build();
				
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotOne, result.get());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithMissingChanges() {
		
		String tableId = idAndVersion.getId().toString();
		
		Long snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, false);
		addTableChanges(tableId, false);
		
		// This is a snapshot whose version in the node does not exist
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(2L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
	
		// The snapshot exists, but there is not version in the tx for it anymore, use the previous snapshot
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).setVersion(snapshotTwo.getVersion()).build();
				
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotOne, result.get());
	}
	
	// Reproduce https://sagebionetworks.jira.com/browse/PLFM-7897
	@Test
	public void testGetMostRecentTableSnapshotWithMissingNodeRevision() {
		
		String tableId = idAndVersion.getId().toString();
		
		Long snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, false);
		snapshotVersion = addTableChanges(tableId, true);
		
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
	
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).setVersion(snapshotTwo.getVersion()).build();
		
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotTwo, result.get());
		
		// Now delete the node revision
		nodeDao.deleteVersion(tableId, snapshotTwo.getVersion());
		
		// The snapshot exists, and there is a version in the tx, but the revision is missing, use the previous snapshot
		result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotOne, result.get());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithNoTableChanges() {
		
		tableSnapshot = viewSnapshotDao.createSnapshot(tableSnapshot);
		
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		// Even though the snapshot exists there is not table tx linked to the version
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithNoSnapshot() {
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);
		assertTrue(result.isEmpty());
	}	

	private Long addTableChanges(String tableId, boolean newTableVersion) {
		final Node table = nodeDao.getNode(tableId);
		
		// We need to wrap all this in tx since linkTransactionToVersion needs a manadatory tx
		readCommitedTransactionTemplate.executeWithoutResult( status -> {
			long txId = tableTransactionDao.startTransaction(tableId, adminUserId);
			
			SparseChangeSetDto changeSet = new SparseChangeSetDto().setRows(Collections.emptyList());
	
			long changeNumber = tableTruthDao.reserveIdsInRange(tableId, 1).getVersionNumber();
			
			tableTruthDao.appendRowSetToTable(String.valueOf(adminUserId), tableId, UUID.randomUUID().toString(), changeNumber, null, changeSet, txId, false);
			
			if (newTableVersion) {
				tableTransactionDao.linkTransactionToVersion(txId, table.getVersionNumber());
				nodeDao.createNewVersion(table.setVersionLabel(null));
			}
		});
		
		return table.getVersionNumber();
	}
}

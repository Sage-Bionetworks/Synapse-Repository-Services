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
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
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
	private TransactionTemplate txTemplate;
	
	TableSnapshot viewSnapshot;
	IdAndVersion idAndVersion;
	long adminUserId;

	@BeforeEach
	public void beforeEach() {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		idAndVersion = IdAndVersion.parse("syn222.1");
		viewSnapshot = new TableSnapshot().withSnapshotId(111L).withTableId(idAndVersion.getId())
				.withVersion(idAndVersion.getVersion().get()).withCreatedBy(adminUserId).withCreatedOn(new Date())
				.withBucket("some bucket").withKey("some key");
		
		tableTruthDao.truncateAllRowData();
		viewSnapshotDao.truncateAll();
		tableTransactionDao.deleteTable(idAndVersion.getId().toString());
	}

	@AfterEach
	public void afterEach() {
		tableTruthDao.truncateAllRowData();
		viewSnapshotDao.truncateAll();
		if (idAndVersion != null) {
			tableTransactionDao.deleteTable(idAndVersion.getId().toString());
		}
	}

	@Test
	public void testTranslate() {
		// call under test
		DBOTableSnapshot dbo = TableSnapshotDaoImpl.translate(viewSnapshot);
		assertNotNull(dbo);
		// call under test
		TableSnapshot clone = TableSnapshotDaoImpl.translate(dbo);
		assertEquals(viewSnapshot, clone);
	}

	@Test
	public void testCreateSnapshot() {
		viewSnapshot.withSnapshotId(null);
		// call under test
		TableSnapshot result = viewSnapshotDao.createSnapshot(viewSnapshot);
		assertNotNull(result);
		assertNotNull(result.getSnapshotId());
		assertEquals(viewSnapshot.getTableId(), result.getTableId());
		assertEquals(viewSnapshot.getVersion(), result.getVersion());
		assertEquals(viewSnapshot.getCreatedBy(), result.getCreatedBy());
		assertEquals(viewSnapshot.getCreatedOn(), result.getCreatedOn());
		assertEquals(viewSnapshot.getBucket(), result.getBucket());
		assertEquals(viewSnapshot.getKey(), result.getKey());
	}

	@Test
	public void testCreateSnapshotDuplicate() {
		viewSnapshot.withSnapshotId(null);
		TableSnapshot result = viewSnapshotDao.createSnapshot(viewSnapshot);
		assertNotNull(result);
		assertNotNull(result.getSnapshotId());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		}).getMessage();
		assertEquals("Snapshot already exists for: syn222.1", message);
	}

	@Test
	public void testCreateSnapshotNullViewId() {
		viewSnapshot.withTableId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullVersion() {
		viewSnapshot.withVersion(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullCreatedBy() {
		viewSnapshot.withCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullCreatedOn() {
		viewSnapshot.withCreatedOn(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullBucket() {
		viewSnapshot.withBucket(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		});
	}

	@Test
	public void testCreateSnapshotNullKey() {
		viewSnapshot.withKey(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			viewSnapshotDao.createSnapshot(viewSnapshot);
		});
	}

	@Test
	public void testGetSnapshot() {
		viewSnapshot.withSnapshotId(null);
		TableSnapshot created = viewSnapshotDao.createSnapshot(viewSnapshot);
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
		viewSnapshot.withSnapshotId(null);
		TableSnapshot created = viewSnapshotDao.createSnapshot(viewSnapshot);
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
		
		addTableChanges(tableId, 1L);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(1L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, null);
		addTableChanges(tableId, 2L);
				
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(2L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
		
		addTableChanges(tableId, null);
		
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).build();
		
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotTwo, result.get());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithLatestSnapshot() {
				
		String tableId = idAndVersion.getId().toString();
		
		addTableChanges(tableId, 1L);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(1L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, 2L);
		
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(2L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some other key")
		);
		
		addTableChanges(tableId, null);
		
		idAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId()).setVersion(snapshotTwo.getVersion()).build();
				
		// Call under test
		Optional<TableSnapshot> result = viewSnapshotDao.getMostRecentTableSnapshot(idAndVersion);

		assertEquals(snapshotTwo, result.get());
	}
	
	@Test
	public void testGetMostRecentTableSnapshotWithPreviousSnapshot() {
		
		String tableId = idAndVersion.getId().toString();
		
		addTableChanges(tableId, 1L);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(1L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, null);
		addTableChanges(tableId, 2L);
		
		TableSnapshot snapshotTwo = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(2L)
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
		
		addTableChanges(tableId, 1L);
		
		TableSnapshot snapshotOne = viewSnapshotDao.createSnapshot(new TableSnapshot()
			.withTableId(idAndVersion.getId())
			.withVersion(1L)
			.withCreatedBy(adminUserId)
			.withCreatedOn(new Date())
			.withBucket("some bucket")
			.withKey("some key")
		);
		
		addTableChanges(tableId, null);
		addTableChanges(tableId, null);
		
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
	
	@Test
	public void testGetMostRecentTableSnapshotWithNoTableChanges() {
		
		viewSnapshot = viewSnapshotDao.createSnapshot(viewSnapshot);
		
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

	private void addTableChanges(String tableId, Long tableVersion) {
		// We need to wrap all this in tx since linkTransactionToVersion needs a manadatory tx
		txTemplate.executeWithoutResult( status -> {
			long txId = tableTransactionDao.startTransaction(tableId, adminUserId);
			
			SparseChangeSetDto changeSet = new SparseChangeSetDto().setRows(Collections.emptyList());
	
			long changeNumber = tableTruthDao.reserveIdsInRange(tableId, 1).getVersionNumber();
			
			tableTruthDao.appendRowSetToTable(String.valueOf(adminUserId), tableId, UUID.randomUUID().toString(), changeNumber, null, changeSet, txId, false);
			
			if (tableVersion != null) {
				tableTransactionDao.linkTransactionToVersion(txId, tableVersion);
			}
		});
	}
}

package org.sagebionetworks.repo.manager.table.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.migration.MigrationManagerImpl;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowChangeUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableRowChangeMigrationListenerTest {
	
	@Autowired
	private MigrationManagerImpl migrationManager;
	
	@Autowired
	private TableRowTruthDAO dao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private Long tableId;
	private Long currentRowVersion;
	
	@BeforeEach
	public void before() {
		tableId = 1L;
		currentRowVersion = 1L;
		dao.truncateAllRowData();
		dao.reserveIdsInRange(tableId.toString(), 10);
	}
	
	@AfterEach
	public void after() {
		dao.truncateAllRowData();
	}
	
	@Test
	public void testRestoreTableChangesWithoutId() {
		
		DBOTableRowChange change1 = generateTableChange(false);
		DBOTableRowChange change2 = generateTableChange(false);
		
		List<DatabaseObject<?>> batch = Arrays.asList(change1, change2);
		
		// Call under test
		migrationManager.restoreBatch(MigrationType.TABLE_CHANGE, batch);
		
		assertNotNull(change1.getId());
		assertNotNull(change2.getId());
		
		List<TableRowChange> expectedChanges = Arrays.asList(TableRowChangeUtils.ceateDTOFromDBO(change1), TableRowChangeUtils.ceateDTOFromDBO(change2));
 		List<TableRowChange> changes = dao.getTableChangePage(tableId.toString(), 10, 0);
 		
 		assertEquals(expectedChanges, changes);
	}
	
	@Test
	public void testRestoreTableChangesWithId() {
		
		DBOTableRowChange change1 = generateTableChange(true);
		DBOTableRowChange change2 = generateTableChange(true);
		
		List<Long> expectedIds = Arrays.asList(change1.getId(), change2.getId());
		
		List<DatabaseObject<?>> batch = Arrays.asList(change1, change2);
		
		// Call under test
		migrationManager.restoreBatch(MigrationType.TABLE_CHANGE, batch);
		
		assertEquals(expectedIds, Arrays.asList(change1.getId(), change2.getId()));
		
		List<TableRowChange> expectedChanges = Arrays.asList(TableRowChangeUtils.ceateDTOFromDBO(change1), TableRowChangeUtils.ceateDTOFromDBO(change2));
 		List<TableRowChange> changes = dao.getTableChangePage(tableId.toString(), 10, 0);
 		
 		assertEquals(expectedChanges, changes);
	}

	@Test
	public void testRestoreTableChangesBatch() {
		
		List<DatabaseObject<?>> batch = IntStream.range(0, 100).boxed().map( i -> generateTableChange(false)).collect(Collectors.toList());
		
		// Call under test
		migrationManager.restoreBatch(MigrationType.TABLE_CHANGE, batch);
		
		List<Long> ids = batch.stream().map( c -> ((DBOTableRowChange) c).getId()).collect(Collectors.toList());
		
		// the whole batch should have an id
		assertEquals(batch.size(), ids.size());
		
		Long lastBatchId = ids.get(ids.size() - 1);
		
		assertEquals(ids.get(0) + ids.size() - 1, lastBatchId);
		
		// Create another batch to check that there are no id collisions
		batch = IntStream.range(0, 50).boxed().map( i -> generateTableChange(false)).collect(Collectors.toList());
		
		migrationManager.restoreBatch(MigrationType.TABLE_CHANGE, batch);
		
		ids = batch.stream().map( c -> ((DBOTableRowChange) c).getId()).collect(Collectors.toList());
		
		// the whole batch should have an id
		assertEquals(batch.size(), ids.size());
		
		assertEquals(lastBatchId + 1, ids.get(0));
		
	}	

	DBOTableRowChange generateTableChange(boolean withId) {
		DBOTableRowChange change = new DBOTableRowChange();
		
		change.setBucket("bucket");
		change.setKeyNew("key");
		change.setChangeType("ROW");
		change.setTableId(tableId);
		change.setRowVersion(currentRowVersion++);
		change.setCreatedBy(1L);
		change.setCreatedOn(System.currentTimeMillis());
		change.setRowCount(10L);
		change.setTransactionId(1L);
		change.setEtag(UUID.randomUUID().toString());
		if (withId) {
			change.setId(idGenerator.generateNewId(IdType.TABLE_CHANGE_ID));
		}
		return change;
	}
	
}

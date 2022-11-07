package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableSnapshotDaoImplTest {

	@Autowired
	private TableSnapshotDao viewSnapshotDao;

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
		
		viewSnapshotDao.truncateAll();
	}

	@AfterEach
	public void afterEach() {
		viewSnapshotDao.truncateAll();
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
}

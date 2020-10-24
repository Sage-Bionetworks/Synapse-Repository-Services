package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnModel;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.IdRange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RangeChecksum;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MigratableTableDAOImplAutowireTest {

	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private ColumnModelDAO columnModelDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private StackConfiguration stackConfiguration;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private MigratableTableDAO migratableTableDAO;
	
	private List<String> filesToDelete;
	
	private String creatorUserGroupId;
	
	@BeforeEach
	public void before() {
		filesToDelete = new LinkedList<String>();
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		assertNotNull(creatorUserGroupId);
	}
	
	@AfterEach
	public void after(){
		if(fileHandleDao != null && filesToDelete != null){
			for(String id: filesToDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
	@Test
	public void testRunWithForeignKeyIgnored() throws Exception{
		final S3FileHandle fh = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh.setFileName("withPreview.txt");
		// This does not exists but we should be able to set while foreign keys are ignored.
		fh.setPreviewId("-123");
		// This should fail
		try{
			fileHandleDao.createFile(fh);
			fail("A foreign key should have prevented this change.");
		}catch(Exception e){
			// expected
		}
		// While the check is off we can violate foreign keys.
		Boolean result = migratableTableDAO.runWithKeyChecksIgnored(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				// We should be able to do this now that foreign keys are disabled.
				S3FileHandle updated = (S3FileHandle) fileHandleDao.createFile(fh);
				filesToDelete.add(updated.getId());
				return true;
			}});
		assertTrue(result);
		
		// This should fail if constraints are back on.
		final S3FileHandle fh2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh2.setFileName("withPreview2.txt");
		// This does not exists but we should be able to set while foreign keys are ignored.
		fh2.setPreviewId("-123");
		// This should fail
		try{
			fileHandleDao.createFile(fh2);
			fail("A foreign key should have prevented this change.");
		}catch(Exception e){
			// expected
		}
	}
	
	/**
	 * Currently this test does not pass. Here is a quote from the MySQL docs:
	 * 
	 * "Setting this variable to 0 does not require storage engines to ignore
	 * duplicate keys. An engine is still permitted to check for them and issue
	 * duplicate-key errors if it detects them"
	 * 
	 * @throws Exception
	 */
	@Disabled
	@Test
	public void testRunWithUniquenessIgnored() throws Exception{
		jdbcTemplate.execute("DROP TABLE IF EXISTS `KEY_TEST`");
		// setup a simple table.
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `KEY_TEST` (" + 
				"  `ID` BIGINT NOT NULL," +
				"  `ETAG` char(36) NOT NULL," + 
				"  PRIMARY KEY (`ID`)," + 
				"  UNIQUE KEY `ETAG` (`ETAG`)" + 
				")");
		// While the check is off we can violate foreign keys.
		Boolean result = migratableTableDAO.runWithKeyChecksIgnored(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				// first row with an etag
				jdbcTemplate.execute("INSERT INTO KEY_TEST VALUES ('1','E1')");
				// new row with duplicate etag
				jdbcTemplate.execute("INSERT INTO KEY_TEST VALUES ('2','E1')");
				return true;
			}});
		assertTrue(result);
		assertEquals(new Long(2), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KEY_TEST", Long.class));
		jdbcTemplate.execute("DROP TABLE `KEY_TEST`");
	}
	
	@Deprecated
	@Test
	public void testGetMigrationTypeCount() throws Exception {
		long startCount = fileHandleDao.getCount();
		assertEquals(startCount, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setFileName("handle");
		handle = (S3FileHandle) fileHandleDao.createFile(handle);
		filesToDelete.add(handle.getId());
		assertEquals(startCount+1, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
		fileHandleDao.delete(handle.getId());
		assertEquals(startCount, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
	}
	
	@Test
	public void testGetMigrationTypeCountForType() {
		long startCount = fileHandleDao.getCount();
		assertEquals(startCount, migratableTableDAO.getMigrationTypeCount(MigrationType.FILE_HANDLE).getCount().longValue());
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setFileName("handle");
		handle = (S3FileHandle) fileHandleDao.createFile(handle);
		filesToDelete.add(handle.getId());
		assertEquals(startCount+1, migratableTableDAO.getMigrationTypeCount(MigrationType.FILE_HANDLE).getCount().longValue());
		fileHandleDao.delete(handle.getId());
		assertEquals(startCount, migratableTableDAO.getMigrationTypeCount(MigrationType.FILE_HANDLE).getCount().longValue());
	}
	
	@Test
	public void testGetMigrationTypeCountForTypeNoData() {
		MigrationTypeCount mtc = migratableTableDAO.getMigrationTypeCount(MigrationType.VERIFICATION_SUBMISSION);
		assertNotNull(mtc);
		assertNotNull(mtc.getCount());
		assertEquals(0L, mtc.getCount().longValue());
		assertNull(mtc.getMaxid());
		assertNull(mtc.getMinid());
		assertNotNull(mtc.getType());
		assertEquals(MigrationType.VERIFICATION_SUBMISSION, mtc.getType());
	}
	
	@Test
	public void testGetChecksumForType() throws Exception {
		// Start checksum
		String checksum1 = migratableTableDAO.getChecksumForType(MigrationType.FILE_HANDLE);
		// Add file handle
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle1.setFileName("handle1");
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		filesToDelete.add(handle1.getId());
		// Checksum again
		String checksum2 = migratableTableDAO.getChecksumForType(MigrationType.FILE_HANDLE);
		// Test
		assertFalse(checksum1.equals(checksum2));
		// Delete file handle
		fileHandleDao.delete(handle1.getId());
		checksum2 = migratableTableDAO.getChecksumForType(MigrationType.FILE_HANDLE);
		// Test
		assertEquals(checksum1, checksum2);
	}
	
	@Test
	public void testGetChecksumForIdRange1() throws Exception {
		long startId = fileHandleDao.getMaxId() + 1;
		long startCount = fileHandleDao.getCount();

		// Add file handle
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle1.setFileName("handle1");
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		filesToDelete.add(handle1.getId());
		// Add a preview
		CloudProviderFileHandleInterface previewHandle1 = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewHandle1.setFileName("preview1");
		previewHandle1 = (CloudProviderFileHandleInterface) fileHandleDao.createFile(previewHandle1);
		filesToDelete.add(previewHandle1.getId());
		
		// Checksum file only
		String checksum1 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", startId, Long.parseLong(handle1.getId()));
		// Checksum file and preview
		String checksum2 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", startId, Long.parseLong(previewHandle1.getId()));
		// Test
		assertFalse(checksum1.equals(checksum2));
		// Checksum preview only
		String checksum3 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(handle1.getId())+1, Long.parseLong(previewHandle1.getId()));
		// Test
		assertFalse(checksum1.equals(checksum3));
		assertFalse(checksum2.equals(checksum3));
		// Different salt
		String checksum4 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "Y", startId, Long.parseLong(handle1.getId()));
		assertFalse(checksum4.equals(checksum1));
		String checksum5 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "Y", startId, Long.parseLong(previewHandle1.getId()));
		assertFalse(checksum5.equals(checksum2));
		// Different way to specify range
		String checksum6 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(handle1.getId()), Long.parseLong(previewHandle1.getId()));
		assertEquals(checksum2, checksum6);
		checksum6 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(handle1.getId()), Long.parseLong(previewHandle1.getId())+100);
		assertEquals(checksum2, checksum6);
		// Empty range
		String checksum7 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(previewHandle1.getId())+1, Long.parseLong(previewHandle1.getId())+100);
		assertNull(checksum7);
		
	}
	
	@Test
	public void testGetChecksumForType2() throws Exception {
		// Before
		UserProfile profile = userProfileDAO.get(creatorUserGroupId);
		String etag1 = profile.getEtag();
		String checksum1 = migratableTableDAO.getChecksumForType(MigrationType.USER_PROFILE);

		// Update
		profile.setCompany("newCompany");
		profile = userProfileDAO.update(profile);
		String etag2 = profile.getEtag();
		String checksum2 = migratableTableDAO.getChecksumForType(MigrationType.USER_PROFILE);
		
		// Test
		assertFalse(etag1.equals(etag2));
		assertFalse(checksum1.equals(checksum2));
		
	}

	@Test
	public void testGetChecksumForRange2() throws Exception {
		// Before
		UserProfile profile = userProfileDAO.get(creatorUserGroupId);
		String etag1 = profile.getEtag();
		String checksum1 = migratableTableDAO.getChecksumForIdRange(MigrationType.USER_PROFILE, "X", Long.parseLong(profile.getOwnerId()), Long.parseLong(profile.getOwnerId()));

		// Update
		profile.setCompany("newCompany");
		profile = userProfileDAO.update(profile);
		String etag2 = profile.getEtag();
		String checksum2 = migratableTableDAO.getChecksumForIdRange(MigrationType.USER_PROFILE, "X", Long.parseLong(profile.getOwnerId()), Long.parseLong(profile.getOwnerId()));
		
		// Test
		assertFalse(etag1.equals(etag2));
		assertFalse(checksum1.equals(checksum2));
		
	}
	
	@Test
	public void testGetChecksumForRangeNoData() {
		String checksum = migratableTableDAO.getChecksumForIdRange(MigrationType.VERIFICATION_SUBMISSION, "salt", 0, 10);
		assertNull(checksum);
	}
	
	@Test
	public void testAllMigrationTypesRegistered() {
		for (MigrationType t: MigrationType.values()) {
			assertTrue(migratableTableDAO.isMigrationTypeRegistered(t));
		}
	}
	
	@Test
	public void testListNonRestrictedForeignKeys() {
		List<ForeignKeyInfo> keys = migratableTableDAO.listNonRestrictedForeignKeys();
		assertNotNull(keys);
		assertTrue(keys.size() > 1);
		ForeignKeyInfo info = keys.get(0);
		assertNotNull(info.getConstraintName());
		assertNotNull(info.getDeleteRule());
		assertNotNull(info.getReferencedTableName());
		assertNotNull(info.getTableName());
	}
	
	@Test
	public void testStreamDatabaseObjectsByRange() {
		List<Long> ids = new LinkedList<>();
		ColumnModel one = new ColumnModel();
		one.setColumnType(ColumnType.INTEGER);
		one.setName("one");
		one  = columnModelDao.createColumnModel(one);
		ids.add(Long.parseLong(one.getId()));
		
		ColumnModel two = new ColumnModel();
		two.setColumnType(ColumnType.INTEGER);
		two.setName("two");
		two = columnModelDao.createColumnModel(two);
		ids.add(Long.parseLong(two.getId()));
		
		ColumnModel three = new ColumnModel();
		three.setColumnType(ColumnType.INTEGER);
		three.setName("three");
		three = columnModelDao.createColumnModel(three);
		ids.add(Long.parseLong(three.getId()));
		
		// Stream over the results
		long minId = ids.get(0);
		long maxId = ids.get(1);
		long batchSize  = 1;
		Iterable<MigratableDatabaseObject<?,?>> it = migratableTableDAO.streamDatabaseObjects(MigrationType.COLUMN_MODEL, minId, maxId, batchSize);
		List<DBOColumnModel> results = new LinkedList<>();
		for(DatabaseObject data: it) {
			assertTrue(data instanceof DBOColumnModel);
			results.add((DBOColumnModel) data);
		}
		// third should be excluded.
		assertEquals(2, results.size());
		assertEquals(one.getId(), results.get(0).getId().toString());
		assertEquals(two.getId(), results.get(1).getId().toString());
	}
	
	@Test
	public void testDeleteByRange() {
		List<Long> ids = new LinkedList<>();
		ColumnModel one = new ColumnModel();
		one.setColumnType(ColumnType.INTEGER);
		one.setName("one");
		one  = columnModelDao.createColumnModel(one);
		ids.add(Long.parseLong(one.getId()));
		
		ColumnModel two = new ColumnModel();
		two.setColumnType(ColumnType.INTEGER);
		two.setName("two");
		two = columnModelDao.createColumnModel(two);
		ids.add(Long.parseLong(two.getId()));
		
		ColumnModel three = new ColumnModel();
		three.setColumnType(ColumnType.INTEGER);
		three.setName("three");
		three = columnModelDao.createColumnModel(three);
		ids.add(Long.parseLong(three.getId()));
		
		// Stream over the results
		long minId = ids.get(0);
		long maxId = ids.get(1);
		// should exclude last row since max is exclusive
		int count = migratableTableDAO.deleteByRange(MigrationType.COLUMN_MODEL, minId, maxId);
		assertEquals(2, count);
		// add one to the max to delete the last.
		count = migratableTableDAO.deleteByRange(MigrationType.COLUMN_MODEL, minId, maxId+1);
		assertEquals(1, count);
	}
	
	@Test
	public void testCreateOrUpdate() {
		// Note: Principal does not need to exists since foreign keys will be off.
		DBOCredential one = new DBOCredential();
		one.setPassHash("hash1");
		one.setPrincipalId(111L);
		one.setSecretKey("secrete1");
		
		DBOCredential two = new DBOCredential();
		two.setPassHash("hash2");
		two.setPrincipalId(222L);
		two.setSecretKey("secrete2");
		
		List<DatabaseObject<?>> batch = Lists.newArrayList(two, one);
		MigrationType type = MigrationType.CREDENTIAL;
		List<Long> ids = migratableTableDAO.createOrUpdate(type, batch);
		assertNotNull(ids);
		assertEquals(batch.size(), ids.size());
		assertEquals(two.getPrincipalId(), ids.get(0));
		assertEquals(one.getPrincipalId(), ids.get(1));
		
		// Update the values and call it again.
		one.setPassHash("updatedHash1");
		two.setPassHash("updatedHash2");
		ids = migratableTableDAO.createOrUpdate(type, batch);
		assertNotNull(ids);
	}
	
	@Test
	public void testGetPrimaryCardinalitySql() {
		String expected = 
				"SELECT P0.ID, 1  + T0.CARD AS CARD"
				+ " FROM JDONODE AS P0"
				+ " JOIN"
				+ " (SELECT P.ID, + COUNT(S.OWNER_NODE_ID) AS CARD"
				+ " FROM JDONODE AS P"
				+ " LEFT JOIN JDOREVISION AS S ON (P.ID =  S.OWNER_NODE_ID)"
				+ " WHERE P.ID >= :BMINID AND P.ID <= :BMAXID GROUP BY P.ID) T0"
				+ " ON (P0.ID = T0.ID)"
				+ " WHERE P0.ID >= :BMINID AND P0.ID <= :BMAXID"
				+ " ORDER BY P0.ID ASC";
		String sql = migratableTableDAO.getPrimaryCardinalitySql(MigrationType.NODE);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetPrimaryCardinalitySqlPLFM_4857() {
		String expected = 
				"SELECT P0.ID, 1  + T0.CARD + T1.CARD AS CARD"
				+ " FROM V2_WIKI_PAGE AS P0"
				+ " JOIN"
				+ " (SELECT P.ID, + COUNT(S.WIKI_ID) AS CARD FROM V2_WIKI_PAGE AS P"
				+ " LEFT JOIN V2_WIKI_ATTACHMENT_RESERVATION AS S"
				+ " ON (P.ID =  S.WIKI_ID)"
				+ " WHERE P.ID >= :BMINID AND P.ID <= :BMAXID GROUP BY P.ID) T0"
				+ " ON (P0.ID = T0.ID)"
				+ " JOIN"
				+ " (SELECT P.ID, + COUNT(S.WIKI_ID) AS CARD FROM V2_WIKI_PAGE AS P"
				+ " LEFT JOIN V2_WIKI_MARKDOWN AS S"
				+ " ON (P.ID =  S.WIKI_ID)"
				+ " WHERE P.ID >= :BMINID AND P.ID <= :BMAXID GROUP BY P.ID) T1"
				+ " ON (P0.ID = T1.ID)"
				+ " WHERE P0.ID >= :BMINID AND P0.ID <= :BMAXID"
				+ " ORDER BY P0.ID ASC";
		String sql = migratableTableDAO.getPrimaryCardinalitySql(MigrationType.V2_WIKI_PAGE);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testCalculateRangesForType() {
		List<Long> ids = new LinkedList<>();
		ColumnModel one = new ColumnModel();
		one.setColumnType(ColumnType.INTEGER);
		one.setName("one");
		one  = columnModelDao.createColumnModel(one);
		ids.add(Long.parseLong(one.getId()));
		
		ColumnModel two = new ColumnModel();
		two.setColumnType(ColumnType.INTEGER);
		two.setName("two");
		two = columnModelDao.createColumnModel(two);
		ids.add(Long.parseLong(two.getId()));
		
		ColumnModel three = new ColumnModel();
		three.setColumnType(ColumnType.INTEGER);
		three.setName("three");
		three = columnModelDao.createColumnModel(three);
		ids.add(Long.parseLong(three.getId()));
		
		MigrationType migrationType = MigrationType.COLUMN_MODEL;
		long minimumId = ids.get(0);
		long maximumId = ids.get(2)+1;
		long optimalNumberOfRows = 2;
		// call under test
		List<IdRange> range = migratableTableDAO.calculateRangesForType(migrationType, minimumId, maximumId, optimalNumberOfRows);
		// one should include the first two rows
		IdRange expectedOne = new IdRange();
		expectedOne.setMinimumId(ids.get(0));
		expectedOne.setMaximumId(ids.get(1)+1);
		// two should include the third row.
		IdRange expectedTwo = new IdRange();
		expectedTwo.setMinimumId(ids.get(2));
		expectedTwo.setMaximumId(ids.get(2)+1);
		List<IdRange> expected = Lists.newArrayList(expectedOne, expectedTwo);
		assertEquals(expected, range);
	}
	
	@Test
	public void testCalculateBatchChecksums() {
		// Create files with a predictable ID range.
		idGenerator.reserveId(10L, IdType.FILE_IDS);
		fileHandleDao.truncateTable();
		List<Long> files = new LinkedList<>();
		for (int i = 0; i < 10; i++) {
			S3FileHandle file = TestUtils.createS3FileHandle(creatorUserGroupId,
					""+i);
			file = (S3FileHandle) fileHandleDao.createFile(file);
			files.add(Long.parseLong(file.getId()));
			filesToDelete.add(file.getId());
		}
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(files.get(0));
		request.setMaximumId(files.get(9));
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		// call under test
		List<RangeChecksum> range = this.migratableTableDAO.calculateBatchChecksums(request);
		assertNotNull(range);
		assertEquals(4, range.size());
		
		Long firstBinNumber = request.getMinimumId()/request.getBatchSize();
		// zero
		RangeChecksum sum = range.get(0);
		assertNotNull(sum);
		assertEquals(firstBinNumber, sum.getBinNumber());
		assertEquals(files.get(0), sum.getMinimumId());
		assertEquals(files.get(2), sum.getMaximumId());
		assertEquals(request.getBatchSize(), sum.getCount());
		assertNotNull(sum.getChecksum());
		// one
		sum = range.get(1);
		assertNotNull(sum);
		assertEquals(new Long(firstBinNumber+1L), sum.getBinNumber());
		assertEquals(files.get(3), sum.getMinimumId());
		assertEquals(files.get(5), sum.getMaximumId());
		assertEquals(request.getBatchSize(), sum.getCount());
		assertNotNull(sum.getChecksum());
		// two
		sum = range.get(2);
		assertNotNull(sum);
		assertEquals(new Long(firstBinNumber+2L), sum.getBinNumber());
		assertEquals(files.get(6), sum.getMinimumId());
		assertEquals(files.get(8), sum.getMaximumId());
		assertEquals(request.getBatchSize(), sum.getCount());
		assertNotNull(sum.getChecksum());
		// three
		sum = range.get(3);
		assertNotNull(sum);
		assertEquals(new Long(firstBinNumber+3L), sum.getBinNumber());
		assertEquals(files.get(9), sum.getMinimumId());
		assertEquals(files.get(9), sum.getMaximumId());
		assertEquals(new Long(1), sum.getCount());
		assertNotNull(sum.getChecksum());
	}
	
	@Test
	public void testCalculateBatchChecksumsEmptyTable() {
		fileHandleDao.truncateTable();
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		// call under test
		List<RangeChecksum> range = this.migratableTableDAO.calculateBatchChecksums(request);
		assertNotNull(range);
		assertTrue(range.isEmpty());
	}
	
	@Test
	public void testCalculateBatchChecksumsRequestNull() {
		BatchChecksumRequest request = null;
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			this.migratableTableDAO.calculateBatchChecksums(request);
		});
	}
	
	@Test
	public void testCalculateBatchChecksumsNullBatch() {
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(null);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			this.migratableTableDAO.calculateBatchChecksums(request);
		});
	}
	
	@Test
	public void testCalculateBatchChecksumsNullMin() {
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(null);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			this.migratableTableDAO.calculateBatchChecksums(request);
		});
	}
	
	@Test
	public void testCalculateBatchChecksumsNullMax() {
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(null);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			this.migratableTableDAO.calculateBatchChecksums(request);
		});
	}
	
	@Test
	public void testCalculateBatchChecksumsNullSalt() {
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt(null);
		request.setMigrationType(MigrationType.FILE_HANDLE);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			this.migratableTableDAO.calculateBatchChecksums(request);
		});
	}
	
	@Test
	public void testCalculateBatchChecksumsNullType() {
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(null);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			this.migratableTableDAO.calculateBatchChecksums(request);
		});
	}
	
	/**
	 * Types that cannot be tested due to the use of objects to blobs.
	 */
	public static HashSet<MigrationType> UNTESTABLE_TYPES = Sets.newHashSet(MigrationType.STORAGE_LOCATION,
			MigrationType.PROJECT_SETTINGS,
			MigrationType.QUIZ_RESPONSE,
			MigrationType.VERIFICATION_SUBMISSION);
	
	/**
	 * This is a test that validates the a DBO's FieldColumn[] directly map the DBO's row RowMapper.
	 * @throws SQLException
	 */
	@Test
	public void testTranslateAllTypes() throws SQLException{
		for(MigrationType type: MigrationType.values()) {
			if(UNTESTABLE_TYPES.contains(type)) {
				System.out.println("Cannot test translation of type: "+type.name());
				continue;
			}
			MigratableDatabaseObject migratableObject = migratableTableDAO.getObjectForType(type);
			// The sample DBO will have values for all fields defined as FieldColumns.
			DatabaseObject<?> sample = DBOTestUtils.createSampleObjectForType(migratableObject);
			// Wrap the sample in a ResultSet proxy.
			ResultSet resultSetProxy = ResultSetProxy.createProxy(migratableObject.getTableMapping().getFieldColumns(), sample);
			// Use the ResultSet proxy to create a clone of the sample object.
			DatabaseObject<?> clone;
			try {
				clone = (DatabaseObject<?>) migratableObject.getTableMapping().mapRow(resultSetProxy, 1);
				// The clone should match the original sample DBO.
				assertEquals(sample, clone);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed to translate: "+type+" message: "+e.getMessage());
			}
		}
	}
	
	/**
	 * Test added for PLFM-6131.  In that case, updated data was not migrating because the etag
	 * column was not marked as "etag".  Therefore, the migration system did not detect the changes
	 * so they were not migrated.
	 */
	@Test
	public void testEtagColumn() {
		for(MigrationType type: MigrationType.values()) {
			MigratableDatabaseObject migratableObject = migratableTableDAO.getObjectForType(type);
			TableMapping tableMapping = migratableObject.getTableMapping();
			for(FieldColumn field: tableMapping.getFieldColumns()) {
				// This is not a synapse etag, but the S3 etag
				if (MigrationType.MULTIPART_UPLOAD.equals(type) && field.getFieldName().equals("sourceFileEtag")) {
					continue;
				}
				if(field.getFieldName().toLowerCase().contains("etag")) {
					assertTrue(field.isEtag(), "MigrationType: "+type+" has a field containing 'etag' but isEtag() is false");
				}
			}
		}
	}
	
	@Test
	public void testBackupColumnValidationWithMissingUniqueness() throws SQLException {
		// We use the implemenation as to access the validation method
		MigratableTableDAOImpl daoImpl = new MigratableTableDAOImpl(jdbcTemplate, stackConfiguration);
		
		String tableName = "BACKUP_VALIDATION_TEST";
		
		jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
		
		// setup a simple table.
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + tableName + "` (" + 
				"  `ID` varchar(20) NOT NULL" + 
				")");
		
		// Setup the corresponding table mapping
		TableMapping tableMapping = new TableMapping () {

			@Override
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}

			@Override
			public String getTableName() {
				return tableName;
			}

			@Override
			public String getDDLFileName() {
				return null;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return new FieldColumn[] { 
						new FieldColumn("id", "ID", true).withIsBackupId(true)
				};
			}

			@Override
			public Class getDBOClass() {
				return null;
			}
		};
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// Call under test
			daoImpl.validateBackupColumn(tableMapping);
		});
		
		assertEquals("BackupId columns must have a uniqueness constraint.  Could not find such a constraint for table: BACKUP_VALIDATION_TEST column: ID", ex.getMessage());
		
	}
	
	@Test
	public void testBackupColumnValidationWithWrongType() throws SQLException {
		// We use the implemenation as to access the validation method
		MigratableTableDAOImpl daoImpl = new MigratableTableDAOImpl(jdbcTemplate, stackConfiguration);
		
		String tableName = "BACKUP_VALIDATION_TEST";
		
		jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
		
		// setup a simple table.
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + tableName + "` (" + 
				"  `ID` varchar(20) NOT NULL," +
				"  PRIMARY KEY (`ID`)" +  
				")");
		
		// Setup the corresponding table mapping
		TableMapping tableMapping = new TableMapping () {

			@Override
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return null;
			}

			@Override
			public String getTableName() {
				return tableName;
			}

			@Override
			public String getDDLFileName() {
				return null;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return new FieldColumn[] { 
						new FieldColumn("id", "ID", true).withIsBackupId(true)
				};
			}

			@Override
			public Class getDBOClass() {
				return null;
			}
		};
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()->{
			// Call under test
			daoImpl.validateBackupColumn(tableMapping);
		});
		
		assertEquals("Backup columns must be of \"bigint\" type. Found varchar for table: BACKUP_VALIDATION_TEST column: ID", ex.getMessage());
		
	}
	

}

package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.DBOSubjectAccessRequirementBackup;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

/**
 * The Unit test for MigrationManagerImpl;
 * @author jmhill
 *
 */
public class MigrationManagerImplTest {
	
	private MigratableTableDAO mockDao;
	MigrationManagerImpl manager;
	
	@Before
	public void before(){
		mockDao = Mockito.mock(MigratableTableDAO.class);
		manager = new MigrationManagerImpl(mockDao, 10);
	}
	
	@Test
	public void testGetBackupDataBatched(){
		// Set the batch size to be two for this test
		manager.setBackupBatchMax(2);
		// This is the full list of data we expect
		List<Long> fullList = Arrays.asList(1l,2l,3l,4l,5l);
		List<StubDatabaseObject> expected = createExpected(fullList);
		List<Long> batchOne = Arrays.asList(1l,2l);
		List<Long> batchTwo = Arrays.asList(3l,4l);
		List<Long> batchThree = Arrays.asList(5l);
		// We expect the dao to be called three times, once for each batch.
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchOne)).thenReturn(expected.subList(0, 2));
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchTwo)).thenReturn(expected.subList(2, 4));
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchThree)).thenReturn(expected.subList(4, 5));
		// Make the real call
		List<StubDatabaseObject> resutls = manager.getBackupDataBatched(StubDatabaseObject.class, fullList);
		assertNotNull(resutls);
		// The result should match the expected
		assertEquals(expected, resutls);
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSubjectAccessRequirementRoundTrip() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		DBOSubjectAccessRequirement sar1 = new DBOSubjectAccessRequirement();
		sar1.setAccessRequirementId(101L);
		sar1.setSubjectId(987L);
		sar1.setSubjectType("ENTITY");
		{
			List<DBOSubjectAccessRequirement> databaseList = Arrays.asList(new DBOSubjectAccessRequirement[]{sar1});
			// Translate to the backup objects
			MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirementBackup> translator = 
				sar1.getTranslator();
			List<DBOSubjectAccessRequirementBackup> backupList = new LinkedList<DBOSubjectAccessRequirementBackup>();
			for(DBOSubjectAccessRequirement dbo: databaseList){
				backupList.add(translator.createBackupFromDatabaseObject(dbo));
			}
			// Now write the backup list to the stream
			// we use the table name as the Alias
			String alias = sar1.getTableMapping().getTableName();
			// Now write the backup to the stream
			BackupMarshalingUtils.writeBackupToWriter(backupList, alias, writer);
			writer.close();
		}
		
		// now read back in
		{
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			DBOSubjectAccessRequirement sar2 = new DBOSubjectAccessRequirement();
			String alias = sar2.getTableMapping().getTableName();
			List<DBOSubjectAccessRequirementBackup> backupList = 
				(List<DBOSubjectAccessRequirementBackup>) BackupMarshalingUtils.readBackupFromStream(sar2.getBackupClass(), alias, in);
			assertTrue(backupList!=null);
			assertTrue(!backupList.isEmpty());
			// Now translate from the backup objects to the database objects.
			MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirementBackup> translator = sar2.getTranslator();
			List<DBOSubjectAccessRequirement> databaseList = new LinkedList<DBOSubjectAccessRequirement>();
			for(DBOSubjectAccessRequirementBackup backup: backupList){
				databaseList.add(translator.createDatabaseObjectFromBackup(backup));
			}
			// check content
			assertEquals(1, databaseList.size());
			assertEquals(sar1, databaseList.iterator().next());
		}
		
	}

	/**
	 * This test is used during migrating the DBORevision from stack 99 to stack 100.
	 * The old DBORevision contains reference and references.
	 * The new DBORevision only contains reference and does not contain references.
	 * The purpose of this test is to make sure that MigrationManagerImpl.createOrUpdateBatch()
	 * successfully migrates the old object to the new one.
	 * 
	 * For more information, please see PLFM-3499.
	 * @throws IOException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void createOrUpdateBatchOfDBORevisionTestPLFM3499() throws IOException {
		MigratableDatabaseObject mdo = new DBORevision();
		String alias = mdo.getTableMapping().getTableName();

		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(1L);

		String xml = 
				"<linked-list>" +
					"<JDOREVISION>" +
						"<owner>4490</owner>" +
						"<revisionNumber>1</revisionNumber>" +
						"<label>0.0.0</label>" +
						"<comment>0.0.0</comment>" +
						"<modifiedBy>273954</modifiedBy>" +
						"<modifiedOn>1312679694610</modifiedOn>" +
						"<annotations>H4sIAAAAAAAAALPJS8xN1S0uSExOteNSULDJTSwA0UBWal5JUSWEDeQVlxRl5qXbObq4eIZ4+vs5" +
								"+tjoQ4VgKhLz8vJLEksy8/OKYWJwfY4IOX0kyZT80qScVBySOfm49SWW4NKVlJOfhE3KRh/DfTb6" +
								"SF7E7t2AIE9fx6DIYeVXG31wHNvoI8U8AD9tTV8GAgAA</annotations>" +
						"<reference>H4sIAAAAAAAAALPJL0rXK05MT03KzM9LLSnPL8ou1itKLcjXy81PSc3RC0pNSy1KzUtOteNSULAp" +
								"SSxKTy3xTLEzNDK20YfzEFJhqUXFQIP8SnOTUovsDGFqUIW5bPSJthUA9SLq6KAAAAA=</reference>" +
					"</JDOREVISION>" +
				"</linked-list>";
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));

		List<DBORevision> backupList = (List<DBORevision>) BackupMarshalingUtils.readBackupFromStream(mdo.getBackupClass(), alias, in);
		assertNotNull(backupList);
		assertEquals(backupList.size(), 1);

		MigratableTableTranslation<DBORevision, DBORevision> translator = mdo.getTranslator();
		DBORevision backup = backupList.get(0);
		assertNotNull(backup.getReference());

		DBORevision databaseObject = translator.createDatabaseObjectFromBackup(backup);
		assertNotNull(databaseObject);
		assertEquals(ref, JDOSecondaryPropertyUtils.decompressedReference(databaseObject.getReference()));
	}

	/**
	 * Build a list of objects from a list of IDs
	 * @param fullList
	 * @return
	 */
	private List<StubDatabaseObject> createExpected(List<Long> fullList) {
		List<StubDatabaseObject> result = new LinkedList<StubDatabaseObject>();
		for(Long id: fullList){
			result.add(new StubDatabaseObject(id));
		}
		return result;
	}

	/**
	 * Simple Stub DatabaseObject for testing.
	 * @author jmhill
	 *
	 */
	public static class StubDatabaseObject implements DatabaseObject<StubDatabaseObject>{
		
		Long id;
		
		public StubDatabaseObject(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public TableMapping<StubDatabaseObject> getTableMapping() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StubDatabaseObject other = (StubDatabaseObject) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "StubDatabaseObject [id=" + id + "]";
		}
		
	}

}

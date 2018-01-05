package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class BackupFileStreamImplTest {
	
	@Mock
	MigrationTypeProvider mockTypeProvider;
	
	BackupFileStreamImpl backupFileStream;

	@Before
	public void before() {
		backupFileStream = new BackupFileStreamImpl();
		ReflectionTestUtils.setField(backupFileStream, "typeProvider", mockTypeProvider);
		
		when(mockTypeProvider.getObjectForType(MigrationType.ACL)).thenReturn(new DBOAccessControlList());
		when(mockTypeProvider.getObjectForType(MigrationType.ACL_ACCESS)).thenReturn(new DBOResourceAccess());
		when(mockTypeProvider.getObjectForType(MigrationType.ACL_ACCESS_TYPE)).thenReturn(new DBOResourceAccessType());
		when(mockTypeProvider.getObjectForType(MigrationType.NODE)).thenReturn(new DBONode());
		when(mockTypeProvider.getObjectForType(MigrationType.NODE_REVISION)).thenReturn(new DBORevision());
	}
	
	@Test
	public void testLegacyMigrationBackupFile() {
		// Read a legacy back file.
		String fileName = "LegacyMigrationBackupACL.zip";
		InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on classplath",input);
		try {
			List<RowData> readData = new LinkedList<>();
			// call under test
			Iterable<RowData> iterator = backupFileStream.readBackupFile(input, BackupAliasType.TABLE_NAME);
			assertNotNull(iterator);
			for(RowData row: iterator) {
				readData.add(row);
			}
			// Validate the expected contents.
			assertEquals(38, readData.size());
			// The first five entries should be for ACL
			RowData row = readData.get(0);
			assertEquals(MigrationType.ACL, row.getType());
			assertTrue(row.getDatabaseObject() instanceof DBOAccessControlList);
			DBOAccessControlList dboACL = (DBOAccessControlList) row.getDatabaseObject();
			assertEquals(new Long(1242), dboACL.getId());
			assertEquals(new Long(4489), dboACL.getOwnerId());
			// four
			row = readData.get(4);
			assertEquals(MigrationType.ACL, row.getType());
			assertTrue(row.getDatabaseObject() instanceof DBOAccessControlList);
			
			// Next should be DBOResourceAccess
			row = readData.get(5);
			assertEquals(MigrationType.ACL_ACCESS, row.getType());
			assertTrue(row.getDatabaseObject() instanceof DBOResourceAccess);
			DBOResourceAccess dboRA = (DBOResourceAccess) row.getDatabaseObject();
			assertEquals(new Long(1242),dboRA.getOwner());
			
			// The rest of the data should be DBOResourceAccessType
			row = readData.get(10);
			assertEquals(MigrationType.ACL_ACCESS_TYPE, row.getType());
			assertTrue(row.getDatabaseObject() instanceof DBOResourceAccessType);
			DBOResourceAccessType dboType = (DBOResourceAccessType) row.getDatabaseObject();
			assertEquals(new Long(1242), dboType.getOwner());
			
		}finally {
			IOUtils.closeQuietly(input);
		}
	}
	
	
	/**
	 * An empty file should be skipped but it should not terminate the read.
	 */
	@Test
	public void testMigrationBackupFileWithEmptyFile() {
		// Read a legacy back file.
		String fileName = "MigrationBackupWithEmptyFile.zip";
		InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on classplath",input);
		try {
			List<RowData> readData = new LinkedList<>();
			// call under test
			Iterable<RowData> iterator = backupFileStream.readBackupFile(input, BackupAliasType.TABLE_NAME);
			assertNotNull(iterator);
			for(RowData row: iterator) {
				readData.add(row);
			}
			// Validate the expected contents.
			assertEquals(6, readData.size());
			// The first three are Nodes
			RowData row = readData.get(0);
			assertEquals(MigrationType.NODE, row.getType());
			// Last three are revisions.
			row = readData.get(3);
			assertEquals(MigrationType.NODE_REVISION, row.getType());
		}finally {
			IOUtils.closeQuietly(input);
		}
	}
	
	@Test
	public void testCreateFileName() {
		// call under test
		String name = BackupFileStreamImpl.createFileName(MigrationType.ACCESS_REQUIREMENT, 3);
		assertEquals("ACCESS_REQUIREMENT.3.xml", name);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateFileNameNull() {
		// call under test
		BackupFileStreamImpl.createFileName(null, 3);
	}
	
	@Test
	public void testGetTypeFromFileName() {
		MigrationType type = MigrationType.ACL;
		String name = BackupFileStreamImpl.createFileName(type, 12);
		// call under test
		MigrationType result = BackupFileStreamImpl.getTypeFromFileName(name);
		assertEquals(type, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTypeFromFileNameNull() {
		String name = null;
		// call under test
		BackupFileStreamImpl.getTypeFromFileName(name);
	}
	
	@Test
	public void testGetTypeFromFileNameLegacyName() {
		MigrationType type = MigrationType.ACL;
		// Legacy names do not have an index.
		String name = type.name()+".xml";
		// call under test
		MigrationType result = BackupFileStreamImpl.getTypeFromFileName(name);
		assertEquals(type, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTypeFromFileNameWrongFormat() {
		// Names should have at least one dot.
		String name = MigrationType.ACL.name();
		// call under test
		BackupFileStreamImpl.getTypeFromFileName(name);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTypeFromFileNameWrongNotType() {
		// Type name does not match
		String name = MigrationType.ACL.name()+"1"+".xml";
		// call under test
		BackupFileStreamImpl.getTypeFromFileName(name);
	}
	
	@Test
	public void testGetAliasTypeName() {
		MigratableDatabaseObject mdo = new DBONode();
		BackupAliasType type = BackupAliasType.MIGRATION_TYPE_NAME;
		// Call under test
		String allias = BackupFileStreamImpl.getAlias(mdo, type);
		assertEquals(mdo.getMigratableTableType().name(), allias);
	}
	
	@Test
	public void testGetAliasTableName() {
		MigratableDatabaseObject mdo = new DBONode();
		BackupAliasType type = BackupAliasType.TABLE_NAME;
		// Call under test
		String allias = BackupFileStreamImpl.getAlias(mdo, type);
		assertEquals(mdo.getTableMapping().getTableName(), allias);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAliasTableNameNullType() {
		MigratableDatabaseObject mdo = new DBONode();
		BackupAliasType type = null;
		// Call under test
		BackupFileStreamImpl.getAlias(mdo, type);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAliasTableNameNullObject() {
		MigratableDatabaseObject mdo = null;
		BackupAliasType type = BackupAliasType.TABLE_NAME;
		// Call under test
		BackupFileStreamImpl.getAlias(mdo, type);
	}
	
}

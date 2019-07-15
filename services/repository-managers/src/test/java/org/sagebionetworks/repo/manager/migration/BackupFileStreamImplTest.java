package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProvider;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredentialBackup;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

@RunWith(MockitoJUnitRunner.class)
public class BackupFileStreamImplTest {
	
	@Mock
	MigrationTypeProvider mockTypeProvider;

	UnmodifiableXStream tableNameXStream;
	UnmodifiableXStream migrationTypeXStream;


	BackupFileStreamImpl backupFileStream;
	
	ByteArrayOutputStream byteArrayOutputStream;
	ZipOutputStream zipOutputStream;
	DBONode dboNodeOne;
	DBONode dboNodeTwo;
	DBORevision dboRevisionOne;
	DBORevision dboRevisionTwo;
	
	List<MigratableDatabaseObject<?,?>> currentBatch;
	int index;
	MigrationType currentType;
	BackupAliasType backupAliasType;
	
	List<MigratableDatabaseObject<?,?>> rowsToWrite;
	
	DBOCredential credentialOne;
	DBOCredential credentialTwo;
	List<MigratableDatabaseObject<?, ?>> credentials;
	
	int maximumRowsPerFile;
	
	@Before
	public void before() {
		backupFileStream = new BackupFileStreamImpl();
		ReflectionTestUtils.setField(backupFileStream, "typeProvider", mockTypeProvider);
		
		when(mockTypeProvider.getObjectForType(MigrationType.ACL)).thenReturn(new DBOAccessControlList());
		when(mockTypeProvider.getObjectForType(MigrationType.ACL_ACCESS)).thenReturn(new DBOResourceAccess());
		when(mockTypeProvider.getObjectForType(MigrationType.ACL_ACCESS_TYPE)).thenReturn(new DBOResourceAccessType());
		when(mockTypeProvider.getObjectForType(MigrationType.NODE)).thenReturn(new DBONode());
		when(mockTypeProvider.getObjectForType(MigrationType.NODE_REVISION)).thenReturn(new DBORevision());
		when(mockTypeProvider.getObjectForType(MigrationType.CREDENTIAL)).thenReturn(new DBOCredential());
		
		byteArrayOutputStream = new ByteArrayOutputStream();
		zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
		
		dboNodeOne = new DBONode();
		dboNodeOne.setId(123L);
		
		dboRevisionOne = new DBORevision();
		dboRevisionOne.setOwner(dboNodeOne.getId());
		dboRevisionOne.setRevisionNumber(2L);
		
		dboNodeTwo = new DBONode();
		dboNodeTwo.setId(456L);
		
		dboRevisionTwo = new DBORevision();
		dboRevisionTwo.setOwner(dboNodeTwo.getId());
		dboRevisionTwo.setRevisionNumber(1L);
		
		currentBatch = Lists.newArrayList(dboNodeOne, dboNodeTwo);
		index = 12; 
		currentType = dboNodeOne.getMigratableTableType();
		backupAliasType = BackupAliasType.TABLE_NAME;
		
		rowsToWrite = Lists.newArrayList(dboNodeOne, dboNodeTwo, dboRevisionOne, dboRevisionTwo);
		
		maximumRowsPerFile = 1000;
		
		credentialOne = new DBOCredential();
		credentialOne.setPassHash("adminHash");
		credentialOne.setSecretKey("adminKey");
		credentialOne.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		credentialTwo = new DBOCredential();
		credentialTwo.setPassHash("hashTwo");
		credentialTwo.setSecretKey("keyTwo");
		credentialTwo.setPrincipalId(456L);
		credentials = Lists.newArrayList(credentialOne, credentialTwo);

		tableNameXStream = UnmodifiableXStream.builder()
				.alias(SqlConstants.TABLE_NODE, DBONode.class)
				.alias(SqlConstants.TABLE_REVISION, DBORevision.class)
				.alias(SqlConstants.TABLE_ACCESS_CONTROL_LIST, DBOAccessControlList.class)
				.alias(SqlConstants.TABLE_RESOURCE_ACCESS, DBOResourceAccess.class)
				.alias(SqlConstants.TABLE_RESOURCE_ACCESS_TYPE, DBOResourceAccessType.class)
				.alias(SqlConstants.TABLE_CREDENTIAL, DBOCredentialBackup.class).build();
		migrationTypeXStream = UnmodifiableXStream.builder().build();
		when(mockTypeProvider.getXStream(BackupAliasType.TABLE_NAME)).thenReturn(tableNameXStream);
		when(mockTypeProvider.getXStream(BackupAliasType.MIGRATION_TYPE_NAME)).thenReturn(migrationTypeXStream);
	}
	
	@Test
	public void testLegacyMigrationBackupFile() {
		// Read a legacy back file.
		String fileName = "LegacyMigrationBackupACL.zip";
		InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on classplath",input);
		try {
			List<MigratableDatabaseObject<?,?>> readData = new LinkedList<>();
			// call under test
			Iterable<MigratableDatabaseObject<?,?>> iterator = backupFileStream.readBackupFile(input, BackupAliasType.TABLE_NAME);
			assertNotNull(iterator);
			for(MigratableDatabaseObject<?,?> row: iterator) {
				readData.add(row);
			}
			// Validate the expected contents.
			assertEquals(38, readData.size());
			// The first five entries should be for ACL
			MigratableDatabaseObject<?,?> row = readData.get(0);
			assertEquals(MigrationType.ACL, row.getMigratableTableType());
			assertTrue(row instanceof DBOAccessControlList);
			DBOAccessControlList dboACL = (DBOAccessControlList) row;
			assertEquals(new Long(1242), dboACL.getId());
			assertEquals(new Long(4489), dboACL.getOwnerId());
			// four
			row = readData.get(4);
			assertEquals(MigrationType.ACL, row.getMigratableTableType());
			assertTrue(row instanceof DBOAccessControlList);
			
			// Next should be DBOResourceAccess
			row = readData.get(5);
			assertEquals(MigrationType.ACL_ACCESS, row.getMigratableTableType());
			assertTrue(row instanceof DBOResourceAccess);
			DBOResourceAccess dboRA = (DBOResourceAccess) row;
			assertEquals(new Long(1242),dboRA.getOwner());
			
			// The rest of the data should be DBOResourceAccessType
			row = readData.get(10);
			assertEquals(MigrationType.ACL_ACCESS_TYPE, row.getMigratableTableType());
			assertTrue(row instanceof DBOResourceAccessType);
			DBOResourceAccessType dboType = (DBOResourceAccessType) row;
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
			List<MigratableDatabaseObject<?,?>> readData = new LinkedList<>();
			// call under test
			Iterable<MigratableDatabaseObject<?,?>> iterator = backupFileStream.readBackupFile(input, BackupAliasType.TABLE_NAME);
			assertNotNull(iterator);
			for(MigratableDatabaseObject<?,?> row: iterator) {
				readData.add(row);
			}
			// Validate the expected contents.
			assertEquals(6, readData.size());
			// The first three are Nodes
			MigratableDatabaseObject<?,?> row = readData.get(0);
			assertEquals(MigrationType.NODE, row.getMigratableTableType());
			// Last three are revisions.
			row = readData.get(3);
			assertEquals(MigrationType.NODE_REVISION, row.getMigratableTableType());
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
	
	/**
	 * Support for types that have been removed.  See: PLFM-5682.
	 */
	@Test
	public void testGetTypeFromFileNameRemovedType() {
		String typeName = "RemovedType";
		String fileName = typeName+".12.xml";
		// call under test
		try {
			BackupFileStreamImpl.getTypeFromFileName(fileName);
			fail();
		} catch (NotFoundException e) {
			assertTrue(e.getMessage().contains(typeName));
		}
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
	
	@Test (expected=NotFoundException.class)
	public void testGetTypeFromFileNameWrongNotType() {
		// Type name does not match
		String name = MigrationType.ACL.name()+"1"+".xml";
		// call under test
		BackupFileStreamImpl.getTypeFromFileName(name);
	}
	
	@Test
	public void testWriteBatchToZipTableName() throws IOException {
		backupAliasType = BackupAliasType.TABLE_NAME;
		// call under test
		backupFileStream.writeBatchToZip(zipOutputStream, currentBatch, index, currentType, backupAliasType);
		IOUtils.closeQuietly(zipOutputStream);
		
		// Read the resuls
		ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		ZipEntry entry = zipIn.getNextEntry();
		assertNotNull(entry);
		String expectedName = BackupFileStreamImpl.createFileName(dboNodeOne.getMigratableTableType(), index);
		assertEquals(expectedName, entry.getName());
		XStream xstream = new XStream();
		xstream.alias(dboNodeOne.getTableMapping().getTableName(), dboNodeOne.getBackupClass());
		List<DBONode> resultList = (List<DBONode>) xstream.fromXML(zipIn);
		assertEquals(currentBatch.size(), resultList.size());
		assertEquals(currentBatch.get(0), resultList.get(0));
		assertEquals(currentBatch.get(1), resultList.get(1));
	}
	
	@Test
	public void testWriteBatchToZipTypeName() throws IOException {
		BackupAliasType backupAliasType = BackupAliasType.MIGRATION_TYPE_NAME;
		// call under test
		backupFileStream.writeBatchToZip(zipOutputStream, currentBatch, index, currentType, backupAliasType);
		IOUtils.closeQuietly(zipOutputStream);
		
		// Read the results
		ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		ZipEntry entry = zipIn.getNextEntry();
		assertNotNull(entry);
		String expectedName = BackupFileStreamImpl.createFileName(dboNodeOne.getMigratableTableType(), index);
		assertEquals(expectedName, entry.getName());
		XStream xstream = new XStream();
		xstream.alias(dboNodeOne.getMigratableTableType().name(), dboNodeOne.getBackupClass());
		List<DBONode> resultList = (List<DBONode>) xstream.fromXML(zipIn);
		assertEquals(currentBatch.size(), resultList.size());
		assertEquals(currentBatch.get(0), resultList.get(0));
		assertEquals(currentBatch.get(1), resultList.get(1));
	}
	
	@Test
	public void testWriteBackupFileWithLargeMax() throws IOException {
		int maximumRowsPerFile = 100;
		backupFileStream.writeBackupFile(byteArrayOutputStream, rowsToWrite, backupAliasType, maximumRowsPerFile);
		// Read the results
		ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		ZipEntry entry = zipIn.getNextEntry();
		assertNotNull(entry);
		assertEquals("NODE.0.xml", entry.getName());
		entry = zipIn.getNextEntry();
		assertNotNull(entry);
		assertEquals("NODE_REVISION.1.xml", entry.getName());
		// no more files
		entry = zipIn.getNextEntry();
		assertEquals(null, entry);
	}
	
	@Test
	public void testWriteBackupFileWithSmallMax() throws IOException {
		int maximumRowsPerFile = 1;
		backupFileStream.writeBackupFile(byteArrayOutputStream, rowsToWrite, backupAliasType, maximumRowsPerFile);
		// Read the results
		ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		ZipEntry entry = zipIn.getNextEntry();
		assertNotNull(entry);
		assertEquals("NODE.0.xml", entry.getName());
		entry = zipIn.getNextEntry();
		assertNotNull(entry);
		assertEquals("NODE.1.xml", entry.getName());
		entry = zipIn.getNextEntry();
		assertNotNull(entry);
		assertEquals("NODE_REVISION.2.xml", entry.getName());
		entry = zipIn.getNextEntry();
		assertNotNull(entry);
		assertEquals("NODE_REVISION.3.xml", entry.getName());
		// no more files
		entry = zipIn.getNextEntry();
		assertEquals(null, entry);
	}
	
	@Test
	public void testWriteThenReadSmallMax() throws IOException {
		backupAliasType = BackupAliasType.TABLE_NAME;
		int maximumRowsPerFile = 1;
		// call under test
		backupFileStream.writeBackupFile(byteArrayOutputStream, rowsToWrite, backupAliasType, maximumRowsPerFile);
		ByteArrayInputStream input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		// call under test
		Iterable<MigratableDatabaseObject<?, ?>> resultIterator = backupFileStream.readBackupFile(input, backupAliasType);
		List<MigratableDatabaseObject<?, ?>> allResults = new LinkedList<>();
		for(MigratableDatabaseObject<?, ?> row: resultIterator) {
			allResults.add(row);
		}
		assertEquals(rowsToWrite, allResults);
	}
	
	@Test
	public void testWriteThenReadLargeMax() throws IOException {
		int maximumRowsPerFile = 1000;
		backupAliasType = BackupAliasType.MIGRATION_TYPE_NAME;
		// call under test
		backupFileStream.writeBackupFile(byteArrayOutputStream, rowsToWrite, backupAliasType, maximumRowsPerFile);
		ByteArrayInputStream input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		// call under test
		Iterable<MigratableDatabaseObject<?, ?>> resultIterator = backupFileStream.readBackupFile(input, backupAliasType);
		List<MigratableDatabaseObject<?, ?>> allResults = new LinkedList<>();
		for(MigratableDatabaseObject<?, ?> row: resultIterator) {
			allResults.add(row);
		}
		assertEquals(rowsToWrite, allResults);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testNextBeforeHasNext() throws IOException {
		backupFileStream.writeBackupFile(byteArrayOutputStream, rowsToWrite, backupAliasType, maximumRowsPerFile);
		ByteArrayInputStream input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		Iterable<MigratableDatabaseObject<?, ?>> resultIterator = backupFileStream.readBackupFile(input, backupAliasType);
		// calling next() before hasNext() should fail.
		resultIterator.iterator().next();
	}
	
	@Test
	public void testWriteExceptionClose() throws IOException {
		OutputStream mockOut = Mockito.mock(OutputStream.class);
		Exception exception = new IOException("something");
		doThrow(exception).when(mockOut).write(any(byte[].class), any(int.class), any(int.class));
		// call under test
		try {
			backupFileStream.writeBackupFile(mockOut, rowsToWrite, backupAliasType, maximumRowsPerFile);
			fail();
		} catch (Exception e) {
			// expected
		}
		// close should still occur
		verify(mockOut).close();
	}
	
	@Test
	public void testReadExceptionClose() throws IOException {
		InputStream mockInput = Mockito.mock(InputStream.class);
		Exception exception = new IOException("something");
		doThrow(exception).when(mockInput).read(any(byte[].class), any(int.class), any(int.class));
		doThrow(exception).when(mockInput).read(any(byte[].class));
		doThrow(exception).when(mockInput).read();
		try {
			Iterable<MigratableDatabaseObject<?,?>> iterable = backupFileStream.readBackupFile(mockInput, backupAliasType);
			// start the read
			iterable.iterator().hasNext();
			fail();
		} catch (RuntimeException e) {
			// expected
			assertTrue(e.getMessage().contains(exception.getMessage()));
		}
	}
	
	/**
	 * PLFM-4829 occurs when there are no rows in the stream.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPLFM_4829() throws IOException {
		// setup an empty stream
		currentBatch = new LinkedList<>();
		// call under test
		backupFileStream.writeBackupFile(byteArrayOutputStream, currentBatch, backupAliasType, maximumRowsPerFile);
		IOUtils.closeQuietly(zipOutputStream);
		ByteArrayInputStream input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		// call under test
		Iterable<MigratableDatabaseObject<?, ?>> resultIterator = backupFileStream.readBackupFile(input, backupAliasType);
		List<MigratableDatabaseObject<?, ?>> allResults = new LinkedList<>();
		for(MigratableDatabaseObject<?, ?> row: resultIterator) {
			allResults.add(row);
		}
		assertTrue(allResults.isEmpty());
	}
	
	@Test
	public void testWriteBatchToStreamTableName() throws IOException {
		BackupAliasType aliasType = BackupAliasType.TABLE_NAME;
		MigrationType type = MigrationType.CREDENTIAL;
		StringWriter writer = new StringWriter();
		// call under test
		backupFileStream.writeBatchToStream(credentials, type, aliasType, writer);
		String xml = writer.toString();
		assertTrue(xml.contains(credentialOne.getTableMapping().getTableName()));
		assertTrue(xml.contains(""+credentialOne.getPrincipalId()));
		assertTrue(xml.contains(""+credentialTwo.getPrincipalId()));
	}

	
	@Test(expected = EmptyFileException.class)
	public void testReadFileFromStreamEmptyFile() throws Exception {
		StringInputStream input = new StringInputStream("");
		int index = 0;
		String fileName = BackupFileStreamImpl.createFileName(MigrationType.CREDENTIAL, index);
		// Call under test
		backupFileStream.readFileFromStream(input, backupAliasType, fileName);
	}
	
	@Test(expected = EmptyFileException.class)
	public void testReadFileFromStreamMigrationTypeDoesNotExist() throws Exception {
		StringInputStream input = new StringInputStream("");
		String fileName = "RemovedType.4.xml";
		// Call under test
		backupFileStream.readFileFromStream(input, backupAliasType, fileName);
	}
	
	@Test
	public void testReadFileFromStreamNotXML() throws Exception {
		StringInputStream input = new StringInputStream("This is not xml");
		int index = 0;
		String fileName = BackupFileStreamImpl.createFileName(MigrationType.CREDENTIAL, index);
		// Call under test
		try {
			backupFileStream.readFileFromStream(input, backupAliasType, fileName);
			fail();
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof StreamException);
		}
	}
	
	@Test
	public void testReadFileFromStream() throws Exception {
		BackupAliasType aliasType = BackupAliasType.TABLE_NAME;
		MigrationType type = MigrationType.CREDENTIAL;
		StringWriter writer = new StringWriter();
		backupFileStream.writeBatchToStream(credentials, type, aliasType, writer);
		String xml = writer.toString();
		
		StringInputStream input = new StringInputStream(xml);
		int index = 0;
		String fileName = BackupFileStreamImpl.createFileName(MigrationType.CREDENTIAL, index);
		// Call under test
		List<MigratableDatabaseObject<?, ?>> results = backupFileStream.readFileFromStream(input, backupAliasType, fileName);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(credentialTwo, results.get(1));
	}
	
}

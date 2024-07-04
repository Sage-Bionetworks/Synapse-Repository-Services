package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProvider;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProviderImpl;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

//@ExtendWith(MockitoExtension.class)
public class BackupFileStreamImplTest {
	
	private MigrationTypeProvider typeProvider;

	private BackupFileStreamImpl backupFileStream;
	
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
	
	@BeforeEach
	public void before() {
		
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
		credentialOne.setEtag(UUID.randomUUID().toString());
		credentialOne.setExpiresOn(new Date());
		credentialOne.setPassHash("adminHash");
		credentialOne.setSecretKey("adminKey");
		credentialOne.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		credentialTwo = new DBOCredential();
		credentialTwo.setEtag(UUID.randomUUID().toString());
		credentialTwo.setExpiresOn(new Date());
		credentialTwo.setPassHash("hashTwo");
		credentialTwo.setSecretKey("keyTwo");
		credentialTwo.setPrincipalId(456L);
		credentials = Lists.newArrayList(credentialOne, credentialTwo);
		
		this.typeProvider = new MigrationTypeProviderImpl(List.of(new DBONode(), new DBORevision(),
				new DBOAccessControlList(), new DBOResourceAccess(), new DBOResourceAccessType(), new DBOCredential()));
		
		this.backupFileStream = new BackupFileStreamImpl(typeProvider);
		
	}
	
	@Test
	public void testLegacyMigrationBackupFile() {
		// Read a legacy back file.
		String fileName = "LegacyMigrationBackupACL.zip";
		InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
		assertNotNull(input, "Failed to find: "+fileName+" on classplath");
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
		assertNotNull(input, "Failed to find: "+fileName+" on classplath");
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
		assertEquals("ACCESS_REQUIREMENT.3.json", name);
	}
	
	@Test
	public void testCreateFileNameNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			BackupFileStreamImpl.createFileName(null, 3);
		});
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
		
		String result = assertThrows(NotFoundException.class, () -> {
			// call under test			
			BackupFileStreamImpl.getTypeFromFileName(fileName);
		}).getMessage();
		
		assertTrue(result.contains(typeName));
	}
	
	@Test
	public void testGetTypeFromFileNameNull() {
		String name = null;
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			BackupFileStreamImpl.getTypeFromFileName(name);
		});
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
	
	@Test
	public void testGetTypeFromFileNameWrongFormat() {
		// Names should have at least one dot.
		String name = MigrationType.ACL.name();
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			BackupFileStreamImpl.getTypeFromFileName(name);
		});
	}
	
	@Test
	public void testGetTypeFromFileNameWrongNotType() {
		// Type name does not match
		String name = MigrationType.ACL.name()+"1"+".xml";
		
		assertThrows(NotFoundException.class, () -> {
			// call under test
			BackupFileStreamImpl.getTypeFromFileName(name);
		});
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
	
	@Test
	public void testNextBeforeHasNext() throws IOException {
		backupFileStream.writeBackupFile(byteArrayOutputStream, rowsToWrite, backupAliasType, maximumRowsPerFile);
		ByteArrayInputStream input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		Iterable<MigratableDatabaseObject<?, ?>> resultIterator = backupFileStream.readBackupFile(input, backupAliasType);
		
		assertThrows(IllegalStateException.class, () -> {
			// calling next() before hasNext() should fail.
			resultIterator.iterator().next();
		});
	}
	
	@Test
	public void testWriteExceptionClose() throws IOException {
		OutputStream mockOut = Mockito.mock(OutputStream.class);
		Exception exception = new IOException("something");
		doThrow(exception).when(mockOut).write(any(byte[].class), any(int.class), any(int.class));
		// call under test
		
		assertThrows(StreamException.class, () -> {
			backupFileStream.writeBackupFile(mockOut, rowsToWrite, backupAliasType, maximumRowsPerFile);
		});
		
		// close should still occur
		verify(mockOut).close();
	}
	
	@Test
	public void testReadExceptionClose() throws IOException {
		InputStream mockInput = Mockito.mock(InputStream.class);
		Exception exception = new IOException("something");
		doThrow(exception).when(mockInput).read(any(byte[].class), any(int.class), any(int.class));

		Iterable<MigratableDatabaseObject<?,?>> iterable = backupFileStream.readBackupFile(mockInput, backupAliasType);
			
		String result = assertThrows(RuntimeException.class, () -> {			
			// start the read
			iterable.iterator().hasNext();
		}).getMessage();
		
		assertTrue(result.contains(exception.getMessage()));
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

	
	@Test
	public void testReadFileFromStreamEmptyFile() throws Exception {
		StringInputStream input = new StringInputStream("");
		int index = 0;
		String fileName = BackupFileStreamImpl.createFileName(MigrationType.CREDENTIAL, index);
		
		// Call under test
		assertEquals(Optional.empty(), backupFileStream.readFileFromStream(input, backupAliasType, fileName));
	}
	
	@Test
	public void testReadFileFromStreamMigrationTypeDoesNotExist() throws Exception {
		StringInputStream input = new StringInputStream("");
		String fileName = "RemovedType.4.xml";
		
		// Call under test
		assertEquals(Optional.empty(), backupFileStream.readFileFromStream(input, backupAliasType, fileName));
	}
	
	@Test
	public void testReadFileFromStreamNotXML() throws Exception {
		StringInputStream input = new StringInputStream("This is not xml");
		int index = 0;
		String fileName = BackupFileStreamImpl.createFileName(MigrationType.CREDENTIAL, index);
		
		Exception result = assertThrows(Exception.class, () -> {
			// Call under test
			backupFileStream.readFileFromStream(input, backupAliasType, fileName);
		});
		
		assertTrue(result.getCause() instanceof StreamException);
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
		Optional<List<MigratableDatabaseObject<?, ?>>> results = backupFileStream.readFileFromStream(input, backupAliasType, fileName);
		assertNotNull(results.get());
		assertEquals(2, results.get().size());
		assertEquals(credentialTwo, results.get().get(1));
	}
	
}

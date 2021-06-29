package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BucketAndKey;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileHandleDaoImplTest {

	@Autowired
	private DBOFileHandleDaoImpl fileHandleDao;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOChangeDAO changeDAO;
	@Autowired
	private StorageLocationDAO storageLocationDao;
	
	private List<Long> storageLocationsToDelete;
	private String creatorUserGroupId;
	private String creatorUserGroupId2;
	private Long creatorUserGroupIdL;
	private Long creatorUserGroupId2L;
	
	@BeforeEach
	public void before(){
		fileHandleDao.truncateTable();
		storageLocationsToDelete = new LinkedList<>();
		creatorUserGroupIdL = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		creatorUserGroupId = creatorUserGroupIdL.toString();
		assertNotNull(creatorUserGroupId);
		creatorUserGroupId2L = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		creatorUserGroupId2 = creatorUserGroupId2L.toString();
		assertNotNull(creatorUserGroupId2);
	}
	
	@AfterEach
	public void after(){
		fileHandleDao.truncateTable();
		for (Long storageLocationId : storageLocationsToDelete) {
			storageLocationDao.delete(storageLocationId);
		}
	}
	
	@Test
	public void testCreateNull() throws MalformedURLException{
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.createFile(null);
		});
	}
	
	@Test
	public void testCreateFileNameNull() throws MalformedURLException{
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setFileName(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.createFile(handle);
		});
	}
	
	@Test
	public void testDeleteNull(){
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.delete(null);
		});
	}
	
	@Test
	public void testDoesExistNull(){
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.doesExist(null);
		});
	}
	
	@Test
	public void testGetNull() throws DatastoreException, NotFoundException{
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.get(null);
		});
	}
	
	@Test
	public void testSetPreviewIdNullFirst() throws DatastoreException, NotFoundException{
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.setPreviewId(null, "1");
		});
	}
	
	@Test
	public void testS3FileCURD() throws DatastoreException, NotFoundException{
		S3FileHandle meta = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta.getId());
		assertNotNull(meta.getCreatedOn());
		String id = meta.getId();
		assertNotNull(id);
		FileHandle clone = fileHandleDao.get(id);
		assertEquals(meta, clone);
	}
	
	@Test
	public void testGetCreator() throws NotFoundException{
		// Create the metadata
		S3FileHandle meta = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta.getId());
		String id = meta.getId();
		assertNotNull(id);
		// Get the creator
		String lookupCreator = fileHandleDao.getHandleCreator(id);
		assertEquals(creatorUserGroupId, lookupCreator);
	}

	@Test
	public void testGetCreators() throws NotFoundException {
		S3FileHandle meta1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle meta2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle meta3 = TestUtils.createS3FileHandle(creatorUserGroupId2, idGenerator.generateNewId(IdType.FILE_IDS).toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(meta1);
		fileHandleToCreate.add(meta2);
		fileHandleToCreate.add(meta3);
		fileHandleDao.createBatch(fileHandleToCreate);
		meta1 = (S3FileHandle) fileHandleDao.get(meta1.getId());
		meta2 = (S3FileHandle) fileHandleDao.get(meta2.getId());
		meta3 = (S3FileHandle) fileHandleDao.get(meta3.getId());
		
		List<String> allFileHandleId = Arrays.asList(meta3.getId(), meta1.getId(),	meta2.getId());
		// Match to user one.
		Set<String> expected = Sets.newHashSet(meta1.getId(), meta2.getId());
		Set<String> createdByIds = fileHandleDao.getFileHandleIdsCreatedByUser(creatorUserGroupIdL, allFileHandleId);
		assertEquals(expected, createdByIds);
		
		// Match to user two.
		expected = Sets.newHashSet(meta3.getId());
		createdByIds = fileHandleDao.getFileHandleIdsCreatedByUser(creatorUserGroupId2L, allFileHandleId);
		assertEquals(expected, createdByIds);
		
	}
	
	@Test
	public void testGetFileHandleIdsWithPreviewIds() {
		S3FileHandle meta1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle meta2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		
		fileHandleToCreate.add(meta1);
		fileHandleToCreate.add(meta2);
		fileHandleToCreate.add(preview);
		
		fileHandleDao.createBatch(fileHandleToCreate);
		
		meta1 = (S3FileHandle) fileHandleDao.get(meta1.getId());
		meta2 = (S3FileHandle) fileHandleDao.get(meta2.getId());
		preview = (S3FileHandle) fileHandleDao.get(preview.getId());
		
		fileHandleDao.setPreviewId(meta2.getId(), preview.getId());
		
		List<String> allFileHandleIds = Arrays.asList(meta1.getId(), meta2.getId(), preview.getId());
		
		Map<String, String> fileHandleIds = fileHandleDao.getFileHandlePreviewIds(allFileHandleIds);
		
		assertEquals(1, fileHandleIds.size());
		assertEquals(Maps.immutableEntry(preview.getId(), meta2.getId()), fileHandleIds.entrySet().iterator().next());
		
	}
	
	@Test
	public void testGetFileHandleIdsWithPreviewIdsWithEmptyInput() {
		Map<String, String> previewIds = fileHandleDao.getFileHandlePreviewIds(Collections.emptyList());
		assertEquals(Collections.emptyMap(), previewIds);
	}
	
	@Test
	public void testGetFileHandleIdsWithPreviewIdsWithNoPreview() {
		S3FileHandle meta1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle meta2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		
		fileHandleToCreate.add(meta1);
		fileHandleToCreate.add(meta2);
		
		fileHandleDao.createBatch(fileHandleToCreate);
		
		meta1 = (S3FileHandle) fileHandleDao.get(meta1.getId());
		meta2 = (S3FileHandle) fileHandleDao.get(meta2.getId());
		
		List<String> allFileHandleId = Arrays.asList(meta1.getId(),	meta2.getId());
		
		Map<String, String> previewIds = fileHandleDao.getFileHandlePreviewIds(allFileHandleId);
		
		assertEquals(Collections.emptyMap(), previewIds);
		
	}

	@Test
	public void testGetCreatorNotFound() throws NotFoundException{
		assertThrows(NotFoundException.class, () -> {
			// Use an invalid file handle id.
			fileHandleDao.getHandleCreator("99999");
		});
	}
	
	@Test
	public void testGetPreviewFileHandleNotFound() throws NotFoundException{
		assertThrows(NotFoundException.class, () -> {
			// Use an invalid file handle id.
			fileHandleDao.getPreviewFileHandleId("9999");
		});
	}
	
	@Test
	public void testExternalFileCRUD() throws DatastoreException, NotFoundException{
		ExternalFileHandle meta = TestUtils.createExternalFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();
		
		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}
	
	@Test
	public void testPreviewFileCRUD() throws DatastoreException, NotFoundException{
		S3FileHandle meta = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();

		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}


	
	@Test
	public void testLongURL() throws DatastoreException, NotFoundException{
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("fileName");
		// Create a URL that is is 700 chars long
		//according to HTTP specs each section in the domain name can have at most 63 characters
		String domain = Strings.repeat("a", 63) + ".com" ;
		String path = Strings.repeat("b", 700-7-domain.length() - 1 );
		meta.setExternalURL("http://" + domain + "/" + path);
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();

		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}
	
	@Test
	public void testLongNameTooLong() throws DatastoreException, NotFoundException {
		S3FileHandle meta = new S3FileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		// Create a name that 260 chars long
		char[] chars = new char[260];
		Arrays.fill(chars, 'x');
		meta.setFileName(new String(chars));
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Create
			fileHandleDao.createFile(meta);
		});
	}
	

	public void testLongName() throws DatastoreException, NotFoundException {
		S3FileHandle meta = new S3FileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		// Create a name that 255 chars long
		char[] chars = new char[255];
		Arrays.fill(chars, 'x');
		meta.setFileName(new String(chars));
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Create
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();

		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}
	
	@Test
	public void testURLWithSpace() throws DatastoreException, NotFoundException {
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("fileName");
		// Create a URL that is is 700 chars long
		meta.setExternalURL("http://synapse.org/some space");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Save it
			fileHandleDao.createFile(meta);
		});
	}

	@Test
	public void testS3FileWithPreview() throws DatastoreException, NotFoundException{
		// Create the metadata
		S3FileHandle meta = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);

		// Currently there is no preview for this object
		try{
			fileHandleDao.getPreviewFileHandleId(fileId);
			fail("A preview does not exist for this file so a NotFoundException should be thrown.");
		}catch(NotFoundException e){
			// expected
		}
		// Now create a preview for this file.
		S3FileHandle preview = new S3FileHandle();
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentType("content type");
		preview.setContentSize(123l);
		preview.setContentMd5("md5");
		preview.setCreatedBy(creatorUserGroupId);
		preview.setFileName("fileName");
		preview.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setEtag(UUID.randomUUID().toString());
		// Save it
		preview = (S3FileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		String previewId = preview.getId();
		assertNotNull(previewId);

		// Now set the preview for this file
		fileHandleDao.setPreviewId(fileId, previewId);
		FileHandle clone = fileHandleDao.get(fileId);
		S3FileHandle previewClone = (S3FileHandle) fileHandleDao.get(previewId);
		assertNotNull(clone);
		assertTrue(clone instanceof S3FileHandle);
		S3FileHandle s3Clone = (S3FileHandle) clone;
		// The preview ID should be set
		assertEquals(previewId, s3Clone.getPreviewId());
		assertTrue(previewClone.getIsPreview());
		// Lookup the preview id
		String previewIdLookup = fileHandleDao.getPreviewFileHandleId(fileId);
		assertEquals(previewId, previewIdLookup);
		
		//now try clearing the preview
		// Now set the preview for this file
		fileHandleDao.setPreviewId(fileId, null);
		clone = fileHandleDao.get(fileId);
		assertNotNull(clone);
		assertTrue(clone instanceof S3FileHandle);
		s3Clone = (S3FileHandle) clone;
		// The preview ID should not be set
		assertNull(s3Clone.getPreviewId());
		
	}
	
	@Test
	public void testSetPreviewWherePreviewDoesNotExist() throws DatastoreException, NotFoundException{
		S3FileHandle meta = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		
		assertThrows(NotFoundException.class, () -> {
			// Set it to a fake preview
			fileHandleDao.setPreviewId(fileId, "-1");
		});
	}
	
	@Test
	public void testSetPreviewWhereFileDoesNotExist() throws DatastoreException, NotFoundException{
		// Create a real preview.
		S3FileHandle preview = new S3FileHandle();
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentType("content type");
		preview.setContentSize(123l);
		preview.setContentMd5("md5");
		preview.setCreatedBy(creatorUserGroupId);
		preview.setFileName("fileName");
		preview.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setEtag(UUID.randomUUID().toString());
		preview.setIsPreview(true);
		preview = (S3FileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		String previewId = preview.getId();
		assertNotNull(previewId);
		
		assertThrows(NotFoundException.class, () -> {
			// Set the real preview on a file that does not exist.
			fileHandleDao.setPreviewId("-1", previewId);
		});
	}
	
	@Test
	public void testSetIdAlreadyExists(){
		// create one.
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setFileName("fileName");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		
		// Now create another with the same ID
		ExternalFileHandle duplicate = new ExternalFileHandle();
		duplicate.setCreatedBy(creatorUserGroupId);
		duplicate.setExternalURL("http://google.com");
		duplicate.setId(fileId);
		
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleDao.createFile(duplicate);
		});
	}
	
	/**
	 * This tests that when we set the ID, using a value that has not been used
	 * that the Idgenerator.reserveId is called to ensure that the ID and anything less than it is 
	 * never used again.
	 */
	@Test
	public void testSetIdBeyondRange(){
		// create one.
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setFileName("fileName");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		
		// Now create another with the same ID
		meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com/2");
		meta.setFileName("fileName");
		// Set the ID beyond what the current ID generator range
		meta.setId(new Long(Long.parseLong(fileId)+10l).toString());
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		String file2Id = meta.getId();
		assertNotNull(file2Id);
		// Now if we create another one its ID should be one larger than the previous
		meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com/3");
		meta.setFileName("fileName");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		meta.setFileName("fileName");
		String file3Id = meta.getId();
		assertNotNull(file3Id);
		assertEquals(new Long(Long.parseLong(file2Id) + 1).toString(), file3Id);
		
	}
	
	@Test
	public void testgetAllFileHandles() throws Exception{
		// Create one without a preview
		S3FileHandle noPreviewHandle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = (S3FileHandle) fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		withPreview = (S3FileHandle) fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		// The Preview
		S3FileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");
		preview = (S3FileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etags should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		preview = (S3FileHandle) fileHandleDao.get(preview.getId());

		// Now get all file handles without previews
		List<String> toFetch = new ArrayList<String>();
		toFetch.add(noPreviewHandle.getId());
		toFetch.add(withPreview.getId());
		FileHandleResults results = fileHandleDao.getAllFileHandles(toFetch, false);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(2, results.getList().size(), "With previews false, only two should be returned");
		assertEquals(noPreviewHandle, results.getList().get(0));
		assertEquals(withPreview, results.getList().get(1));
		
		// Same call but with previews included
		results = fileHandleDao.getAllFileHandles(toFetch, true);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(3, results.getList().size(), "With previews true, three should be returned");
		assertEquals(noPreviewHandle, results.getList().get(0));
		assertEquals(withPreview, results.getList().get(1));
		assertEquals(preview, results.getList().get(2));
	}

	@Test
	public void testgetAllFileHandlesBatch() throws Exception {
		// Create one without a preview
		S3FileHandle noPreviewHandle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = (S3FileHandle) fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		withPreview = (S3FileHandle) fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		// The Preview
		S3FileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");
		preview = (S3FileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());

		// Now get all file handles without previews
		List<String> toFetch = new ArrayList<String>();
		toFetch.add(noPreviewHandle.getId());
		toFetch.add(withPreview.getId());
		Map<String, FileHandle> results = fileHandleDao.getAllFileHandlesBatch(toFetch);
		assertEquals(2, results.size());
		assertEquals(noPreviewHandle, results.get(noPreviewHandle.getId()));
		assertEquals(withPreview, results.get(withPreview.getId()));
	}

	@Test
	public void testCountReferences() throws Exception {
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle1.setKey(UUID.randomUUID().toString());
		assertEquals(0, fileHandleDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, handle1.getBucketName(), handle1.getKey()));
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		assertEquals(1, fileHandleDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, handle1.getBucketName(), handle1.getKey()));
		S3FileHandle handle2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle2.setKey(handle1.getKey());
		handle2 = (S3FileHandle) fileHandleDao.createFile(handle2);
		assertEquals(2, fileHandleDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, handle1.getBucketName(), handle1.getKey()));
		fileHandleDao.delete(handle2.getId());
		assertEquals(1, fileHandleDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, handle1.getBucketName(), handle1.getKey()));
		fileHandleDao.delete(handle1.getId());
		assertEquals(0, fileHandleDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, handle1.getBucketName(), handle1.getKey()));
	}

	@Test
	public void testCreateFileHandleWithNoPreview() {
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setPreviewId(handle.getId());
		handle = (S3FileHandle) fileHandleDao.createFile(handle);
		assertEquals(handle.getId(), handle.getPreviewId());
	}
	
	@Test
	public void testProxyFileHandle(){
		ProxyFileHandle pfh = new ProxyFileHandle();
		pfh.setContentType("text/plain");
		pfh.setContentMd5("md5");
		pfh.setContentSize(123L);
		pfh.setFilePath("/foo/bar/text.txt");
		pfh.setFileName("text.txt");
		pfh.setCreatedBy(creatorUserGroupId);
		pfh.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		pfh.setEtag(UUID.randomUUID().toString());
		pfh = (ProxyFileHandle) fileHandleDao.createFile(pfh);
		assertNotNull(pfh);
		ProxyFileHandle clone = (ProxyFileHandle) fileHandleDao.get(pfh.getId());
		assertEquals(pfh, clone);
	}

	@Test
	public void testExternalObjectStoreFileHandle(){
		ExternalObjectStoreFileHandle externalObjectStoreFileHandle = new ExternalObjectStoreFileHandle();
		externalObjectStoreFileHandle.setContentType("text/plain");
		externalObjectStoreFileHandle.setContentMd5("md5");
		externalObjectStoreFileHandle.setContentSize(123L);
		externalObjectStoreFileHandle.setFileKey("/foo/bar/text.txt");
		externalObjectStoreFileHandle.setFileName("text.txt");
		externalObjectStoreFileHandle.setCreatedBy(creatorUserGroupId);
		externalObjectStoreFileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		externalObjectStoreFileHandle.setEtag(UUID.randomUUID().toString());

		externalObjectStoreFileHandle = (ExternalObjectStoreFileHandle) fileHandleDao.createFile(externalObjectStoreFileHandle);
		assertNotNull(externalObjectStoreFileHandle);
		ExternalObjectStoreFileHandle clone = (ExternalObjectStoreFileHandle) fileHandleDao.get(externalObjectStoreFileHandle.getId());
		assertEquals(externalObjectStoreFileHandle, clone);
	}

	@Test
	public void testCreateBatchWithNullBatch() {
		assertThrows(IllegalArgumentException.class, () -> {			
			fileHandleDao.createBatch(null);
		});
	}

	@Test
	public void testCreateBatchWithEmptyBatch() {
		assertThrows(IllegalArgumentException.class, () -> {			
			fileHandleDao.createBatch(new ArrayList<FileHandle>(0));
		});
	}

	@Test
	public void testCreateBatch() {
		changeDAO.deleteAllChanges();
		long startChangeNumber = changeDAO.getCurrentChangeNumber();
		// imitate database time
		Timestamp now = new Timestamp(System.currentTimeMillis()/1000*1000);
		ArrayList<FileHandle> batch = new ArrayList<FileHandle>();
		S3FileHandle s3 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		s3.setCreatedOn(now);
		batch.add(s3);
		ExternalFileHandle external = TestUtils.createExternalFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		external.setCreatedOn(now);
		batch.add(external);
		fileHandleDao.createBatch(batch);
		
		// Align the auto-assigned modfiedOn
		s3.setModifiedOn(fileHandleDao.get(s3.getId()).getModifiedOn());
		external.setModifiedOn(fileHandleDao.get(external.getId()).getModifiedOn());
		
		assertEquals(s3, fileHandleDao.get(s3.getId()));
		assertEquals(external, fileHandleDao.get(external.getId()));

		List<ChangeMessage> changes = changeDAO.listChanges(startChangeNumber, ObjectType.FILE, Long.MAX_VALUE);
		assertNotNull(changes);
		assertEquals(batch.size(), changes.size());
		for (int i = 0; i < batch.size(); i++) {
			ChangeMessage message = changes.get(i);
			assertEquals(ChangeType.CREATE, message.getChangeType());
			assertEquals(ObjectType.FILE, message.getObjectType());
		}
	}
	
	@Test
	public void testIsMatchingMD5() {
		S3FileHandle source = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle target = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		fileHandleDao.createBatch(Arrays.asList(source, target));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5(source.getId(), target.getId());
	
		assertTrue(matchResult);
	}
	
	@Test
	public void testIsMatchingMD5WithNoMatch() {
		S3FileHandle source = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle target = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		source.setContentMd5("md51");
		target.setConcreteType("md52");
		
		fileHandleDao.createBatch(Arrays.asList(source, target));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5(source.getId(), target.getId());
	
		assertFalse(matchResult);
	}
	
	@Test
	public void testIsMatchingMD5WithNonExistingSource() {
		S3FileHandle source = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		source.setContentMd5("md51");
		
		fileHandleDao.createBatch(Arrays.asList(source));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5(source.getId(), "-1");
	
		assertFalse(matchResult);
	}
	
	@Test
	public void testIsMatchingMD5WithNonExistingTarget() {
		S3FileHandle target = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		target.setContentMd5("md52");
		
		fileHandleDao.createBatch(Arrays.asList(target));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5("-1", target.getId());
	
		assertFalse(matchResult);
	}
	
	@Test
	public void testIsMatchingMD5WithNullSourceMD5() {
		S3FileHandle source = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle target = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		source.setContentMd5(null);
		target.setConcreteType("md52");
		
		fileHandleDao.createBatch(Arrays.asList(source, target));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5(source.getId(), target.getId());
	
		assertFalse(matchResult);
	}
	
	@Test
	public void testIsMatchingMD5WithNullTargetMD5() {
		S3FileHandle source = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle target = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		source.setContentMd5("md51");
		target.setConcreteType(null);
		
		fileHandleDao.createBatch(Arrays.asList(source, target));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5(source.getId(), target.getId());
	
		assertFalse(matchResult);
	}
	
	@Test
	public void testIsMatchingMD5WithBothNull() {
		S3FileHandle source = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle target = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		source.setContentMd5(null);
		target.setConcreteType(null);
		
		fileHandleDao.createBatch(Arrays.asList(source, target));
		
		// Call under test
		boolean matchResult = fileHandleDao.isMatchingMD5(source.getId(), target.getId());
	
		assertFalse(matchResult);
	}
	
	@Test
	public void testGetFileHandlesBatchByStatus() {
		S3FileHandle file1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle file2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		fileHandleDao.createBatch(Arrays.asList(file1, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId()).stream().map(id-> Long.valueOf(id)).collect(Collectors.toList());

		// Call under test
		List<Long> result = fileHandleDao.getFileHandlesBatchByStatus(ids, FileHandleStatus.AVAILABLE).stream().map(file-> Long.valueOf(file.getId())).collect(Collectors.toList());
		
		assertEquals(ids, result);
		
		// Change the first to UNLINKED
		fileHandleDao.updateStatusForBatch(Arrays.asList(Long.valueOf(file1.getId())), FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE, /* updated before */ 0);
		
		result = fileHandleDao.getFileHandlesBatchByStatus(ids, FileHandleStatus.AVAILABLE).stream().map(file-> Long.valueOf(file.getId())).collect(Collectors.toList());
		
		// Only the second is available now
		assertEquals(Arrays.asList(Long.valueOf(file2.getId())), result);
		
		result = fileHandleDao.getFileHandlesBatchByStatus(ids, FileHandleStatus.UNLINKED).stream().map(file-> Long.valueOf(file.getId())).collect(Collectors.toList());
		
		// Only the second is available now
		assertEquals(Arrays.asList(Long.valueOf(file1.getId())), result);
	}
	
	@Test
	public void testGetDBOFileHandlesBatchWithNoUpdatedOnFilter() {
		S3FileHandle file1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle file2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		fileHandleDao.createBatch(Arrays.asList(file1, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId(), "567").stream().map(id-> Long.valueOf(id)).collect(Collectors.toList());

		List<DBOFileHandle> expected = Arrays.asList(
				FileMetadataUtils.createDBOFromDTO(file1),
				FileMetadataUtils.createDBOFromDTO(file2)
		);
		
		int updatedOnBeforeDays = 0;
		
		// Call under test
		List<DBOFileHandle> result = fileHandleDao.getDBOFileHandlesBatch(ids, updatedOnBeforeDays);
		
		for (int i=0; i<result.size(); i++) {
			DBOFileHandle truth = result.get(i);
			DBOFileHandle toAlign = expected.get(i);
			
			toAlign.setCreatedOn(truth.getCreatedOn());
			toAlign.setUpdatedOn(truth.getUpdatedOn());
		}
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testGetDBOFileHandlesBatchWithUpdatedOnFilter() {
		
		int updatedOnBeforeDays = 30;
		
		Timestamp updatedOn = Timestamp.from(Instant.now().minus(updatedOnBeforeDays + 1, ChronoUnit.DAYS));
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		file2.setUpdatedOn(updatedOn);
		
		basicDao.createBatch(Arrays.asList(file1, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId(), 567L);

		List<DBOFileHandle> expected = Arrays.asList(
				file2
		);
		
		// Call under test
		List<DBOFileHandle> result = fileHandleDao.getDBOFileHandlesBatch(ids, updatedOnBeforeDays);
		
		for (int i=0; i<result.size(); i++) {
			DBOFileHandle truth = result.get(i);
			DBOFileHandle toAlign = expected.get(i);
			
			toAlign.setCreatedOn(truth.getCreatedOn());
			toAlign.setUpdatedOn(truth.getUpdatedOn());
		}
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testUpdateStatusForBatchWithNoUpdatedOnFilter() {
		
		// Slightly in the past to account for database time drifting
		Date createdOn = Date.from(Instant.now().minusSeconds(60));
		
		S3FileHandle file1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()).setCreatedOn(createdOn);
		S3FileHandle file2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()).setCreatedOn(createdOn);
		S3FileHandle file3 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()).setCreatedOn(createdOn);
		
		fileHandleDao.createBatch(Arrays.asList(file1, file2, file3));

		DBOFileHandle dbo1 = fileHandleDao.getDBO(file1.getId());
		DBOFileHandle dbo2 = fileHandleDao.getDBO(file2.getId());
		DBOFileHandle dbo3 = fileHandleDao.getDBO(file3.getId());
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId()).stream().map(id-> Long.valueOf(id)).collect(Collectors.toList());
		
		// Call under test
		List<Long> updatedIds = fileHandleDao.updateStatusForBatch(ids, FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE, 0);
		
		assertEquals(ids, updatedIds);
		
		assertNotEquals(dbo1, fileHandleDao.getDBO(file1.getId().toString()));
		assertNotEquals(dbo2, fileHandleDao.getDBO(file2.getId().toString()));
		
		// Should not have been updated (not in the list)
		assertEquals(dbo3, fileHandleDao.getDBO(file3.getId().toString()));
				
		dbo1 = fileHandleDao.getDBO(file1.getId());
		dbo2 = fileHandleDao.getDBO(file2.getId());
		dbo3 = fileHandleDao.getDBO(file3.getId());
		
		assertEquals(FileHandleStatus.UNLINKED.name(), dbo1.getStatus());
		assertEquals(FileHandleStatus.UNLINKED.name(), dbo2.getStatus());
		assertEquals(FileHandleStatus.AVAILABLE.name(), dbo3.getStatus());
		
		assertTrue(dbo1.getUpdatedOn().after(createdOn));
		assertTrue(dbo2.getUpdatedOn().after(createdOn));
	}
	
	@Test
	public void testUpdateStatusForBatchWithUpdatedOnFilter() {
		
		int updatedOnBeforeDays = 2;
		
		// A bit more than the days to account for database time drifting 
		Date createdOn = Date.from(Instant.now().minus(updatedOnBeforeDays, ChronoUnit.DAYS).minusSeconds(60));
		Timestamp updatedOn = Timestamp.from(createdOn.toInstant());
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()).setCreatedOn(createdOn));
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()).setCreatedOn(createdOn));
		DBOFileHandle file3 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()).setCreatedOn(createdOn));
		
		file1.setUpdatedOn(updatedOn);
		file3.setUpdatedOn(updatedOn);
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file2, file3));

		DBOFileHandle dbo1 = fileHandleDao.getDBO(file1.getId().toString());
		DBOFileHandle dbo2 = fileHandleDao.getDBO(file2.getId().toString());
		DBOFileHandle dbo3 = fileHandleDao.getDBO(file3.getId().toString());
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId());
		
		// Call under test
		List<Long> updatedIds = fileHandleDao.updateStatusForBatch(ids, FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE, updatedOnBeforeDays);
		
		assertEquals(Arrays.asList(file1.getId()), updatedIds);
		
		assertNotEquals(dbo1, fileHandleDao.getDBO(file1.getId().toString()));
		// Should not have been updated
		assertEquals(dbo2, fileHandleDao.getDBO(file2.getId().toString()));
		assertEquals(dbo3, fileHandleDao.getDBO(file3.getId().toString()));
				
		dbo1 = fileHandleDao.getDBO(file1.getId().toString());
		dbo2 = fileHandleDao.getDBO(file2.getId().toString());
		dbo3 = fileHandleDao.getDBO(file3.getId().toString());
		
		assertEquals(FileHandleStatus.UNLINKED.name(), dbo1.getStatus());
		assertEquals(FileHandleStatus.AVAILABLE.name(), dbo2.getStatus());
		assertEquals(FileHandleStatus.AVAILABLE.name(), dbo3.getStatus());
		
		assertTrue(dbo1.getUpdatedOn().after(createdOn));
	}
	
	@Test
	public void testHasStatusBatch() {
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId());
		
		boolean result = fileHandleDao.hasStatusBatch(ids, FileHandleStatus.AVAILABLE);
		
		assertTrue(result);
		
	}
	
	@Test
	public void testHasStatusBatchNone() {
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId());
		
		boolean result = fileHandleDao.hasStatusBatch(ids, FileHandleStatus.UNLINKED);
		
		assertFalse(result);
		
	}
	
	@Test
	public void testHasStatusBatchPartial() {
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		file2.setStatus(FileHandleStatus.UNLINKED.name());
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId());
		
		boolean result = fileHandleDao.hasStatusBatch(ids, FileHandleStatus.UNLINKED);
		
		assertTrue(result);
		
	}
	
	@Test
	public void testGetUnlinkedKeysForBucket() {
		
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		Instant modifiedAfter = now.minus(days + 1, ChronoUnit.DAYS);
		
		Instant inRange = modifiedBefore.minus(1, ChronoUnit.HOURS);
		Instant afterRange = modifiedBefore.plus(1, ChronoUnit.HOURS);
		Instant beforeRange = modifiedAfter.minus(1, ChronoUnit.HOURS);
				
		// After the range
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setUpdatedOn(Timestamp.from(afterRange));
		file1.setStatus(FileHandleStatus.UNLINKED.name());
		file1.setKey("key1");
				
		// In the range
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2.setBucketName(bucket);
		file2.setUpdatedOn(Timestamp.from(inRange));
		file2.setStatus(FileHandleStatus.UNLINKED.name());
		file2.setKey("key2");
		
		// In the range duplicated key
		DBOFileHandle file2Dup = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2Dup.setBucketName(bucket);
		file2Dup.setUpdatedOn(Timestamp.from(inRange));
		file2Dup.setStatus(FileHandleStatus.UNLINKED.name());
		file2Dup.setKey("key2");
		
		// In the range but different bucket
		DBOFileHandle file3 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file3.setBucketName("anotherBucket");
		file3.setUpdatedOn(Timestamp.from(inRange));
		file3.setStatus(FileHandleStatus.UNLINKED.name());
		file3.setKey("key3");
		
		// In the range but different AVAILABLE
		DBOFileHandle file4 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file4.setBucketName(bucket);
		file4.setUpdatedOn(Timestamp.from(inRange));
		file4.setStatus(FileHandleStatus.AVAILABLE.name());
		file4.setKey("key4");
		
		// Before the range
		DBOFileHandle file5 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file5.setBucketName(bucket);
		file5.setUpdatedOn(Timestamp.from(beforeRange));
		file5.setStatus(FileHandleStatus.UNLINKED.name());
		file5.setKey("key5");
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file2, file2Dup, file3, file4, file5));
		
		// Call under test		
		List<String> keys = fileHandleDao.getUnlinkedKeysForBucket(bucket, modifiedBefore, modifiedAfter, 10);
		
		assertEquals(Arrays.asList("key2"), keys);
	}
	
	@Test
	public void testGetUnlinkedKeysForBucketWithEmptyBucket() {
		String bucket = "";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		Instant modifiedAfter = now.minus(days + 1, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getUnlinkedKeysForBucket(bucket, modifiedBefore, modifiedAfter, 1);
		});
		
		assertEquals("The bucketName is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testGetUnlinkedKeysForBucketWithNoModifiedBefore() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = null;
		Instant modifiedAfter = now.minus(days + 1, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getUnlinkedKeysForBucket(bucket, modifiedBefore, modifiedAfter, 1);
		});
		
		assertEquals("The modifiedBefore is required.", ex.getMessage());
	}
	
	@Test
	public void testGetUnlinkedKeysForBucketWithNoModifiedAfter() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);;
		Instant modifiedAfter = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getUnlinkedKeysForBucket(bucket, modifiedBefore, modifiedAfter, 1);
		});
		
		assertEquals("The modifiedAfter is required.", ex.getMessage());
	}
	
	@Test
	public void testGetUnlinkedKeysForBucketWithModifiedBeforeBeforeModifiedAfter() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days + 1, ChronoUnit.DAYS);
		Instant modifiedAfter = now.minus(days, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getUnlinkedKeysForBucket(bucket, modifiedBefore, modifiedAfter, 1);
		});
		
		assertEquals("modifiedAfter must be before modifiedBefore.", ex.getMessage());
	}
	
	@Test
	public void testGetUnlinkedKeysForBucketWithNegativeLimit() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		Instant modifiedAfter = now.minus(days + 1, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getUnlinkedKeysForBucket(bucket, modifiedBefore, modifiedAfter, -1);
		});
		
		assertEquals("The limit must be greater than 0.", ex.getMessage());
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKey() {
		
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		
		Instant inRange = modifiedBefore.minus(1, ChronoUnit.HOURS);
		Instant afterRange = modifiedBefore.plus(1, ChronoUnit.HOURS);
				
		// After the range
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setUpdatedOn(Timestamp.from(afterRange));
		file1.setStatus(FileHandleStatus.UNLINKED.name());
		file1.setKey("key1");
		
		// In the range, matching key
		DBOFileHandle file1Matching = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Matching.setBucketName(bucket);
		file1Matching.setUpdatedOn(Timestamp.from(inRange));
		file1Matching.setStatus(FileHandleStatus.UNLINKED.name());
		file1Matching.setKey("key1");
				
		// In the range but different key
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2.setBucketName(bucket);
		file2.setUpdatedOn(Timestamp.from(inRange));
		file2.setStatus(FileHandleStatus.UNLINKED.name());
		file2.setKey("key2");
		
		// In the range but different bucket
		DBOFileHandle file3 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file3.setBucketName("anotherBucket");
		file3.setUpdatedOn(Timestamp.from(inRange));
		file3.setStatus(FileHandleStatus.UNLINKED.name());
		file3.setKey("key1");
		
		// In the range but different status
		DBOFileHandle file4 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file4.setBucketName(bucket);
		file4.setUpdatedOn(Timestamp.from(inRange));
		file4.setStatus(FileHandleStatus.AVAILABLE.name());
		file4.setKey("key1");
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file1Matching, file2, file3, file4));
		
		// Call under test		
		int result = fileHandleDao.updateStatusByBucketAndKey(bucket, "key1", FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		
		assertEquals(1, result);
		
		List<DBOFileHandle> files = fileHandleDao.getDBOFileHandlesBatch(Arrays.asList(file1.getId(), file1Matching.getId(), file2.getId(), file3.getId(), file4.getId()), 0);
		
		assertEquals(file1.getStatus(), files.get(0).getStatus());
		assertEquals(file1.getEtag(), files.get(0).getEtag());
		
		assertEquals(FileHandleStatus.ARCHIVED.name(), files.get(1).getStatus());
		assertNotEquals(file1Matching.getEtag(), files.get(1).getEtag());
		
		assertEquals(file2.getStatus(), files.get(2).getStatus());
		assertEquals(file2.getEtag(), files.get(2).getEtag());
		
		assertEquals(file3.getStatus(), files.get(3).getStatus());
		assertEquals(file3.getEtag(), files.get(3).getEtag());
		
		assertEquals(file4.getStatus(), files.get(4).getStatus());
		assertEquals(file4.getEtag(), files.get(4).getEtag());
		
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKeyWithNoUpdates() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		
		Instant inRange = modifiedBefore.minus(1, ChronoUnit.HOURS);
						
		// In the range, matching key
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setUpdatedOn(Timestamp.from(inRange));
		file1.setStatus(FileHandleStatus.UNLINKED.name());
		file1.setKey("key1");
		
		// Call under test		
		int result = fileHandleDao.updateStatusByBucketAndKey(bucket, "key2", FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		
		assertEquals(0, result);
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKeyWithEmptyBucket() {
		String bucket = "";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.updateStatusByBucketAndKey(bucket, "key2", FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		});
		
		assertEquals("The bucketName is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKeyWithEmptyKey() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.updateStatusByBucketAndKey(bucket, "", FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		});
		
		assertEquals("The key is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKeyWithNullStatus() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.updateStatusByBucketAndKey(bucket, "key1", null, FileHandleStatus.UNLINKED, modifiedBefore);
		});
		
		assertEquals("The newStatus is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKeyWithNullCurrentStatus() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedBefore = now.minus(days, ChronoUnit.DAYS);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.updateStatusByBucketAndKey(bucket, "key1", FileHandleStatus.ARCHIVED, null, modifiedBefore);
		});
		
		assertEquals("The currentStatus is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateStatusBatchByBucketAndKeyWithNullModifedBefore() {
		String bucket = "bucket";
		
		Instant modifiedBefore = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.updateStatusByBucketAndKey(bucket, "key1", FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		});
		
		assertEquals("The modifiedBefore is required.", ex.getMessage());
	}
	
	@Test
	public void testGetAvailableOrEarlyUnlinkedFileHandlesCount() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedAfter = now.minus(days, ChronoUnit.DAYS);
		
		Instant inRange = modifiedAfter.minus(1, ChronoUnit.HOURS);
		Instant afterRange = modifiedAfter.plus(1, ChronoUnit.HOURS);
				
		// Non available but after the range
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setUpdatedOn(Timestamp.from(afterRange));
		file1.setStatus(FileHandleStatus.UNLINKED.name());
		file1.setKey("key1");
		
		// In the range, matching key
		DBOFileHandle file1Matching = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Matching.setBucketName(bucket);
		file1Matching.setUpdatedOn(Timestamp.from(inRange));
		file1Matching.setStatus(FileHandleStatus.UNLINKED.name());
		file1Matching.setKey("key1");
				
		// In the range but different key
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2.setBucketName(bucket);
		file2.setUpdatedOn(Timestamp.from(inRange));
		file2.setStatus(FileHandleStatus.AVAILABLE.name());
		file2.setKey("key2");
		
		// In the range but different bucket
		DBOFileHandle file3 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file3.setBucketName("anotherBucket");
		file3.setUpdatedOn(Timestamp.from(inRange));
		file3.setStatus(FileHandleStatus.AVAILABLE.name());
		file3.setKey("key1");
		
		// In the range and available
		DBOFileHandle file4 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file4.setBucketName(bucket);
		file4.setUpdatedOn(Timestamp.from(inRange));
		file4.setStatus(FileHandleStatus.AVAILABLE.name());
		file4.setKey("key1");
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file1Matching, file2, file3, file4));
		
		// Call under test		
		int result = fileHandleDao.getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, "key1", modifiedAfter);
		
		assertEquals(2, result);
	}
	
	@Test
	public void testGetAvailableOrEarlyUnlinkedFileHandlesCountWithEmptyBucket() {
		String bucket = "";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedAfter = now.minus(days, ChronoUnit.DAYS);
		
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, "key1", modifiedAfter);
		});
		
		assertEquals("The bucketName is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testGetAvailableOrEarlyUnlinkedFileHandlesCountWithEmptyKey() {
		String bucket = "bucket";
		
		int days = 5;
		
		Instant now = Instant.parse("2021-02-03T10:00:00.00Z");
		Instant modifiedAfter = now.minus(days, ChronoUnit.DAYS);
		
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, "", modifiedAfter);
		});
		
		assertEquals("The key is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testGetAvailableOrEarlyUnlinkedFileHandlesCountWithNullModifiedAfter() {
		String bucket = "bucket";
		
		Instant modifiedAfter = null;
		
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test		
			fileHandleDao.getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, "key1", modifiedAfter);
		});
		
		assertEquals("The modifiedAfter is required.", ex.getMessage());
	}
	
	@Test
	public void testClearPreviewByKeyAndStatus() {
		String bucket = "bucket";
		
		DBOFileHandle file1Preview = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Preview.setBucketName(bucket);
		file1Preview.setKey("file1_preview");
		file1Preview.setIsPreview(true);
		
		// Matching status
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setStatus(FileHandleStatus.ARCHIVED.name());
		file1.setKey("key1");
		file1.setPreviewId(file1Preview.getId());
		
		// Different file handle, same preview
		DBOFileHandle file1Copy = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Copy.setBucketName(bucket);
		file1Copy.setStatus(FileHandleStatus.ARCHIVED.name());
		file1Copy.setKey("key1");
		file1Copy.setPreviewId(file1.getPreviewId());
		
		DBOFileHandle file2Preview = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2Preview.setBucketName(bucket);
		file2Preview.setKey("file2_preview");
		file2Preview.setIsPreview(true);
		
		// Same key, different status
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2.setBucketName(bucket);
		file2.setStatus(FileHandleStatus.UNLINKED.name());
		file2.setKey("key1");
		file2.setPreviewId(file2Preview.getId());
		
		// Different key
		DBOFileHandle file3 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file3.setBucketName(bucket);
		file3.setStatus(FileHandleStatus.ARCHIVED.name());
		file3.setKey("key2");
		file3.setPreviewId(file2.getPreviewId());
		
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1Preview, file1, file1Copy, file2Preview, file2, file3));
		
		// Call under test
		Set<Long> previewIds = fileHandleDao.clearPreviewByKeyAndStatus(bucket, "key1", FileHandleStatus.ARCHIVED);
		
		assertEquals(new HashSet<>(Arrays.asList(file1.getPreviewId())), previewIds);
		
		List<DBOFileHandle> files = fileHandleDao.getDBOFileHandlesBatch(Arrays.asList(file1.getId(), file1Copy.getId(), file2.getId(), file3.getId()), 0);
		
		assertNull(files.get(0).getPreviewId());
		assertNull(files.get(1).getPreviewId());
		assertEquals(file2Preview.getId(), files.get(2).getPreviewId());
		assertEquals(file2Preview.getId(), files.get(3).getPreviewId());
	}
	
	@Test
	public void testClearPreviewByKeyAndStatusWithEmptyBucket() {
		String bucket = "";
		String key = "key1";
		FileHandleStatus status = FileHandleStatus.ARCHIVED;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			fileHandleDao.clearPreviewByKeyAndStatus(bucket, key, status);
		});
		
		assertEquals("The bucketName is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testClearPreviewByKeyAndStatusWithEmptyKey() {
		String bucket = "bucket";
		String key = "";
		FileHandleStatus status = FileHandleStatus.ARCHIVED;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			fileHandleDao.clearPreviewByKeyAndStatus(bucket, key, status);
		});
		
		assertEquals("The key is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testClearPreviewByKeyAndStatusWithNullStatus() {
		String bucket = "bucket";
		String key = "key1";
		FileHandleStatus status = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			fileHandleDao.clearPreviewByKeyAndStatus(bucket, key, status);
		});
		
		assertEquals("The status is required.", ex.getMessage());
	}
	
	@Test
	public void testGetReferencedPreviews() {
		
		String bucket = "bucket";
		
		// Referenced preview
		DBOFileHandle file1Preview = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Preview.setBucketName(bucket);
		file1Preview.setKey("file1_preview");
		file1Preview.setIsPreview(true);
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setStatus(FileHandleStatus.ARCHIVED.name());
		file1.setKey("key1");
		file1.setPreviewId(file1Preview.getId());
		
		// Different file handle, same preview
		DBOFileHandle file1Copy = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Copy.setBucketName(bucket);
		file1Copy.setStatus(FileHandleStatus.ARCHIVED.name());
		file1Copy.setKey("key1");
		file1Copy.setPreviewId(file1.getPreviewId());
		
		DBOFileHandle file2Preview = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2Preview.setBucketName(bucket);
		file2Preview.setKey("file2_preview");
		file2Preview.setIsPreview(true);
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1Preview, file1, file1Copy, file2Preview));
	
		Set<Long> previewIds = new HashSet<>(Arrays.asList(file1Preview.getId(), file2Preview.getId()));
		
		// Call under test
		Set<Long> result = fileHandleDao.getReferencedPreviews(previewIds);
		
		assertEquals(new HashSet<>(Arrays.asList(file1Preview.getId())), result);
	}
	
	@Test
	public void testGetReferencedPreviewsWithEmptyInput() {
		Set<Long> previewIds = Collections.emptySet();
		
		// Call under test
		Set<Long> result = fileHandleDao.getReferencedPreviews(previewIds);
	
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void testGetReferencedPreviewsWithNullSet() {
		Set<Long> previewIds = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			fileHandleDao.getReferencedPreviews(previewIds);
		});
		
		assertEquals("The previewIds is required.", ex.getMessage());
	
	}
	
	@Test
	public void testGetBucketAndKeyBatch() {
		String bucket = "bucket";
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setStatus(FileHandleStatus.ARCHIVED.name());
		file1.setKey("key1");
		
		// Different file handle, same preview
		DBOFileHandle file1Copy = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Copy.setBucketName(bucket);
		file1Copy.setStatus(FileHandleStatus.ARCHIVED.name());
		file1Copy.setKey("key1");
		file1Copy.setPreviewId(file1.getPreviewId());
		
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2.setBucketName("anotherBucket");
		file2.setStatus(FileHandleStatus.ARCHIVED.name());
		file2.setKey("key2");
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file1Copy, file2));
	
		Set<Long> ids = new HashSet<>(Arrays.asList(file1.getId(), file1Copy.getId(), file2.getId()));
		
		Set<BucketAndKey> expected = new HashSet<>(Arrays.asList(
				new BucketAndKey().withBucket(bucket).withtKey("key1"),
				new BucketAndKey().withBucket("anotherBucket").withtKey("key2")
		));
		
		// Call under test
		Set<BucketAndKey> result = fileHandleDao.getBucketAndKeyBatch(ids);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testDeleteBatch() {
		String bucket = "bucket";
		
		DBOFileHandle file1 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1.setBucketName(bucket);
		file1.setStatus(FileHandleStatus.ARCHIVED.name());
		file1.setKey("key1");
		
		// Different file handle, same preview
		DBOFileHandle file1Copy = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file1Copy.setBucketName(bucket);
		file1Copy.setStatus(FileHandleStatus.ARCHIVED.name());
		file1Copy.setKey("key1");
		file1Copy.setPreviewId(file1.getPreviewId());
		
		DBOFileHandle file2 = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		file2.setBucketName(bucket);
		file2.setStatus(FileHandleStatus.ARCHIVED.name());
		file2.setKey("key2");
		
		fileHandleDao.createBatchDbo(Arrays.asList(file1, file1Copy, file2));
		
		List<Long> ids = Arrays.asList(file1.getId(), file2.getId());
		
		// Call under test
		fileHandleDao.deleteBatch(new HashSet<>(ids));

		List<DBOFileHandle> files = fileHandleDao.getDBOFileHandlesBatch(Arrays.asList(file1.getId(), file1Copy.getId(), file2.getId()), 0);
		
		assertEquals(1, files.size());
		assertEquals(file1Copy.getId(), files.get(0).getId());
	}
}

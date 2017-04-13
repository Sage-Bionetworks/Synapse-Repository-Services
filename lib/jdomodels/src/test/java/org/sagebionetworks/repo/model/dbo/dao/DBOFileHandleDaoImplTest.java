package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileHandleDaoImplTest {

	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOChangeDAO changeDAO;
	
	private List<String> toDelete;
	private String creatorUserGroupId;
	private String creatorUserGroupId2;
	private Long creatorUserGroupIdL;
	private Long creatorUserGroupId2L;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
		creatorUserGroupIdL = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		creatorUserGroupId = creatorUserGroupIdL.toString();
		assertNotNull(creatorUserGroupId);
		creatorUserGroupId2L = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		creatorUserGroupId2 = creatorUserGroupId2L.toString();
		assertNotNull(creatorUserGroupId2);
	}
	
	@After
	public void after(){
		if(fileHandleDao != null && toDelete != null){
			for(String id: toDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNull() throws MalformedURLException{
		fileHandleDao.createFile(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateFileNameNull() throws MalformedURLException{
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setFileName(null);
		fileHandleDao.createFile(handle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNull(){
		fileHandleDao.delete(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDoesExistNull(){
		fileHandleDao.doesExist(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNull() throws DatastoreException, NotFoundException{
		fileHandleDao.get(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetPreviewIdNullFirst() throws DatastoreException, NotFoundException{
		fileHandleDao.setPreviewId(null, "1");
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
		toDelete.add(id);
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
		toDelete.add(id);
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
		
		toDelete.add(meta1.getId());
		toDelete.add(meta2.getId());
		toDelete.add(meta3.getId());
		
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

	@Test (expected=NotFoundException.class)
	public void testGetCreatorNotFound() throws NotFoundException{
		// Use an invalid file handle id.
		fileHandleDao.getHandleCreator("99999");
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetPreviewFileHandleNotFound() throws NotFoundException{
		// Use an invalid file handle id.
		fileHandleDao.getPreviewFileHandleId("9999");
	}
	
	@Test
	public void testExternalFileCRUD() throws DatastoreException, NotFoundException{
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setFileName("fileName");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();
		toDelete.add(id);
		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}
	
	@Test
	public void testPreviewFileCRUD() throws DatastoreException, NotFoundException{
		PreviewFileHandle meta = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		// Save it
		meta = (PreviewFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();
		toDelete.add(id);
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
		char[] chars = new char[700 - 9 - 4];
		Arrays.fill(chars, 'a');
		meta.setExternalURL("http://" + new String(chars) + ".com");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String id = meta.getId();
		toDelete.add(id);
		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
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
		// Create
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNull(meta);
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
		toDelete.add(id);
		FileHandle clone = fileHandleDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		assertEquals(meta, clone);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testURLWithSpace() throws DatastoreException, NotFoundException {
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("fileName");
		// Create a URL that is is 700 chars long
		meta.setExternalURL("http://synapse.org/some space");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
	}

	@Test
	public void testS3FileWithPreview() throws DatastoreException, NotFoundException{
		// Create the metadata
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("fileName");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (S3FileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		toDelete.add(fileId);
		// Currently there is no preview for this object
		try{
			fileHandleDao.getPreviewFileHandleId(fileId);
			fail("A preview does not exist for this file so a NotFoundException should be thrown.");
		}catch(NotFoundException e){
			// expected
		}
		// Now create a preview for this file.
		PreviewFileHandle preview = new PreviewFileHandle();
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
		preview = (PreviewFileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		String previewId = preview.getId();
		assertNotNull(previewId);
		toDelete.add(previewId);
		// Now set the preview for this file
		fileHandleDao.setPreviewId(fileId, previewId);
		FileHandle clone = fileHandleDao.get(fileId);
		assertNotNull(clone);
		assertTrue(clone instanceof S3FileHandle);
		S3FileHandle s3Clone = (S3FileHandle) clone;
		// The preview ID should be set
		assertEquals(previewId, s3Clone.getPreviewId());
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
	public void testExteranlFileWithPreview() throws DatastoreException, NotFoundException{
		// Create the metadata
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setFileName("fileName");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		toDelete.add(fileId);
		// Now create a preview for this file.
		PreviewFileHandle preview = new PreviewFileHandle();
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
		preview = (PreviewFileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		String previewId = preview.getId();
		assertNotNull(previewId);
		toDelete.add(previewId);
		// Now set the preview for this file
		fileHandleDao.setPreviewId(fileId, previewId);
		FileHandle clone = fileHandleDao.get(fileId);
		assertNotNull(clone);
		assertTrue(clone instanceof ExternalFileHandle);
		ExternalFileHandle s3Clone = (ExternalFileHandle) clone;
		// The preview ID should be set
		assertEquals(previewId, s3Clone.getPreviewId());
	}
	
	@Test (expected=NotFoundException.class)
	public void testSetPrevieWherePreviewDoesNotExist() throws DatastoreException, NotFoundException{
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setFileName("fileName");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		toDelete.add(fileId);
		// Set it to a fake preview
		fileHandleDao.setPreviewId(fileId, "-1");
	}
	
	@Test (expected=NotFoundException.class)
	public void testSetPreviewWhereFileDoesNotExist() throws DatastoreException, NotFoundException{
		// Create a real preview.
		PreviewFileHandle preview = new PreviewFileHandle();
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentType("content type");
		preview.setContentSize(123l);
		preview.setContentMd5("md5");
		preview.setCreatedBy(creatorUserGroupId);
		preview.setFileName("fileName");
		preview.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setEtag(UUID.randomUUID().toString());
		preview = (PreviewFileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		String previewId = preview.getId();
		assertNotNull(previewId);
		toDelete.add(previewId);
		// Set the real preview on a file that does not exist.
		fileHandleDao.setPreviewId("-1", previewId);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testSetIdAlreadyExists(){
		// create one.
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		meta.setEtag(UUID.randomUUID().toString());
		// Save it
		meta = (ExternalFileHandle) fileHandleDao.createFile(meta);
		assertNotNull(meta);
		String fileId = meta.getId();
		assertNotNull(fileId);
		toDelete.add(fileId);
		
		// Now create another with the same ID
		meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setId(fileId);
		fileHandleDao.createFile(meta);
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
		toDelete.add(fileId);
		
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
		toDelete.add(file2Id);
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
		toDelete.add(file3Id);
		assertEquals(new Long(Long.parseLong(file2Id) + 1).toString(), file3Id);
		
	}
	
	@Test
	public void testgetAllFileHandles() throws Exception{
		// Create one without a preview
		S3FileHandle noPreviewHandle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = (S3FileHandle) fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		toDelete.add(noPreviewHandle.getId());
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		withPreview = (S3FileHandle) fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");
		preview = (PreviewFileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		
		// Now get all file handles without previews
		List<String> toFetch = new ArrayList<String>();
		toFetch.add(noPreviewHandle.getId());
		toFetch.add(withPreview.getId());
		FileHandleResults results = fileHandleDao.getAllFileHandles(toFetch, false);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals("With previews false, only two should be returned", 2, results.getList().size());
		assertEquals(noPreviewHandle, results.getList().get(0));
		assertEquals(withPreview, results.getList().get(1));
		
		// Same call but with previews included
		results = fileHandleDao.getAllFileHandles(toFetch, true);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals("With previews true, three should be returned", 3, results.getList().size());
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
		toDelete.add(noPreviewHandle.getId());
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		withPreview = (S3FileHandle) fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");
		preview = (PreviewFileHandle) fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
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
		assertEquals(0, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		assertEquals(1, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		S3FileHandle handle2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle2.setKey(handle1.getKey());
		handle2 = (S3FileHandle) fileHandleDao.createFile(handle2);
		assertEquals(2, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		fileHandleDao.delete(handle2.getId());
		assertEquals(1, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		fileHandleDao.delete(handle1.getId());
		assertEquals(0, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
	}

	@Test
	public void testCreateFileHandleWithNoPreview() {
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setPreviewId(handle.getId());
		handle = (S3FileHandle) fileHandleDao.createFile(handle);
		toDelete.add(handle.getId());
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
		toDelete.add(pfh.getId());
		ProxyFileHandle clone = (ProxyFileHandle) fileHandleDao.get(pfh.getId());
		assertEquals(pfh, clone);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateBatchWithNullBatch() {
		fileHandleDao.createBatch(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateBatchWithEmptyBatch() {
		fileHandleDao.createBatch(new ArrayList<FileHandle>(0));
	}

	@Test
	public void testCreateBatch() {
		changeDAO.deleteAllChanges();
		long startChangeNumber = changeDAO.getCurrentChangeNumber();
		// imitate database time
		Timestamp now = new Timestamp(System.currentTimeMillis()/1000*1000);
		ArrayList<FileHandle> batch = new ArrayList<FileHandle>();
		S3FileHandle s3 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		s3.setId(""+idGenerator.generateNewId(IdType.FILE_IDS));
		s3.setEtag(UUID.randomUUID().toString());
		s3.setCreatedOn(now);
		batch.add(s3);
		ExternalFileHandle external = new ExternalFileHandle();
		external.setCreatedBy(creatorUserGroupId);
		external.setExternalURL("http://google.com");
		external.setFileName("fileName");
		external.setId(""+idGenerator.generateNewId(IdType.FILE_IDS));
		external.setEtag(UUID.randomUUID().toString());
		external.setCreatedOn(now);
		batch.add(external);
		fileHandleDao.createBatch(batch);
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
}

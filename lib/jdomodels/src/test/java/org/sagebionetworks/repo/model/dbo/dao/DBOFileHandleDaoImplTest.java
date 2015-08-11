package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileHandleDaoImplTest {

	@Autowired
	private FileHandleDao fileHandleDao;
	
	private List<String> toDelete;
	private String creatorUserGroupId;
	private String creatorUserGroupId2;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		assertNotNull(creatorUserGroupId);
		creatorUserGroupId2 = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
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
		S3FileHandle handle = new S3FileHandle();
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
		S3FileHandle meta = TestUtils.createS3FileHandle(creatorUserGroupId);
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		S3FileHandle meta = TestUtils.createS3FileHandle(creatorUserGroupId);
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		S3FileHandle meta1 = TestUtils.createS3FileHandle(creatorUserGroupId);
		meta1 = fileHandleDao.createFile(meta1);
		S3FileHandle meta2 = TestUtils.createS3FileHandle(creatorUserGroupId);
		meta2 = fileHandleDao.createFile(meta2);
		S3FileHandle meta3 = TestUtils.createS3FileHandle(creatorUserGroupId2);
		meta3 = fileHandleDao.createFile(meta3);
		toDelete.add(meta1.getId());
		toDelete.add(meta2.getId());
		toDelete.add(meta3.getId());

		Multimap<String, String> lookupCreator = fileHandleDao.getHandleCreators(Lists.newArrayList(meta3.getId(), meta1.getId(),
				meta2.getId()));
		assertEquals(2, lookupCreator.get(creatorUserGroupId).size());
		assertEquals(1, lookupCreator.get(creatorUserGroupId2).size());
		assertTrue(lookupCreator.get(creatorUserGroupId).contains(meta1.getId()));
		assertTrue(lookupCreator.get(creatorUserGroupId).contains(meta2.getId()));
		assertTrue(lookupCreator.get(creatorUserGroupId2).contains(meta3.getId()));
	}

	@Test
	public void testGetCreatorBatches() throws NotFoundException {
		S3FileHandle meta1 = TestUtils.createS3FileHandle(creatorUserGroupId);
		meta1 = fileHandleDao.createFile(meta1);
		S3FileHandle meta2 = TestUtils.createS3FileHandle(creatorUserGroupId2);
		meta2 = fileHandleDao.createFile(meta2);
		toDelete.add(meta1.getId());
		toDelete.add(meta2.getId());

		final int SIZE = 100;
		List<String> handles = Lists.newArrayListWithCapacity(SIZE * 2);
		for (int i = 0; i < SIZE * 2; i += 2) {
			handles.add(meta1.getId());
			handles.add(meta2.getId());
		}
		Multimap<String, String> lookupCreator = fileHandleDao.getHandleCreators(handles);
		// because the inclause has repeated ids, only two rows are returned per sql query.
		// if batching works, each id returns twice
		assertEquals(2, lookupCreator.get(creatorUserGroupId).size());
		assertEquals(2, lookupCreator.get(creatorUserGroupId2).size());
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		PreviewFileHandle meta = TestUtils.createPreviewFileHandle(creatorUserGroupId);
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		// Create
		meta = fileHandleDao.createFile(meta);
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
		// Create
		meta = fileHandleDao.createFile(meta);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		// Save it
		preview = fileHandleDao.createFile(preview);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		// Save it
		preview = fileHandleDao.createFile(preview);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		preview = fileHandleDao.createFile(preview);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		// Save it
		meta = fileHandleDao.createFile(meta);
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
		meta = fileHandleDao.createFile(meta);
		String file2Id = meta.getId();
		assertNotNull(file2Id);
		toDelete.add(file2Id);
		// Now if we create another one its ID should be one larger than the previous
		meta = new ExternalFileHandle();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com/3");
		meta.setFileName("fileName");
		meta = fileHandleDao.createFile(meta);
		meta.setFileName("fileName");
		String file3Id = meta.getId();
		assertNotNull(file3Id);
		toDelete.add(file3Id);
		assertEquals(new Long(Long.parseLong(file2Id) + 1).toString(), file3Id);
		
	}
	
	@Test
	public void testgetAllFileHandles() throws Exception{
		// Create one without a preview
		S3FileHandle noPreviewHandle = TestUtils.createS3FileHandle(creatorUserGroupId);
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		toDelete.add(noPreviewHandle.getId());
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId);
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId);
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
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
		S3FileHandle noPreviewHandle = TestUtils.createS3FileHandle(creatorUserGroupId);
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		toDelete.add(noPreviewHandle.getId());
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId);
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId);
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
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
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId);
		handle1.setKey(UUID.randomUUID().toString());
		assertEquals(0, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		handle1 = fileHandleDao.createFile(handle1);
		assertEquals(1, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		S3FileHandle handle2 = TestUtils.createS3FileHandle(creatorUserGroupId);
		handle2.setKey(handle1.getKey());
		handle2 = fileHandleDao.createFile(handle2);
		assertEquals(2, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		fileHandleDao.delete(handle2.getId());
		assertEquals(1, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
		fileHandleDao.delete(handle1.getId());
		assertEquals(0, fileHandleDao.getS3objectReferenceCount(handle1.getBucketName(), handle1.getKey()));
	}

	@Test
	public void testCreateFileHandleWithNoPreview() {
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId);
		handle = fileHandleDao.createFile(handle, false);
		toDelete.add(handle.getId());
		assertEquals(handle.getId(), handle.getPreviewId());
	}
}

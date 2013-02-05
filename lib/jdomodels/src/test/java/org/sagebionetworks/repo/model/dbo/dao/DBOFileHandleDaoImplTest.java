package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileHandleDaoImplTest {

	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<String> toDelete;
	String creatorUserGroupId;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
		creatorUserGroupId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(creatorUserGroupId);
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetPreviewIdNullSecond() throws DatastoreException, NotFoundException{
		fileHandleDao.setPreviewId("1", null);
	}
	
	@Test
	public void testS3FileCURD() throws DatastoreException, NotFoundException{
		// Create the metadata
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("foobar.txt");
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
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("foobar.txt");
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

	@Test (expected=NotFoundException.class)
	public void testGetCreatorNotFound() throws NotFoundException{
		// Use an invalid file handle id.
		String lookupCreator = fileHandleDao.getHandleCreator("99999");
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
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("preview.jpg");
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
		// Create a URL that is is 2K chars long
		char[] chars = new char[2000-9];
		Arrays.fill(chars, 'a');
		meta.setExternalURL("http://"+new String(chars));
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

}

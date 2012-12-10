package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.ExternalFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileMetadataDaoTest {

	@Autowired
	FileMetadataDao fileMetadataDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<Long> toDelete;
	Long creatorUserGroupId;
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		if(fileMetadataDao != null && toDelete != null){
			for(Long id: toDelete){
				fileMetadataDao.delete(id);
			}
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNull(){
		fileMetadataDao.create(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNull(){
		fileMetadataDao.delete(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDoesExistNull(){
		fileMetadataDao.doesExist(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNull() throws DatastoreException, NotFoundException{
		fileMetadataDao.get(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetPreviewIdNullFirst() throws DatastoreException, NotFoundException{
		fileMetadataDao.setPreviewId(null, 1l);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetPreviewIdNullSecond() throws DatastoreException, NotFoundException{
		fileMetadataDao.setPreviewId(1l, null);
	}
	
	@Test
	public void testS3FileCURD() throws DatastoreException, NotFoundException{
		// Create the metadata
		S3FileMetadata meta = new S3FileMetadata();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		// Save it
		Long id = fileMetadataDao.create(meta);
		assertNotNull(id);
		toDelete.add(id);
		FileMetadata clone = fileMetadataDao.get(id);
		// Does the clone match the expected.
		validateExpected(meta, clone);
	}


	
	@Test
	public void testExternalFileCRUD() throws DatastoreException, NotFoundException{
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		// Save it
		Long id = fileMetadataDao.create(meta);
		assertNotNull(id);
		toDelete.add(id);
		FileMetadata clone = fileMetadataDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		validateExpected(meta, clone);
	}
	
	@Test
	public void testPreviewFileCRUD() throws DatastoreException, NotFoundException{
		PreviewFileMetadata meta = new PreviewFileMetadata();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		// Save it
		Long id = fileMetadataDao.create(meta);
		assertNotNull(id);
		toDelete.add(id);
		FileMetadata clone = fileMetadataDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		validateExpected(meta, clone);
	}
	
	@Test
	public void testLongURL() throws DatastoreException, NotFoundException{
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		// Create a URL that is is 2K chars long
		char[] chars = new char[2000];
		Arrays.fill(chars, 'a');
		meta.setExternalURL(new String(chars));
		// Save it
		Long id = fileMetadataDao.create(meta);
		assertNotNull(id);
		toDelete.add(id);
		FileMetadata clone = fileMetadataDao.get(id);
		assertNotNull(clone);
		// Does the clone match the expected.
		validateExpected(meta, clone);
	}
	
	@Test
	public void testS3FileWithPreview() throws DatastoreException, NotFoundException{
		// Create the metadata
		S3FileMetadata meta = new S3FileMetadata();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		// Save it
		Long fileId = fileMetadataDao.create(meta);
		assertNotNull(fileId);
		toDelete.add(fileId);
		// Now create a preview for this file.
		PreviewFileMetadata preview = new PreviewFileMetadata();
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentType("content type");
		preview.setContentSize(123l);
		preview.setContentMd5("md5");
		preview.setCreatedBy(creatorUserGroupId);
		// Save it
		Long previewId = fileMetadataDao.create(preview);
		assertNotNull(previewId);
		toDelete.add(previewId);
		// Now set the preview for this file
		fileMetadataDao.setPreviewId(fileId, previewId);
		FileMetadata clone = fileMetadataDao.get(fileId);
		assertNotNull(clone);
		assertTrue(clone instanceof S3FileMetadata);
		S3FileMetadata s3Clone = (S3FileMetadata) clone;
		// The preview ID should be set
		assertEquals(previewId, s3Clone.getPreviewId());
	}
	
	@Test
	public void testExteranlFileWithPreview() throws DatastoreException, NotFoundException{
		// Create the metadata
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		// Save it
		Long fileId = fileMetadataDao.create(meta);
		assertNotNull(fileId);
		toDelete.add(fileId);
		// Now create a preview for this file.
		PreviewFileMetadata preview = new PreviewFileMetadata();
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentType("content type");
		preview.setContentSize(123l);
		preview.setContentMd5("md5");
		preview.setCreatedBy(creatorUserGroupId);
		// Save it
		Long previewId = fileMetadataDao.create(preview);
		assertNotNull(previewId);
		toDelete.add(previewId);
		// Now set the preview for this file
		fileMetadataDao.setPreviewId(fileId, previewId);
		FileMetadata clone = fileMetadataDao.get(fileId);
		assertNotNull(clone);
		assertTrue(clone instanceof ExternalFileMetadata);
		ExternalFileMetadata s3Clone = (ExternalFileMetadata) clone;
		// The preview ID should be set
		assertEquals(previewId, s3Clone.getPreviewId());
	}
	
	@Test (expected=NotFoundException.class)
	public void testSetPrevieWherePreviewDoesNotExist() throws DatastoreException, NotFoundException{
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		// Save it
		Long fileId = fileMetadataDao.create(meta);
		assertNotNull(fileId);
		toDelete.add(fileId);
		// Set it to a fake preview
		fileMetadataDao.setPreviewId(fileId, -1l);
	}
	
	@Test (expected=NotFoundException.class)
	public void testSetPreviewWhereFileDoesNotExist() throws DatastoreException, NotFoundException{
		// Create a real preview.
		PreviewFileMetadata preview = new PreviewFileMetadata();
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentType("content type");
		preview.setContentSize(123l);
		preview.setContentMd5("md5");
		preview.setCreatedBy(creatorUserGroupId);
		Long previewId = fileMetadataDao.create(preview);
		assertNotNull(previewId);
		toDelete.add(previewId);
		// Set the real preview on a file that does not exist.
		fileMetadataDao.setPreviewId(-1l, previewId);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testSetIdAlreadyExists(){
		// create one.
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		// Save it
		Long fileId = fileMetadataDao.create(meta);
		assertNotNull(fileId);
		toDelete.add(fileId);
		
		// Now create another with the same ID
		meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		meta.setId(fileId);
		fileMetadataDao.create(meta);
	}
	
	/**
	 * This tests that when we set the ID, using a value that has not been used
	 * that the Idgenerator.reserveId is called to ensure that the ID and anything less than it is 
	 * never used again.
	 */
	@Test
	public void testSetIdBeyondRange(){
		// create one.
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com");
		// Save it
		Long fileId = fileMetadataDao.create(meta);
		assertNotNull(fileId);
		toDelete.add(fileId);
		
		// Now create another with the same ID
		meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com/2");
		// Set the ID beyond what the current ID generator range
		meta.setId(fileId+10l);
		Long file2Id = fileMetadataDao.create(meta);
		assertNotNull(file2Id);
		toDelete.add(file2Id);
		// Now if we create another one its ID should be one larger than the previous
		meta = new ExternalFileMetadata();
		meta.setCreatedBy(creatorUserGroupId);
		meta.setExternalURL("http://google.com/3");
		Long file3Id = fileMetadataDao.create(meta);
		assertNotNull(file3Id);
		toDelete.add(file3Id);
		assertEquals(new Long(file2Id + 1), file3Id);
		
	}
	
	
	/**
	 * Helper to validate the expected metadata is the same as the clone.
	 * @param meta
	 * @param clone
	 */
	public void validateExpected(FileMetadata meta, FileMetadata clone) {
		assertNotNull(clone);
		// The ID will get filled in.
		assertNotNull(clone.getId());
		// Created on will get filled in by the DB
		assertNotNull(clone.getCreatedOn());
		// Add ID and createOn to the clone to make the equals check work.
		meta.setCreatedOn(clone.getCreatedOn());
		meta.setId(clone.getId());
		// They should now be the same.
		assertEquals(meta, clone);
	}
}

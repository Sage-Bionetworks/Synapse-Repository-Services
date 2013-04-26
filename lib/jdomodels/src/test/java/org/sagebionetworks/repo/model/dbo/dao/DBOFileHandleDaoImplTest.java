package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.migration.MigatableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.util.BinaryUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileHandleDaoImplTest {

	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<String> toDelete;
	String creatorUserGroupId;
	
	@Autowired
	MigatableTableDAO migatableTableDAO;
	
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
		S3FileHandle meta = createS3FileHandle();
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
		S3FileHandle meta = createS3FileHandle();
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
	
	@Test (expected=NotFoundException.class)
	public void testGetPreviewFileHandleNotFound() throws NotFoundException{
		// Use an invalid file handle id.
		String prewviewId = fileHandleDao.getPreviewFileHandleId("9999");
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
		PreviewFileHandle meta = createPreviewFileHandle();
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
		S3FileHandle noPreviewHandle = createS3FileHandle();
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		toDelete.add(noPreviewHandle.getId());
		// The one will have a preview
		S3FileHandle withPreview = createS3FileHandle();
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = createPreviewFileHandle();
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
	public void testgetMigrationObjectDatas() throws Exception{
		long startCount = fileHandleDao.getCount();
		// Create one without a preview
		S3FileHandle noPreviewHandle = createS3FileHandle();
		noPreviewHandle.setFileName("newPreview.txt");
		noPreviewHandle = fileHandleDao.createFile(noPreviewHandle);
		assertNotNull(noPreviewHandle);
		toDelete.add(noPreviewHandle.getId());
		// The one will have a preview
		S3FileHandle withPreview = createS3FileHandle();
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = createPreviewFileHandle();
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		// Get the current count
		long currentCount =  fileHandleDao.getCount();
		assertTrue(startCount +3l == currentCount);
		// Get all of the migration data
		QueryResults<MigratableObjectData> results = fileHandleDao.getMigrationObjectData(startCount, Long.MAX_VALUE, true);
		System.out.println(results);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(currentCount, results.getTotalNumberOfResults());
		assertEquals(3l, results.getResults().size());
		// the results must be sorted by ID descending so that previews migrate before the handles that depend on them.
		MigratableObjectData mod = results.getResults().get(0);
		assertEquals(preview.getId(),  mod.getId().getId());
		assertEquals(preview.getEtag(), mod.getEtag());
		// The should have no previews
		assertNotNull(mod.getDependencies());
		assertEquals(0, mod.getDependencies().size());
		// next item
		mod = results.getResults().get(1);
		assertEquals(withPreview.getId(),  mod.getId().getId());
		assertEquals(withPreview.getEtag(),  mod.getEtag());
		// This should depend on the preview
		assertNotNull(mod.getDependencies());
		assertEquals(1, mod.getDependencies().size());
		MigratableObjectDescriptor depend = mod.getDependencies().iterator().next();
		assertNotNull(depend);
		assertEquals(preview.getId(), depend.getId());
		assertEquals(MigratableObjectType.FILEHANDLE, depend.getType());
		// preview
		mod = results.getResults().get(2);
		assertEquals(noPreviewHandle.getId(),  mod.getId().getId());
		assertEquals(noPreviewHandle.getEtag(),  mod.getEtag());
		// The preview should have no dependencies.
		assertNotNull(mod.getDependencies());
		assertEquals(0, mod.getDependencies().size());
		
		// test paging.
		// Only select the second to last.
		results = fileHandleDao.getMigrationObjectData(startCount+2, 1, true);
		System.out.println(results);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(currentCount, results.getTotalNumberOfResults());
		assertEquals(1l, results.getResults().size());
		assertEquals(noPreviewHandle.getId(),  results.getResults().get(0).getId().getId());
		assertEquals(noPreviewHandle.getEtag(),  results.getResults().get(0).getEtag());
		
		// Check the type
		assertEquals(MigratableObjectType.FILEHANDLE, fileHandleDao.getMigratableObjectType());
	}
	
	@Test
	public void testBackupRestore() throws Exception{
		long startCount = fileHandleDao.getCount();
		// The one will have a preview
		S3FileHandle withPreview = createS3FileHandle();
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = createPreviewFileHandle();
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());

		Map<String, FileHandleBackup> backupMap = new HashMap<String, FileHandleBackup>();
		// Get the list of items to restore.  The order is important.  Preview file handles must be listed before their dependent files handles.
		QueryResults<MigratableObjectData> results = fileHandleDao.getMigrationObjectData(startCount, 2l, true);
		List<MigratableObjectData> toMigrate = results.getResults();
		// Capture backups in a map
		for(MigratableObjectData mob: toMigrate){
			FileHandleBackup backup = fileHandleDao.getFileHandleBackup(mob.getId().getId());
			assertNotNull(backup);
			backupMap.put(mob.getId().getId(), backup);
		}
		// Delete both
		fileHandleDao.delete(withPreview.getId());
		fileHandleDao.delete(preview.getId());
		// Now migrate them in order of the list
		for(MigratableObjectData mob: toMigrate){
			FileHandleBackup backup = backupMap.get(mob.getId().getId());
			assertNotNull(backup);
			// Do a create
			assertTrue("For a create, true should have been returned",fileHandleDao.createOrUpdateFromBackup(backup));
			// Do an update
			assertFalse("For an update, false should have been returned",fileHandleDao.createOrUpdateFromBackup(backup));
		}

		// No data should have been lost in the process
		S3FileHandle withPrevieClone = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		assertEquals(withPreview, withPrevieClone);
		// Now the preview
		PreviewFileHandle previewClone = (PreviewFileHandle) fileHandleDao.get(preview.getId());
		assertEquals(preview, previewClone);
		
	}
	
	@Test
	public void testTableMigration() throws Exception {
		long startCount = fileHandleDao.getCount();
		long migrationCount = migatableTableDAO.getCount(MigratableTableType.FILE_HANDLE);
		assertEquals(startCount, migrationCount);
		// The one will have a preview
		S3FileHandle withPreview = createS3FileHandle();
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = createPreviewFileHandle();
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		
		// Now list all of the objects
		QueryResults<RowMetadata> totalList = migatableTableDAO.listRowMetadata(MigratableTableType.FILE_HANDLE, 1000, startCount);
		assertNotNull(totalList);
		assertEquals(startCount+2,  totalList.getTotalNumberOfResults());
		assertNotNull(totalList.getResults());
		assertEquals(2, totalList.getResults().size());
		System.out.println(totalList.getResults());
		// The preview should be first
		RowMetadata row = totalList.getResults().get(0);
		assertEquals(preview.getId(), row.getId());
		assertEquals(preview.getEtag(), row.getEtag());
		// Followed by the withPreview
		row = totalList.getResults().get(1);
		assertEquals(withPreview.getId(), row.getId());
		assertEquals(withPreview.getEtag(), row.getEtag());
		
		// Now list the deltas
		List<String> idsToFind = new LinkedList<String>();
		// This should not exist
		idsToFind.add(""+(Long.MAX_VALUE - 10));
		idsToFind.add(preview.getId());
		idsToFind.add(withPreview.getId());
		// This should not exist
		idsToFind.add(""+(Long.MAX_VALUE - 101));
		// Get the detla
		List<RowMetadata> delta = migatableTableDAO.listDeltaRowMetadata(MigratableTableType.FILE_HANDLE, idsToFind);
		assertNotNull(delta);
		assertEquals(2, delta.size());
		// The preview should be first
		row = delta.get(0);
		assertEquals(preview.getId(), row.getId());
		assertEquals(preview.getEtag(), row.getEtag());
		// Followed by the withPreview
		row = delta.get(1);
		assertEquals(withPreview.getId(), row.getId());
		assertEquals(withPreview.getEtag(), row.getEtag());
		
		// Get the full back object
		List<String> idsToBackup = new LinkedList<String>();
		idsToBackup.add(preview.getId());
		idsToBackup.add(withPreview.getId());
		List<DBOFileHandle> backupList = migatableTableDAO.getBackupBatch(DBOFileHandle.class, idsToBackup);
		assertNotNull(backupList);
		assertEquals(2, backupList.size());
		// preview
		DBOFileHandle dbfh = backupList.get(0);
		assertEquals(preview.getId(), ""+dbfh.getId());
		//with preview.
		dbfh = backupList.get(1);
		assertEquals(withPreview.getId(), ""+dbfh.getId());
	}
	
	@Test
	public void testFindFileHandleWithKeyAndMD5(){
		S3FileHandle handle = createS3FileHandle();
		// Use a random UUID for the key
		handle.setKey(UUID.randomUUID().toString());
		// Calculate an MD5 from the key.
		String md5 = calculateMD5(handle.getKey());
		handle.setContentMd5(md5);
		// Create the handle
		handle = fileHandleDao.createFile(handle);
		System.out.println(handle);
		toDelete.add(handle.getId());
		// Make sure we can find it
		List<String> list = fileHandleDao.findFileHandleWithKeyAndMD5(handle.getKey(), handle.getContentMd5());
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(handle.getId(), list.get(0));
	}
	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public S3FileHandle createS3FileHandle() {
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("foobar.txt");
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public PreviewFileHandle createPreviewFileHandle() {
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("preview.jpg");
		return meta;
	}
	
	/**
	 * Calcualte the MD5 digest of a given string.
	 * @param tocalculate
	 * @return
	 */
	public String calculateMD5(String tocalculate){
		try {
			MessageDigest digetst = MessageDigest.getInstance("MD5");
			byte[] bytes = digetst.digest(tocalculate.getBytes("UTF-8"));
			return  BinaryUtils.toHex(bytes);	
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}
}

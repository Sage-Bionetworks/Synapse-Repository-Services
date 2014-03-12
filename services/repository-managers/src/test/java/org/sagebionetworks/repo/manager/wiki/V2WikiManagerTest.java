package org.sagebionetworks.repo.manager.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiMarkdownVersion;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test for the WikiManager
 * @author hso
 *
 */
public class V2WikiManagerTest {
	@Autowired
	V2WikiPageDao mockWikiDao;
	@Autowired
	V2WikiManagerImpl wikiManager;
	@Autowired
	AuthorizationManager mockAuthManager;
	@Autowired
	FileHandleDao mockFileDao;
	WikiPageKey key;
	UserInfo user;
	
	@Before
	public void before() {
		user = new UserInfo(false, "987");
		// setup the mocks
		mockWikiDao = Mockito.mock(V2WikiPageDao.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		mockFileDao = Mockito.mock(FileHandleDao.class);
		key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		wikiManager = new V2WikiManagerImpl(mockWikiDao, mockAuthManager, mockFileDao);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.createWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test
	public void testCreateAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		V2WikiPage page = new V2WikiPage();
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		page.setMarkdownFileHandleId(markdown.getId());
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		// Was it passed to the DAO?
		List<String> newIds = new ArrayList<String>();
		newIds.add(markdown.getId());
		verify(mockWikiDao, times(1)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, newIds);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateWikiPageMarkdownUnauthorized() throws DatastoreException, NotFoundException{
		// Setup filehandles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("one");
		// Set the user as the creator of one
		markdown.setCreatedBy("007");
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(one.getId());
		page.setMarkdownFileHandleId(markdown.getId());
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(0)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, new ArrayList<String>());
	}

	@Test
	public void testCreateModifiedByCreatedBy() throws DatastoreException, NotFoundException{
		// setup allow
		V2WikiPage page = new V2WikiPage();
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockWikiDao.create(any(V2WikiPage.class), any(HashMap.class), any(String.class), any(ObjectType.class), any(ArrayList.class))).thenReturn(page);
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		page.setMarkdownFileHandleId(markdown.getId());

		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		V2WikiPage result = wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		assertNotNull(result);
		assertEquals("CreatedBy should have set", user.getId().toString(), result.getCreatedBy());
		assertEquals("ModifiedBy should have set", user.getId().toString(), result.getModifiedBy());
		// Was it passed to the DAO?
		List<String> newIds = new ArrayList<String>();
		newIds.add(markdown.getId());
		verify(mockWikiDao, times(1)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, newIds);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test
	public void testUpadateAuthorized() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etag");
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		page.setMarkdownFileHandleId(markdown.getId());
		// setup allow
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, page);
		// Was it passed to the DAO?
		List<String> newIds = new ArrayList<String>();
		newIds.add(markdown.getId());
		verify(mockWikiDao, times(1)).updateWikiPage(page,new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, newIds);
		// The lock must be acquired
		verify(mockWikiDao, times(1)).lockForUpdate("000");
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testUpdateConflict() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		// return a different etag to trigger a conflict
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etagUpdate!!!");
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, page);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateWikiPageWithNewAttachment() throws DatastoreException, NotFoundException{
		String ownerId = "556";
	    ObjectType ownerType = ObjectType.EVALUATION;
	    String wikiId = "0";
	    
		// Setup filehandles
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("one");
		// Set creator
		markdown.setCreatedBy("007");
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		// Add new attachment, created by a DIFFERENT user, but markdown remains the same.
		S3FileHandle one = new S3FileHandle();
		one.setId("2");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		
		V2WikiPage page = new V2WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(one.getId());
		page.setMarkdownFileHandleId(markdown.getId());
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		List<Long> allFileHandleIds = new LinkedList<Long>();
	    when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(allFileHandleIds);
		List<Long> allMarkdownFileHandleIds = new LinkedList<Long>();
		// Pretend markdown was already successfully uploaded by another user
		allMarkdownFileHandleIds.add(new Long(markdown.getId()));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(allMarkdownFileHandleIds);
		
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		
	    wikiManager.updateWikiPage(user, ownerId, ownerType, page);
		List<String> newIds = new ArrayList<String>();
	    newIds.add(one.getId());
		Map<String, FileHandle> fileHandleMap = wikiManager.buildFileNameMap(page);
	    verify(mockWikiDao, times(1)).updateWikiPage(page, fileHandleMap, ownerId, ownerType, newIds);   
	}	
	
	@Test (expected=UnauthorizedException.class)
	public void testGetUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getWikiPage(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"), null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetRootUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		when(mockWikiDao.getRootWiki("123",  ObjectType.EVALUATION)).thenReturn(345l);
		wikiManager.getRootWikiPage(new UserInfo(false), "123",ObjectType.EVALUATION);
	}
	
	@Test
	public void testGetAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getWikiPage(new UserInfo(false),key, null);
		verify(mockWikiDao, times(1)).get(key, null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetVersionUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getWikiPage(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
	}
	
	@Test
	public void testGetVersionAuthorized() throws UnauthorizedException, NotFoundException {
		Long version = new Long(0);
		WikiPageKey key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getWikiPage(new UserInfo(false),key, version);
		verify(mockWikiDao, times(1)).get(key, version);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testFileHandleIdForFileNameUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getFileHandleIdForFileName(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"), "fileName", null);
	}
	
	@Test
	public void testFileHandleIdForFileNameAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getFileHandleIdForFileName(new UserInfo(false),key,"fileName", null);
		verify(mockWikiDao, times(1)).getWikiAttachmentFileHandleForFileName(key, "fileName", null);
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test (expected=UnauthorizedException.class)
	public void testMarkdownFileHandleIdForVersionUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getMarkdownFileHandleId(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test
	public void testMarkdownFileHandleIdForVersion() throws UnauthorizedException, NotFoundException {
		WikiPageKey key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getMarkdownFileHandleId(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
		verify(mockWikiDao, times(1)).getMarkdownHandleId(key, new Long(0));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetTreeUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.EVALUATION, null, null);
	}
	
	@Test
	public void testGetTreeAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.EVALUATION, null, null);
		verify(mockWikiDao, times(1)).getHeaderTree(any(String.class), any(ObjectType.class));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.deleteWiki(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test
	public void testDeleteAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.deleteWiki(new UserInfo(false),key);
		verify(mockWikiDao, times(1)).delete(key);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetAttachmentFileHandlesUnauthroized() throws DatastoreException, NotFoundException{
		// deny
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(false);
		// Ready to make the call
		wikiManager.getAttachmentFileHandles(user, key, null);
	}
	
	@Test
	public void testGetAttachmentFileHandles() throws DatastoreException, NotFoundException{
		S3FileHandle handleOne = new S3FileHandle();
		handleOne.setId("1");
		handleOne.setFileName("One");
		S3FileHandle handleTwo = new S3FileHandle();
		handleTwo.setId("2");
		handleTwo.setFileName("Two");
		List<String> ids = new ArrayList<String>();
		ids.add("2");
		ids.add("1");
		
		FileHandleResults expectedResults = new FileHandleResults();
		expectedResults.setList(new LinkedList<FileHandle>());
		expectedResults.getList().add(handleTwo);
		expectedResults.getList().add(handleOne);
		when(mockFileDao.getAllFileHandles(ids, true)).thenReturn(expectedResults);
		// Setup the wiki
		WikiPageKey key = new WikiPageKey("syn123", ObjectType.WIKI, "456");
		List<String> wikiHandleIds = new LinkedList<String>();
		// The list only contains the S3 handles and not the previews
		wikiHandleIds.add("2");
		wikiHandleIds.add("1");
		when(mockWikiDao.getWikiFileHandleIds(key, null)).thenReturn(wikiHandleIds);
		
		// Allow
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(true);
		// Ready to make the call
		FileHandleResults results = wikiManager.getAttachmentFileHandles(user, key, null);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals("There should be 2 file handles.",2, results.getList().size());
		
		FileHandle handle = results.getList().get(0);
		assertNotNull(handle);
		assertEquals("2", handle.getId());
		assertTrue(handle instanceof S3FileHandle);
		
		// Last should be handleOne
		handle = results.getList().get(1);
		assertNotNull(handle);
		assertEquals("1", handle.getId());
		assertTrue(handle instanceof S3FileHandle);
		
		// Test getting the attachments for another version
		List<String> versionIds = new LinkedList<String>();
		wikiHandleIds.add("1");
		
		FileHandleResults expectedVersionResults = new FileHandleResults();
		expectedVersionResults.setList(new LinkedList<FileHandle>());
		expectedVersionResults.getList().add(handleOne);
		
		when(mockWikiDao.getWikiFileHandleIds(key, new Long(1))).thenReturn(versionIds);
		when(mockFileDao.getAllFileHandles(versionIds, true)).thenReturn(expectedVersionResults);
		FileHandleResults versionResults = wikiManager.getAttachmentFileHandles(user, key, new Long(1));
		assertNotNull(versionResults);
		assertEquals("1", versionResults.getList().get(0).getId());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetAttachmentFileHandlesForVersionUnauthroized() throws DatastoreException, NotFoundException{
		// deny
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(false);
		// Ready to make the call
		wikiManager.getAttachmentFileHandles(user, key, new Long(0));
	}
	
	@Test
	public void testBuildFileNameMap() throws DatastoreException, NotFoundException{
		// Create a WikiPage that includes all three
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		
		Map<String, FileHandle> map = wikiManager.buildFileNameMap(page);
		assertTrue(map.size() == 0);
		
		// Setup some file handles with duplicate names.  When there are duplicate names
		// the manager should use the newest one.
		// one
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("duplicateName");
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		// This one has a newer date so it should get used.
		two.setCreatedOn(new Date(2));
		two.setFileName("duplicateName");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		// three
		S3FileHandle three = new S3FileHandle();
		three.setId("3");
		three.setCreatedOn(new Date(2));
		three.setFileName("uniqueName");
		when(mockFileDao.get(three.getId())).thenReturn(three);
	
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(three.getId());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		// Now test the manager;
		Map<String, FileHandle> nameMap = wikiManager.buildFileNameMap(page);
		assertNotNull(nameMap);
		// the older duplicate should have been removed.
		assertEquals(2, nameMap.size());
		assertEquals("The newer FileHandle should have been used when a duplicate fileName is encountered.", two, nameMap.get("duplicateName"));
		assertEquals(three, nameMap.get("uniqueName"));
		
		// Different order, same result
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(one.getId());
		page.getAttachmentFileHandleIds().add(three.getId());
		page.getAttachmentFileHandleIds().add(two.getId());
		Map<String, FileHandle> nameMap2 = wikiManager.buildFileNameMap(page);
		assertNotNull(nameMap2);
		// the older duplicate should have been removed.
		assertEquals(2, nameMap2.size());
		assertEquals("The newer FileHandle should have been used when a duplicate fileName is encountered.", two, nameMap2.get("duplicateName"));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateWikiPageFileHandleUnauthorized() throws DatastoreException, NotFoundException{
		// Setup two filehandles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy("007");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("one");
		// Set the user as the creator of one
		markdown.setCreatedBy("007");
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		page.setMarkdownFileHandleId(markdown.getId());
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(0)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, new ArrayList<String>());
	}
	
	/**
	 * This is the same test as testCreateWikiPageFileHandleUnauthorized() except this time
	 * the user owns both FileHandles so they are authorized to use them.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testCreateWikiPageFileHandleAuthorized() throws DatastoreException, NotFoundException{
		// Setup two filehandles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set this one to be created by the owner
		two.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(two.getId())).thenReturn(two);
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("one");
		// Set the user as the creator of one
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setMarkdownFileHandleId(markdown.getId());
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		Map<String, FileHandle> fileNameToHandleMap = new HashMap<String, FileHandle>();
		fileNameToHandleMap.put("one", one);
		fileNameToHandleMap.put("two", two);
		List<String> newFileHandleIds = new ArrayList<String>();
		newFileHandleIds.add(one.getId());
		newFileHandleIds.add(two.getId());
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(1)).create(any(V2WikiPage.class), any(Map.class), any(String.class), any(ObjectType.class), any(ArrayList.class));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateWikiPageFileHandleUnauthorized() throws DatastoreException, NotFoundException{
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wikiId);
		// Setup two filehandles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy("007");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		// markdown
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("3");
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("one");
		// Set the user as the creator of one
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		V2WikiPage page = new V2WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setMarkdownFileHandleId(markdown.getId());
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		// For this case the first file handle is already on the wiki-page, but the second is not
		// Since this user is not the creator of the second this call should fail.
		List<Long> allFileHandleIds = new LinkedList<Long>();
		allFileHandleIds.add(new Long(one.getId()));
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(allFileHandleIds);
		List<Long> allMarkdownFileHandleIds = new LinkedList<Long>();
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(allMarkdownFileHandleIds);
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(user, ownerId, ownerType, page);
	}
	
	/**
	 * Note this is the same test as testUpdateWikiPageFileHandleUnauthorized() except this time the user is only adding
	 * a FileHandle that they created.  There is a FileHandle that they did not create already on the WikiPage, but this
	 * should not block them as it is already there.
	 * 
	 * This is a test that the authorization is only enforced for changes.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test 
	public void testUpdateWikiPageFileHandleAuthorized() throws DatastoreException, NotFoundException{
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wikiId);
		// Setup two filehandles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy("007");
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(two.getId())).thenReturn(two);
		// markdown
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("3");
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("one");
		// Set the user as the creator of one
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		V2WikiPage page = new V2WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setMarkdownFileHandleId(markdown.getId());
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		Map<String, FileHandle> fileHandleMap = wikiManager.buildFileNameMap(page);
		
		// For this case the first file is already on the wiki-page. The user is not the
		// creator of the first handle but since the handle is already on the wiki page it is okay
		// since that is not a change made by this user at this time.
		List<Long> allFileHandleIds = new LinkedList<Long>();
		allFileHandleIds.add(new Long(one.getId()));
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(allFileHandleIds);
		List<Long> allMarkdownFileHandleIds = new LinkedList<Long>();
		allMarkdownFileHandleIds.add(new Long(markdown.getId()));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(allMarkdownFileHandleIds);
		
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(user, ownerId, ownerType, page);
		
		// Only file handle two's id should be passed into the DAO to be inserted since it's new
		List<String> newIds = new ArrayList<String>();
		newIds.add(two.getId());
		verify(mockWikiDao, times(1)).updateWikiPage(page, fileHandleMap, ownerId, ownerType, newIds);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetWikiHistoryUnauthorized() throws NotFoundException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "0";
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wikiId);
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getWikiHistory(user, ownerId, ownerType, key, new Long(10), new Long(0));
	}
	
	@Test
	public void testGetWikiHistoryAuthorized() throws NotFoundException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "0";
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wikiId);
		
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getWikiHistory(user, ownerId, ownerType, key, new Long(10), new Long(0));
		verify(mockWikiDao, times(1)).getWikiHistory(key, new Long(10), new Long(0));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testRestoreWikiPageUnauthorized() throws NotFoundException,
	UnauthorizedException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "123";
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.restoreWikiPage(user, ownerId, ownerType, new Long(0), wikiId);
	}
	
	@Test
	public void testRestoreWikiPageAuthorized() throws NotFoundException,
	UnauthorizedException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "0";
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wikiId);
		
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedBy(user.getId().toString());
		when(mockAuthManager.canAccessRawFileHandleByCreator(any(UserInfo.class), any(String.class))).thenReturn(true);
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		// Set up what will be returned in the restore method
		V2WikiMarkdownVersion versionOfContent = new V2WikiMarkdownVersion();
		versionOfContent.setAttachmentFileHandleIds(new ArrayList<String>());
		versionOfContent.setMarkdownFileHandleId(markdown.getId());
		versionOfContent.setTitle("Title");
		versionOfContent.setVersion("0");
		when(mockWikiDao.getVersionOfWikiContent(key, new Long(0))).thenReturn(versionOfContent);
		
		// Set up what will be returned in the update method
		List<Long> allFileHandleIds = new LinkedList<Long>();
	    when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(allFileHandleIds);
		List<Long> allMarkdownFileHandleIds = new LinkedList<Long>();
		allMarkdownFileHandleIds.add(new Long(markdown.getId()));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(allMarkdownFileHandleIds);
		
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		// Create a wiki page. This is version 0.
		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(wikiId);
		wiki.setEtag("etag");
		wiki.setMarkdownFileHandleId(markdown.getId());
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.setCreatedBy(user.getId().toString());
		wiki.setCreatedOn(new Date(1));
		when(mockWikiDao.get(key, null)).thenReturn(wiki);

		wikiManager.restoreWikiPage(user, ownerId, ownerType, new Long(0), wikiId);
		verify(mockWikiDao, times(1)).get(key, null);
		verify(mockWikiDao, times(1)).getVersionOfWikiContent(key, new Long(0));
		verify(mockWikiDao, times(1)).updateWikiPage(any(V2WikiPage.class), any(Map.class), any(String.class), any(ObjectType.class), any(List.class));
	}
	
	@Test
	public void testRestoreWikiPageWithUnauthorizedFiles() throws NotFoundException {
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "restoringWiki";
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, wikiId);
		
		// Setup file handles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy("007");
		when(mockFileDao.get(one.getId())).thenReturn(one);

		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy("007");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		
		S3FileHandle three = new S3FileHandle();
		three.setId("3");
		three.setCreatedOn(new Date(2));
		three.setFileName("three");
		// Set some other creator on two
		three.setCreatedBy("007");
		when(mockFileDao.get(three.getId())).thenReturn(three);
		
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("4");
		markdown.setCreatedBy(user.getId().toString());
		markdown.setCreatedOn(new Date(1));
		markdown.setFileName("markdownContent");
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		
		S3FileHandle markdown2 = new S3FileHandle();
		markdown2.setId("5");
		markdown2.setCreatedBy(user.getId().toString());
		markdown2.setCreatedOn(new Date(1));
		markdown2.setFileName("markdownContent2");
		when(mockFileDao.get(markdown2.getId())).thenReturn(markdown2);
		
		// We are restoring content from an earlier version.
		// The attachments we are restoring are already in the reservation
		List<Long> allFileHandleIds = new ArrayList<Long>();
		allFileHandleIds.add(new Long(two.getId()));
		allFileHandleIds.add(new Long(one.getId()));
		allFileHandleIds.add(new Long(three.getId()));
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(allFileHandleIds);

		List<Long> allMarkdownFileHandleIds = new ArrayList<Long>();
		allMarkdownFileHandleIds.add(new Long(markdown.getId()));
		allMarkdownFileHandleIds.add(new Long(markdown2.getId()));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(allMarkdownFileHandleIds);

		// We are restoring these attachments
		List<String> fileHandleIdsToRestore = new ArrayList<String>();
		fileHandleIdsToRestore.add(one.getId());
		
		// None of these should be checked since all the attachments already exist in the reservation
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getId().toString())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		V2WikiMarkdownVersion versionOfContent = new V2WikiMarkdownVersion();
		versionOfContent.setAttachmentFileHandleIds(fileHandleIdsToRestore);
		versionOfContent.setMarkdownFileHandleId(markdown.getId());
		versionOfContent.setTitle("Title");
		versionOfContent.setVersion("0");
		when(mockWikiDao.getVersionOfWikiContent(key, new Long(0))).thenReturn(versionOfContent);

		// Most recent wiki with this wikiId, whose metadata we will use
		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(wikiId);
		wiki.setEtag("etag");
		wiki.setCreatedBy(user.getId().toString());
		wiki.setCreatedOn(new Date(1));
		when(mockWikiDao.get(key, null)).thenReturn(wiki);
		
		wikiManager.restoreWikiPage(user, ownerId, ownerType, new Long(0), wikiId);
		verify(mockWikiDao, times(1)).getVersionOfWikiContent(key, new Long(0));
		
		verify(mockWikiDao, times(1)).getFileHandleReservationForWiki(key);
		verify(mockWikiDao, times(1)).getMarkdownFileHandleIdsForWiki(key);
		verify(mockWikiDao, times(1)).updateWikiPage(any(V2WikiPage.class), any(Map.class), any(String.class), any(ObjectType.class), any(ArrayList.class));
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(null, "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullId() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true), null, ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullType() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true), "123", null, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullPage() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true), "123", ObjectType.ENTITY, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(null, "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullId() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true), null, ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullType() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true), "123", null, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullPage() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true), "123", ObjectType.ENTITY, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(null, new WikiPageKey("123", ObjectType.EVALUATION, "345"), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(new UserInfo(true), null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(null, new WikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(new UserInfo(true), null);
	}
	
	@Test
	public void testDeleteOwnerNotFound() throws UnauthorizedException, NotFoundException{
		// If the owner does not exist then then we can delete it.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenThrow(new NotFoundException());
		wikiManager.deleteWiki(new UserInfo(true), new WikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetHistoryNullLimit() throws NotFoundException, DatastoreException {
		WikiPageKey key = new WikiPageKey("123", ObjectType.ENTITY, "345");
		wikiManager.getWikiHistory(new UserInfo(true), "123", ObjectType.ENTITY, key, null, new Long(0));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetHistoryNullOffset() throws NotFoundException, DatastoreException {
		WikiPageKey key = new WikiPageKey("123", ObjectType.ENTITY, "345");
		wikiManager.getWikiHistory(new UserInfo(true), "123", ObjectType.ENTITY, key, new Long(10), null);
	}
}

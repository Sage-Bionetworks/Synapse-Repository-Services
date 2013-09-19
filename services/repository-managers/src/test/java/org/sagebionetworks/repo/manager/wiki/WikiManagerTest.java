package org.sagebionetworks.repo.manager.wiki;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Unit test for the WikiManager
 * @author jmhill
 *
 */
public class WikiManagerTest {
	
	WikiPageDao mockWikiDao;
	AuthorizationManager mockAuthManager;
	FileHandleDao mockFileDao;
	WikiManagerImpl wikiManager;
	WikiPageKey key;
	UserInfo user;
	
	@Before
	public void before(){
		user = new UserInfo(false);
		user.setIndividualGroup(new UserGroup());
		user.getIndividualGroup().setId("987");
		// setup the mocks
		mockWikiDao = Mockito.mock(WikiPageDao.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		mockFileDao = Mockito.mock(FileHandleDao.class);
		key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		wikiManager = new WikiManagerImpl(mockWikiDao, mockAuthManager, mockFileDao);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(null, "123", ObjectType.ENTITY, new WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullId() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true), null, ObjectType.ENTITY, new WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullType() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true), "123", null, new WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullPage() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true), "123", ObjectType.ENTITY, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(null, "123", ObjectType.ENTITY, new WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullId() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true), null, ObjectType.ENTITY, new WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullType() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true), "123", null, new WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullPage() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true), "123", ObjectType.ENTITY, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(null, new WikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(new UserInfo(true), null);
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
	
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.createWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, new WikiPage());
	}
	
	@Test
	public void testCreateAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPage page = new WikiPage();
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		// Was it passed to the DAO?
		verify(mockWikiDao, times(1)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY);
	}
	
	@Test
	public void testCreateModifiedByCreatedBy() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPage page = new WikiPage();
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockWikiDao.create(any(WikiPage.class), any(HashMap.class), any(String.class), any(ObjectType.class))).thenReturn(page);
		WikiPage result = wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		assertNotNull(result);
		assertEquals("CreatedBy should have set", user.getIndividualGroup().getId(), result.getCreatedBy());
		assertEquals("ModifiedBy should have set", user.getIndividualGroup().getId(), result.getModifiedBy());
		// Was it passed to the DAO?
		verify(mockWikiDao, times(1)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY);
	}
	
	@Test
	public void testUpdateModifiedBy() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etag");
		// Start with a different modified by
		page.setModifiedBy("to be replaced");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockWikiDao.updateWikiPage(any(WikiPage.class), any(HashMap.class), any(String.class), any(ObjectType.class), anyBoolean())).thenReturn(page);
		WikiPage result = wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, page);
		assertNotNull(result);
		assertEquals("ModifiedBy should have set", user.getIndividualGroup().getId(), result.getModifiedBy());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, new WikiPage());
	}
	
	@Test
	public void testUpadateAuthorized() throws DatastoreException, NotFoundException{
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etag");
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, page);
		// Was it passed to the DAO?
		verify(mockWikiDao, times(1)).updateWikiPage(page,new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, false);
		// The lock must be acquired
		verify(mockWikiDao, times(1)).lockForUpdate("000");
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testUpadateConflict() throws DatastoreException, NotFoundException{
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
		// return a different etag to trigger a conflict
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etagUpdate!!!");
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, page);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getWikiPage(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"));
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
		wikiManager.getWikiPage(new UserInfo(false),key);
		verify(mockWikiDao, times(1)).get(key);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testFileHandleIdForFileNameUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getFileHandleIdForFileName(new UserInfo(false), new WikiPageKey("123", ObjectType.EVALUATION, "345"), "fileName");
	}
	
	@Test
	public void testFileHandleIdForFileNameAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = new WikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getFileHandleIdForFileName(new UserInfo(false),key,"fileName");
		verify(mockWikiDao, times(1)).getWikiAttachmentFileHandleForFileName(key, "fileName");
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
		wikiManager.getAttachmentFileHandles(user, key);
	}
	
	@Test
	public void testGetAttachmentFileHandles() throws DatastoreException, NotFoundException{
		// Setup the test handles
		// null preview
		S3FileHandle s3NullPrivew = new S3FileHandle();
		s3NullPrivew.setId("1");
		s3NullPrivew.setPreviewId(null);
		// with preview
		S3FileHandle s3WithPrivew = new S3FileHandle();
		s3WithPrivew.setId("2");
		s3WithPrivew.setPreviewId("3");
		// The preview
		PreviewFileHandle preview = new PreviewFileHandle();
		preview.setId("3");
		List<String> ids = new ArrayList<String>();
		ids.add("2");
		ids.add("1");
		FileHandleResults expectedResults = new FileHandleResults();
		expectedResults.setList(new LinkedList<FileHandle>());
		expectedResults.getList().add(s3WithPrivew);
		expectedResults.getList().add(preview);
		expectedResults.getList().add(s3NullPrivew);
		when(mockFileDao.getAllFileHandles(ids, true)).thenReturn(expectedResults);
		// Setup the wiki
		WikiPageKey key = new WikiPageKey("syn123", ObjectType.WIKI, "456");
		List<String> wikiHandleIds = new LinkedList<String>();
		// The list only contains the S3 handles and not the previews
		wikiHandleIds.add("2");
		wikiHandleIds.add("1");
		when(mockWikiDao.getWikiFileHandleIds(key)).thenReturn(wikiHandleIds);
		// Allow
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(true);
		// Ready to make the call
		FileHandleResults results = wikiManager.getAttachmentFileHandles(user, key);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals("There should be 3 file handles, one for each of the two S3 files and for the preview.",3, results.getList().size());
		FileHandle handle = results.getList().get(0);
		assertNotNull(handle);
		assertEquals("2", handle.getId());
		assertTrue(handle instanceof S3FileHandle);
		// next should be the preview
		handle = results.getList().get(1);
		assertNotNull(handle);
		assertEquals("3", handle.getId());
		assertTrue(handle instanceof PreviewFileHandle);
		// Last should be the null preview
		handle = results.getList().get(2);
		assertNotNull(handle);
		assertEquals("1", handle.getId());
		assertTrue(handle instanceof S3FileHandle);
	}
	
	@Test
	public void testBuildFileNameMap() throws DatastoreException, NotFoundException{
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
		
		// Create a WikiPage that includes all three
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
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
	}
	

	@Test (expected=UnauthorizedException.class)
	public void testCreateWikiPageFileHandleUnauthorized() throws DatastoreException, NotFoundException{
		// Setup two filehandles
		S3FileHandle one = new S3FileHandle();
		one.setId("1");
		one.setCreatedOn(new Date(1));
		one.setFileName("one");
		// Set the user as the creator of one
		one.setCreatedBy(user.getIndividualGroup().getId());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy("007");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getIndividualGroup().getId())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
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
		one.setCreatedBy(user.getIndividualGroup().getId());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set this one to be created by the owner
		two.setCreatedBy(user.getIndividualGroup().getId());
		when(mockFileDao.get(two.getId())).thenReturn(two);
		
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getIndividualGroup().getId())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
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
		one.setCreatedBy(user.getIndividualGroup().getId());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy("007");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		
		WikiPage page = new WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		// For this case the first file handle is already on the wiki-page, but the second is not
		// Since this user is not the creator of the second this call should fail.
		List<String> currentFileHandleIds = new LinkedList<String>();
		currentFileHandleIds.add(one.getId());
		when(mockWikiDao.getWikiFileHandleIds(key)).thenReturn(currentFileHandleIds);
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getIndividualGroup().getId())).thenReturn(true);
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
		one.setCreatedBy(user.getIndividualGroup().getId());
		when(mockFileDao.get(one.getId())).thenReturn(one);
		// two
		S3FileHandle two = new S3FileHandle();
		two.setId("2");
		two.setCreatedOn(new Date(2));
		two.setFileName("two");
		// Set some other creator on two
		two.setCreatedBy("007");
		when(mockFileDao.get(two.getId())).thenReturn(two);
		
		WikiPage page = new WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>());
		page.getAttachmentFileHandleIds().add(two.getId());
		page.getAttachmentFileHandleIds().add(one.getId());
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		// For this case the second file handle is already on the wiki-page. The user is not the
		// creator of the seconds handle but since the handle is already on the wiki page it is okay
		// since that is not a change made by this user at this time.
		List<String> currentFileHandleIds = new LinkedList<String>();
		currentFileHandleIds.add(two.getId());
		when(mockWikiDao.getWikiFileHandleIds(key)).thenReturn(currentFileHandleIds);
		
		// Allow one but deny the other.
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, user.getIndividualGroup().getId())).thenReturn(true);
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, "007")).thenReturn(false);
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(user, ownerId, ownerType, page);
		Map<String, FileHandle> fileHandleMap = wikiManager.buildFileNameMap(page);
		
		verify(mockWikiDao, times(1)).updateWikiPage(page, fileHandleMap, ownerId, ownerType, false);
	}
}

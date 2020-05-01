package org.sagebionetworks.repo.manager.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiMarkdownVersion;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Unit test for the WikiManager
 * @author hso
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class V2WikiManagerTest {
	
	@Mock
	List<V2WikiHistorySnapshot> mockPage;
	@Mock
	V2WikiPageDao mockWikiDao;
	
	V2WikiManagerImpl wikiManager;
	@Mock
	AuthorizationManager mockAuthManager;
	@Mock
	FileHandleDao mockFileDao;
	String ownerId;
	ObjectType ownerType;
	String wikiId;
	WikiPageKey key;
	UserInfo user;
	
	@Before
	public void before() {
		user = new UserInfo(false, "987");
		ownerId = "123";
		ownerType = ObjectType.EVALUATION;
		wikiId = "345";
		
		key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		wikiManager = new V2WikiManagerImpl(mockWikiDao, mockAuthManager, mockFileDao);
		
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.accessDenied(""));
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, markdown.getId(), user.getId().toString())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, one.getId(), user.getId().toString())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, one.getId(), "007")).thenReturn(AuthorizationStatus.accessDenied(""));
		// Allow access to the owner.
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(0)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, new ArrayList<String>());
	}

	@Test
	public void testCreateModifiedByCreatedBy() throws DatastoreException, NotFoundException{
		// setup allow
		V2WikiPage page = new V2WikiPage();
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockWikiDao.create(any(V2WikiPage.class), any(HashMap.class), any(String.class), any(ObjectType.class), any(ArrayList.class))).thenReturn(page);
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedBy(user.getId().toString());
		when(mockFileDao.get(markdown.getId())).thenReturn(markdown);
		page.setMarkdownFileHandleId(markdown.getId());

		when(mockAuthManager.canAccessRawFileHandleByCreator(user, markdown.getId(), user.getId().toString())).thenReturn(AuthorizationStatus.authorized());
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
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test
	public void testUpdateAuthorized() throws DatastoreException, NotFoundException{
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, markdown.getId(), user.getId().toString())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		// No versions to delete
		when(mockWikiDao.getWikiVersionByRank(any(WikiPageKey.class), eq(100L))).thenReturn(105L);

		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, page);
		// Was it passed to the DAO?
		List<String> newIds = new ArrayList<String>();
		newIds.add(markdown.getId());
		verify(mockWikiDao, times(1)).updateWikiPage(page,new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, newIds);
		// The lock must be acquired
		verify(mockWikiDao, times(1)).lockForUpdate("000");
		// Called deleteWikiVersions
		verify(mockWikiDao, times(1)).deleteWikiVersions(any(WikiPageKey.class), eq(105L));
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testUpdateConflict() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		// return a different etag to trigger a conflict
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etagUpdate!!!");
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
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
		
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq(user.getId().toString()))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq(markdown.getCreatedBy()))).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq(ownerId))).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
	    wikiManager.updateWikiPage(user, ownerId, ownerType, page);
		List<String> newIds = new ArrayList<String>();
	    newIds.add(one.getId());
		Map<String, FileHandle> fileHandleMap = wikiManager.buildFileNameMap(page);
	    verify(mockWikiDao, times(1)).updateWikiPage(page, fileHandleMap, ownerId, ownerType, newIds);   
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiPage(new UserInfo(false), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetRootUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockWikiDao.getRootWiki("123",  ObjectType.EVALUATION)).thenReturn(345l);
		wikiManager.getRootWikiPage(new UserInfo(false), "123",ObjectType.EVALUATION);
	}
	
	@Test
	public void testGetAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiPage(new UserInfo(false),key, null);
		verify(mockWikiDao, times(1)).get(key, null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetVersionUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiPage(new UserInfo(false), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
	}
	
	@Test
	public void testGetVersionAuthorized() throws UnauthorizedException, NotFoundException {
		Long version = new Long(0);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiPage(new UserInfo(false),key, version);
		verify(mockWikiDao, times(1)).get(key, version);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testFileHandleIdForFileNameUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getFileHandleIdForFileName(new UserInfo(false), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), "fileName", null);
	}
	
	@Test
	public void testFileHandleIdForFileNameAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getFileHandleIdForFileName(new UserInfo(false),key,"fileName", null);
		verify(mockWikiDao, times(1)).getWikiAttachmentFileHandleForFileName(key, "fileName", null);
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test (expected=UnauthorizedException.class)
	public void testMarkdownFileHandleIdForVersionUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getMarkdownFileHandleId(new UserInfo(false), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test
	public void testMarkdownFileHandleIdForVersion() throws UnauthorizedException, NotFoundException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getMarkdownFileHandleId(new UserInfo(false), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
		verify(mockWikiDao, times(1)).getMarkdownHandleId(key, new Long(0));
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test (expected=UnauthorizedException.class)
	public void testGetRootWikiKeyUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getRootWikiKey(new UserInfo(false), "owner", ObjectType.ENTITY);
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test
	public void testGetRootWikiKey() throws UnauthorizedException, NotFoundException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getRootWikiKey(new UserInfo(false), "owner", ObjectType.ENTITY);
		verify(mockWikiDao, times(1)).getRootWiki("owner", ObjectType.ENTITY);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetTreeUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.EVALUATION, null, null);
	}
	
	@Test
	public void testGetTreeAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.EVALUATION, null, null);
		verify(mockWikiDao, times(1)).getHeaderTree(any(String.class), any(ObjectType.class), eq(V2WikiManagerImpl.MAX_LIMIT), eq(0L));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.deleteWiki(new UserInfo(false), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTreeOverLimit() throws DatastoreException, NotFoundException{
		long limit = V2WikiManagerImpl.MAX_LIMIT +1;
		long offset = 0L;
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.EVALUATION, limit, offset);
	}
	
	@Test
	public void testDeleteAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.deleteWiki(new UserInfo(false),key);
		verify(mockWikiDao, times(1)).delete(key);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetAttachmentFileHandlesUnauthroized() throws DatastoreException, NotFoundException{
		// deny
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
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
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("syn123", ObjectType.WIKI, "456");
		List<String> wikiHandleIds = new LinkedList<String>();
		// The list only contains the S3 handles and not the previews
		wikiHandleIds.add("2");
		wikiHandleIds.add("1");
		when(mockWikiDao.getWikiFileHandleIds(key, null)).thenReturn(wikiHandleIds);
		
		// Allow
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
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
		when(mockAuthManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, one.getId(), user.getId().toString())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, one.getId(), "007")).thenReturn(AuthorizationStatus.accessDenied(""));
		// Allow access to the owner.
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq(user.getId().toString()))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq("007"))).thenReturn(AuthorizationStatus.accessDenied(""));
		// Allow access to the owner.
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(1)).create(any(V2WikiPage.class), any(Map.class), any(String.class), any(ObjectType.class), any(ArrayList.class));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateWikiPageFileHandleUnauthorized() throws DatastoreException, NotFoundException{
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq(user.getId().toString()))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq("007"))).thenReturn(AuthorizationStatus.accessDenied(""));
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
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
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq(user.getId().toString()))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(eq(user), anyString(), eq("007"))).thenReturn(AuthorizationStatus.accessDenied(""));
		// Allow access to the owner.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
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
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiHistory(user, ownerId, ownerType, key, new Long(10), new Long(0));
	}
	
	@Test
	public void testGetWikiHistoryAuthorized() throws NotFoundException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiHistory(user, ownerId, ownerType, key, new Long(10), new Long(0));
		verify(mockWikiDao, times(1)).getWikiHistory(key, new Long(10), new Long(0));
	}
	
	@Test
	public void testGetWikiHistoryFirstPage() throws NotFoundException {
		long limit = 10;
		long offset = 0;
		int resultSize = (int)limit;
		when(mockPage.size()).thenReturn(resultSize);
		// setup a page size equal to limit
		when(mockWikiDao.getWikiHistory(key, limit, offset)).thenReturn(mockPage);
		PaginatedResults<V2WikiHistorySnapshot> page = wikiManager.getWikiHistory(user, ownerId, ownerType, key, new Long(10), new Long(0));
		assertNotNull(page);
		// total number of results must be larger than the limit to indicate more pages.
		assertTrue(page.getTotalNumberOfResults() > limit);
	}
	
	@Test
	public void testGetWikiHistoryLastPage() throws NotFoundException {
		long limit = 10;
		long offset = 0;
		int resultSize = (int)(limit-1);
		when(mockPage.size()).thenReturn(resultSize);
		// setup a page size equal to limit
		when(mockWikiDao.getWikiHistory(key, limit, offset)).thenReturn(mockPage);
		PaginatedResults<V2WikiHistorySnapshot> page = wikiManager.getWikiHistory(user, ownerId, ownerType, key, new Long(10), new Long(0));
		assertNotNull(page);
		// total number of results should match the result size.
		assertEquals(resultSize, page.getTotalNumberOfResults());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testRestoreWikiPageUnauthorized() throws NotFoundException,
	UnauthorizedException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "123";
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.restoreWikiPage(user, ownerId, ownerType, new Long(0), wikiId);
	}
	
	@Test
	public void testRestoreWikiPageAuthorized() throws NotFoundException,
	UnauthorizedException {
		String ownerId = "111";
		ObjectType ownerType = ObjectType.ENTITY;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		
		S3FileHandle markdown = new S3FileHandle();
		markdown.setId("1");
		markdown.setCreatedBy(user.getId().toString());
		when(mockAuthManager.canAccessRawFileHandleByCreator(any(UserInfo.class), eq(markdown.getId()), any(String.class))).thenReturn(AuthorizationStatus.authorized());
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
		
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
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
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		
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
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, one.getId(), user.getId().toString())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleByCreator(user, one.getId(), "007")).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
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
	
	@Test 
	public void testUpdateOrderHintAuthorized() throws DatastoreException, NotFoundException {
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		
		// Return OrderHint DTO 
		V2WikiOrderHint orderHintDTO = new V2WikiOrderHint();
		orderHintDTO.setEtag("etag");
		orderHintDTO.setOwnerId("123");
		orderHintDTO.setOwnerObjectType(ObjectType.EVALUATION);
		orderHintDTO.setIdList(Arrays.asList(new String[] {"A", "B", "C"}));
		when(mockWikiDao.getWikiOrderHint(any(WikiPageKey.class))).thenReturn(orderHintDTO);
		when(mockWikiDao.updateOrderHint(orderHintDTO, key)).thenReturn(orderHintDTO);
		
		// Return etag when locking Wiki Owner database.
		when(mockWikiDao.lockWikiOwnersForUpdate(anyString())).thenReturn("etag");
		
		// Allow user to access order hint.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
		wikiManager.updateOrderHint(user, orderHintDTO);
		
		// Verify that the order hint was updated.
		verify(mockWikiDao, times(1)).updateOrderHint(eq(orderHintDTO), any(WikiPageKey.class));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateOrderHintUnauthorized() throws DatastoreException, NotFoundException {
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		
		// Return OrderHint DTO 
		V2WikiOrderHint orderHintDTO = new V2WikiOrderHint();
		orderHintDTO.setEtag("etag");
		orderHintDTO.setOwnerId("123");
		orderHintDTO.setOwnerObjectType(ObjectType.EVALUATION);
		orderHintDTO.setIdList(Arrays.asList(new String[] {"A", "B", "C"}));
		when(mockWikiDao.updateOrderHint(orderHintDTO, key)).thenReturn(orderHintDTO);
		
		// Return etag when locking Wiki Owner database.
		when(mockWikiDao.lockWikiOwnersForUpdate(anyString())).thenReturn("etag");
		
		// Allow user to access order hint.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		
		wikiManager.updateOrderHint(user, orderHintDTO);
	}
	
	
	@Test(expected=ConflictingUpdateException.class)
	public void testUpdateOrderHintConflict() throws DatastoreException, NotFoundException {
		V2WikiOrderHint hint = new V2WikiOrderHint();
		hint.setOwnerId("000");
		hint.setEtag("etag");
		hint.setOwnerObjectType(ObjectType.EVALUATION);
		// return a different etag to trigger a conflict
		when(mockWikiDao.lockWikiOwnersForUpdate(anyString())).thenReturn("etagUpdate!!!");
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.updateOrderHint(user, hint);
	}
	
	@Test
	public void testGetOrderHintAuthorized() throws DatastoreException, NotFoundException {
		// Allow user to access order hint.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
		wikiManager.getOrderHint(user, key.getOwnerObjectId(), key.getOwnerObjectType());
		
		ArgumentCaptor<WikiPageKey> keyCaptor = ArgumentCaptor.forClass(WikiPageKey.class);
		verify(mockWikiDao).getWikiOrderHint(keyCaptor.capture());
		
		assertEquals(keyCaptor.getValue().getOwnerObjectId(), key.getOwnerObjectId());
		assertEquals(keyCaptor.getValue().getOwnerObjectType(), key.getOwnerObjectType());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOrderHintUnauthorized() throws DatastoreException, NotFoundException {
		// Disallow user to access order hint.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getOrderHint(user, key.getOwnerObjectId(), key.getOwnerObjectType());
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
		wikiManager.getWikiPage(null, WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(new UserInfo(true), null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(null, WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(new UserInfo(true), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetOrderHintNullOwnerId() throws UnauthorizedException, NotFoundException {
		wikiManager.getOrderHint(new UserInfo(true), null, key.getOwnerObjectType());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetOrderHintNullUserInfo() throws UnauthorizedException, NotFoundException {
		wikiManager.getOrderHint(null, key.getOwnerObjectId(), key.getOwnerObjectType());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetOrderHintNullObjectType() throws UnauthorizedException, NotFoundException {
		wikiManager.getOrderHint(new UserInfo(true), key.getOwnerObjectId(), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateOrderHintNullUserInfo() throws UnauthorizedException, NotFoundException {
		wikiManager.updateOrderHint(null, new V2WikiOrderHint());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateOrderHintNullUserOrderHint() throws UnauthorizedException, NotFoundException {
		wikiManager.updateOrderHint(new UserInfo(true), null);
	}
	
	@Test
	public void testDeleteOwnerNotFound() throws UnauthorizedException, NotFoundException{
		// If the owner does not exist then then we can delete it.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenThrow(new NotFoundException());
		wikiManager.deleteWiki(new UserInfo(true), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetHistoryNullLimit() throws NotFoundException, DatastoreException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.ENTITY, "345");
		wikiManager.getWikiHistory(new UserInfo(true), "123", ObjectType.ENTITY, key, null, new Long(0));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetHistoryNullOffset() throws NotFoundException, DatastoreException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.ENTITY, "345");
		wikiManager.getWikiHistory(new UserInfo(true), "123", ObjectType.ENTITY, key, new Long(10), null);
	}

}

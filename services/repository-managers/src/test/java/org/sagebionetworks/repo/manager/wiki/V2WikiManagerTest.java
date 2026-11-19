package org.sagebionetworks.repo.manager.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
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
		user = new UserInfo(false, 987L, DEFAULT_REALM_ID);
		ownerId = "123";
		ownerType = ObjectType.EVALUATION;
		wikiId = "345";
		
		key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		wikiManager = new V2WikiManagerImpl(mockWikiDao, mockAuthManager, mockFileDao);
		
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
	}

	private static S3FileHandle createHandle(String id, String createdBy, long createdOn, String fileName) {
		S3FileHandle handle = new S3FileHandle();
		handle.setId(id);
		handle.setCreatedBy(createdBy);
		handle.setCreatedOn(new Date(createdOn));
		handle.setFileName(fileName);
		return handle;
	}

	/**
	 * Stub the single batch load of the given handles, in the order the manager requests them (markdown first, then attachments).
	 */
	private void stubGetFileHandles(FileHandle... handles) {
		Map<String, FileHandle> map = new LinkedHashMap<>();
		for (FileHandle handle : handles) {
			map.put(handle.getId(), handle);
		}
		when(mockFileDao.getFileHandlesOrThrow(new ArrayList<>(map.keySet()))).thenReturn(map);
	}

	private static Map<String, FileHandle> nameMap(FileHandle... handles) {
		Map<String, FileHandle> map = new HashMap<>();
		for (FileHandle handle : handles) {
			map.put(handle.getFileName(), handle);
		}
		return map;
	}

	private static Map<String, FileHandle> idMap(FileHandle... handles) {
		Map<String, FileHandle> map = new HashMap<>();
		for (FileHandle handle : handles) {
			map.put(handle.getId(), handle);
		}
		return map;
	}

	private static Collection<FileHandle> sameHandles(FileHandle... expected) {
		List<FileHandle> expectedList = Arrays.asList(expected);
		return argThat(c -> c.size() == expectedList.size() && c.containsAll(expectedList));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.createWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID), "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test
	public void testCreateAuthorized() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		S3FileHandle markdown = createHandle("1", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown);
		page.setMarkdownFileHandleId(markdown.getId());
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandles(user, Arrays.asList(markdown))).thenReturn(AuthorizationStatus.authorized());
		// call under test
		wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(1)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, Arrays.asList(markdown.getId()));
	}
	
	@Test
	public void testCreateWikiPageMarkdownUnauthorized() throws DatastoreException, NotFoundException{
		S3FileHandle one = createHandle("1", user.getId().toString(), 1, "one");
		// markdown created by someone else
		S3FileHandle markdown = createHandle("2", "007", 1, "markdown");
		stubGetFileHandles(markdown, one);
		
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>(Arrays.asList(one.getId())));
		page.setMarkdownFileHandleId(markdown.getId());
		
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandles(user, Arrays.asList(one, markdown))).thenReturn(AuthorizationStatus.accessDenied("denied"));
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		});
		assertEquals("denied", ex.getMessage());
		verify(mockWikiDao, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	public void testCreateWikiPageWithMissingFileHandle() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		page.setMarkdownFileHandleId("1");
		page.setAttachmentFileHandleIds(new LinkedList<String>(Arrays.asList("2")));
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockFileDao.getFileHandlesOrThrow(Arrays.asList("1", "2"))).thenThrow(new NotFoundException("The FileHandle does not exist: 2"));
		NotFoundException ex = assertThrows(NotFoundException.class, () -> {
			// call under test
			wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		});
		assertEquals("The FileHandle does not exist: 2", ex.getMessage());
		verify(mockAuthManager, never()).canAccessRawFileHandles(any(), any());
		verify(mockWikiDao, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	public void testCreateModifiedByCreatedBy() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockWikiDao.create(any(V2WikiPage.class), any(HashMap.class), any(String.class), any(ObjectType.class), any(List.class))).thenReturn(page);
		S3FileHandle markdown = createHandle("1", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown);
		when(mockAuthManager.canAccessRawFileHandles(user, Arrays.asList(markdown))).thenReturn(AuthorizationStatus.authorized());
		page.setMarkdownFileHandleId(markdown.getId());

		// call under test
		V2WikiPage result = wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		assertNotNull(result);
		assertEquals("CreatedBy should have set", user.getId().toString(), result.getCreatedBy());
		assertEquals("ModifiedBy should have set", user.getId().toString(), result.getModifiedBy());
		verify(mockWikiDao, times(1)).create(page, new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, Arrays.asList(markdown.getId()));
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
		S3FileHandle markdown = createHandle("1", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown);
		page.setMarkdownFileHandleId(markdown.getId());
		when(mockAuthManager.canAccessRawFileHandles(user, Arrays.asList(markdown))).thenReturn(AuthorizationStatus.authorized());
		// No versions to delete
		when(mockWikiDao.getWikiVersionByRank(any(WikiPageKey.class), eq(100L))).thenReturn(105L);

		// call under test
		wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, page);
		verify(mockWikiDao, times(1)).updateWikiPage(page,new HashMap<String, FileHandle>(), "123", ObjectType.ENTITY, Arrays.asList(markdown.getId()));
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
		wikiManager.updateWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID), "123", ObjectType.ENTITY, page);
	}

	@Test
	public void testUpdateWikiPageWithMissingFileHandle() throws DatastoreException, NotFoundException{
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setMarkdownFileHandleId("1");
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etag");
		when(mockFileDao.getFileHandlesOrThrow(Arrays.asList("1"))).thenThrow(new NotFoundException("The FileHandle does not exist: 1"));
		NotFoundException ex = assertThrows(NotFoundException.class, () -> {
			// call under test
			wikiManager.updateWikiPage(user, "123", ObjectType.ENTITY, page);
		});
		assertEquals("The FileHandle does not exist: 1", ex.getMessage());
		verify(mockAuthManager, never()).canAccessRawFileHandles(any(), any());
		verify(mockWikiDao, never()).updateWikiPage(any(), any(), any(), any(), any());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetRootUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockWikiDao.getRootWiki("123",  ObjectType.EVALUATION)).thenReturn(345l);
		wikiManager.getRootWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID), "123",ObjectType.EVALUATION);
	}
	
	@Test
	public void testGetAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID),key, null);
		verify(mockWikiDao, times(1)).get(key, null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetVersionUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
	}
	
	@Test
	public void testGetVersionAuthorized() throws UnauthorizedException, NotFoundException {
		Long version = new Long(0);
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiPage(new UserInfo(false, 1L, DEFAULT_REALM_ID),key, version);
		verify(mockWikiDao, times(1)).get(key, version);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testFileHandleIdForFileNameUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getFileHandleIdForFileName(new UserInfo(false, 1L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), "fileName", null);
	}
	
	@Test
	public void testFileHandleIdForFileNameAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getFileHandleIdForFileName(new UserInfo(false, 1L, DEFAULT_REALM_ID),key,"fileName", null);
		verify(mockWikiDao, times(1)).getWikiAttachmentFileHandleForFileName(key, "fileName", null);
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test (expected=UnauthorizedException.class)
	public void testMarkdownFileHandleIdForVersionUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getMarkdownFileHandleId(new UserInfo(false, 1L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test
	public void testMarkdownFileHandleIdForVersion() throws UnauthorizedException, NotFoundException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getMarkdownFileHandleId(new UserInfo(false, 1L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), new Long(0));
		verify(mockWikiDao, times(1)).getMarkdownHandleId(key, new Long(0));
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test (expected=UnauthorizedException.class)
	public void testGetRootWikiKeyUnauthorized() throws DatastoreException, NotFoundException {
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getRootWikiKey(new UserInfo(false, 1L, DEFAULT_REALM_ID), "owner", ObjectType.ENTITY);
	}
	
	// Same test for getMarkdownFileHandleId()
	@Test
	public void testGetRootWikiKey() throws UnauthorizedException, NotFoundException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getRootWikiKey(new UserInfo(false, 1L, DEFAULT_REALM_ID), "owner", ObjectType.ENTITY);
		verify(mockWikiDao, times(1)).getRootWiki("owner", ObjectType.ENTITY);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetTreeUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.getWikiHeaderTree(new UserInfo(false, 1L, DEFAULT_REALM_ID), "123", ObjectType.EVALUATION, null, null);
	}
	
	@Test
	public void testGetTreeAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.getWikiHeaderTree(new UserInfo(false, 1L, DEFAULT_REALM_ID), "123", ObjectType.EVALUATION, null, null);
		verify(mockWikiDao, times(1)).getHeaderTree(any(String.class), any(ObjectType.class), eq(V2WikiManagerImpl.MAX_LIMIT), eq(0L));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		wikiManager.deleteWiki(new UserInfo(false, 1L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetTreeOverLimit() throws DatastoreException, NotFoundException{
		long limit = V2WikiManagerImpl.MAX_LIMIT +1;
		long offset = 0L;
		// setup allow
		wikiManager.getWikiHeaderTree(new UserInfo(false, 1L, DEFAULT_REALM_ID), "123", ObjectType.EVALUATION, limit, offset);
	}
	
	@Test
	public void testDeleteAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		wikiManager.deleteWiki(new UserInfo(false, 1L, DEFAULT_REALM_ID),key);
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
	public void testBuildFileNameMapWithNoAttachments() {
		V2WikiPage page = new V2WikiPage();
		// call under test
		Map<String, FileHandle> map = V2WikiManagerImpl.buildFileNameMap(page, new HashMap<>());
		assertEquals(new HashMap<String, FileHandle>(), map);
	}

	@Test
	public void testBuildFileNameMapWithDuplicateNames() {
		// When there are duplicate names the newest one is used.
		S3FileHandle one = createHandle("1", "007", 1, "duplicateName");
		S3FileHandle two = createHandle("2", "007", 2, "duplicateName");
		S3FileHandle three = createHandle("3", "007", 2, "uniqueName");
		Map<String, FileHandle> handlesById = idMap(one, two, three);
		Map<String, FileHandle> expected = new HashMap<>();
		expected.put("duplicateName", two);
		expected.put("uniqueName", three);

		V2WikiPage page = new V2WikiPage();
		page.setAttachmentFileHandleIds(Arrays.asList(three.getId(), two.getId(), one.getId()));
		// call under test
		assertEquals(expected, V2WikiManagerImpl.buildFileNameMap(page, handlesById));

		// Different order, same result
		page.setAttachmentFileHandleIds(Arrays.asList(one.getId(), three.getId(), two.getId()));
		// call under test
		assertEquals(expected, V2WikiManagerImpl.buildFileNameMap(page, handlesById));
	}
	
	@Test
	public void testCreateWikiPageFileHandleUnauthorized() throws DatastoreException, NotFoundException{
		S3FileHandle one = createHandle("1", user.getId().toString(), 1, "one");
		// Some other creator on two
		S3FileHandle two = createHandle("2", "007", 2, "two");
		S3FileHandle markdown = createHandle("3", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown, two, one);
		
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setAttachmentFileHandleIds(new LinkedList<String>(Arrays.asList(two.getId(), one.getId())));
		page.setMarkdownFileHandleId(markdown.getId());
		
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandles(eq(user), sameHandles(one, two, markdown))).thenReturn(AuthorizationStatus.accessDenied("denied"));
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		});
		assertEquals("denied", ex.getMessage());
		verify(mockWikiDao, never()).create(any(), any(), any(), any(), any());
	}
	
	@Test
	public void testCreateWikiPageFileHandleAuthorized() throws DatastoreException, NotFoundException{
		S3FileHandle one = createHandle("1", user.getId().toString(), 1, "one");
		S3FileHandle two = createHandle("2", user.getId().toString(), 2, "two");
		S3FileHandle markdown = createHandle("3", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown, two, one);
		
		V2WikiPage page = new V2WikiPage();
		page.setId("000");
		page.setEtag("etag");
		page.setMarkdownFileHandleId(markdown.getId());
		page.setAttachmentFileHandleIds(new LinkedList<String>(Arrays.asList(two.getId(), one.getId())));
		
		when(mockAuthManager.canCreateWiki(any(UserInfo.class), any(String.class), any(ObjectType.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandles(eq(user), sameHandles(one, two, markdown))).thenReturn(AuthorizationStatus.authorized());
		// call under test
		wikiManager.createWikiPage(user, "syn123", ObjectType.ENTITY, page);
		ArgumentCaptor<List<String>> newIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(mockWikiDao).create(eq(page), eq(nameMap(one, two)), eq("syn123"), eq(ObjectType.ENTITY), newIdsCaptor.capture());
		List<String> newIds = newIdsCaptor.getValue();
		assertEquals(3, newIds.size());
		assertTrue(newIds.containsAll(Arrays.asList(one.getId(), two.getId(), markdown.getId())));
	}
	
	@Test
	public void testUpdateWikiPageFileHandleUnauthorized() throws DatastoreException, NotFoundException{
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		S3FileHandle one = createHandle("1", user.getId().toString(), 1, "one");
		// Some other creator on two
		S3FileHandle two = createHandle("2", "007", 2, "two");
		S3FileHandle markdown = createHandle("3", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown, two, one);
		
		V2WikiPage page = new V2WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setMarkdownFileHandleId(markdown.getId());
		page.setAttachmentFileHandleIds(new LinkedList<String>(Arrays.asList(two.getId(), one.getId())));
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		// The first file handle is already on the wiki-page, but the second and the markdown are not
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(Arrays.asList(Long.valueOf(one.getId())));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(new LinkedList<Long>());
		
		when(mockAuthManager.canAccessRawFileHandles(user, Arrays.asList(markdown, two))).thenReturn(AuthorizationStatus.accessDenied("denied"));
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			wikiManager.updateWikiPage(user, ownerId, ownerType, page);
		});
		assertEquals("denied", ex.getMessage());
		verify(mockWikiDao, never()).updateWikiPage(any(), any(), any(), any(), any());
	}
	
	/**
	 * The user is only adding a FileHandle that they created. There is a FileHandle that they did not create already on the WikiPage,
	 * but this should not block them as it is already there: authorization is only enforced for changes.
	 */
	@Test 
	public void testUpdateWikiPageFileHandleAuthorized() throws DatastoreException, NotFoundException{
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "0";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		// Already reserved, created by someone else
		S3FileHandle one = createHandle("1", "007", 1, "one");
		S3FileHandle two = createHandle("2", user.getId().toString(), 2, "two");
		S3FileHandle markdown = createHandle("3", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown, two, one);
		
		V2WikiPage page = new V2WikiPage();
		page.setId(wikiId);
		page.setEtag("etag");
		page.setMarkdownFileHandleId(markdown.getId());
		page.setAttachmentFileHandleIds(new LinkedList<String>(Arrays.asList(two.getId(), one.getId())));
		
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(Arrays.asList(Long.valueOf(one.getId())));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(Arrays.asList(Long.valueOf(markdown.getId())));
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		// Only the new file handle is checked
		when(mockAuthManager.canAccessRawFileHandles(user, Arrays.asList(two))).thenReturn(AuthorizationStatus.authorized());
		// call under test
		wikiManager.updateWikiPage(user, ownerId, ownerType, page);
		
		// Only file handle two's id should be passed into the DAO to be inserted since it's new
		verify(mockWikiDao, times(1)).updateWikiPage(page, nameMap(one, two), ownerId, ownerType, Arrays.asList(two.getId()));
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
		
		S3FileHandle markdown = createHandle("1", user.getId().toString(), 1, "markdown");
		stubGetFileHandles(markdown);
		
		V2WikiMarkdownVersion versionOfContent = new V2WikiMarkdownVersion();
		versionOfContent.setAttachmentFileHandleIds(new ArrayList<String>());
		versionOfContent.setMarkdownFileHandleId(markdown.getId());
		versionOfContent.setTitle("Title");
		versionOfContent.setVersion("0");
		when(mockWikiDao.getVersionOfWikiContent(key, new Long(0))).thenReturn(versionOfContent);
		
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(new LinkedList<Long>());
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(Arrays.asList(Long.valueOf(markdown.getId())));
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		// Nothing is new
		when(mockAuthManager.canAccessRawFileHandles(user, Collections.emptyList())).thenReturn(AuthorizationStatus.authorized());
		
		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(wikiId);
		wiki.setEtag("etag");
		wiki.setMarkdownFileHandleId(markdown.getId());
		wiki.setAttachmentFileHandleIds(new ArrayList<String>());
		wiki.setCreatedBy(user.getId().toString());
		wiki.setCreatedOn(new Date(1));
		when(mockWikiDao.get(key, null)).thenReturn(wiki);

		// call under test
		wikiManager.restoreWikiPage(user, ownerId, ownerType, new Long(0), wikiId);
		verify(mockWikiDao, times(1)).get(key, null);
		verify(mockWikiDao, times(1)).getVersionOfWikiContent(key, new Long(0));
		verify(mockWikiDao, times(1)).updateWikiPage(any(V2WikiPage.class), eq(new HashMap<String, FileHandle>()), eq(ownerId), eq(ownerType), eq(Collections.emptyList()));
	}
	
	@Test
	public void testRestoreWikiPageWithUnauthorizedFiles() throws NotFoundException {
		String ownerId = "556";
		ObjectType ownerType = ObjectType.EVALUATION;
		String wikiId = "restoringWiki";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(ownerId, ownerType, wikiId);
		
		// Created by someone else, but already reserved on the wiki
		S3FileHandle one = createHandle("1", "007", 1, "one");
		S3FileHandle markdown = createHandle("4", user.getId().toString(), 1, "markdownContent");
		stubGetFileHandles(markdown, one);
		
		// The attachments we are restoring are already in the reservation
		when(mockWikiDao.getFileHandleReservationForWiki(key)).thenReturn(Arrays.asList(2L, 1L, 3L));
		when(mockWikiDao.getMarkdownFileHandleIdsForWiki(key)).thenReturn(Arrays.asList(4L, 5L));
		// Nothing is new so nothing is denied
		when(mockAuthManager.canAccessRawFileHandles(user, Collections.emptyList())).thenReturn(AuthorizationStatus.authorized());
		when(mockWikiDao.lockForUpdate(wikiId)).thenReturn("etag");
		
		V2WikiMarkdownVersion versionOfContent = new V2WikiMarkdownVersion();
		versionOfContent.setAttachmentFileHandleIds(Arrays.asList(one.getId()));
		versionOfContent.setMarkdownFileHandleId(markdown.getId());
		versionOfContent.setTitle("Title");
		versionOfContent.setVersion("0");
		when(mockWikiDao.getVersionOfWikiContent(key, new Long(0))).thenReturn(versionOfContent);

		V2WikiPage wiki = new V2WikiPage();
		wiki.setId(wikiId);
		wiki.setEtag("etag");
		wiki.setCreatedBy(user.getId().toString());
		wiki.setCreatedOn(new Date(1));
		when(mockWikiDao.get(key, null)).thenReturn(wiki);
		
		// call under test
		wikiManager.restoreWikiPage(user, ownerId, ownerType, new Long(0), wikiId);
		verify(mockWikiDao, times(1)).getVersionOfWikiContent(key, new Long(0));
		verify(mockWikiDao, times(1)).updateWikiPage(any(V2WikiPage.class), eq(nameMap(one)), eq(ownerId), eq(ownerType), eq(Collections.emptyList()));
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
		when(mockWikiDao.updateOrderHint(any(), any())).thenReturn(orderHintDTO);
		
		// Return etag when locking Wiki Owner database.
		when(mockWikiDao.lockWikiOwnersForUpdate(anyString())).thenReturn("etag");
		
		// Allow user to access order hint.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
		V2WikiOrderHint result = wikiManager.updateOrderHint(user, orderHintDTO);
		
		orderHintDTO.setEtag(result.getEtag());
		
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
		wikiManager.createWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), null, ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullType() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), "123", null, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWikiPageNullPage() throws UnauthorizedException, NotFoundException{
		wikiManager.createWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), "123", ObjectType.ENTITY, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(null, "123", ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullId() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), null, ObjectType.ENTITY, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullType() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), "123", null, new V2WikiPage());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateWikiPageNullPage() throws UnauthorizedException, NotFoundException{
		wikiManager.updateWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), "123", ObjectType.ENTITY, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(null, WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(new UserInfo(true, 2L, DEFAULT_REALM_ID), null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(null, WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(new UserInfo(true, 2L, DEFAULT_REALM_ID), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetOrderHintNullOwnerId() throws UnauthorizedException, NotFoundException {
		wikiManager.getOrderHint(new UserInfo(true, 2L, DEFAULT_REALM_ID), null, key.getOwnerObjectType());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetOrderHintNullUserInfo() throws UnauthorizedException, NotFoundException {
		wikiManager.getOrderHint(null, key.getOwnerObjectId(), key.getOwnerObjectType());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetOrderHintNullObjectType() throws UnauthorizedException, NotFoundException {
		wikiManager.getOrderHint(new UserInfo(true, 2L, DEFAULT_REALM_ID), key.getOwnerObjectId(), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateOrderHintNullUserInfo() throws UnauthorizedException, NotFoundException {
		wikiManager.updateOrderHint(null, new V2WikiOrderHint());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateOrderHintNullUserOrderHint() throws UnauthorizedException, NotFoundException {
		wikiManager.updateOrderHint(new UserInfo(true, 2L, DEFAULT_REALM_ID), null);
	}
	
	@Test
	public void testDeleteOwnerNotFound() throws UnauthorizedException, NotFoundException{
		// If the owner does not exist then then we can delete it.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenThrow(new NotFoundException(""));
		wikiManager.deleteWiki(new UserInfo(true, 2L, DEFAULT_REALM_ID), WikiPageKeyHelper.createWikiPageKey("123", ObjectType.EVALUATION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetHistoryNullLimit() throws NotFoundException, DatastoreException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.ENTITY, "345");
		wikiManager.getWikiHistory(new UserInfo(true, 2L, DEFAULT_REALM_ID), "123", ObjectType.ENTITY, key, null, new Long(0));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetHistoryNullOffset() throws NotFoundException, DatastoreException {
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey("123", ObjectType.ENTITY, "345");
		wikiManager.getWikiHistory(new UserInfo(true, 2L, DEFAULT_REALM_ID), "123", ObjectType.ENTITY, key, new Long(10), null);
	}

}

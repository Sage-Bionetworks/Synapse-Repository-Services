package org.sagebionetworks.repo.manager.wiki;

import java.util.LinkedList;
import java.util.List;

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
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
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
	FileMetadataDao mockFileDao;
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
		mockFileDao = Mockito.mock(FileMetadataDao.class);
		key = new WikiPageKey("123", ObjectType.COMPETITION, "345");
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
		wikiManager.getWikiPage(null, new WikiPageKey("123", ObjectType.COMPETITION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.getWikiPage(new UserInfo(true), null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullUser() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(null, new WikiPageKey("123", ObjectType.COMPETITION, "345"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullKey() throws UnauthorizedException, NotFoundException{
		wikiManager.deleteWiki(new UserInfo(true), null);
	}
	
	@Test
	public void testDeleteOwnerNotFound() throws UnauthorizedException, NotFoundException{
		// If the owner does not exist then then we can delete it.
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenThrow(new NotFoundException());
		wikiManager.deleteWiki(new UserInfo(true), new WikiPageKey("123", ObjectType.COMPETITION, "345"));
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
		verify(mockWikiDao, times(1)).create(page, "123", ObjectType.ENTITY);
	}
	
	@Test
	public void testCreateModifiedByCreatedBy() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPage page = new WikiPage();
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockWikiDao.create(any(WikiPage.class), any(String.class), any(ObjectType.class))).thenReturn(page);
		WikiPage result = wikiManager.createWikiPage(user, "123", ObjectType.ENTITY, page);
		assertNotNull(result);
		assertEquals("CreatedBy should have set", user.getIndividualGroup().getId(), result.getCreatedBy());
		assertEquals("ModifiedBy should have set", user.getIndividualGroup().getId(), result.getModifiedBy());
		// Was it passed to the DAO?
		verify(mockWikiDao, times(1)).create(page, "123", ObjectType.ENTITY);
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
		when(mockWikiDao.updateWikiPage(any(WikiPage.class), any(String.class), any(ObjectType.class), anyBoolean())).thenReturn(page);
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
		verify(mockWikiDao, times(1)).updateWikiPage(page, "123", ObjectType.ENTITY, false);
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
		wikiManager.getWikiPage(new UserInfo(false), new WikiPageKey("123", ObjectType.COMPETITION, "345"));
	}
	
	@Test
	public void testGetAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = new WikiPageKey("123", ObjectType.COMPETITION, "345");
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getWikiPage(new UserInfo(false),key);
		verify(mockWikiDao, times(1)).get(key);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetTreeUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.COMPETITION, null, null);
	}
	
	@Test
	public void testGetTreeAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.getWikiHeaderTree(new UserInfo(false), "123", ObjectType.COMPETITION, null, null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.deleteWiki(new UserInfo(false), new WikiPageKey("123", ObjectType.COMPETITION, "345"));
	}
	
	@Test
	public void testDeleteAuthorized() throws DatastoreException, NotFoundException{
		// setup allow
		WikiPageKey key = new WikiPageKey("123", ObjectType.COMPETITION, "345");
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
		when(mockFileDao.get("1")).thenReturn(s3NullPrivew);
		// with preview
		S3FileHandle s3WithPrivew = new S3FileHandle();
		s3WithPrivew.setId("2");
		s3WithPrivew.setPreviewId("3");
		when(mockFileDao.get("2")).thenReturn(s3WithPrivew);
		// The preview
		PreviewFileHandle preview = new PreviewFileHandle();
		preview.setId("3");
		when(mockFileDao.get("3")).thenReturn(preview);
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
}

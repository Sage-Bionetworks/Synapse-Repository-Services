package org.sagebionetworks.repo.manager.wiki;

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
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
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
	WikiManagerImpl wikiManager;
	WikiPageKey key;
	
	@Before
	public void before(){
		// setup the mocks
		mockWikiDao = Mockito.mock(WikiPageDao.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		key = new WikiPageKey("123", ObjectType.COMPETITION, "345");
		wikiManager = new WikiManagerImpl(mockWikiDao, mockAuthManager);
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
		wikiManager.createWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, page);
		// Was it passed to the DAO?
		verify(mockWikiDao, times(1)).create(page, "123", ObjectType.ENTITY);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateUnauthorized() throws DatastoreException, NotFoundException{
		// setup deny
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(false);
		wikiManager.updateWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, new WikiPage());
	}
	
	@Test
	public void testUpadateAuthorized() throws DatastoreException, NotFoundException{
		WikiPage page = new WikiPage();
		page.setId("000");
		page.setEtag("etag");
		when(mockWikiDao.lockForUpdate("000")).thenReturn("etag");
		// setup allow
		when(mockAuthManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(true);
		wikiManager.updateWikiPage(new UserInfo(false), "123", ObjectType.ENTITY, page);
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
}

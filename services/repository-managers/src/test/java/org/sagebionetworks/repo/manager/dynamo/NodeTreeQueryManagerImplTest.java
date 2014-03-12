package org.sagebionetworks.repo.manager.dynamo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.nodetree.IncompletePathException;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class NodeTreeQueryManagerImplTest {

	private final Long userId = 43560L;
	private final Long adminUserId = 24571L;
	private final String nodeRoot = "syn4489";
	private final String nodeCanAccessX = "syn11028";
	private final String nodeCanAccessY = "syn11029";
	private final String nodeCannotAccess = "syn11030";
	private final String nodeIpe = "syn192903"; // IncompletePathException
	private final NodeTreeQueryManager manager = new NodeTreeQueryManagerImpl();

	@Before
	public void before() throws Exception {

		UserGroup userGroup = mock(UserGroup.class);
		when(userGroup.getId()).thenReturn("0");

		UserInfo userInfo = mock(UserInfo.class);
		when(userInfo.isAdmin()).thenReturn(false);

		UserInfo adminUserInfo = mock(UserInfo.class);
		when(adminUserInfo.isAdmin()).thenReturn(true);

		UserManager userMan = mock(UserManager.class);
		when(userMan.getUserInfo(userId)).thenReturn(userInfo);
		when(userMan.getUserInfo(adminUserId)).thenReturn(adminUserInfo);

		AuthorizationManager auMan = mock(AuthorizationManager.class);
		when(auMan.canAccess(userInfo, nodeRoot, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		when(auMan.canAccess(userInfo, nodeCanAccessX, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(auMan.canAccess(userInfo, nodeCanAccessY, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(auMan.canAccess(userInfo, nodeCannotAccess, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);

		NodeTreeQueryDao ntDao = mock(NodeTreeQueryDao.class);
		when(ntDao.isRoot(KeyFactory.stringToKey(nodeRoot).toString())).thenReturn(true);
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		String keyNodeCanAccessX = KeyFactory.stringToKey(nodeCanAccessX).toString();
		when(ntDao.getAncestors(keyNodeCanAccessX)).thenReturn(list);
		when(ntDao.getParent(keyNodeCanAccessX)).thenReturn("1");
		when(ntDao.getDescendants(keyNodeCanAccessX, 2, null)).thenReturn(list);
		when(ntDao.getDescendants(keyNodeCanAccessX, 1, 2, null)).thenReturn(list);
		String keyNodeCanAccessY = KeyFactory.stringToKey(nodeCanAccessY).toString();
		when(ntDao.getDescendants(keyNodeCanAccessX, 2, keyNodeCanAccessY)).thenReturn(list);
		when(ntDao.getDescendants(keyNodeCanAccessX, 1, 2, keyNodeCanAccessY)).thenReturn(list);
		String keyNodeIpe = KeyFactory.stringToKey(nodeIpe).toString();
		when(ntDao.getAncestors(keyNodeIpe)).thenThrow(new IncompletePathException("ipe"));
		when(ntDao.getParent(keyNodeIpe)).thenThrow(new IncompletePathException("ipe"));
		when(ntDao.getDescendants(keyNodeIpe, 1, null)).thenThrow(new IncompletePathException("ipe"));
		when(ntDao.getDescendants(keyNodeIpe, 1, 1, null)).thenThrow(new IncompletePathException("ipe"));

		NodeTreeQueryManager srv = manager;
		if(AopUtils.isAopProxy(srv) && srv instanceof Advised) {
			Object target = ((Advised)srv).getTargetSource().getTarget();
			srv = (NodeTreeQueryManager)target;
		}
		ReflectionTestUtils.setField(srv, "userManager", userMan);
		ReflectionTestUtils.setField(srv, "authorizationManager", auMan);
		ReflectionTestUtils.setField(srv, "nodeTreeQueryDao", ntDao);
	}

	@Test
	public void testGetRoot() {
		Assert.assertTrue(this.manager.isRoot(this.adminUserId, "syn4489"));
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetRootUnauthorizedException() {
		this.manager.isRoot(this.userId, "syn4489");
	}

	@Test
	public void testGetAncestors() {
		EntityIdList results = this.manager.getAncestors(this.userId, this.nodeCanAccessX);
		Assert.assertNotNull(results);
		List<EntityId> idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAncestorsUnauthorizedException() {
		this.manager.getAncestors(this.userId, this.nodeCannotAccess);
	}

	@Test
	public void testGetParent() {
		EntityId parent = this.manager.getParent(this.userId, this.nodeCanAccessX);
		Assert.assertEquals("syn1", parent.getId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetParentUnauthorizedException() {
		this.manager.getParent(this.userId, this.nodeCannotAccess);
	}

	@Test
	public void testGetDescendants1() {
		EntityIdList results = this.manager.getDescendants(this.userId, this.nodeCanAccessX, 2, null);
		Assert.assertNotNull(results);
		List<EntityId> idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
		results = this.manager.getDescendants(this.userId, this.nodeCanAccessX, 2, this.nodeCanAccessY);
		Assert.assertNotNull(results);
		idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetDescendants1UnauthorizedException() {
		this.manager.getDescendants(this.userId, this.nodeCannotAccess, 1, null);
	}

	@Test
	public void testGetDescendants2() {
		EntityIdList results = this.manager.getDescendants(this.userId, this.nodeCanAccessX, 1, 2, null);
		Assert.assertNotNull(results);
		List<EntityId> idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
		results = this.manager.getDescendants(this.userId, this.nodeCanAccessX, 1, 2, this.nodeCanAccessY);
		Assert.assertNotNull(results);
		idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetDescendants2UnauthorizedException() {
		this.manager.getDescendants(this.userId, this.nodeCannotAccess, 1, 1, null);
	}

	@Test
	public void testGetChildren() {
		EntityIdList results = this.manager.getChildren(this.userId, this.nodeCanAccessX, 2, null);
		Assert.assertNotNull(results);
		List<EntityId> idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
		results = this.manager.getChildren(this.userId, this.nodeCanAccessX, 2, this.nodeCanAccessY);
		Assert.assertNotNull(results);
		idList = results.getIdList();
		Assert.assertEquals(2, idList.size());
		Assert.assertEquals("syn1", idList.get(0).getId());
		Assert.assertEquals("syn2", idList.get(1).getId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetChildrenUnauthorizedException() {
		this.manager.getChildren(this.userId, this.nodeCannotAccess, 1, null);
	}

	@Test
	public void testHandlingIncompletePathException() {
		EntityIdList idList = this.manager.getAncestors(this.adminUserId, this.nodeIpe);
		Assert.assertNotNull(idList);
		Assert.assertNotNull(idList.getIdList());
		Assert.assertEquals(0, idList.getIdList().size());
		EntityId id = this.manager.getParent(this.adminUserId, this.nodeIpe);
		Assert.assertNotNull(id);
		Assert.assertNull(id.getId());
		idList = this.manager.getDescendants(this.adminUserId, this.nodeIpe, 1, null);
		Assert.assertNotNull(idList);
		Assert.assertNotNull(idList.getIdList());
		Assert.assertEquals(0, idList.getIdList().size());
		idList = this.manager.getDescendants(this.adminUserId, this.nodeIpe, 1, 1, null);
		Assert.assertNotNull(idList);
		Assert.assertNotNull(idList.getIdList());
		Assert.assertEquals(0, idList.getIdList().size());
		idList = this.manager.getChildren(this.adminUserId, this.nodeIpe, 1, null);
		Assert.assertNotNull(idList);
		Assert.assertNotNull(idList.getIdList());
		Assert.assertEquals(0, idList.getIdList().size());
	}
}

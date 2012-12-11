package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class NodeLineageQueryServiceImplTest {

	private final String userId = "0";
	private final String adminUserId = "1";
	private final String nodeRoot = "syn4489";
	private final String nodeCanAccessX = "syn11028";
	private final String nodeCanAccessY = "syn11029";
	private final String nodeCannotAccess = "syn11030";
	private final NodeLineageQueryService service = new NodeLineageQueryServiceImpl();

	@Before
	public void before() throws Exception {

		UserGroup userGroup = mock(UserGroup.class);
		when(userGroup.getId()).thenReturn("0");

		UserInfo userInfo = mock(UserInfo.class);
		when(userInfo.isAdmin()).thenReturn(false);
		when(userInfo.getIndividualGroup()).thenReturn(userGroup);

		UserInfo adminUserInfo = mock(UserInfo.class);
		when(adminUserInfo.isAdmin()).thenReturn(true);
		when(adminUserInfo.getIndividualGroup()).thenReturn(userGroup);

		UserManager userMan = mock(UserManager.class);
		when(userMan.getUserInfo(userId)).thenReturn(userInfo);
		when(userMan.getUserInfo(adminUserId)).thenReturn(adminUserInfo);

		AuthorizationManager auMan = mock(AuthorizationManager.class);
		when(auMan.canAccess(userInfo, nodeRoot, ACCESS_TYPE.READ)).thenReturn(false);
		when(auMan.canAccess(userInfo, nodeCanAccessX, ACCESS_TYPE.READ)).thenReturn(true);
		when(auMan.canAccess(userInfo, nodeCanAccessY, ACCESS_TYPE.READ)).thenReturn(true);
		when(auMan.canAccess(userInfo, nodeCannotAccess, ACCESS_TYPE.READ)).thenReturn(false);

		NodeTreeDao ntDao = mock(NodeTreeDao.class);
		when(ntDao.getRoot()).thenReturn(KeyFactory.stringToKey(nodeRoot).toString());
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		String keyNodeCanAccessX = KeyFactory.stringToKey(nodeCanAccessX).toString();
		String keyNodeCanAccessY = KeyFactory.stringToKey(nodeCanAccessY).toString();
		when(ntDao.getAncestors(keyNodeCanAccessX)).thenReturn(list);
		when(ntDao.getParent(keyNodeCanAccessX)).thenReturn("1");
		when(ntDao.getDescendants(keyNodeCanAccessX, 2, null)).thenReturn(list);
		when(ntDao.getDescendants(keyNodeCanAccessX, 1, 2, null)).thenReturn(list);
		when(ntDao.getChildren(keyNodeCanAccessX, 2, null)).thenReturn(list);
		when(ntDao.getLowestCommonAncestor(keyNodeCanAccessX, keyNodeCanAccessY)).thenReturn(keyNodeCanAccessX);

		NodeLineageQueryService srv = service;
		if(AopUtils.isAopProxy(srv) && srv instanceof Advised) {
			Object target = ((Advised)srv).getTargetSource().getTarget();
			srv = (NodeLineageQueryService)target;
		}
		ReflectionTestUtils.setField(srv, "userManager", userMan);
		ReflectionTestUtils.setField(srv, "authorizationManager", auMan);
		ReflectionTestUtils.setField(srv, "nodeTreeDao", ntDao);
	}

	@Test
	public void testGetRoot() {
		Assert.assertEquals("syn4489", this.service.getRoot(this.adminUserId));
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetRootUnauthorizedException() {
		this.service.getRoot(this.userId);
	}

	@Test
	public void testGetAncestors() {
		List<String> results = this.service.getAncestors(this.userId, this.nodeCanAccessX);
		Assert.assertEquals(2, results.size());
		Assert.assertEquals("syn1", results.get(0));
		Assert.assertEquals("syn2", results.get(1));
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAncestorsUnauthorizedException() {
		this.service.getAncestors(this.userId, this.nodeCannotAccess);
	}

	@Test
	public void testGetParent() {
		String parent = this.service.getParent(this.userId, this.nodeCanAccessX);
		Assert.assertEquals("syn1", parent);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetParentUnauthorizedException() {
		this.service.getParent(this.userId, this.nodeCannotAccess);
	}

	@Test
	public void testGetDescendant1s() {
		List<String> results = this.service.getDescendants(this.userId, this.nodeCanAccessX, 2, null);
		Assert.assertEquals(2, results.size());
		Assert.assertEquals("syn1", results.get(0));
		Assert.assertEquals("syn2", results.get(1));
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetDescendants1UnauthorizedException() {
		this.service.getDescendants(this.userId, this.nodeCannotAccess, 1, null);
	}

	@Test
	public void testGetDescendants2() {
		List<String> results = this.service.getDescendants(this.userId, this.nodeCanAccessX, 1, 2, null);
		Assert.assertEquals(2, results.size());
		Assert.assertEquals("syn1", results.get(0));
		Assert.assertEquals("syn2", results.get(1));
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetDescendants2UnauthorizedException() {
		this.service.getDescendants(this.userId, this.nodeCannotAccess, 1, 1, null);
	}

	@Test
	public void testGetChildren() {
		List<String> results = this.service.getChildren(this.userId, this.nodeCanAccessX, 2, null);
		Assert.assertEquals(2, results.size());
		Assert.assertEquals("syn1", results.get(0));
		Assert.assertEquals("syn2", results.get(1));
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetChildrenUnauthorizedException() {
		this.service.getChildren(this.userId, this.nodeCannotAccess, 1, null);
	}

	@Test
	public void testGetLowestCommonAncestor() {
		String lca = this.service.getLowestCommonAncestor(this.userId, this.nodeCanAccessX, this.nodeCanAccessY);
		Assert.assertEquals(this.nodeCanAccessX, lca);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetLowestCommonAncestorUnauthorizedException1() {
		this.service.getLowestCommonAncestor(this.userId, this.nodeCannotAccess, this.nodeCanAccessX);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetLowestCommonAncestorUnauthorizedException2() {
		this.service.getLowestCommonAncestor(this.userId, this.nodeCanAccessX, this.nodeCannotAccess);
	}
}

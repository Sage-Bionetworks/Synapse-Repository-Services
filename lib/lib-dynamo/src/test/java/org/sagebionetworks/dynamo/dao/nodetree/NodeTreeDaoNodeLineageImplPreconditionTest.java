package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;

public class NodeTreeDaoNodeLineageImplPreconditionTest {

	private NodeTreeDaoNodeLineageImpl nodeTreeDao;

	@Before
	public void before() {
		AmazonDynamoDB client = Mockito.mock(AmazonDynamoDB.class);
		this.nodeTreeDao = new NodeTreeDaoNodeLineageImpl(client);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateIllegalArgumentException1() {
		nodeTreeDao.create(null, "parent", new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateIllegalArgumentException2() {
		nodeTreeDao.create("child", null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateIllegalArgumentException3() {
		nodeTreeDao.create("child", "parent", null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateIllegalArgumentException1() {
		nodeTreeDao.update(null, "parent", new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateIllegalArgumentException2() {
		nodeTreeDao.update("child", null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateIllegalArgumentException3() {
		nodeTreeDao.update("child", "parent", null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeleteIllegalArgumentException1() {
		nodeTreeDao.delete(null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testDeleteIllegalArgumentException2() {
		nodeTreeDao.delete("child", null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetAncestorsIllegalArgumentException() {
		nodeTreeDao.getAncestors(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetParentIllegalArgumentException() {
		nodeTreeDao.getParent(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException1() {
		nodeTreeDao.getDescendants(null, 3, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException2() {
		nodeTreeDao.getDescendants(null, 1, 3, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException3() {
		nodeTreeDao.getDescendants("child", 0, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException4() {
		nodeTreeDao.getDescendants("child", 0, 3, null);
	}
}

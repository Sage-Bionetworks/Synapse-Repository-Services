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
	public void testCreateNullPointerException1() {
		nodeTreeDao.create(null, "parent", new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateNullPointerException2() {
		nodeTreeDao.create("child", null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateNullPointerException3() {
		nodeTreeDao.create("child", "parent", null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateNullPointerException1() {
		nodeTreeDao.update(null, "parent", new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateNullPointerException2() {
		nodeTreeDao.update("child", null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateNullPointerException3() {
		nodeTreeDao.update("child", "parent", null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeleteNullPointerException1() {
		nodeTreeDao.delete(null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testDeleteNullPointerException2() {
		nodeTreeDao.delete("child", null);
	}

	@Test(expected=NullPointerException.class)
	public void testGetAncestorsNullPointerException() {
		nodeTreeDao.getAncestors(null);
	}

	@Test(expected=NullPointerException.class)
	public void testGetParentNullPointerException() {
		nodeTreeDao.getParent(null);
	}

	@Test(expected=NullPointerException.class)
	public void testGetDescendantsNullPointerException1() {
		nodeTreeDao.getDescendants(null, 3, null);
	}

	@Test(expected=NullPointerException.class)
	public void testGetDescendantsNullPointerException2() {
		nodeTreeDao.getDescendants(null, 1, 3, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException1() {
		nodeTreeDao.getDescendants("child", 0, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException2() {
		nodeTreeDao.getDescendants("child", 0, 3, null);
	}
}

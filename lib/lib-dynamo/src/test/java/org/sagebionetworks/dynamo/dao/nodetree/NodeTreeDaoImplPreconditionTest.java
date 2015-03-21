package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

public class NodeTreeDaoImplPreconditionTest {

	private NodeTreeUpdateDao updateDao;
	private NodeTreeQueryDao queryDao;

	@Before
	public void before() {
		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		
		AmazonDynamoDB client = Mockito.mock(AmazonDynamoDB.class);
		updateDao = new NodeTreeUpdateDaoImpl(client);
		((NodeTreeUpdateDaoImpl)updateDao).setDynamoEnabled(true);
		queryDao = new NodeTreeQueryDaoImpl(client);
		((NodeTreeQueryDaoImpl)queryDao).setDynamoEnabled(true);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateIllegalArgumentException1() {
		updateDao.create(null, "parent", new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateIllegalArgumentException2() {
		updateDao.create("child", null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateIllegalArgumentException3() {
		updateDao.create("child", "parent", null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateIllegalArgumentException1() {
		updateDao.update(null, "parent", new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateIllegalArgumentException2() {
		updateDao.update("child", null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateIllegalArgumentException3() {
		updateDao.update("child", "parent", null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDeleteIllegalArgumentException1() {
		updateDao.delete(null, new Date());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testDeleteIllegalArgumentException2() {
		updateDao.delete("child", null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetAncestorsIllegalArgumentException() {
		queryDao.getAncestors(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetParentIllegalArgumentException() {
		queryDao.getParent(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException1() {
		queryDao.getDescendants(null, 3, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException2() {
		queryDao.getDescendants(null, 1, 3, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException3() {
		queryDao.getDescendants("child", 0, null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetDescendantsIllegalArgumentException4() {
		queryDao.getDescendants("child", 0, 3, null);
	}
}

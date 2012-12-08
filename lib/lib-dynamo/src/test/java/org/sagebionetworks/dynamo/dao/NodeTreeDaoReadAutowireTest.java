package org.sagebionetworks.dynamo.dao;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.model.AttributeValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dynamo-dao-spb.xml" })
public class NodeTreeDaoReadAutowireTest {

	@Autowired
	static private AmazonDynamoDB dynamoClient;

	@Autowired
	static private NodeTreeDao nodeTreeDao;

	static private DynamoDBMapper dynamoMapper;

	// Map of letters to random IDs
	static private Map<String, String> idMap;

	@BeforeClass
	public static void before() throws Exception {
		long start = System.currentTimeMillis();
		System.out.println("String beforeMethod");
		// Clear dynamo
		String root = nodeTreeDao.getRoot();
		if (root != null) {
			nodeTreeDao.delete(root, new Date());
		}

		dynamoMapper = new DynamoDBMapper(dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());
		idMap = DynamoTestUtil.createRandomIdMap(26);

		nodeTreeDao.create(idMap.get("a"), idMap.get("a"), new Date());
		nodeTreeDao.create(idMap.get("b"), idMap.get("a"), new Date());
		nodeTreeDao.create(idMap.get("e"), idMap.get("b"), new Date());
		nodeTreeDao.create(idMap.get("f"), idMap.get("b"), new Date());
		nodeTreeDao.create(idMap.get("u"), idMap.get("b"), new Date());
		nodeTreeDao.create(idMap.get("h"), idMap.get("u"), new Date());
		nodeTreeDao.create(idMap.get("i"), idMap.get("u"), new Date());
		nodeTreeDao.create(idMap.get("j"), idMap.get("u"), new Date());
		nodeTreeDao.create(idMap.get("k"), idMap.get("u"), new Date());
		nodeTreeDao.create(idMap.get("l"), idMap.get("h"), new Date());
		nodeTreeDao.create(idMap.get("m"), idMap.get("j"), new Date());
		nodeTreeDao.create(idMap.get("n"), idMap.get("j"), new Date());
		nodeTreeDao.create(idMap.get("c"), idMap.get("a"), new Date());
		nodeTreeDao.create(idMap.get("d"), idMap.get("a"), new Date());
		nodeTreeDao.create(idMap.get("g"), idMap.get("d"), new Date());

		// Pause for 1.2 seconds to deal with eventual consistency
		// As all the read methods do not use consistent reads
		Thread.sleep(1200L);
		
		long elepase = System.currentTimeMillis() - start;
		System.out.println("before done in "+elepase+" ms");
	}

	@AfterClass
	public static void after() {
		if (idMap != null) {
			Collection<String> ids = idMap.values();
			for (String id : ids) {
				String hashKey = DboNodeLineage.createHashKey(id, LineageType.ANCESTOR);
				AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
				DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
				List<DboNodeLineage> dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
				dynamoMapper.batchDelete(dboList);
				hashKey = DboNodeLineage.createHashKey(id, LineageType.DESCENDANT);
				hashKeyAttr = new AttributeValue().withS(hashKey);
				queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
				dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
				dynamoMapper.batchDelete(dboList);
			}
		}
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		List<DboNodeLineage> dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		dynamoMapper.batchDelete(dboList);
	}

	@Test
	public void testGetRoot() {
		String root = nodeTreeDao.getRoot();
		Assert.assertEquals(idMap.get("a"), root);
	}

	@Test
	public void testGetAncestors() {
		// Root has 0 ancestors
		List<String> ancestorList = nodeTreeDao.getAncestors(idMap.get("a"));
		Assert.assertTrue(ancestorList.isEmpty());
		ancestorList = nodeTreeDao.getAncestors(idMap.get("b"));
		Assert.assertEquals(1, ancestorList.size());
		Assert.assertEquals(idMap.get("a"), ancestorList.get(0));
		ancestorList = nodeTreeDao.getAncestors(idMap.get("u"));
		Assert.assertEquals(2, ancestorList.size());
		Assert.assertEquals(idMap.get("a"), ancestorList.get(0));
		Assert.assertEquals(idMap.get("b"), ancestorList.get(1));
		ancestorList = nodeTreeDao.getAncestors(idMap.get("m"));
		Assert.assertEquals(4, ancestorList.size());
		Assert.assertEquals(idMap.get("a"), ancestorList.get(0));
		Assert.assertEquals(idMap.get("b"), ancestorList.get(1));
		Assert.assertEquals(idMap.get("u"), ancestorList.get(2));
		Assert.assertEquals(idMap.get("j"), ancestorList.get(3));
		// A node that does not exist has 0 ancestors
		ancestorList = nodeTreeDao.getAncestors("fakeNode");
		Assert.assertTrue(ancestorList.isEmpty());
	}

	@Test
	public void testGetParent() {
		// Root's parent is the dummy ROOT
		String parent = nodeTreeDao.getParent(idMap.get("a"));
		Assert.assertNotNull(parent);
		Assert.assertEquals(DboNodeLineage.ROOT, parent);
		// A non-existent node's parent is null
		parent = nodeTreeDao.getParent("fakeNode");
		Assert.assertNull(parent);
		parent = nodeTreeDao.getParent(idMap.get("b"));
		Assert.assertNotNull(parent);
		Assert.assertEquals(idMap.get("a"), parent);
		parent = nodeTreeDao.getParent(idMap.get("e"));
		Assert.assertNotNull(parent);
		Assert.assertEquals(idMap.get("b"), parent);
		parent = nodeTreeDao.getParent(idMap.get("u"));
		Assert.assertNotNull(parent);
		Assert.assertEquals(idMap.get("b"), parent);
		parent = nodeTreeDao.getParent(idMap.get("m"));
		Assert.assertNotNull(parent);
		Assert.assertEquals(idMap.get("j"), parent);
		parent = nodeTreeDao.getParent(idMap.get("g"));
		Assert.assertNotNull(parent);
		Assert.assertEquals(idMap.get("d"), parent);
	}

	@Test
	public void testGetDescendants() {

		List<String> descList = nodeTreeDao.getDescendants(idMap.get("a"), 100, null);
		Set<String> descSet = new HashSet<String>(descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(14, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("b")));
		Assert.assertTrue(descSet.contains(idMap.get("c")));
		Assert.assertTrue(descSet.contains(idMap.get("d")));
		Assert.assertTrue(descSet.contains(idMap.get("e")));
		Assert.assertTrue(descSet.contains(idMap.get("f")));
		Assert.assertTrue(descSet.contains(idMap.get("g")));
		Assert.assertTrue(descSet.contains(idMap.get("h")));
		Assert.assertTrue(descSet.contains(idMap.get("i")));
		Assert.assertTrue(descSet.contains(idMap.get("j")));
		Assert.assertTrue(descSet.contains(idMap.get("k")));
		Assert.assertTrue(descSet.contains(idMap.get("l")));
		Assert.assertTrue(descSet.contains(idMap.get("m")));
		Assert.assertTrue(descSet.contains(idMap.get("n")));
		Assert.assertTrue(descSet.contains(idMap.get("u")));

		descList = nodeTreeDao.getDescendants(idMap.get("u"), 100, null);
		descSet = new HashSet<String>(descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(7, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("h")));
		Assert.assertTrue(descSet.contains(idMap.get("i")));
		Assert.assertTrue(descSet.contains(idMap.get("j")));
		Assert.assertTrue(descSet.contains(idMap.get("k")));
		Assert.assertTrue(descSet.contains(idMap.get("l")));
		Assert.assertTrue(descSet.contains(idMap.get("m")));
		Assert.assertTrue(descSet.contains(idMap.get("n")));

		descList = nodeTreeDao.getDescendants(idMap.get("d"), 100, null);
		descSet = new HashSet<String>(descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(1, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("g")));

		descList = nodeTreeDao.getDescendants(idMap.get("f"), 100, null);
		descSet = new HashSet<String>(descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(0, descSet.size());
	}

	@Test
	public void testGetDescendantsPaging() {
		List<String> descList = nodeTreeDao.getDescendants(idMap.get("a"), 2, null);
		Assert.assertEquals(2, descList.size());
		Set<String> descSet = new HashSet<String>();
		descSet.addAll(descList);
		descList = nodeTreeDao.getDescendants(idMap.get("a"), 7, descList.get(1));
		Assert.assertEquals(7, descList.size());
		descSet.addAll(descList);
		descList = nodeTreeDao.getDescendants(idMap.get("a"), 7, descList.get(6));
		Assert.assertEquals((14 - 2 - 7), descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(14, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("b")));
		Assert.assertTrue(descSet.contains(idMap.get("c")));
		Assert.assertTrue(descSet.contains(idMap.get("d")));
		Assert.assertTrue(descSet.contains(idMap.get("e")));
		Assert.assertTrue(descSet.contains(idMap.get("f")));
		Assert.assertTrue(descSet.contains(idMap.get("g")));
		Assert.assertTrue(descSet.contains(idMap.get("h")));
		Assert.assertTrue(descSet.contains(idMap.get("i")));
		Assert.assertTrue(descSet.contains(idMap.get("j")));
		Assert.assertTrue(descSet.contains(idMap.get("k")));
		Assert.assertTrue(descSet.contains(idMap.get("l")));
		Assert.assertTrue(descSet.contains(idMap.get("m")));
		Assert.assertTrue(descSet.contains(idMap.get("n")));
		Assert.assertTrue(descSet.contains(idMap.get("u")));
	}

	@Test
	public void testGetDescendantsGeneration() {
		List<String> descList = nodeTreeDao.getDescendants(idMap.get("u"), 1, 100, null);
		Set<String> descSet = new HashSet<String>(descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(4, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("h")));
		Assert.assertTrue(descSet.contains(idMap.get("i")));
		Assert.assertTrue(descSet.contains(idMap.get("j")));
		Assert.assertTrue(descSet.contains(idMap.get("k")));
		descList = nodeTreeDao.getDescendants(idMap.get("u"), 2, 100, null);
		descSet = new HashSet<String>(descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(3, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("l")));
		Assert.assertTrue(descSet.contains(idMap.get("m")));
		Assert.assertTrue(descSet.contains(idMap.get("n")));
	}

	@Test
	public void testGetDescendantsGenerationPaging() {
		List<String> descList = nodeTreeDao.getDescendants(idMap.get("a"), 2, 1, null);
		Assert.assertEquals(1, descList.size());
		Set<String> descSet = new HashSet<String>();
		descSet.addAll(descList);
		descList = nodeTreeDao.getDescendants(idMap.get("a"), 2, 1, descList.get(0));
		Assert.assertEquals(1, descList.size());
		descSet.addAll(descList);
		descList = nodeTreeDao.getDescendants(idMap.get("a"), 2, 2, descList.get(0));
		Assert.assertEquals(2, descList.size());
		descSet.addAll(descList);
		descList = nodeTreeDao.getDescendants(idMap.get("a"), 2, 2, descList.get(1));
		Assert.assertEquals(0, descList.size());
		descSet.addAll(descList);
		Assert.assertEquals(4, descSet.size());
		Assert.assertTrue(descSet.contains(idMap.get("e")));
		Assert.assertTrue(descSet.contains(idMap.get("f")));
		Assert.assertTrue(descSet.contains(idMap.get("u")));
		Assert.assertTrue(descSet.contains(idMap.get("g")));
	}

	@Test
	public void testGetChildren() {
		List<String> childList = nodeTreeDao.getChildren(idMap.get("a"), 2, null);
		Assert.assertEquals(2, childList.size());
		Set<String> childSet = new HashSet<String>();
		childSet.addAll(childList);
		childList = nodeTreeDao.getChildren(idMap.get("a"), 2, childList.get(1));
		Assert.assertEquals(1, childList.size());
		childSet.addAll(childList);
		Assert.assertEquals(3, childSet.size());
		Assert.assertTrue(childSet.contains(idMap.get("b")));
		Assert.assertTrue(childSet.contains(idMap.get("c")));
		Assert.assertTrue(childSet.contains(idMap.get("d")));
	}

	@Test
	public void testGetPath() {
		List<String> path = nodeTreeDao.getPath(idMap.get("a"), idMap.get("b"));
		Assert.assertEquals(2, path.size());
		Assert.assertEquals(idMap.get("a"), path.get(0));
		Assert.assertEquals(idMap.get("b"), path.get(1));
		path = nodeTreeDao.getPath(idMap.get("b"), idMap.get("a"));
		Assert.assertEquals(2, path.size());
		Assert.assertEquals(idMap.get("a"), path.get(0));
		Assert.assertEquals(idMap.get("b"), path.get(1));
		path = nodeTreeDao.getPath(idMap.get("m"), idMap.get("b"));
		Assert.assertEquals(4, path.size());
		Assert.assertEquals(idMap.get("b"), path.get(0));
		Assert.assertEquals(idMap.get("u"), path.get(1));
		Assert.assertEquals(idMap.get("j"), path.get(2));
		Assert.assertEquals(idMap.get("m"), path.get(3));
		path = nodeTreeDao.getPath(idMap.get("d"), idMap.get("d"));
		Assert.assertEquals(1, path.size());
		Assert.assertEquals(idMap.get("d"), path.get(0));
		path = nodeTreeDao.getPath(idMap.get("m"), idMap.get("k"));
		Assert.assertNull(path);
	}

	@Test
	public void testGetLowestCommonAncestor() {
		String anc = nodeTreeDao.getLowestCommonAncestor(idMap.get("b"), idMap.get("d"));
		Assert.assertEquals(idMap.get("a"), anc);
		anc = nodeTreeDao.getLowestCommonAncestor(idMap.get("j"), idMap.get("b"));
		Assert.assertEquals(idMap.get("b"), anc);
		anc = nodeTreeDao.getLowestCommonAncestor(idMap.get("e"), idMap.get("n"));
		Assert.assertEquals(idMap.get("b"), anc);
	}
}

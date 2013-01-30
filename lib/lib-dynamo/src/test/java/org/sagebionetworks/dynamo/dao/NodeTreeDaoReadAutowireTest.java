package org.sagebionetworks.dynamo.dao;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore // see: PLFM-1714
public class NodeTreeDaoReadAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	private DynamoDBMapper dynamoMapper;

	// Map of letters to random IDs
	private Map<String, String> idMap;

	@Before
	public void before() throws Exception {

		// Clear dynamo
		String root = this.nodeTreeDao.getRoot();
		if (root != null) {
			this.nodeTreeDao.delete(root, new Date());
		}

		this.dynamoMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());
		this.idMap = DynamoTestUtil.createRandomIdMap(26);

		this.nodeTreeDao.create(this.idMap.get("a"), this.idMap.get("a"), new Date());
		this.nodeTreeDao.create(this.idMap.get("b"), this.idMap.get("a"), new Date());
		this.nodeTreeDao.create(this.idMap.get("e"), this.idMap.get("b"), new Date());
		this.nodeTreeDao.create(this.idMap.get("f"), this.idMap.get("b"), new Date());
		this.nodeTreeDao.create(this.idMap.get("u"), this.idMap.get("b"), new Date());
		this.nodeTreeDao.create(this.idMap.get("h"), this.idMap.get("u"), new Date());
		this.nodeTreeDao.create(this.idMap.get("i"), this.idMap.get("u"), new Date());
		this.nodeTreeDao.create(this.idMap.get("j"), this.idMap.get("u"), new Date());
		this.nodeTreeDao.create(this.idMap.get("k"), this.idMap.get("u"), new Date());
		this.nodeTreeDao.create(this.idMap.get("l"), this.idMap.get("h"), new Date());
		this.nodeTreeDao.create(this.idMap.get("m"), this.idMap.get("j"), new Date());
		this.nodeTreeDao.create(this.idMap.get("n"), this.idMap.get("j"), new Date());
		this.nodeTreeDao.create(this.idMap.get("c"), this.idMap.get("a"), new Date());
		this.nodeTreeDao.create(this.idMap.get("d"), this.idMap.get("a"), new Date());
		this.nodeTreeDao.create(this.idMap.get("g"), this.idMap.get("d"), new Date());

		// Pause for 1.2 seconds to deal with eventual consistency
		// As all the read methods do not use consistent reads
		Thread.sleep(1200L);
	}

	@After
	public void after() {
		if (this.idMap != null) {
			Collection<String> ids = this.idMap.values();
			for (String id : ids) {
				String hashKey = DboNodeLineage.createHashKey(id, LineageType.ANCESTOR);
				AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
				DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
				List<DboNodeLineage> dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
				this.dynamoMapper.batchDelete(dboList);
				hashKey = DboNodeLineage.createHashKey(id, LineageType.DESCENDANT);
				hashKeyAttr = new AttributeValue().withS(hashKey);
				queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
				dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
				this.dynamoMapper.batchDelete(dboList);
			}
		}
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		List<DboNodeLineage> dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		this.dynamoMapper.batchDelete(dboList);
	}

	@Test
	public void test() {

		// testGetRoot()
		{
			String root = this.nodeTreeDao.getRoot();
			Assert.assertEquals(this.idMap.get("a"), root);
		}

		// testGetAncestors()
		{
			// Root has 0 ancestors
			List<String> ancestorList = this.nodeTreeDao.getAncestors(this.idMap.get("a"));
			Assert.assertTrue(ancestorList.isEmpty());
			ancestorList = this.nodeTreeDao.getAncestors(this.idMap.get("b"));
			Assert.assertEquals(1, ancestorList.size());
			Assert.assertEquals(this.idMap.get("a"), ancestorList.get(0));
			ancestorList = this.nodeTreeDao.getAncestors(this.idMap.get("u"));
			Assert.assertEquals(2, ancestorList.size());
			Assert.assertEquals(this.idMap.get("a"), ancestorList.get(0));
			Assert.assertEquals(this.idMap.get("b"), ancestorList.get(1));
			ancestorList = this.nodeTreeDao.getAncestors(this.idMap.get("m"));
			Assert.assertEquals(4, ancestorList.size());
			Assert.assertEquals(this.idMap.get("a"), ancestorList.get(0));
			Assert.assertEquals(this.idMap.get("b"), ancestorList.get(1));
			Assert.assertEquals(this.idMap.get("u"), ancestorList.get(2));
			Assert.assertEquals(this.idMap.get("j"), ancestorList.get(3));
			// A node that does not exist has 0 ancestors
			ancestorList = this.nodeTreeDao.getAncestors("fakeNode");
			Assert.assertTrue(ancestorList.isEmpty());
		}

		// testGetParent()
		{
			// Root's parent is the dummy ROOT
			String parent = this.nodeTreeDao.getParent(this.idMap.get("a"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(DboNodeLineage.ROOT, parent);
			// A non-existent node's parent is null
			parent = this.nodeTreeDao.getParent("fakeNode");
			Assert.assertNull(parent);
			parent = this.nodeTreeDao.getParent(this.idMap.get("b"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("a"), parent);
			parent = this.nodeTreeDao.getParent(this.idMap.get("e"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("b"), parent);
			parent = this.nodeTreeDao.getParent(this.idMap.get("u"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("b"), parent);
			parent = this.nodeTreeDao.getParent(this.idMap.get("m"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("j"), parent);
			parent = this.nodeTreeDao.getParent(this.idMap.get("g"));
			Assert.assertNotNull(parent);
			Assert.assertEquals(this.idMap.get("d"), parent);
		}

		// testGetDescendants()
		{

			List<String> descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 100, null);
			Set<String> descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(14, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("b")));
			Assert.assertTrue(descSet.contains(this.idMap.get("c")));
			Assert.assertTrue(descSet.contains(this.idMap.get("d")));
			Assert.assertTrue(descSet.contains(this.idMap.get("e")));
			Assert.assertTrue(descSet.contains(this.idMap.get("f")));
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));
			Assert.assertTrue(descSet.contains(this.idMap.get("u")));

			descList = this.nodeTreeDao.getDescendants(this.idMap.get("u"), 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(7, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));

			descList = this.nodeTreeDao.getDescendants(this.idMap.get("d"), 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(1, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));

			descList = this.nodeTreeDao.getDescendants(this.idMap.get("f"), 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(0, descSet.size());
		}

		// testGetDescendantsPaging()
		{
			List<String> descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 2, null);
			Assert.assertEquals(2, descList.size());
			Set<String> descSet = new HashSet<String>();
			descSet.addAll(descList);
			descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 7, descList.get(1));
			Assert.assertEquals(7, descList.size());
			descSet.addAll(descList);
			descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 7, descList.get(6));
			Assert.assertEquals((14 - 2 - 7), descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(14, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("b")));
			Assert.assertTrue(descSet.contains(this.idMap.get("c")));
			Assert.assertTrue(descSet.contains(this.idMap.get("d")));
			Assert.assertTrue(descSet.contains(this.idMap.get("e")));
			Assert.assertTrue(descSet.contains(this.idMap.get("f")));
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));
			Assert.assertTrue(descSet.contains(this.idMap.get("u")));
		}

		// testGetDescendantsGeneration()
		{
			List<String> descList = this.nodeTreeDao.getDescendants(this.idMap.get("u"), 1, 100, null);
			Set<String> descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(4, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("h")));
			Assert.assertTrue(descSet.contains(this.idMap.get("i")));
			Assert.assertTrue(descSet.contains(this.idMap.get("j")));
			Assert.assertTrue(descSet.contains(this.idMap.get("k")));
			descList = this.nodeTreeDao.getDescendants(this.idMap.get("u"), 2, 100, null);
			descSet = new HashSet<String>(descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(3, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("l")));
			Assert.assertTrue(descSet.contains(this.idMap.get("m")));
			Assert.assertTrue(descSet.contains(this.idMap.get("n")));
		}

		// testGetDescendantsGenerationPaging()
		{
			List<String> descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 2, 1, null);
			Assert.assertEquals(1, descList.size());
			Set<String> descSet = new HashSet<String>();
			descSet.addAll(descList);
			descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 2, 1, descList.get(0));
			Assert.assertEquals(1, descList.size());
			descSet.addAll(descList);
			descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 2, 2, descList.get(0));
			Assert.assertEquals(2, descList.size());
			descSet.addAll(descList);
			descList = this.nodeTreeDao.getDescendants(this.idMap.get("a"), 2, 2, descList.get(1));
			Assert.assertEquals(0, descList.size());
			descSet.addAll(descList);
			Assert.assertEquals(4, descSet.size());
			Assert.assertTrue(descSet.contains(this.idMap.get("e")));
			Assert.assertTrue(descSet.contains(this.idMap.get("f")));
			Assert.assertTrue(descSet.contains(this.idMap.get("u")));
			Assert.assertTrue(descSet.contains(this.idMap.get("g")));
		}

		// testGetChildren()
		{
			List<String> childList = this.nodeTreeDao.getChildren(this.idMap.get("a"), 2, null);
			Assert.assertEquals(2, childList.size());
			Set<String> childSet = new HashSet<String>();
			childSet.addAll(childList);
			childList = this.nodeTreeDao.getChildren(this.idMap.get("a"), 2, childList.get(1));
			Assert.assertEquals(1, childList.size());
			childSet.addAll(childList);
			Assert.assertEquals(3, childSet.size());
			Assert.assertTrue(childSet.contains(this.idMap.get("b")));
			Assert.assertTrue(childSet.contains(this.idMap.get("c")));
			Assert.assertTrue(childSet.contains(this.idMap.get("d")));
		}

		// testGetPath()
		{
			List<String> path = this.nodeTreeDao.getPath(this.idMap.get("a"), this.idMap.get("b"));
			Assert.assertEquals(2, path.size());
			Assert.assertEquals(this.idMap.get("a"), path.get(0));
			Assert.assertEquals(this.idMap.get("b"), path.get(1));
			path = this.nodeTreeDao.getPath(this.idMap.get("b"), this.idMap.get("a"));
			Assert.assertEquals(2, path.size());
			Assert.assertEquals(this.idMap.get("a"), path.get(0));
			Assert.assertEquals(this.idMap.get("b"), path.get(1));
			path = this.nodeTreeDao.getPath(this.idMap.get("m"), this.idMap.get("b"));
			Assert.assertEquals(4, path.size());
			Assert.assertEquals(this.idMap.get("b"), path.get(0));
			Assert.assertEquals(this.idMap.get("u"), path.get(1));
			Assert.assertEquals(this.idMap.get("j"), path.get(2));
			Assert.assertEquals(this.idMap.get("m"), path.get(3));
			path = this.nodeTreeDao.getPath(this.idMap.get("d"), this.idMap.get("d"));
			Assert.assertEquals(1, path.size());
			Assert.assertEquals(this.idMap.get("d"), path.get(0));
			path = this.nodeTreeDao.getPath(this.idMap.get("m"), this.idMap.get("k"));
			Assert.assertNull(path);
		}

		// testGetLowestCommonAncestor()
		{
			String anc = this.nodeTreeDao.getLowestCommonAncestor(this.idMap.get("b"), this.idMap.get("d"));
			Assert.assertEquals(this.idMap.get("a"), anc);
			anc = this.nodeTreeDao.getLowestCommonAncestor(this.idMap.get("j"), this.idMap.get("b"));
			Assert.assertEquals(this.idMap.get("b"), anc);
			anc = this.nodeTreeDao.getLowestCommonAncestor(this.idMap.get("e"), this.idMap.get("n"));
			Assert.assertEquals(this.idMap.get("b"), anc);
		}
	}
}

package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.model.AttributeValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:dynamo-dao-spb.xml" })
public class NodeTreeDaoWriteAutowireTest {

	@Autowired private AmazonDynamoDB dynamoClient;
	@Autowired private DynamoAdminDao dynamoAdminDao;
	@Autowired private NodeTreeUpdateDao nodeTreeUpdateDao;
	@Autowired private NodeTreeQueryDao nodeTreeQueryDao;

	private DynamoDBMapper dynamoMapper;

	// Map of letters to random IDs
	private Map<String, String> idMap;

	@Before
	public void before() {

		// Clear dynamo
		this.dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);

		this.dynamoMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());
		this.idMap = DynamoTestUtil.createRandomIdMap(26);
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
		this.dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);
	}

	@Test
	public void testCreateRoot() {

		String root = this.idMap.get("a");
		Date timestamp = new Date();
		this.nodeTreeUpdateDao.create(root, root, timestamp);
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		String rangeKey = DboNodeLineage.createRangeKey(1, root);
		DboNodeLineage dbo = this.dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		NodeLineage lineage = new NodeLineage(dbo);
		Assert.assertEquals(DboNodeLineage.ROOT, lineage.getNodeId());
		Assert.assertEquals(root, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		Assert.assertEquals(LineageType.DESCENDANT, lineage.getLineageType());
		Assert.assertEquals(timestamp, lineage.getTimestamp());
		Assert.assertEquals(1L, lineage.getVersion().longValue());
		hashKey = DboNodeLineage.createHashKey(root, LineageType.ANCESTOR);
		rangeKey = DboNodeLineage.createRangeKey(1, DboNodeLineage.ROOT);
		dbo = this.dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		lineage = new NodeLineage(dbo);
		Assert.assertEquals(root, lineage.getNodeId());
		Assert.assertEquals(DboNodeLineage.ROOT, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		Assert.assertEquals(LineageType.ANCESTOR, lineage.getLineageType());
		Assert.assertEquals(timestamp, lineage.getTimestamp());
		Assert.assertEquals(1L, lineage.getVersion().longValue());

		// We can "recreate" the same root
		this.nodeTreeUpdateDao.create(root, root, new Date());

		// Create a different root "b"
		String r = this.idMap.get("b");
		this.nodeTreeUpdateDao.create(r, r, new Date());
	}

	@Test
	public void testCreate() {

		// Will create this tree here
		//
		//     a
		//     |
		//     b
		//    / \
		//   c   d
		//
		String a = this.idMap.get("a");
		String b = this.idMap.get("b");
		String c = this.idMap.get("c");
		String d = this.idMap.get("d");

		// The root does not exist yet
		// If we try to add a child, we will get back an IncompletePathException
		try {
			this.nodeTreeUpdateDao.create(b, a, new Date());
		} catch (NoAncestorException e) {
			Assert.assertNotNull(e.getMessage());
		} catch (Throwable t) {
			Assert.fail();
		}

		// Now create the root
		Date timestamp = new Date();
		this.nodeTreeUpdateDao.create(a, a, timestamp);

		// We should be able to add a new child under a
		this.nodeTreeUpdateDao.create(b, a, timestamp);
		String hashKey = DboNodeLineage.createHashKey(b, LineageType.ANCESTOR);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		List<DboNodeLineage> dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		NodeLineage lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(a, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(a, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());

		// Now add c under b
		this.nodeTreeUpdateDao.create(c, b, timestamp);
		hashKey = DboNodeLineage.createHashKey(c, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		lineage = new NodeLineage(dboList.get(1));
		Assert.assertEquals(a, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(2, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(a, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		lineage = new NodeLineage(dboList.get(1));
		Assert.assertEquals(c, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(2, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(c, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		
		// Repeat adding c should have no effect
		this.nodeTreeUpdateDao.create(c, b, timestamp);
		hashKey = DboNodeLineage.createHashKey(c, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		lineage = new NodeLineage(dboList.get(1));
		Assert.assertEquals(a, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(2, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(a, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		lineage = new NodeLineage(dboList.get(1));
		Assert.assertEquals(c, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(2, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(c, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());

		// Before adding d under b, we manually remove b->a pointer
		// Which should cause IncompletePathException
		hashKey = DboNodeLineage.createHashKey(b, LineageType.ANCESTOR);
		String rangeKey = DboNodeLineage.createRangeKey(1, a);
		DboNodeLineage d2aDbo = this.dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		this.dynamoMapper.delete(d2aDbo);
		try {
			this.nodeTreeUpdateDao.create(d, b, new Date());
		} catch (IncompletePathException e) {
			Assert.assertNotNull(e.getMessage());
		} catch (Throwable t) {
			Assert.fail();
		}

		// Add back b->a and we should be able to add d->b
		d2aDbo.setVersion(null);
		this.dynamoMapper.save(d2aDbo);
		this.nodeTreeUpdateDao.create(d, b, new Date());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		lineage = new NodeLineage(dboList.get(1));
		Assert.assertEquals(a, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(2, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(a, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(3, dboList.size());
		lineage = new NodeLineage(dboList.get(0));
		Assert.assertEquals(b, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
	}

	@Test
	public void testUpdateRoot() {

		// When the root does not exist, this should call create() to create the root
		String root = this.idMap.get("a");
		Date timestamp = new Date();
		this.nodeTreeUpdateDao.update(root, root, timestamp);
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		String rangeKey = DboNodeLineage.createRangeKey(1, root);
		DboNodeLineage dbo = this.dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		NodeLineage lineage = new NodeLineage(dbo);
		Assert.assertEquals(DboNodeLineage.ROOT, lineage.getNodeId());
		Assert.assertEquals(root, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(1, lineage.getDistance());
		Assert.assertEquals(LineageType.DESCENDANT, lineage.getLineageType());
		Assert.assertEquals(timestamp, lineage.getTimestamp());
		Assert.assertEquals(1L, lineage.getVersion().longValue());

		// Update with a different root - Ok
		String b = this.idMap.get("b");
		this.nodeTreeUpdateDao.update(b, b, new Date());
	}

	@Test
	public void testUpdate() {

		// Will create this tree here
		//
		//     a
		//     |
		//     b
		//    / \
		//   c   d
		//      / \
		//     e   f
		//
		String a = this.idMap.get("a");
		String b = this.idMap.get("b");
		String c = this.idMap.get("c");
		String d = this.idMap.get("d");
		String e = this.idMap.get("e");
		String f = this.idMap.get("f");
		Date timestamp = new Date();
		this.nodeTreeUpdateDao.create(a, a, timestamp);
		this.nodeTreeUpdateDao.create(b, a, timestamp);
		this.nodeTreeUpdateDao.create(c, b, timestamp);
		this.nodeTreeUpdateDao.create(d, b, timestamp);
		this.nodeTreeUpdateDao.create(e, d, timestamp);
		this.nodeTreeUpdateDao.create(f, d, timestamp);

		// Verify d first
		String hashKey = DboNodeLineage.createHashKey(d, LineageType.ANCESTOR);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		List<DboNodeLineage> dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(b, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Set<String> descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		Assert.assertEquals(2, descSet.size());
		Assert.assertTrue(descSet.contains(e));
		Assert.assertTrue(descSet.contains(f));

		// Now update(d, b) should have no effect
		this.nodeTreeUpdateDao.update(d, b, new Date());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(b, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		Assert.assertEquals(2, descSet.size());
		Assert.assertTrue(descSet.contains(e));
		Assert.assertTrue(descSet.contains(f));

		// Move d under a
		this.nodeTreeUpdateDao.update(d, a, new Date());
		// Test d
		hashKey = DboNodeLineage.createHashKey(d, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		Assert.assertEquals(2, descSet.size());
		Assert.assertTrue(descSet.contains(e));
		Assert.assertTrue(descSet.contains(f));
		// Test b
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		Assert.assertEquals(c, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		// Test e
		hashKey = DboNodeLineage.createHashKey(e, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		// Test f
		hashKey = DboNodeLineage.createHashKey(f, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());

		// Move c under d
		this.nodeTreeUpdateDao.update(c, d, new Date());
		// Test d
		hashKey = DboNodeLineage.createHashKey(d, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(1, dboList.size());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(3, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		Assert.assertEquals(3, descSet.size());
		Assert.assertTrue(descSet.contains(c));
		Assert.assertTrue(descSet.contains(e));
		Assert.assertTrue(descSet.contains(f));
		// Test b
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		// Test c
		hashKey = DboNodeLineage.createHashKey(c, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		// Test e
		hashKey = DboNodeLineage.createHashKey(e, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		// Test f
		hashKey = DboNodeLineage.createHashKey(f, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(2, dboList.size());
		Assert.assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		Assert.assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		
		// After all the change, a's descendants should remain unchanged
		hashKey = DboNodeLineage.createHashKey(a, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(5, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		Assert.assertEquals(5, descSet.size());
		Assert.assertTrue(descSet.contains(b));
		Assert.assertTrue(descSet.contains(c));
		Assert.assertTrue(descSet.contains(d));
		Assert.assertTrue(descSet.contains(e));
		Assert.assertTrue(descSet.contains(f));
	}

	@Test
	public void testDelete() {

		// Will create this tree here
		//
		//     a
		//     |
		//     b
		//    / \
		//   c   d
		//      / \
		//     e   f
		//
		String a = this.idMap.get("a");
		String b = this.idMap.get("b");
		String c = this.idMap.get("c");
		String d = this.idMap.get("d");
		String e = this.idMap.get("e");
		String f = this.idMap.get("f");
		Date timestamp = new Date();
		this.nodeTreeUpdateDao.create(a, a, timestamp);
		this.nodeTreeUpdateDao.create(b, a, timestamp);
		this.nodeTreeUpdateDao.create(c, b, timestamp);
		this.nodeTreeUpdateDao.create(d, b, timestamp);
		this.nodeTreeUpdateDao.create(e, d, timestamp);
		this.nodeTreeUpdateDao.create(f, d, timestamp);

		// Now if the time is too old, we should get back an exception and nothing happens
		try {
			Date oldTimestamp = new Date();
			oldTimestamp.setTime(timestamp.getTime() - 10000L);
			this.nodeTreeUpdateDao.delete(e, oldTimestamp);
		} catch (RuntimeException ex) {
			Assert.assertNotNull(ex.getMessage());
		} catch (Throwable t) {
			Assert.fail();
		}
		String hashKey = DboNodeLineage.createHashKey(e, LineageType.ANCESTOR);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		List<DboNodeLineage> dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(3, dboList.size());

		// Now delete with a newer time stamp
		this.nodeTreeUpdateDao.delete(e, new Date());
		hashKey = DboNodeLineage.createHashKey(e, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Set<String> idSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			idSet.add((new NodeLineage(dbo)).getAncestorOrDescendantId());
		}
		Assert.assertFalse(idSet.contains(e));

		// Delete an internal node d
		this.nodeTreeUpdateDao.delete(d, new Date());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(d, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(f, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(f, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(b, LineageType.DESCENDANT);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		idSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			idSet.add((new NodeLineage(dbo)).getAncestorOrDescendantId());
		}
		Assert.assertFalse(idSet.contains(d));
		Assert.assertFalse(idSet.contains(e));
		Assert.assertFalse(idSet.contains(f));

		// Now delete the root
		this.nodeTreeUpdateDao.delete(a, new Date());
		hashKey = DboNodeLineage.createHashKey(c, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(b, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKey(a, LineageType.ANCESTOR);
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.ROOT_HASH_KEY;
		hashKeyAttr = new AttributeValue().withS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		dboList = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Assert.assertEquals(0, dboList.size());
	}
}

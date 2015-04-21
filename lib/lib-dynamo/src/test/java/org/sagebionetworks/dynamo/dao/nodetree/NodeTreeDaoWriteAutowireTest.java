package org.sagebionetworks.dynamo.dao.nodetree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
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
		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		
		dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);
		dynamoMapper = new DynamoDBMapper(dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());
		idMap = DynamoTestUtil.createRandomIdMap(26);
	}

	@After
	public void after() {

		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;
		
		dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);
	}

	@Test
	public void testCreateRoot() {

		final String a = idMap.get("a");
		final Date timestampA = new Date();
		assertTrue(nodeTreeUpdateDao.create(a, a, timestampA));
		verifyRoot(a, timestampA);

		// We can "recreate" the same root though nothing should change
		assertTrue(nodeTreeUpdateDao.create(a, a, new Date()));
		verifyRoot(a, timestampA);

		// We can create a different root at "b"
		final String b = idMap.get("b");
		final Date timestampB = new Date();
		assertTrue(nodeTreeUpdateDao.create(b, b, timestampB));
		verifyRoot(a, timestampA);
		verifyRoot(b, timestampB);
	}

	@Test
	public void testCreatePair() {

		// Create a tree like the below
		//
		//     a
		//     |
		//     b
		//    / \
		//   c   d
		//
		final String a = idMap.get("a");
		final String b = idMap.get("b");
		final String c = idMap.get("c");
		final String d = idMap.get("d");

		// The root does not exist yet
		// If we try to add a child, we will get back an NoAncestorException
		try {
			nodeTreeUpdateDao.create(b, a, new Date());
		} catch (NoAncestorException e) {
			assertNotNull(e.getMessage());
		} catch (Throwable t) {
			fail();
		}

		// Now create the root
		final Date timestampA = new Date();
		assertTrue(nodeTreeUpdateDao.create(a, a, timestampA));

		// We should be able to add a new child under a
		final Date timestampB = new Date();
		assertTrue(nodeTreeUpdateDao.create(b, a, timestampB));
		verifyRoot(a, timestampA);
		verifyPair(b, a, timestampB);

		// Now add c under b
		final Date timestampC = new Date();
		assertTrue(nodeTreeUpdateDao.create(c, b, timestampC));
		verifyRoot(a, timestampA);
		verifyPair(b, a, timestampB);
		verifyPair(c, b, timestampC);
		verifyPair(c, a, timestampC, 2);

		// Repeat adding c should have no effect
		assertTrue(nodeTreeUpdateDao.create(c, b, new Date()));
		verifyRoot(a, timestampA);
		verifyPair(b, a, timestampB);
		verifyPair(c, b, timestampC);
		verifyPair(c, a, timestampC, 2);

		// Before adding d under b, we manually remove b->a pointer
		// Which should cause IncompletePathException
		String hashKey = DboNodeLineage.createHashKey(b, LineageType.ANCESTOR);
		String rangeKey = DboNodeLineage.createRangeKey(1, a);
		DboNodeLineage d2aDbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		dynamoMapper.delete(d2aDbo);
		try {
			nodeTreeUpdateDao.create(d, b, new Date());
		} catch (IncompletePathException e) {
			assertNotNull(e.getMessage());
		} catch (Throwable t) {
			fail();
		}

		// Add back b->a and we should be able to add d->b
		d2aDbo.setVersion(null);
		dynamoMapper.save(d2aDbo);
		final Date timestampD = new Date();
		assertTrue(nodeTreeUpdateDao.create(d, b, timestampD));
		verifyRoot(a, timestampA);
		verifyPair(b, a, timestampB);
		verifyPair(c, b, timestampC);
		verifyPair(c, a, timestampC, 2);
		verifyPair(d, b, timestampD);
		verifyPair(d, a, timestampD, 2);
	}

	@Test
	public void testUpdateRoot() {

		// When the root does not exist,  should call create() to create the root
		final String a = idMap.get("a");
		final Date timestampA = new Date();
		assertTrue(nodeTreeUpdateDao.update(a, a, timestampA));
		verifyRoot(a, timestampA);

		// Update by adding a different root - should be Ok
		final String b = idMap.get("b");
		final Date timestampB = new Date();
		assertTrue(nodeTreeUpdateDao.update(b, b, timestampB));
		verifyRoot(a, timestampA);
		verifyRoot(b, timestampB);

		// Add c, d to a, will have two trees like 
		//
		//     a    b
		//     |
		//     c
		//     |
		//     d
		//
		final String c = idMap.get("c");
		assertTrue(nodeTreeUpdateDao.create(c, a, new Date()));
		final String d = idMap.get("d");
		final Date timestampD = new Date();
		assertTrue(nodeTreeUpdateDao.create(d, c, timestampD));

		// Now promote c to be a new root
		// We should have three trees after the update
		//
		//     a    b    c
		//               |
		//               d
		//
		final Date timestampC = new Date();
		assertTrue(nodeTreeUpdateDao.update(c, c, timestampC));
		verifyRoot(a, timestampA);
		verifyRoot(b, timestampB);
		verifyRoot(c, timestampC);
		verifyPair(d, c, timestampD);

		//  has no further effect
		assertTrue(nodeTreeUpdateDao.update(c, c, new Date()));
		verifyRoot(a, timestampA);
		verifyRoot(b, timestampB);
		verifyRoot(c, timestampC);
		verifyPair(d, c, timestampD);

		//  also has no further effect
		assertTrue(nodeTreeUpdateDao.create(c, c, new Date()));
		verifyRoot(a, timestampA);
		verifyRoot(b, timestampB);
		verifyRoot(c, timestampC);
		verifyPair(d, c, timestampD);

		// Now move c to b
		// We should have three trees after the update
		//
		//     a    b
		//          |
		//          c
		//          |
		//          d
		//
		final Date timestampC2 = new Date();
		assertTrue(nodeTreeUpdateDao.update(c, b, timestampC2));
		verifyRoot(a, timestampA);
		verifyRoot(b, timestampB);
		verifyPair(c, b, timestampC2);
		verifyPair(d, c, timestampD);
		verifyPair(d, b, timestampC2, 2);
		verifyNotRoot(c);

		// Add e to d to make sure c and d are in a valid state
		final String e = idMap.get("e");
		final Date timestampE = new Date();
		assertTrue(nodeTreeUpdateDao.update(e, d, timestampE));
	}

	@Test
	public void testUpdatePair() {

		// Will create  tree here
		//
		//     a
		//     |
		//     b
		//    / \
		//   c   d
		//      / \
		//     e   f
		//
		String a = idMap.get("a");
		String b = idMap.get("b");
		String c = idMap.get("c");
		String d = idMap.get("d");
		String e = idMap.get("e");
		String f = idMap.get("f");
		Date timestamp = new Date();
		nodeTreeUpdateDao.create(a, a, timestamp);
		nodeTreeUpdateDao.create(b, a, timestamp);
		nodeTreeUpdateDao.create(c, b, timestamp);
		nodeTreeUpdateDao.create(d, b, timestamp);
		nodeTreeUpdateDao.create(e, d, timestamp);
		nodeTreeUpdateDao.create(f, d, timestamp);

		// Verify d first
		DboNodeLineage hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.ANCESTOR);
		DynamoDBQueryExpression<DboNodeLineage> queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		List<DboNodeLineage> dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(b, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		Set<String> descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		assertEquals(2, descSet.size());
		assertTrue(descSet.contains(e));
		assertTrue(descSet.contains(f));

		// Now update(d, b) should have no effect
		nodeTreeUpdateDao.update(d, b, new Date());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(b, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		assertEquals(2, descSet.size());
		assertTrue(descSet.contains(e));
		assertTrue(descSet.contains(f));

		// Move d under a
		nodeTreeUpdateDao.update(d, a, new Date());
		// Test d
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(1, dboList.size());
		assertEquals(a, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		assertEquals(2, descSet.size());
		assertTrue(descSet.contains(e));
		assertTrue(descSet.contains(f));
		// Test b
		hashKey = DboNodeLineage.createHashKeyValue(b, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(1, dboList.size());
		assertEquals(c, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		// Test e
		hashKey = DboNodeLineage.createHashKeyValue(e, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		// Test f
		hashKey = DboNodeLineage.createHashKeyValue(f, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());

		// Move c under d
		nodeTreeUpdateDao.update(c, d, new Date());
		// Test d
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(1, dboList.size());
		assertEquals(a, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(3, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		assertEquals(3, descSet.size());
		assertTrue(descSet.contains(c));
		assertTrue(descSet.contains(e));
		assertTrue(descSet.contains(f));
		// Test b
		hashKey = DboNodeLineage.createHashKeyValue(b, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		// Test c
		hashKey = DboNodeLineage.createHashKeyValue(c, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		// Test e
		hashKey = DboNodeLineage.createHashKeyValue(e, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		// Test f
		hashKey = DboNodeLineage.createHashKeyValue(f, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(2, dboList.size());
		assertEquals(d, (new NodeLineage(dboList.get(0))).getAncestorOrDescendantId());
		assertEquals(a, (new NodeLineage(dboList.get(1))).getAncestorOrDescendantId());
		
		// After all the change, a's descendants should remain unchanged
		hashKey = DboNodeLineage.createHashKeyValue(a, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(5, dboList.size());
		descSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			NodeLineage lineage = new NodeLineage(dbo);
			descSet.add(lineage.getAncestorOrDescendantId());
		}
		assertEquals(5, descSet.size());
		assertTrue(descSet.contains(b));
		assertTrue(descSet.contains(c));
		assertTrue(descSet.contains(d));
		assertTrue(descSet.contains(e));
		assertTrue(descSet.contains(f));
	}

	@Test
	public void testDelete() {

		// Will create  tree here
		//
		//     a
		//     |
		//     b
		//    / \
		//   c   d
		//      / \
		//     e   f
		//
		String a = idMap.get("a");
		String b = idMap.get("b");
		String c = idMap.get("c");
		String d = idMap.get("d");
		String e = idMap.get("e");
		String f = idMap.get("f");
		Date timestamp = new Date();
		nodeTreeUpdateDao.create(a, a, timestamp);
		nodeTreeUpdateDao.create(b, a, timestamp);
		nodeTreeUpdateDao.create(c, b, timestamp);
		nodeTreeUpdateDao.create(d, b, timestamp);
		nodeTreeUpdateDao.create(e, d, timestamp);
		nodeTreeUpdateDao.create(f, d, timestamp);

		// Now if the time is too old, we should get back an exception and nothing happens
		try {
			Date oldTimestamp = new Date();
			oldTimestamp.setTime(timestamp.getTime() - 10000L);
			nodeTreeUpdateDao.delete(e, oldTimestamp);
		} catch (RuntimeException ex) {
			assertNotNull(ex.getMessage());
		} catch (Throwable t) {
			fail();
		}
		DboNodeLineage hashKey = DboNodeLineage.createHashKeyValue(e, LineageType.ANCESTOR);
		DynamoDBQueryExpression<DboNodeLineage> queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		List<DboNodeLineage> dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(3, dboList.size());

		// Now delete with a newer time stamp
		nodeTreeUpdateDao.delete(e, new Date());
		hashKey = DboNodeLineage.createHashKeyValue(e, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(b, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		Set<String> idSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			idSet.add((new NodeLineage(dbo)).getAncestorOrDescendantId());
		}
		assertFalse(idSet.contains(e));

		// Delete an internal node d
		nodeTreeUpdateDao.delete(d, new Date());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(d, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(f, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(f, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(b, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		idSet = new HashSet<String>();
		for (DboNodeLineage dbo : dboList) {
			idSet.add((new NodeLineage(dbo)).getAncestorOrDescendantId());
		}
		assertFalse(idSet.contains(d));
		assertFalse(idSet.contains(e));
		assertFalse(idSet.contains(f));

		// Now delete the root
		nodeTreeUpdateDao.delete(a, new Date());
		hashKey = DboNodeLineage.createHashKeyValue(c, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(b, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(a, LineageType.ANCESTOR);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
		hashKey = DboNodeLineage.createHashKeyValue(DboNodeLineage.ROOT, LineageType.DESCENDANT);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertEquals(0, dboList.size());
	}

	private void verifyRoot(final String root, final Date timestamp) {
		// Verify the descendant pointer
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		String rangeKey = DboNodeLineage.createRangeKey(1, root);
		DboNodeLineage dbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		NodeLineage lineage = new NodeLineage(dbo);
		assertEquals(DboNodeLineage.ROOT, lineage.getNodeId());
		assertEquals(root, lineage.getAncestorOrDescendantId());
		assertEquals(1, lineage.getDistance());
		assertEquals(LineageType.DESCENDANT, lineage.getLineageType());
		assertEquals(timestamp, lineage.getTimestamp());
		assertEquals(1L, lineage.getVersion().longValue());
		// Verify the ancestor pointer
		hashKey = DboNodeLineage.createHashKey(root, LineageType.ANCESTOR);
		rangeKey = DboNodeLineage.createRangeKey(1, DboNodeLineage.ROOT);
		dbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		lineage = new NodeLineage(dbo);
		assertEquals(root, lineage.getNodeId());
		assertEquals(DboNodeLineage.ROOT, lineage.getAncestorOrDescendantId());
		assertEquals(1, lineage.getDistance());
		assertEquals(LineageType.ANCESTOR, lineage.getLineageType());
		assertEquals(timestamp, lineage.getTimestamp());
		assertEquals(1L, lineage.getVersion().longValue());
	}

	private void verifyNotRoot(final String node) {
		// Verify the descendant pointer
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		String rangeKey = DboNodeLineage.createRangeKey(1, node);
		DboNodeLineage dbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		assertNull(dbo);
		// Verify the ancestor pointer
		hashKey = DboNodeLineage.createHashKey(node, LineageType.ANCESTOR);
		rangeKey = DboNodeLineage.createRangeKey(1, DboNodeLineage.ROOT);
		dbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		assertNull(dbo);
	}

	private void verifyPair(final String child, final String parent, final Date timestamp) {
		// Verify the ancestor pointer
		DboNodeLineage hashKey = DboNodeLineage.createHashKeyValue(child, LineageType.ANCESTOR);
		DynamoDBQueryExpression<DboNodeLineage> queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(hashKey);
		List<DboNodeLineage> dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		assertTrue(dboList.size() > 0);
		NodeLineage lineage = new NodeLineage(dboList.get(0));
		assertEquals(child, lineage.getNodeId());
		assertEquals(parent, lineage.getAncestorOrDescendantId());
		assertEquals(1, lineage.getDistance());
		assertEquals(LineageType.ANCESTOR, lineage.getLineageType());
		assertEquals(timestamp, lineage.getTimestamp());
		assertEquals(1L, lineage.getVersion().longValue());
		// Verify the descendant pointer
		String hashKeyString = DboNodeLineage.createHashKey(parent, LineageType.DESCENDANT);
		String rangeKey = DboNodeLineage.createRangeKey(1, child);
		DboNodeLineage dbo = dynamoMapper.load(DboNodeLineage.class, hashKeyString, rangeKey);
		lineage = new NodeLineage(dbo);
		assertEquals(parent, lineage.getNodeId());
		assertEquals(child, lineage.getAncestorOrDescendantId());
		assertEquals(1, lineage.getDistance());
		assertEquals(LineageType.DESCENDANT, lineage.getLineageType());
		assertEquals(timestamp, lineage.getTimestamp());
		assertEquals(1L, lineage.getVersion().longValue());
	}

	private void verifyPair(final String child, final String parent, final Date timestamp, final int distance) {
		// Verify the ancestor pointer
		String hashKey = DboNodeLineage.createHashKey(child, LineageType.ANCESTOR);
		String rangeKey = DboNodeLineage.createRangeKey(distance, parent);
		DboNodeLineage dbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		NodeLineage lineage = new NodeLineage(dbo);
		assertEquals(child, lineage.getNodeId());
		assertEquals(parent, lineage.getAncestorOrDescendantId());
		assertEquals(distance, lineage.getDistance());
		assertEquals(LineageType.ANCESTOR, lineage.getLineageType());
		assertEquals(timestamp, lineage.getTimestamp());
		assertEquals(1L, lineage.getVersion().longValue());
		// Verify the descendant pointer
		hashKey = DboNodeLineage.createHashKey(parent, LineageType.DESCENDANT);
		rangeKey = DboNodeLineage.createRangeKey(distance, child);
		dbo = dynamoMapper.load(DboNodeLineage.class, hashKey, rangeKey);
		lineage = new NodeLineage(dbo);
		assertEquals(parent, lineage.getNodeId());
		assertEquals(child, lineage.getAncestorOrDescendantId());
		assertEquals(distance, lineage.getDistance());
		assertEquals(LineageType.DESCENDANT, lineage.getLineageType());
		assertEquals(timestamp, lineage.getTimestamp());
		assertEquals(1L, lineage.getVersion().longValue());
	}

	@Test
	public void testUpdate() {

		final String a = idMap.get("a");
		final Date timestampA = new Date();
		assertTrue(nodeTreeUpdateDao.create(a, a, timestampA));
		verifyRoot(a, timestampA);

		final String c = idMap.get("c");
		final Date timestampC = new Date();
		assertTrue(nodeTreeUpdateDao.create(c, a, timestampC));
		verifyPair(c, a, timestampC);

		final String b = idMap.get("b");
		final Date timestampB = new Date();
		assertTrue(nodeTreeUpdateDao.create(b, a, timestampB));
		verifyPair(b, a, timestampB);

		// With
		//
		//     a
		//    / \
		//   c   b
		//
		// Move c under b
		final Date timestamp = new Date();
		nodeTreeUpdateDao.update(c, b, timestamp);
		verifyPair(c, b, timestamp);

		// Now make sure the old ancestor pointers of c are cleaned
		List<String> ancestors = nodeTreeQueryDao.getAncestors(c);
		assertEquals(2, ancestors.size());
		assertEquals(a, ancestors.get(0));
		assertEquals(b, ancestors.get(1));
	}
}

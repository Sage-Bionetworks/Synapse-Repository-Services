package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LineagePairPutDeleteAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private DynamoDBMapper dynamoMapper;

	private String ancId;
	private String descId;
	private Date timestamp;
	private int depth;
	private int distance;
	private DboNodeLineage a2dDboSaved;
	private DboNodeLineage d2aDboSaved;

	@Before
	public void before() {

		// Run tests only if DynamoDB is enabled
		StackConfiguration config = new StackConfiguration();
		Assume.assumeTrue(config.getDynamoEnabled());

		this.dynamoMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());

		// Create a pair of random nodes
		ancId = DynamoTestUtil.nextRandomId();
		descId = DynamoTestUtil.nextRandomId();
		timestamp = new Date();
		depth = 3;
		distance = 96;
		NodeLineagePair pairToPut = new NodeLineagePair(ancId, descId, depth, distance, timestamp);

		// Put to DynamoDB
		LineagePairPut put = new LineagePairPut(pairToPut, this.dynamoMapper);
		Assert.assertEquals(ancId, put.getAncestorId());
		Assert.assertEquals(descId, put.getDescendantId());
		Assert.assertEquals(depth, put.getAncestorDepth());
		Assert.assertEquals(distance, put.getDistance());
		put.write(0);
		DboNodeLineage a2dDbo = pairToPut.getAncestor2Descendant();
		DboNodeLineage d2aDbo = pairToPut.getDescendant2Ancestor();
		a2dDboSaved = this.dynamoMapper.load(DboNodeLineage.class,
				a2dDbo.getHashKey(), a2dDbo.getRangeKey());
		d2aDboSaved = this.dynamoMapper.load(DboNodeLineage.class,
				d2aDbo.getHashKey(), d2aDbo.getRangeKey());
	}

	@After
	public void after() {

		if (a2dDboSaved != null) {
			NodeLineagePair pairToDelete = new NodeLineagePair(a2dDboSaved, depth);
			LineagePairDelete delete = new LineagePairDelete(pairToDelete, this.dynamoMapper);
			Assert.assertEquals(ancId, delete.getAncestorId());
			Assert.assertEquals(descId, delete.getDescendantId());
			Assert.assertEquals(depth, delete.getAncestorDepth());
			Assert.assertEquals(distance, delete.getDistance());
			delete.write(0);
			DboNodeLineage a2dDboDeleted = this.dynamoMapper.load(DboNodeLineage.class,
					a2dDboSaved.getHashKey(), a2dDboSaved.getRangeKey());
			DboNodeLineage d2aDboDeleted = this.dynamoMapper.load(DboNodeLineage.class,
					d2aDboSaved.getHashKey(), d2aDboSaved.getRangeKey());
			Assert.assertNull(a2dDboDeleted);
			Assert.assertNull(d2aDboDeleted);
		}
	}

	@Test
	public void test() {
		Assert.assertNotNull(a2dDboSaved);
		Assert.assertNotNull(d2aDboSaved);
		NodeLineage a2dSaved = new NodeLineage(a2dDboSaved);
		NodeLineage d2aSaved = new NodeLineage(d2aDboSaved);
		Assert.assertEquals(ancId, a2dSaved.getNodeId());
		Assert.assertEquals(LineageType.DESCENDANT, a2dSaved.getLineageType());
		Assert.assertEquals(distance, a2dSaved.getDistance());
		Assert.assertEquals(descId, a2dSaved.getAncestorOrDescendantId());
		Assert.assertEquals(timestamp, a2dSaved.getTimestamp());
		Assert.assertEquals(1L, a2dSaved.getVersion().longValue());
		Assert.assertEquals(descId, d2aSaved.getNodeId());
		Assert.assertEquals(LineageType.ANCESTOR, d2aSaved.getLineageType());
		Assert.assertEquals(distance, d2aSaved.getDistance());
		Assert.assertEquals(ancId, d2aSaved.getAncestorOrDescendantId());
		Assert.assertEquals(timestamp, d2aSaved.getTimestamp());
		Assert.assertEquals(1L, d2aSaved.getVersion().longValue());
	}
}

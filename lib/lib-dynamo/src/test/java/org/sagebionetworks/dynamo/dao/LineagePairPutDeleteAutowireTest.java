package org.sagebionetworks.dynamo.dao;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:dynamo-dao-spb.xml" })
public class LineagePairPutDeleteAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private DynamoDBMapper dynamoMapper;

	@Before
	public void before() {
		this.dynamoMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());
	}

	@Test
	public void test() {

		String ancId = DynamoTestUtil.nextRandomId();
		String descId = DynamoTestUtil.nextRandomId();
		Date timestamp = new Date();
		int depth = 3;
		int distance = 96;
		NodeLineagePair pairToPut = new NodeLineagePair(ancId, descId, depth, distance, timestamp);

		// Test put
		LineagePairPut put = new LineagePairPut(pairToPut, this.dynamoMapper);
		Assert.assertEquals(ancId, put.getAncestorId());
		Assert.assertEquals(descId, put.getDescendantId());
		Assert.assertEquals(depth, put.getAncestorDepth());
		Assert.assertEquals(distance, put.getDistance());
		put.write(0);
		DboNodeLineage a2dDbo = pairToPut.getAncestor2Descendant();
		DboNodeLineage d2aDbo = pairToPut.getDescendant2Ancestor();
		DboNodeLineage a2dDboSaved = this.dynamoMapper.load(DboNodeLineage.class,
				a2dDbo.getHashKey(), a2dDbo.getRangeKey());
		DboNodeLineage d2aDboSaved = this.dynamoMapper.load(DboNodeLineage.class,
				d2aDbo.getHashKey(), d2aDbo.getRangeKey());
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

		// Test delete
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

package org.sagebionetworks.dynamo.dao;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig.TableNameOverride;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:dynamo-dao-spb.xml" })
public class NodeLineageAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private DynamoDBMapper mapper;

	@Before
	public void before() {
		String stackPrefix = StackConfiguration.getStack() + "-";
		TableNameOverride tableNameOverride = new TableNameOverride(
				stackPrefix + NodeLineage.TABLE_NAME);
		DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(null, ConsistentReads.CONSISTENT,
				tableNameOverride);
		this.mapper = new DynamoDBMapper(this.dynamoClient, mapperConfig);
	}

	@Test
	public void testRoundTrip() {
		String u = this.nextRandomId();
		String v = this.nextRandomId();
		NodeLineage u2v = new NodeLineage(u, v, LineageType.DESCENDANT, 1, new Date());
		NodeLineage v2u = new NodeLineage(v, u, LineageType.ANCESTOR, 0, new Date());
		this.mapper.save(u2v);
		this.mapper.save(u2v);
		this.mapper.save(v2u);
		NodeLineage u2vSaved = this.mapper.load(NodeLineage.class, u2v.getHashKey(),
				u2v.getRangeKey());
		NodeLineage v2uSaved = this.mapper.load(NodeLineage.class, v2u.getHashKey(),
				v2u.getRangeKey());
		Assert.assertEquals(u2v.getAncestorOrDescendantId(), u2vSaved.getAncestorOrDescendantId());
		Assert.assertEquals(u2v.getDistance(), u2vSaved.getDistance());
		Assert.assertEquals(u2v.getRangeKey(), u2vSaved.getRangeKey());
		Assert.assertEquals(u2v.getNodeId(), u2vSaved.getNodeId());
		Assert.assertEquals(u2v.getHashKey(), u2vSaved.getHashKey());
		Assert.assertEquals(u2v.getLineageType(), u2vSaved.getLineageType());
		Assert.assertEquals(2L, u2vSaved.getVersion().longValue());
		Assert.assertEquals(u2v.getTimestamp(), u2vSaved.getTimestamp());
		Assert.assertEquals(v2u.getAncestorOrDescendantId(), v2uSaved.getAncestorOrDescendantId());
		Assert.assertEquals(v2u.getDistance(), v2uSaved.getDistance());
		Assert.assertEquals(v2u.getRangeKey(), v2uSaved.getRangeKey());
		Assert.assertEquals(v2u.getNodeId(), v2uSaved.getNodeId());
		Assert.assertEquals(v2u.getHashKey(), v2uSaved.getHashKey());
		Assert.assertEquals(v2u.getLineageType(), v2uSaved.getLineageType());
		Assert.assertEquals(1L, v2uSaved.getVersion().longValue());
		Assert.assertEquals(v2u.getTimestamp(), v2uSaved.getTimestamp());
		this.mapper.delete(u2vSaved);
		this.mapper.delete(v2uSaved);
		u2vSaved = this.mapper.load(NodeLineage.class, u2vSaved.getHashKey(),
				u2vSaved.getRangeKey());
		v2uSaved = this.mapper.load(NodeLineage.class, v2uSaved.getHashKey(),
				v2uSaved.getRangeKey());
		Assert.assertNull(u2vSaved);
		Assert.assertNull(v2uSaved);
	}

	private String nextRandomId() {
		return Long.toString((long)(Math.random()*1000000000.0));
	}
}

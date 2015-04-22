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
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeLineageAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private DynamoDBMapper mapper;

	private NodeLineage u2vLineage;
	private NodeLineage v2uLineage;
	private DboNodeLineage u2vSavedDbo;
	private DboNodeLineage v2uSavedDbo;

	@Before
	public void before() throws Exception {
		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		
		DynamoDBMapperConfig mapperConfig = NodeLineageMapperConfig.getMapperConfigWithConsistentReads();
		this.mapper = new DynamoDBMapper(this.dynamoClient, mapperConfig);
		String u = DynamoTestUtil.nextRandomId();
		String v = DynamoTestUtil.nextRandomId();
		u2vLineage = new NodeLineage(u, LineageType.DESCENDANT, 1, v, new Date());
		v2uLineage = new NodeLineage(v, LineageType.ANCESTOR, 1, u, new Date());
		DboNodeLineage u2vDbo = u2vLineage.createDbo();
		DboNodeLineage v2uDbo = v2uLineage.createDbo();
		this.mapper.save(u2vDbo);
		Thread.sleep(5000);       // Sleep 5 seconds
		this.mapper.save(u2vDbo); // Note that put with null version skips optimistic locking
		this.mapper.save(v2uDbo);
		u2vSavedDbo = this.mapper.load(DboNodeLineage.class,
				u2vDbo.getHashKey(), u2vDbo.getRangeKey());
		v2uSavedDbo = this.mapper.load(DboNodeLineage.class,
				v2uDbo.getHashKey(), v2uDbo.getRangeKey());
	}

	@After
	public void after() {
		
		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;
		
		if (u2vSavedDbo != null) {
			this.mapper.delete(u2vSavedDbo);
		}
		if (v2uSavedDbo != null) {
			this.mapper.delete(v2uSavedDbo);
		}
		// Delete more than once will get 400 ConditionalCheckFailedException
		// The following deletes would fail:
		// this.mapper.delete(u2vSavedDbo);
		// this.mapper.delete(u2vDbo);
		DboNodeLineage u2vDbo = this.mapper.load(DboNodeLineage.class,
				u2vSavedDbo.getHashKey(), u2vSavedDbo.getRangeKey());
		DboNodeLineage v2uDbo = this.mapper.load(DboNodeLineage.class,
				v2uSavedDbo.getHashKey(), v2uSavedDbo.getRangeKey());
		Assert.assertNull(u2vDbo);
		Assert.assertNull(v2uDbo);
	}

	@Test
	public void testRoundTrip() throws Exception {
		NodeLineage u2vSaved = new NodeLineage(u2vSavedDbo);
		NodeLineage v2uSaved = new NodeLineage(v2uSavedDbo);
		Assert.assertEquals(u2vLineage.getNodeId(), u2vSaved.getNodeId());
		Assert.assertEquals(u2vLineage.getLineageType(), u2vSaved.getLineageType());
		Assert.assertEquals(u2vLineage.getDistance(), u2vSaved.getDistance());
		Assert.assertEquals(u2vLineage.getAncestorOrDescendantId(), u2vSaved.getAncestorOrDescendantId());
		Assert.assertEquals(u2vLineage.getTimestamp(), u2vSaved.getTimestamp());
		Assert.assertEquals(2L, u2vSaved.getVersion().longValue());
		Assert.assertEquals(v2uLineage.getNodeId(), v2uSaved.getNodeId());
		Assert.assertEquals(v2uLineage.getLineageType(), v2uSaved.getLineageType());
		Assert.assertEquals(v2uLineage.getDistance(), v2uSaved.getDistance());
		Assert.assertEquals(v2uLineage.getAncestorOrDescendantId(), v2uSaved.getAncestorOrDescendantId());
		Assert.assertEquals(1L, v2uSaved.getVersion().longValue());
		Assert.assertEquals(v2uLineage.getTimestamp(), v2uSaved.getTimestamp());
		// Deleting a non-existing node should run just fine without errors
		String p = DynamoTestUtil.nextRandomId();
		String q = DynamoTestUtil.nextRandomId();
		NodeLineage lineage = new NodeLineage(p, LineageType.DESCENDANT, 1, q, new Date());
		DboNodeLineage dbo = lineage.createDbo();
		this.mapper.delete(dbo);
	}
}

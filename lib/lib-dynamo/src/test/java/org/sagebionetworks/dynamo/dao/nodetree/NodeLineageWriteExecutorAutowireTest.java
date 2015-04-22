package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.sagebionetworks.dynamo.DynamoWriteExecution;
import org.sagebionetworks.dynamo.DynamoWriteOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeLineageWriteExecutorAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private DynamoDBMapper dynamoMapper;

	private NodeLineagePair a2r;
	private NodeLineagePair b2a;
	private NodeLineagePair c2a;
	private NodeLineagePair d2b;
	private NodeLineagePair d2a;

	@Before
	public void before() {

		// Run tests only if DynamoDB is enabled
		StackConfiguration config = new StackConfiguration();
		Assume.assumeTrue(config.getDynamoEnabled());

		this.dynamoMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());

		//
		//         a
		//        / \
		//       b   c
		//      / \
		//     d  [e]
		//
		//    a#A  0#r  // r is the dummy root
		//    b#A  1#a
		//    c#A  1#a
		//    d#A  1#b
		//    d#A  2#a
		//    a#D  1#b
		//    a#D  1#c
		//    a#D  2#d
		//    b#D  2#d
		//
		//    e is to be added
		//

		Map<String, String> idMap = DynamoTestUtil.createRandomIdMap(26);
		this.a2r = LineageTestUtil.parse("a#A 1#r", 0, idMap);
		this.b2a = LineageTestUtil.parse("b#A 1#a", 0, idMap);
		this.c2a = LineageTestUtil.parse("c#A 1#a", 0, idMap);
		this.d2b = LineageTestUtil.parse("d#A 1#b", 1, idMap);
		this.d2a = LineageTestUtil.parse("d#A 2#a", 0, idMap);

		DynamoWriteOperation a2rOp = new LineagePairPut(a2r, dynamoMapper);
		DynamoWriteOperation b2aOp = new LineagePairPut(b2a, dynamoMapper);
		DynamoWriteOperation c2aOp = new LineagePairPut(c2a, dynamoMapper);
		DynamoWriteOperation d2bOp = new LineagePairPut(d2b, dynamoMapper);
		DynamoWriteOperation d2aOp = new LineagePairPut(d2a, dynamoMapper);

		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>();
		opList.add(a2rOp);
		opList.add(b2aOp);
		opList.add(c2aOp);
		opList.add(d2bOp);
		opList.add(d2aOp);

		NodeLineageWriteExecutor executor = new NodeLineageWriteExecutor();
		DynamoWriteExecution exe = new DynamoWriteExecution("putList", opList);
		executor.execute(exe);
	}

	@After
	public void after() {
		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;

		DboNodeLineage a2rDbo = this.a2r.getDescendant2Ancestor();
		DboNodeLineage b2aDbo = this.b2a.getDescendant2Ancestor();
		DboNodeLineage c2aDbo = this.c2a.getDescendant2Ancestor();
		DboNodeLineage d2bDbo = this.d2b.getDescendant2Ancestor();
		DboNodeLineage d2aDbo = this.d2a.getDescendant2Ancestor();

		DboNodeLineage a2rDboLoaded = dynamoMapper.load(DboNodeLineage.class, a2rDbo.getHashKey(), a2rDbo.getRangeKey());
		DboNodeLineage b2aDboLoaded = dynamoMapper.load(DboNodeLineage.class, b2aDbo.getHashKey(), b2aDbo.getRangeKey());
		DboNodeLineage c2aDboLoaded = dynamoMapper.load(DboNodeLineage.class, c2aDbo.getHashKey(), c2aDbo.getRangeKey());
		DboNodeLineage d2bDboLoaded = dynamoMapper.load(DboNodeLineage.class, d2bDbo.getHashKey(), d2bDbo.getRangeKey());
		DboNodeLineage d2aDboLoaded = dynamoMapper.load(DboNodeLineage.class, d2aDbo.getHashKey(), d2aDbo.getRangeKey());

		DynamoWriteOperation a2aOp = new LineagePairDelete(new NodeLineagePair(a2rDboLoaded, 0), dynamoMapper);
		DynamoWriteOperation b2aOp = new LineagePairDelete(new NodeLineagePair(b2aDboLoaded, 0), dynamoMapper);
		DynamoWriteOperation c2aOp = new LineagePairDelete(new NodeLineagePair(c2aDboLoaded, 0), dynamoMapper);
		DynamoWriteOperation d2bOp = new LineagePairDelete(new NodeLineagePair(d2bDboLoaded, 1), dynamoMapper);
		DynamoWriteOperation d2aOp = new LineagePairDelete(new NodeLineagePair(d2aDboLoaded, 0), dynamoMapper);

		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>();
		opList.add(a2aOp);
		opList.add(b2aOp);
		opList.add(c2aOp);
		opList.add(d2bOp);
		opList.add(d2aOp);

		NodeLineageWriteExecutor executor = new NodeLineageWriteExecutor();
		DynamoWriteExecution exe = new DynamoWriteExecution("deleteList", opList);
		executor.execute(exe);
	}

	@Test
	public void verify() {

		// Verify root
		DboNodeLineage a2r = this.a2r.getDescendant2Ancestor();
		String hashKey = a2r.getHashKey();
		AttributeValue hashKeyAttr = new AttributeValue();
		hashKeyAttr.setS(hashKey);
		DynamoDBQueryExpression<DboNodeLineage> queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(a2r);
		PaginatedQueryList<DboNodeLineage> results = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		results.loadAllResults();

		Assert.assertEquals(1, results.size());
		Assert.assertEquals(a2r.getHashKey(), results.get(0).getHashKey());
		Assert.assertEquals(a2r.getRangeKey(), results.get(0).getRangeKey());

		// Verify ancestor-to-descendant
		DboNodeLineage a2d = this.d2a.getAncestor2Descendant();
		hashKey = a2d.getHashKey();
		hashKeyAttr = new AttributeValue();
		hashKeyAttr.setS(hashKey);
		queryExpression = new DynamoDBQueryExpression<DboNodeLineage>().withHashKeyValues(a2d);
		results = this.dynamoMapper.query(DboNodeLineage.class, queryExpression);
		results.loadAllResults();

		Assert.assertEquals(3, results.size());
		Assert.assertEquals(a2d.getHashKey(), results.get(2).getHashKey());
		Assert.assertEquals(a2d.getRangeKey(), results.get(2).getRangeKey());
	}
}

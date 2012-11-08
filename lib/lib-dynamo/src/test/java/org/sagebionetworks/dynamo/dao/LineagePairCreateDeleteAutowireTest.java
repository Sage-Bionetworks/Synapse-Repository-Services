package org.sagebionetworks.dynamo.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dynamo.DynamoTestUtil;
import org.sagebionetworks.dynamo.DynamoWriteOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodb.model.AttributeValue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:dynamo-dao-spb.xml" })
public class LineagePairCreateDeleteAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private DynamoDBMapper dynamoMapper;

	private NodeLineagePair a2a;
	private NodeLineagePair b2a;
	private NodeLineagePair c2a;
	private NodeLineagePair d2b;
	private NodeLineagePair d2a;

	@Before
	public void before() {

		this.dynamoMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());

		//
		//         a
		//        / \
		//       b   c
		//      / \
		//     d  [e]
		//
		//    a#A  0#a
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
		this.a2a = LineageTestUtil.parse("a#A 0#a", 0, idMap);
		this.b2a = LineageTestUtil.parse("b#A 1#a", 1, idMap);
		this.c2a = LineageTestUtil.parse("c#A 1#a", 1, idMap);
		this.d2b = LineageTestUtil.parse("d#A 1#b", 2, idMap);
		this.d2a = LineageTestUtil.parse("d#A 2#a", 2, idMap);

		DynamoWriteOperation a2aOp = new LineagePairCreate(a2a, dynamoMapper);
		DynamoWriteOperation b2aOp = new LineagePairCreate(b2a, dynamoMapper);
		DynamoWriteOperation c2aOp = new LineagePairCreate(c2a, dynamoMapper);
		DynamoWriteOperation d2bOp = new LineagePairCreate(d2b, dynamoMapper);
		DynamoWriteOperation d2aOp = new LineagePairCreate(d2a, dynamoMapper);

		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>();
		opList.add(a2aOp);
		opList.add(b2aOp);
		opList.add(c2aOp);
		opList.add(d2bOp);
		opList.add(d2aOp);

		NodeLineageWriteExecutor executor = new NodeLineageWriteExecutor();
		executor.execute(opList);
	}

	@After
	public void after() {

		DynamoWriteOperation a2aOp = new LineagePairDelete(a2a, dynamoMapper);
		DynamoWriteOperation b2aOp = new LineagePairDelete(b2a, dynamoMapper);
		DynamoWriteOperation c2aOp = new LineagePairDelete(c2a, dynamoMapper);
		DynamoWriteOperation d2bOp = new LineagePairDelete(d2b, dynamoMapper);
		DynamoWriteOperation d2aOp = new LineagePairDelete(d2a, dynamoMapper);

		List<DynamoWriteOperation> opList = new ArrayList<DynamoWriteOperation>();
		opList.add(a2aOp);
		opList.add(b2aOp);
		opList.add(c2aOp);
		opList.add(d2bOp);
		opList.add(d2aOp);

		NodeLineageWriteExecutor executor = new NodeLineageWriteExecutor();
		executor.execute(opList);
	}

	@Test
	public void verify() {

		// Verify root
		NodeLineage a2a = this.a2a.getDescendant2Ancestor();
		String hashKey = a2a.getHashKey();
		AttributeValue hashKeyAttr = new AttributeValue();
		hashKeyAttr.setS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		PaginatedQueryList<NodeLineage> results = this.dynamoMapper.query(NodeLineage.class, queryExpression);
		results.loadAllResults();

		Assert.assertEquals(1, results.size());
		Assert.assertEquals(a2a.getHashKey(), results.get(0).getHashKey());
		Assert.assertEquals(a2a.getRangeKey(), results.get(0).getRangeKey());

		// Verify ancestor-to-descendant
		NodeLineage a2d = this.d2a.getAncestor2Descendant();
		hashKey = a2d.getHashKey();
		hashKeyAttr = new AttributeValue();
		hashKeyAttr.setS(hashKey);
		queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		results = this.dynamoMapper.query(NodeLineage.class, queryExpression);
		results.loadAllResults();

		Assert.assertEquals(3, results.size());
		Assert.assertEquals(a2d.getHashKey(), results.get(2).getHashKey());
		Assert.assertEquals(a2d.getRangeKey(), results.get(2).getRangeKey());
	}
}

package org.sagebionetworks.dynamo.dao.nodetree;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public class LineagePairPutTest {

	@Test
	public void testPutOrder() {

		DboNodeLineage d2a = mock(DboNodeLineage.class);
		DboNodeLineage a2d = mock(DboNodeLineage.class);
		NodeLineagePair pair = mock(NodeLineagePair.class);
		when(pair.getDescendant2Ancestor()).thenReturn(d2a);
		when(pair.getAncestor2Descendant()).thenReturn(a2d);

		DynamoDBMapper mapper = mock(DynamoDBMapper.class);
		InOrder inOrder = inOrder(mapper);

		LineagePairPut put = new LineagePairPut(pair, mapper);
		Assert.assertTrue(put.write(0));
		inOrder.verify(mapper, times(1)).save(d2a);
		inOrder.verify(mapper, times(1)).save(a2d);
	}
}

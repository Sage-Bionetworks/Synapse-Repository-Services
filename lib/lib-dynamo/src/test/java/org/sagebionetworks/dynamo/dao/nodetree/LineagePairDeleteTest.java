package org.sagebionetworks.dynamo.dao.nodetree;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public class LineagePairDeleteTest {

	@Test
	public void testDeleteOrder() {

		DboNodeLineage d2a = mock(DboNodeLineage.class);
		DboNodeLineage a2d = mock(DboNodeLineage.class);
		NodeLineagePair pair = mock(NodeLineagePair.class);
		when(pair.getDescendant2Ancestor()).thenReturn(d2a);
		when(pair.getAncestor2Descendant()).thenReturn(a2d);

		DynamoDBMapper mapper = mock(DynamoDBMapper.class);
		InOrder inOrder = inOrder(mapper);

		LineagePairDelete delete = new LineagePairDelete(pair, mapper);
		Assert.assertTrue(delete.write(0));
		inOrder.verify(mapper, times(1)).delete(a2d);
		inOrder.verify(mapper, times(1)).delete(d2a);
	}
}

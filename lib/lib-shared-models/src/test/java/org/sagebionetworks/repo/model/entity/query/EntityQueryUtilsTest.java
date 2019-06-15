package org.sagebionetworks.repo.model.entity.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

public class EntityQueryUtilsTest {
	
	@Test
	public void testString(){
		EntityFieldCondition result = EntityQueryUtils.buildCondition(EntityFieldName.eTag, Operator.EQUALS, "AnEtag");
		assertNotNull(result);
		assertEquals(EntityFieldName.eTag, result.getLeftHandSide());
		assertEquals(Operator.EQUALS, result.getOperator());
		assertNotNull(result.getRightHandSide());
		assertEquals(1, result.getRightHandSide().size());
		StringValue expected = new StringValue();
		expected.setValue("AnEtag");
		List<Value> values = new ArrayList<Value>(1);
		values.add(expected);
		assertEquals(values, result.getRightHandSide());
	}
	
	@Test
	public void testLong(){
		EntityFieldCondition result = EntityQueryUtils.buildCondition(EntityFieldName.eTag, Operator.EQUALS, 123L);
		assertNotNull(result);
		assertEquals(EntityFieldName.eTag, result.getLeftHandSide());
		assertEquals(Operator.EQUALS, result.getOperator());
		assertNotNull(result.getRightHandSide());
		assertEquals(1, result.getRightHandSide().size());
		IntegerValue expected = new IntegerValue();
		expected.setValue(123L);
		List<Value> values = new ArrayList<Value>(1);
		values.add(expected);
		assertEquals(values, result.getRightHandSide());
	}
	
	@Test
	public void testDate(){
		EntityFieldCondition result = EntityQueryUtils.buildCondition(EntityFieldName.eTag, Operator.EQUALS, new Date(3L));
		assertNotNull(result);
		assertEquals(EntityFieldName.eTag, result.getLeftHandSide());
		assertEquals(Operator.EQUALS, result.getOperator());
		assertNotNull(result.getRightHandSide());
		assertEquals(1, result.getRightHandSide().size());
		DateValue expected = new DateValue();
		expected.setValue(new Date(3L));
		List<Value> values = new ArrayList<Value>(1);
		values.add(expected);
		assertEquals(values, result.getRightHandSide());
	}
	
	@Test
	public void testMoreThanOne(){
		EntityFieldCondition result = EntityQueryUtils.buildCondition(EntityFieldName.eTag, Operator.EQUALS, 123L, 456L);
		assertNotNull(result);
		assertEquals(EntityFieldName.eTag, result.getLeftHandSide());
		assertEquals(Operator.EQUALS, result.getOperator());
		assertNotNull(result.getRightHandSide());
		assertEquals(2, result.getRightHandSide().size());
		IntegerValue expected1 = new IntegerValue();
		expected1.setValue(123L);
		IntegerValue expected2 = new IntegerValue();
		expected2.setValue(456L);
		List<Value> values = new ArrayList<Value>(1);
		values.add(expected1);
		values.add(expected2);
		assertEquals(values, result.getRightHandSide());
	}
}

package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.ValueExpression;

public class SetFunctionSpecificationTest {

	@Test
	public void testCountStar(){
		SetFunctionSpecification element = new SetFunctionSpecification(Boolean.TRUE);
		assertEquals("COUNT(*)", element.toString());
	}
	
	
	@Test
	public void testMax() throws ParseException{
		// The simplest way to create a value expression is to parse it out
		ValueExpression valueExpression = new TableQueryParser("foo").valueExpression();
		SetFunctionSpecification element = new SetFunctionSpecification(SetFunctionType.MAX, null, valueExpression);
		assertEquals("MAX(foo)", element.toString());
	}
	
	@Test
	public void testCountDistict() throws ParseException{
		// The simplest way to create a value expression is to parse it out
		ValueExpression valueExpression = new TableQueryParser("foo").valueExpression();
		SetFunctionSpecification element = new SetFunctionSpecification(SetFunctionType.COUNT, SetQuantifier.DISTINCT, valueExpression);
		assertEquals("COUNT(DISTINCT foo)", element.toString());
	}
	
	@Test
	public void testIsAggregate() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("COUNT(one)").generalSetFunction();
		assertTrue(element.isElementAggregate());
	}
}

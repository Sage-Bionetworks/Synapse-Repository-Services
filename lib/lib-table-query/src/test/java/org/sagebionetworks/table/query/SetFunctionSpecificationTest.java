package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.ValueExpression;

public class SetFunctionSpecificationTest {

	@Test
	public void testCountStar(){
		SetFunctionSpecification element = new SetFunctionSpecification(Boolean.TRUE);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("COUNT(*)", builder.toString());
	}
	
	
	@Test
	public void testMax() throws ParseException{
		// The simplest way to create a value expression is to parse it out
		ValueExpression valueExpression = new TableQueryParser("foo").valueExpression();
		SetFunctionSpecification element = new SetFunctionSpecification(SetFunctionType.MAX, null, valueExpression);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("MAX(foo)", builder.toString());
	}
	
	@Test
	public void testCountDistict() throws ParseException{
		// The simplest way to create a value expression is to parse it out
		ValueExpression valueExpression = new TableQueryParser("foo").valueExpression();
		SetFunctionSpecification element = new SetFunctionSpecification(SetFunctionType.COUNT, SetQuantifier.DISTINCT, valueExpression);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("COUNT(DISTINCT foo)", builder.toString());
	}
	
}

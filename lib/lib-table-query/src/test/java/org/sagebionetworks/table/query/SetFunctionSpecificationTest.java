package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.FunctionType;
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
	
	@Test
	public void testGetFunctionTypeCountStart() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("count(*)").setFunctionSpecification();
		assertEquals(FunctionType.COUNT, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeCount() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("count(foo)").setFunctionSpecification();
		assertEquals(FunctionType.COUNT, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeMax() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("MAX(foo)").setFunctionSpecification();
		assertEquals(FunctionType.MAX, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeMin() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("min(foo)").setFunctionSpecification();
		assertEquals(FunctionType.MIN, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeSum() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("sum(foo)").setFunctionSpecification();
		assertEquals(FunctionType.SUM, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeAvg() throws ParseException{
		SetFunctionSpecification element = new TableQueryParser("avg(foo)").setFunctionSpecification();
		assertEquals(FunctionType.AVG, element.getFunctionType());
	}
}

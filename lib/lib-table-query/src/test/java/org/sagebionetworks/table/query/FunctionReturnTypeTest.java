package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.FunctionReturnType;

public class FunctionReturnTypeTest {

	@Test
	public void testGetColumnTypeString() {
		ColumnType parameterType = ColumnType.BOOLEAN;
		assertEquals(ColumnType.STRING, FunctionReturnType.STRING.getColumnType(parameterType));
	}
	
	@Test
	public void testGetColumnTypeLong() {
		ColumnType parameterType = ColumnType.BOOLEAN;
		assertEquals(ColumnType.INTEGER, FunctionReturnType.LONG.getColumnType(parameterType));
	}
	
	@Test
	public void testGetColumnTypeDouble() {
		ColumnType parameterType = ColumnType.BOOLEAN;
		assertEquals(ColumnType.DOUBLE, FunctionReturnType.DOUBLE.getColumnType(parameterType));
	}
	
	@Test
	public void testGetMatchesParameter() {
		ColumnType parameterType = ColumnType.BOOLEAN;
		assertEquals(ColumnType.BOOLEAN, FunctionReturnType.MATCHES_PARAMETER.getColumnType(parameterType));
	}
	
	@Test
	public void testGetMatchesParameterNull() {
		ColumnType parameterType = null;
		assertEquals(null, FunctionReturnType.MATCHES_PARAMETER.getColumnType(parameterType));
	}
}

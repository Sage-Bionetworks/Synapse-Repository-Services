package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
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

	@Test
	public void testGetUnnestParameterNonList() {
		ColumnType parameterType = ColumnType.STRING;
		assertThrows(IllegalArgumentException.class, ()-> FunctionReturnType.UNNEST_PARAMETER.getColumnType(parameterType));
	}

	@Test
	public void testGetUnnestParameterLList() {
		assertEquals(ColumnType.STRING, FunctionReturnType.UNNEST_PARAMETER.getColumnType(ColumnType.STRING_LIST));
		assertEquals(ColumnType.INTEGER, FunctionReturnType.UNNEST_PARAMETER.getColumnType(ColumnType.INTEGER_LIST));
		assertEquals(ColumnType.DATE, FunctionReturnType.UNNEST_PARAMETER.getColumnType(ColumnType.DATE_LIST));
		assertEquals(ColumnType.BOOLEAN, FunctionReturnType.UNNEST_PARAMETER.getColumnType(ColumnType.BOOLEAN_LIST));

	}

	@Test
	public void testGetUnnestParameterNull() {
		ColumnType parameterType = null;
		assertThrows(IllegalArgumentException.class, ()-> FunctionReturnType.UNNEST_PARAMETER.getColumnType(parameterType));
	}
}

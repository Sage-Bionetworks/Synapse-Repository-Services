package org.sagebionetworks.repo.manager.grid.internal.replica.view.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;

public class CellValueViewFilterTest {

	private CellValueViewFilter filter;

	@BeforeEach
	public void before() {
		filter = new CellValueViewFilter().setColumn(new Column().setName("foo").setVectorIndex(4))
				.setOperator(Operator.gte).setValue(100);
	}

	@Test
	public void testGetConditionSql() {
		// call under test
		assertEquals("JSON_EXTRACT(V1.VEC_VAL, '$.c4.v') >= :cellValue0", filter.getConditionSql(0));
		assertEquals("JSON_EXTRACT(V1.VEC_VAL, '$.c4.v') >= :cellValue2", filter.getConditionSql(2));
	}

	@Test
	public void testGetParameterKey() {
		// call under test
		assertEquals("cellValue0", filter.getParameterKey(0));
		assertEquals("cellValue2", filter.getParameterKey(2));
	}

}

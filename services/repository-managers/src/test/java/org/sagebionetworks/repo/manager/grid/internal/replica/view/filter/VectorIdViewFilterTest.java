package org.sagebionetworks.repo.manager.grid.internal.replica.view.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.filter.VectorIdViewFilter;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class VectorIdViewFilterTest {

	private VectorIdViewFilter filter;

	@BeforeEach
	public void before() {
		filter = new VectorIdViewFilter(List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)));
	}

	@Test
	public void testGetConditions() {
		assertEquals("(V1.VEC_REP, V1.VEC_SEQ) IN (:vectorIds0)", filter.getConditionSql(0));
		assertEquals("(V1.VEC_REP, V1.VEC_SEQ) IN (:vectorIds2)", filter.getConditionSql(2));
	}

	@Test
	public void testGetParameterKey() {
		assertEquals("vectorIds1", filter.getParameterKey(1));
		assertEquals("vectorIds2", filter.getParameterKey(2));
	}

	@Test
	public void testGetParameterValue() {
		List<Object[]> result = (List<Object[]>) filter.getParameterValue();
		List<List<Object>> resultAsLists = result.stream().map(Arrays::asList).collect(Collectors.toList());

		List<List<Object>> expected = List.of(List.of(1L, 2L), List.of(3L, 4L));

		assertEquals(expected, resultAsLists);
	}
}

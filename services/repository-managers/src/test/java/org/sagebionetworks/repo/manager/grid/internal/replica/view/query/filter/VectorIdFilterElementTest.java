package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.query.RowIdFilter;

public class VectorIdFilterElementTest {

	private List<LogicalTimestamp> timestamps;

	private StringBuilder sqlBuilder;
	private Map<String, Object> params;
	private GridHeader header;
	private Context context;

	@BeforeEach
	public void before() {
		timestamps = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		sqlBuilder = new StringBuilder();
		params = new HashMap<>();
		header = new GridHeader().setOrderedColumns(List.of(
				//
				new Column().setName("one").setVectorIndex(1),
				//
				new Column().setName("zero").setVectorIndex(0),
				//
				new Column().setName("two").setVectorIndex(2)));

		context = new Context(header);
	}

	@Test
	public void testToSql() {
		VectorIdFilterElement filter = new VectorIdFilterElement(timestamps);
		// call under test
		filter.toSql(sqlBuilder, params, context);

		assertEquals(" (VEC_REP, VEC_SEQ) IN (:vectorIds0)", sqlBuilder.toString());
		List<Object[]> actualTuples = (List<Object[]>) params.get("vectorIds0");
		assertNotNull(actualTuples);
		assertEquals(timestamps.size(), actualTuples.size());
		for (int i = 0; i < timestamps.size(); i++) {
			Object[] expected = new Object[] { timestamps.get(i).getReplicaId(),
					timestamps.get(i).getSequenceNumber() };
			assertArrayEquals(expected, actualTuples.get(i));
		}

	}

	@Test
	public void testToSqlRowIds() {
		RowIdFilter rowFilter = new RowIdFilter()
				.setRowIdsIn(timestamps.stream().map(ts -> ts.toCompact()).collect(Collectors.toList()));
		VectorIdFilterElement filter = new VectorIdFilterElement(rowFilter);
		// call under test
		filter.toSql(sqlBuilder, params, context);

		assertEquals(" (VEC_REP, VEC_SEQ) IN (:vectorIds0)", sqlBuilder.toString());
		List<Object[]> actualTuples = (List<Object[]>) params.get("vectorIds0");
		assertNotNull(actualTuples);
		assertEquals(timestamps.size(), actualTuples.size());
		for (int i = 0; i < timestamps.size(); i++) {
			Object[] expected = new Object[] { timestamps.get(i).getReplicaId(),
					timestamps.get(i).getSequenceNumber() };
			assertArrayEquals(expected, actualTuples.get(i));
		}

	}

}

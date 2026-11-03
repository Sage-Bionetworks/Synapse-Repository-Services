package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class AggregateQidQueryValidatorTest {

	private static final List<ColumnModel> SCHEMA = List.of(
			new ColumnModel().setId("11").setName("participantId").setColumnType(ColumnType.STRING),
			new ColumnModel().setId("12").setName("age").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setId("13").setName("site").setColumnType(ColumnType.STRING),
			new ColumnModel().setId("14").setName("diagnosis").setColumnType(ColumnType.STRING));

	private static final Set<String> QIDS = Set.of("11", "12");

	/**
	 * The validator only needs the queried object's schema, which every table reference in these
	 * queries resolves to.
	 */
	private static final SchemaProvider SCHEMA_PROVIDER = new SchemaProvider() {

		@Override
		public TableType getTableType(IdAndVersion tableId) {
			return TableType.table;
		}

		@Override
		public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
			return SCHEMA;
		}

		@Override
		public ColumnModel getColumnModel(String id) {
			return SCHEMA.stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
		}
	};

	private static QuerySpecification parse(String sql) throws ParseException {
		// Require the whole statement to parse, as a user query must, so no trailing clause is silently dropped.
		return TableQueryParser.parserQuery(sql);
	}

	@Test
	public void testValidateWithCountOfQid() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithCountDistinctOfQid() throws ParseException {
		QuerySpecification model = parse("select site, count(distinct participantId) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithMultipleQidCounts() throws ParseException {
		QuerySpecification model = parse(
				"select site, count(participantId), count(distinct age) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1, 2), protectedIndexes);
	}

	@Test
	public void testValidateWithQidReferencedByAlias() throws ParseException {
		// The restriction follows the column, not the text used to reference it.
		QuerySpecification model = parse("select P.site, count(P.participantId) from syn123 P group by P.site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithQidInWhereOnly() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 where age > 40 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		// A QID used only to filter is permitted; the count is still protected.
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithNoCount() throws ParseException {
		QuerySpecification model = parse("select site, diagnosis from syn123");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertTrue(protectedIndexes.isEmpty());
	}

	@Test
	public void testValidateWithNoQidColumns() throws ParseException {
		// No output column carries a quasi-identifier, so a projection of every column is permitted.
		QuerySpecification model = parse("select * from syn123");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		assertTrue(protectedIndexes.isEmpty());
	}

	@Test
	public void testValidateWithCountStar() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithCountOfNonQid() throws ParseException {
		QuerySpecification model = parse("select site, count(diagnosis) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithCountDistinctOfNonQid() throws ParseException {
		QuerySpecification model = parse("select site, count(distinct diagnosis) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithCountStarAndNoQidColumns() throws ParseException {
		// The queried object projects no QID-derived column, but each of its rows still comes from the
		// aggregate-only source, so the count is protected.
		QuerySpecification model = parse("select site, count(*) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithMixedCounts() throws ParseException {
		QuerySpecification model = parse(
				"select count(*), site, count(participantId), count(distinct diagnosis) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(0, 2, 3), protectedIndexes);
	}

	@Test
	public void testValidateWithCountOfExpressionArgument() throws ParseException {
		// An expression inside the count's argument still yields a plain count.
		QuerySpecification model = parse("select site, count(distinct concat(site, diagnosis)) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithCountStarInExpression() throws ParseException {
		QuerySpecification model = parse("select site, count(*) + 1 from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_EXPRESSION, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountStarInFunction() throws ParseException {
		QuerySpecification model = parse("select site, round(count(*) / 2) from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_EXPRESSION, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountStarInExpressionAndNoQidColumns() throws ParseException {
		QuerySpecification model = parse("select site, count(*) * 2 from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_EXPRESSION, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountStarInOrderBy() throws ParseException {
		// Ordering by any count sorts rows by their true below-threshold counts even though each cell is
		// masked.
		QuerySpecification model = parse("select site, count(*) from syn123 group by site order by count(*)");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountAliasInOrderBy() throws ParseException {
		// MySQL resolves an ORDER BY name to a select-list alias before a table column.
		QuerySpecification model = parse("select site, count(*) as n from syn123 group by site order by n");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountAliasInOrderByWithDifferentCase() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) as N from syn123 group by site order by n desc");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQuotedCountAliasInOrderBy() throws ParseException {
		// A request-supplied sort item becomes a double-quoted sort key.
		QuerySpecification model = parse("select site, count(*) as n from syn123 group by site order by \"n\"");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountPositionInOrderBy() throws ParseException {
		// MySQL reads an unquoted integer sort key as a one-based select-list position.
		QuerySpecification model = parse("select site, count(*) from syn123 group by site order by 2");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithNonCountPositionInOrderBy() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 group by site order by 1");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithNonCountAliasInOrderBy() throws ParseException {
		QuerySpecification model = parse("select site as s, count(*) as n from syn123 group by site order by s");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithQualifiedColumnNamedLikeCountAliasInOrderBy() throws ParseException {
		// A qualified reference names a table column, never a select-list alias.
		QuerySpecification model = parse(
				"select P.site, count(*) as diagnosis from syn123 P group by P.site order by P.diagnosis");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithQidInDefiningWhere() throws ParseException {
		// A defining condition only filters the rows, like a WHERE clause.
		QuerySpecification model = parse("select site, count(*) from syn123 defining_where age > 40 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(1), protectedIndexes);
	}

	@ParameterizedTest
	@ValueSource(strings = { "sum(1)", "sum(7)", "sum(case when site = 'A' then 1 else 0 end)", "sum(diagnosis)",
			"group_concat(diagnosis)", "json_arrayagg(diagnosis)", "bit_xor(1)", "stddev_samp(1)", "var_samp(1)" })
	public void testValidateWithCardinalityDependentAggregate(String aggregate) throws ParseException {
		// Each of these grows with, or otherwise reveals, the number of rows in a group: an unsuppressed count.
		QuerySpecification model = parse("select site, " + aggregate + " from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCardinalityDependentAggregateAndNoQidColumns() throws ParseException {
		QuerySpecification model = parse("select site, sum(1) from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCardinalityDependentAggregateInOrderBy() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 group by site order by sum(1)");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCardinalityIndependentAggregates() throws ParseException {
		// MIN, MAX, and AVG do not depend on the number of rows in a group, so they are permitted on a
		// non-QID column, including in ORDER BY.
		QuerySpecification model = parse("select site, min(diagnosis), max(diagnosis), avg(diagnosis), count(*)"
				+ " from syn123 group by site order by max(diagnosis)");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(4), protectedIndexes);
	}

	@Test
	public void testValidateWithQidProjectedAndCountInExpression() throws ParseException {
		// A QID violation takes precedence over a count violation.
		QuerySpecification model = parse("select age, count(*) + 1 from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateWithSelectStar() throws ParseException {
		QuerySpecification model = parse("select * from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateWithBareQidProjected() throws ParseException {
		QuerySpecification model = parse("select participantId from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidProjectedUnderAnAlias() throws ParseException {
		// Renaming the output column does not change which column is read.
		QuerySpecification model = parse("select participantId as pid from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInNonCountAggregate() throws ParseException {
		QuerySpecification model = parse("select site, max(age) from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInGroupBy() throws ParseException {
		// The QID appears only in GROUP BY (not projected), so the group-by use is what is caught.
		QuerySpecification model = parse("select count(*) from syn123 group by age");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_GROUP_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInOrderBy() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) from syn123 group by site order by age");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithCountOfQidInOrderBy() throws ParseException {
		// Ordering by the count of a QID would sort rows by their true below-threshold counts even
		// though each cell is masked, so it is rejected rather than treated as a protected count.
		QuerySpecification model = parse(
				"select site, count(participantId) from syn123 group by site order by count(participantId)");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInSelectDistinct() throws ParseException {
		QuerySpecification model = parse("select distinct site, participantId from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_SELECT_DISTINCT, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidCountInExpression() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) + 1 from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		// A count wrapped in arithmetic is no longer the participant count the threshold protects.
		assertEquals(RowSuppressionReasonCode.QID_IN_EXPRESSION, ex.getReasonCode());
	}
}

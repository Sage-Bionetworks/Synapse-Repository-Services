package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
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
			new ColumnModel().setId("14").setName("cohort").setColumnType(ColumnType.STRING));

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
		return new TableQueryParser(sql).querySpecification();
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
		// A QID used only to filter never appears in the results, so nothing is protected.
		assertTrue(protectedIndexes.isEmpty());
	}

	@Test
	public void testValidateWithNoQidUse() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertTrue(protectedIndexes.isEmpty());
	}

	@Test
	public void testValidateWithNoQidColumns() throws ParseException {
		// No output column carries a quasi-identifier, so no use of any column is restricted.
		QuerySpecification model = parse("select * from syn123");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		assertTrue(protectedIndexes.isEmpty());
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

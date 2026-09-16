package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class AggregateQidQueryValidatorTest {

	private static final List<String> QIDS = List.of("participantId", "age");

	private static final List<ColumnModel> SCHEMA = List.of(
			new ColumnModel().setName("participantId").setColumnType(ColumnType.STRING),
			new ColumnModel().setName("age").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("site").setColumnType(ColumnType.STRING),
			new ColumnModel().setName("cohort").setColumnType(ColumnType.STRING));

	private static QuerySpecification parse(String sql) throws ParseException {
		return new TableQueryParser(sql).querySpecification();
	}

	@Test
	public void testValidateWithCountOfQid() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithCountDistinctOfQid() throws ParseException {
		QuerySpecification model = parse("select site, count(distinct participantId) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		assertEquals(List.of(1), protectedIndexes);
	}

	@Test
	public void testValidateWithMultipleQidCounts() throws ParseException {
		QuerySpecification model = parse(
				"select site, count(participantId), count(distinct age) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		assertEquals(List.of(1, 2), protectedIndexes);
	}

	@Test
	public void testValidateWithQidInWhereOnly() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 where age > 40 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		// A QID used only to filter never appears in the results, so nothing is protected.
		assertTrue(protectedIndexes.isEmpty());
	}

	@Test
	public void testValidateWithNoQidUse() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 group by site");
		// call under test
		List<Integer> protectedIndexes = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		assertTrue(protectedIndexes.isEmpty());
	}

	@Test
	public void testValidateWithSelectStar() throws ParseException {
		QuerySpecification model = parse("select * from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateWithBareQidProjected() throws ParseException {
		QuerySpecification model = parse("select participantId from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInNonCountAggregate() throws ParseException {
		QuerySpecification model = parse("select site, max(age) from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_NON_COUNT_AGGREGATE, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInGroupBy() throws ParseException {
		// The QID appears only in GROUP BY (not projected), so the group-by use is what is caught.
		QuerySpecification model = parse("select count(*) from syn123 group by age");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_GROUP_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInOrderBy() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) from syn123 group by site order by age");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
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
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidInSelectDistinct() throws ParseException {
		QuerySpecification model = parse("select distinct site, participantId from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_SELECT_DISTINCT, ex.getReasonCode());
	}

	@Test
	public void testValidateWithQidCountInExpression() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) + 1 from syn123 group by site");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA);
		});
		// A count wrapped in arithmetic is no longer the participant count the threshold protects.
		assertEquals(RowSuppressionReasonCode.QID_IN_EXPRESSION, ex.getReasonCode());
	}

	@Test
	public void testValidateWithUnknownQidConfigured() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) from syn123 group by site");
		// A configured QID that matches no source column is an ACT-side misconfiguration, surfaced
		// as an illegal state (HTTP 500) rather than a bad request the caller could fix.
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, List.of("participantId", "notAColumn"), SCHEMA);
		}).getMessage();
		assertTrue(message.contains("NOTACOLUMN"), message);
	}
}

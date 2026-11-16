package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;

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
	private static final SchemaProvider SCHEMA_PROVIDER = schemaProvider(Map.of(), SCHEMA);

	/**
	 * A participant x file source syn1 and the VirtualTable syn2 over it, whose defining SQL counts the
	 * participants of each file. Its individualCount column is bound by name to the count.
	 */
	private static final ColumnModel INDIVIDUAL_ID = new ColumnModel().setId("10").setName("individualId")
			.setColumnType(ColumnType.INTEGER);
	private static final ColumnModel FILE_ID = new ColumnModel().setId("21").setName("fileId")
			.setColumnType(ColumnType.INTEGER);
	private static final ColumnModel SCORE = new ColumnModel().setId("23").setName("score")
			.setColumnType(ColumnType.DOUBLE);
	private static final ColumnModel MAX_SCORE = new ColumnModel().setId("24").setName("maxScore")
			.setColumnType(ColumnType.DOUBLE);
	private static final ColumnModel INDIVIDUAL_COUNT = new ColumnModel().setId("22").setName("individualCount")
			.setColumnType(ColumnType.INTEGER);
	private static final ColumnModel DATA_TYPE = new ColumnModel().setId("25").setName("dataType")
			.setColumnType(ColumnType.STRING);
	private static final ColumnModel DATA_TYPES = new ColumnModel().setId("26").setName("dataTypes")
			.setColumnType(ColumnType.STRING_LIST);
	private static final SchemaProvider VIRTUAL_TABLE_PROVIDER = schemaProvider(
			Map.of(IdAndVersion.parse("syn1"), List.of(INDIVIDUAL_ID, FILE_ID, SCORE, DATA_TYPE), IdAndVersion.parse("syn2"),
					List.of(FILE_ID, MAX_SCORE, INDIVIDUAL_COUNT, DATA_TYPES)),
			null);
	private static final String FILE_PERSPECTIVE = "with syn2 as (select fileId, cast(score as 24),"
			+ " count(distinct individualId) as individualCount from syn1 group by fileId) ";
	/**
	 * individualId is a quasi-identifier of the source syn1.
	 */
	private static final Set<String> SOURCE_QIDS = Set.of("10");
	/**
	 * individualCount is an aggregate of the quasi-identifier individualId.
	 */
	private static final QidColumns VIRTUAL_TABLE_QIDS = new QidColumns(Set.of("22"), Set.of("22"));

	private static SchemaProvider schemaProvider(Map<IdAndVersion, List<ColumnModel>> schemas,
			List<ColumnModel> defaultSchema) {
		return new SchemaProvider() {

			@Override
			public TableType getTableType(IdAndVersion tableId) {
				return TableType.table;
			}

			@Override
			public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
				return schemas.getOrDefault(tableId, defaultSchema);
			}

			@Override
			public ColumnModel getColumnModel(String id) {
				return java.util.stream.Stream.concat(SCHEMA.stream(), schemas.values().stream().flatMap(List::stream))
						.filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
			}
		};
	}

	private static QuerySpecification parse(String sql) throws ParseException {
		// Require the whole statement to parse, as a user query must, so no trailing clause is silently dropped.
		return TableQueryParser.parserQuery(sql);
	}

	private static QueryExpression parseExpression(String sql) throws ParseException {
		return new TableQueryParser(sql).queryExpression();
	}

	/**
	 * @return the select columns of the model at the given zero-based positions.
	 */
	private static List<DerivedColumn> selectColumns(QuerySpecification model, Integer... positions) {
		List<DerivedColumn> columns = model.getSelectList().getColumns();
		return Arrays.stream(positions).map(columns::get).collect(Collectors.toList());
	}

	@Test
	public void testValidateWithCountOfQid() throws ParseException {
		QuerySpecification model = parse("select site, count(participantId) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithCountDistinctOfQid() throws ParseException {
		QuerySpecification model = parse("select site, count(distinct participantId) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithMultipleQidCounts() throws ParseException {
		QuerySpecification model = parse(
				"select site, count(participantId), count(distinct age) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1, 2), protectedCounts);
	}

	@Test
	public void testValidateWithQidReferencedByAlias() throws ParseException {
		// The restriction follows the column, not the text used to reference it.
		QuerySpecification model = parse("select P.site, count(P.participantId) from syn123 P group by P.site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithQidInWhereOnly() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 where age > 40 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		// A QID used only to filter is permitted; the count is still protected.
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithNoCount() throws ParseException {
		QuerySpecification model = parse("select site, diagnosis from syn123");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(List.of(), protectedCounts);
	}

	@Test
	public void testValidateWithNoQidColumns() throws ParseException {
		// No output column carries a quasi-identifier, so a projection of every column is permitted.
		QuerySpecification model = parse("select * from syn123");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		assertEquals(List.of(), protectedCounts);
	}

	@Test
	public void testValidateWithCountStar() throws ParseException {
		QuerySpecification model = parse("select site, count(*) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithCountOfNonQid() throws ParseException {
		QuerySpecification model = parse("select site, count(diagnosis) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithCountDistinctOfNonQid() throws ParseException {
		QuerySpecification model = parse("select site, count(distinct diagnosis) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithCountStarAndNoQidColumns() throws ParseException {
		// The queried object projects no QID-derived column, but each of its rows still comes from the
		// aggregate-only source, so the count is protected.
		QuerySpecification model = parse("select site, count(*) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, Set.of(), SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithMixedCounts() throws ParseException {
		QuerySpecification model = parse(
				"select count(*), site, count(participantId), count(distinct diagnosis) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 0, 2, 3), protectedCounts);
	}

	@Test
	public void testValidateWithCountOfExpressionArgument() throws ParseException {
		// An expression inside the count's argument still yields a plain count.
		QuerySpecification model = parse("select site, count(distinct concat(site, diagnosis)) from syn123 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
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
		// The position MySQL reads is in the translated select list, which translation can widen, so even a
		// position that names a non-count column in the parsed query is rejected.
		QuerySpecification model = parse("select site, count(*) from syn123 group by site order by 1");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateWithNonCountAliasInOrderBy() throws ParseException {
		QuerySpecification model = parse("select site as s, count(*) as n from syn123 group by site order by s");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithQualifiedColumnNamedLikeCountAliasInOrderBy() throws ParseException {
		// A qualified reference names a table column, never a select-list alias.
		QuerySpecification model = parse(
				"select P.site, count(*) as diagnosis from syn123 P group by P.site order by P.diagnosis");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
	}

	@Test
	public void testValidateWithQidInDefiningWhere() throws ParseException {
		// A defining condition only filters the rows, like a WHERE clause.
		QuerySpecification model = parse("select site, count(*) from syn123 defining_where age > 40 group by site");
		// call under test
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 1), protectedCounts);
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
		List<DerivedColumn> protectedCounts = AggregateQidQueryValidator.validate(model, QIDS, SCHEMA_PROVIDER);
		assertEquals(selectColumns(model, 4), protectedCounts);
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

	@Test
	public void testValidateExpressionWithoutInlinedQuery() throws ParseException {
		QueryExpression model = parseExpression("select site, count(participantId) from syn123 group by site");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, new QidColumns(QIDS, Set.of()), QidColumns.NONE,
				SCHEMA_PROVIDER);
		assertEquals(Set.of(), definingCounts);
	}

	@Test
	public void testValidateExpressionWithoutInlinedQueryAndQidProjected() throws ParseException {
		QueryExpression model = parseExpression("select participantId from syn123");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, new QidColumns(QIDS, Set.of()), QidColumns.NONE, SCHEMA_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testValidateExpressionWithDefiningCountReadBySelect() throws ParseException {
		// The cast to DOUBLE before the count does not change which column the count produces.
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId, individualCount from syn2");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE,
				VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of("22"), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningCountReadByOrderBy() throws ParseException {
		// Sorting by the count's column sorts by the suppressed value, so it is allowed but still a read.
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId from syn2 order by individualCount");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE,
				VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of("22"), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningCountReadByGroupBy() throws ParseException {
		QueryExpression model = parseExpression(
				FILE_PERSPECTIVE + "select individualCount, count(*) from syn2 group by individualCount");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE,
				VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of("22"), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningCountReadBySelectStar() throws ParseException {
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select * from syn2");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE,
				VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of("22"), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningCountNotRead() throws ParseException {
		// Excluding the rows of a count the query never reads would drop rows the caller may see.
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId, maxScore from syn2");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE,
				VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of(), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningCountInWhere() throws ParseException {
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId from syn2 where individualCount = 1");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE, VIRTUAL_TABLE_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testValidateExpressionWithDefiningCountInDefiningOrderBy() throws ParseException {
		QueryExpression model = parseExpression("with syn2 as (select fileId, cast(score as 24),"
				+ " count(distinct individualId) as individualCount from syn1 group by fileId"
				+ " order by count(distinct individualId)) select fileId from syn2");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE, VIRTUAL_TABLE_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateExpressionWithPositionalSortInDefiningSql() throws ParseException {
		QueryExpression model = parseExpression("with syn2 as (select fileId, cast(score as 24),"
				+ " count(distinct individualId) as individualCount from syn1 group by fileId order by 3)"
				+ " select fileId from syn2");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE, VIRTUAL_TABLE_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateExpressionWithBoundSchemaOutOfSync() throws ParseException {
		// The bound schema names the count 'n', but the defining SQL produces 'individualCount'.
		SchemaProvider provider = schemaProvider(Map.of(IdAndVersion.parse("syn1"), List.of(INDIVIDUAL_ID, FILE_ID, SCORE),
				IdAndVersion.parse("syn2"), List.of(FILE_ID, MAX_SCORE,
						new ColumnModel().setId("22").setName("n").setColumnType(ColumnType.INTEGER))),
				null);
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId from syn2");
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, QidColumns.NONE, provider);
		}).getMessage();
		assertEquals("The schema of syn2 has no column named 'individualCount' produced by its defining SQL", message);
	}

	/**
	 * Anyone who can read the source can write a VirtualTable over it, so its defining SQL may compute only a
	 * whole count or a distinct-value list. Each of these would carry a count, or a quasi-identifier's values,
	 * past the suppression.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "sum(1)", "sum(case when fileId = 1 then 1 else 0 end)", "group_concat(dataType)",
			"bit_xor(1)", "max(score)", "avg(score)", "cast(count(fileId) as 22)", "count(fileId) + 0",
			"group_concat(distinct individualId)", "upper(group_concat(distinct dataType))" })
	public void testValidateExpressionWithAggregateNotPermittedInDefiningSql(String aggregate) throws ParseException {
		QueryExpression model = parseExpression("with syn2 as (select fileId, cast(score as 24), " + aggregate
				+ " as individualCount from syn1 group by fileId) select fileId, individualCount from syn2");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QidColumns.NONE, new QidColumns(SOURCE_QIDS, Set.of()),
					VIRTUAL_TABLE_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.AGGREGATE_NOT_PERMITTED, ex.getReasonCode());
	}

	/**
	 * PLFM-10135: the production file perspective lists each file's distinct values next to its participant
	 * count.
	 */
	@Test
	public void testValidateExpressionWithDistinctValueListsInDefiningSql() throws ParseException {
		QueryExpression model = parseExpression("with syn2 as (select fileId, cast(score as 24),"
				+ " cast(concat('[', group_concat(distinct concat('\"', dataType, '\"') order by dataType asc), ']') as 26) as dataTypes,"
				+ " count(distinct individualId) as individualCount from syn1"
				+ " where dataType is null or dataType <> 'Computational Tool' group by fileId) select * from syn2");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS,
				new QidColumns(SOURCE_QIDS, Set.of()), VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of("22"), definingCounts);
	}

	/**
	 * @return the select column of a single-column query over the source syn1.
	 */
	private static DerivedColumn sourceColumn(String expression) throws ParseException {
		return parse("select " + expression + " from syn1 group by fileId").getSelectList().getColumns().get(0);
	}

	private static TableAndColumnMapper sourceMapper(DerivedColumn column) {
		return new TableAndColumnMapper(column.getContext(QuerySpecification.class).orElseThrow(), VIRTUAL_TABLE_PROVIDER);
	}

	@Test
	public void testValidateDefiningAggregatesWithPermittedShapes() throws ParseException {
		QuerySpecification inlined = parse("select fileId, count(distinct individualId) as individualCount,"
				+ " count(fileId) as inFiles,"
				+ " cast(concat('[', group_concat(distinct concat('\"', dataType, '\"') order by dataType asc), ']') as 26) as a,"
				+ " cast(concat('[', group_concat(distinct replace(replace(dataType, '[', ''), ']', '') separator ','), ']') as 26) as b,"
				+ " cast(group_concat(distinct dataType order by dataType asc separator ',') as 26) as c"
				+ " from syn1 group by fileId order by fileId");
		// call under test
		AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
				new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
	}

	@Test
	public void testValidateDefiningAggregatesWithNoAggregates() throws ParseException {
		QuerySpecification inlined = parse("select fileId, cast(score as 24) from syn1");
		// call under test
		AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
				new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
	}

	@Test
	public void testValidateDefiningAggregatesWithAggregateOutsideSelectList() throws ParseException {
		QuerySpecification inlined = parse("select fileId from syn1 group by fileId order by max(score)");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
					new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
		});
		assertEquals(RowSuppressionReasonCode.AGGREGATE_NOT_PERMITTED, ex.getReasonCode());
	}

	@Test
	public void testValidateDefiningAggregatesWithTwoAggregatesInOneColumn() throws ParseException {
		QuerySpecification inlined = parse("select fileId, count(fileId) + count(score) as n from syn1 group by fileId");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
					new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
		});
		assertEquals(RowSuppressionReasonCode.AGGREGATE_NOT_PERMITTED, ex.getReasonCode());
	}

	@Test
	public void testValidateDefiningAggregatesWithCountInOrderBy() throws ParseException {
		QuerySpecification inlined = parse(
				"select fileId, count(fileId) as n from syn1 group by fileId order by count(fileId)");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
					new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateDefiningAggregatesWithCountAliasInOrderBy() throws ParseException {
		QuerySpecification inlined = parse("select fileId, count(fileId) as n from syn1 group by fileId order by n");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
					new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testValidateDefiningAggregatesWithPositionalSort() throws ParseException {
		QuerySpecification inlined = parse("select fileId, count(fileId) as n from syn1 group by fileId order by 2");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validateDefiningAggregates(inlined, SOURCE_QIDS,
					new TableAndColumnMapper(inlined, VIRTUAL_TABLE_PROVIDER));
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@ParameterizedTest
	@ValueSource(strings = { "group_concat(distinct dataType)",
			"group_concat(distinct dataType order by dataType asc separator ',')",
			"cast(group_concat(distinct dataType order by dataType asc separator ',') as 26)",
			"cast(concat('[', group_concat(distinct concat('\"', dataType, '\"') order by dataType asc), ']') as 26)",
			"cast(concat('[', group_concat(distinct replace(replace(dataType, '[', ''), ']', '') separator ','), ']') as 26)" })
	public void testIsDistinctValueListWithPermittedShape(String expression) throws ParseException {
		DerivedColumn column = sourceColumn(expression);
		// call under test
		boolean result = AggregateQidQueryValidator.isDistinctValueList(column,
				column.getFirstElementOfType(SetFunctionSpecification.class), SOURCE_QIDS, sourceMapper(column));
		assertTrue(result);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			// the multiplicity of each value reveals the row count
			"group_concat(dataType)",
			// the values of a quasi-identifier
			"group_concat(distinct individualId)",
			// not a list
			"max(dataType)", "count(dataType)",
			// more than one column
			"group_concat(distinct concat(dataType, score))", "group_concat(distinct dataType, fileId)",
			"group_concat(distinct dataType order by fileId)",
			// a function other than CONCAT or REPLACE
			"group_concat(distinct upper(dataType))", "upper(group_concat(distinct dataType))",
			// formatting other than a CONCAT with literals and a CAST to a column model
			"cast(group_concat(distinct dataType) as STRING)", "concat(group_concat(distinct dataType), dataType)",
			"concat(group_concat(distinct dataType), 1)",
			// no column at all
			"group_concat(distinct concat('a', 'b'))" })
	public void testIsDistinctValueListWithShapeNotPermitted(String expression) throws ParseException {
		DerivedColumn column = sourceColumn(expression);
		// call under test
		boolean result = AggregateQidQueryValidator.isDistinctValueList(column,
				column.getFirstElementOfType(SetFunctionSpecification.class), SOURCE_QIDS, sourceMapper(column));
		assertFalse(result);
	}

	@ParameterizedTest
	@ValueSource(strings = { "group_concat(distinct dataType)", "cast(group_concat(distinct dataType) as 26)",
			"concat('[', group_concat(distinct dataType), ']')",
			"cast(concat('[', group_concat(distinct dataType), ']') as 26)" })
	public void testIsListFormattingWithPermittedFormatting(String expression) throws ParseException {
		DerivedColumn column = sourceColumn(expression);
		// call under test
		boolean result = AggregateQidQueryValidator.isListFormatting(column.getValueExpression(),
				column.getFirstElementOfType(SetFunctionSpecification.class));
		assertTrue(result);
	}

	@ParameterizedTest
	@ValueSource(strings = { "cast(group_concat(distinct dataType) as STRING)",
			"concat(group_concat(distinct dataType), dataType)", "concat(group_concat(distinct dataType), 1)",
			"replace(group_concat(distinct dataType), 'a', 'b')", "upper(group_concat(distinct dataType))",
			"(group_concat(distinct dataType))" })
	public void testIsListFormattingWithFormattingNotPermitted(String expression) throws ParseException {
		DerivedColumn column = sourceColumn(expression);
		// call under test
		boolean result = AggregateQidQueryValidator.isListFormatting(column.getValueExpression(),
				column.getFirstElementOfType(SetFunctionSpecification.class));
		assertFalse(result);
	}

	@ParameterizedTest
	@ValueSource(strings = { "dataType", "concat('\"', dataType, '\"')", "replace(replace(dataType, '[', ''), ']', '')",
			"concat(dataType, score)" })
	public void testIsStringExpressionOfColumnWithStringExpression(String expression) throws ParseException {
		// call under test
		boolean result = AggregateQidQueryValidator.isStringExpressionOfColumn(sourceColumn(expression).getValueExpression());
		assertTrue(result);
	}

	@ParameterizedTest
	@ValueSource(strings = { "'a'", "concat('a', 'b')", "upper(dataType)", "concat(dataType, 1)", "score + 1",
			"cast(dataType as STRING)" })
	public void testIsStringExpressionOfColumnWithOtherExpression(String expression) throws ParseException {
		// call under test
		boolean result = AggregateQidQueryValidator.isStringExpressionOfColumn(sourceColumn(expression).getValueExpression());
		assertFalse(result);
	}

	@Test
	public void testIsStringLiteralWithStringLiteral() throws ParseException {
		// call under test
		assertTrue(AggregateQidQueryValidator.isStringLiteral(sourceColumn("'['").getValueExpression()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "1", "dataType", "\"dataType\"", "concat('a', 'b')" })
	public void testIsStringLiteralWithOtherValue(String expression) throws ParseException {
		// call under test
		assertFalse(AggregateQidQueryValidator.isStringLiteral(sourceColumn(expression).getValueExpression()));
	}

	@Test
	public void testUnwrapWithColumnReference() throws ParseException {
		DerivedColumn column = sourceColumn("dataType");
		// call under test
		Element result = AggregateQidQueryValidator.unwrap(column.getValueExpression());
		assertEquals(column.getFirstElementOfType(ColumnReference.class), result);
	}

	@Test
	public void testUnwrapWithStringLiteral() throws ParseException {
		DerivedColumn column = sourceColumn("'['");
		// call under test
		Element result = AggregateQidQueryValidator.unwrap(column.getValueExpression());
		assertEquals(column.getFirstElementOfType(CharacterStringLiteral.class), result);
	}

	@Test
	public void testUnwrapWithFunction() throws ParseException {
		DerivedColumn column = sourceColumn("concat(dataType, 'a')");
		// call under test
		Element result = AggregateQidQueryValidator.unwrap(column.getValueExpression());
		assertEquals(column.getFirstElementOfType(MySqlFunction.class), result);
	}

	@Test
	public void testUnwrapWithArithmetic() throws ParseException {
		DerivedColumn column = sourceColumn("score + 1");
		// call under test
		Element result = AggregateQidQueryValidator.unwrap(column.getValueExpression());
		// The operator changes the SQL, so no single child is a pass-through.
		assertFalse(result instanceof ColumnReference);
		assertEquals("score+1", result.toSql());
	}

	@Test
	public void testValidateExpressionWithQidProjectedByDefiningSql() throws ParseException {
		// The defining SQL's projection is not returned; the outer query's use of each column is checked
		// against the VirtualTable's own quasi-identifier columns.
		QueryExpression model = parseExpression(
				"with syn2 as (select fileId, individualId as individualCount from syn1) select fileId from syn2");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS,
				new QidColumns(Set.of("10"), Set.of()), VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of(), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningWhereOnSourceAggregate() throws ParseException {
		// A defining condition filters the source before the defining SQL aggregates it, so it is checked
		// against the source's own aggregate quasi-identifier columns; here the source stores score as one.
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId from syn2 defining_where score = 1");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS, new QidColumns(Set.of("23"), Set.of("23")),
					VIRTUAL_TABLE_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testValidateExpressionWithDefiningWhereOnSourceNonAggregateQid() throws ParseException {
		// A quasi-identifier that is not an aggregate may filter the rows, as in a WHERE clause.
		QueryExpression model = parseExpression(
				FILE_PERSPECTIVE + "select fileId from syn2 defining_where individualId = 1");
		// call under test
		Set<String> definingCounts = AggregateQidQueryValidator.validate(model, VIRTUAL_TABLE_QIDS,
				new QidColumns(Set.of("10"), Set.of()), VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of(), definingCounts);
	}

	@Test
	public void testValidateExpressionWithDefiningWhereOnDefiningCount() throws ParseException {
		// The defining condition would see the count before it is suppressed.
		QueryExpression model = parseExpression(
				FILE_PERSPECTIVE + "select fileId from syn2 defining_where individualCount = 1");
		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			AggregateQidQueryValidator.validate(model, QidColumns.NONE, QidColumns.NONE, VIRTUAL_TABLE_PROVIDER);
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testGetReadDefiningCountColumnIdsWithWhere() throws ParseException {
		// Without the QID restrictions, a filter on the count is a read like any other.
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId from syn2 where individualCount > 1");
		// call under test
		Set<String> read = AggregateQidQueryValidator.getReadDefiningCountColumnIds(model, VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of("22"), read);
	}

	@Test
	public void testGetReadDefiningCountColumnIdsWithNotRead() throws ParseException {
		QueryExpression model = parseExpression(FILE_PERSPECTIVE + "select fileId from syn2");
		// call under test
		Set<String> read = AggregateQidQueryValidator.getReadDefiningCountColumnIds(model, VIRTUAL_TABLE_PROVIDER);
		assertEquals(Set.of(), read);
	}

	@Test
	public void testGetReadDefiningCountColumnIdsWithoutInlinedQuery() throws ParseException {
		QueryExpression model = parseExpression("select site, count(*) from syn123 group by site");
		// call under test
		Set<String> read = AggregateQidQueryValidator.getReadDefiningCountColumnIds(model, SCHEMA_PROVIDER);
		assertEquals(Set.of(), read);
	}
}

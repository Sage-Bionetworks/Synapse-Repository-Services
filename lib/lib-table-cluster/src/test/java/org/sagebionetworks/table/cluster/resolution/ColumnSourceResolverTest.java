package org.sagebionetworks.table.cluster.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;

public class ColumnSourceResolverTest {

	private static final ColumnModel S1_ID = column("11", "id", ColumnType.INTEGER);
	private static final ColumnModel S1_FOO = column("12", "foo", ColumnType.STRING);
	private static final ColumnModel S1_SCORE = column("13", "score", ColumnType.DOUBLE);
	private static final ColumnModel S2_ID = column("21", "id", ColumnType.INTEGER);
	private static final ColumnModel S2_BAR = column("22", "bar", ColumnType.STRING);
	private static final ColumnModel VT_FOO = S1_FOO;
	private static final ColumnModel VT_SCORE = column("31", "s", ColumnType.DOUBLE);
	private static final ColumnModel VT_COUNT = column("32", "n", ColumnType.INTEGER);

	private static final IdAndVersion SYN1 = IdAndVersion.parse("syn1");
	private static final IdAndVersion SYN2 = IdAndVersion.parse("syn2");
	private static final IdAndVersion VT = IdAndVersion.parse("syn3");

	private static final Map<IdAndVersion, List<ColumnModel>> SCHEMAS = Map.of(SYN1, List.of(S1_ID, S1_FOO, S1_SCORE),
			SYN2, List.of(S2_ID, S2_BAR), VT, List.of(VT_FOO, VT_SCORE, VT_COUNT));

	private static final SchemaProvider SCHEMA_PROVIDER = new SchemaProvider() {

		@Override
		public TableType getTableType(IdAndVersion tableId) {
			return TableType.table;
		}

		@Override
		public List<ColumnModel> getTableSchema(IdAndVersion tableId) {
			return SCHEMAS.get(tableId);
		}

		@Override
		public ColumnModel getColumnModel(String id) {
			return SCHEMAS.values().stream().flatMap(List::stream).filter(c -> c.getId().equals(id)).findFirst()
					.orElseThrow();
		}
	};

	private static ColumnModel column(String id, String name, ColumnType type) {
		return new ColumnModel().setId(id).setName(name).setColumnType(type);
	}

	private static QueryExpression parse(String sql) throws ParseException {
		return new TableQueryParser(sql).queryExpression();
	}

	private static ColumnSource source(IdAndVersion table, ColumnModel column, ResolvedColumn definition) {
		return new ColumnSource(table, new SchemaColumnTranslationReference(column), definition);
	}

	private static List<DerivedColumn> outerColumns(ColumnSourceResolver resolver) {
		return resolver.getOuterQuery().getSelectList().getColumns();
	}

	/**
	 * The query that resolves the outer query of a VirtualTable inlined as a CTE.
	 */
	private static final String DEFINING_SQL = "select foo, cast(score as 31), count(distinct id) as n from syn1 group by foo, score";

	@Test
	public void testResolveWithJoinAliasesAndLiteral() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("select a.foo as f, b.bar as b, 100 as num from syn1 a join syn2 b on (a.id = b.id)"), SCHEMA_PROVIDER);
		List<DerivedColumn> columns = outerColumns(resolver);

		// call under test
		List<ResolvedColumn> resolved = columns.stream().map(resolver::resolve).toList();

		assertEquals(List.of(
				new ResolvedColumn(columns.get(0), "f", DerivationKind.IDENTITY, null, false,
						List.of(source(SYN1, S1_FOO, null))),
				new ResolvedColumn(columns.get(1), "b", DerivationKind.IDENTITY, null, false,
						List.of(source(SYN2, S2_BAR, null))),
				new ResolvedColumn(columns.get(2), "num", DerivationKind.LITERAL, null, false, List.of())), resolved);
		assertEquals(Optional.of("12"), resolved.get(0).getInputs().get(0).getColumnId());
		assertEquals(Optional.of("22"), resolved.get(1).getInputs().get(0).getColumnId());
	}

	@Test
	public void testResolveWithExpressionAndAggregate() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("select concat(foo, id) as c, count(distinct id) as n, foo from syn1 group by foo"), SCHEMA_PROVIDER);
		List<DerivedColumn> columns = outerColumns(resolver);

		// call under test
		List<ResolvedColumn> resolved = columns.stream().map(resolver::resolve).toList();

		assertEquals(List.of(
				new ResolvedColumn(columns.get(0), "c", DerivationKind.EXPRESSION, null, false,
						List.of(source(SYN1, S1_FOO, null), source(SYN1, S1_ID, null))),
				new ResolvedColumn(columns.get(1), "n", DerivationKind.AGGREGATE, SetFunctionType.COUNT, true,
						List.of(source(SYN1, S1_ID, null))),
				new ResolvedColumn(columns.get(2), "foo", DerivationKind.IDENTITY, null, false,
						List.of(source(SYN1, S1_FOO, null)))), resolved);
	}

	@Test
	public void testResolveWithCountInExpression() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(parse("select count(*) + 1 from syn1"), SCHEMA_PROVIDER);
		DerivedColumn column = outerColumns(resolver).get(0);

		// call under test
		ResolvedColumn resolved = resolver.resolve(column);

		assertEquals(new ResolvedColumn(column, "COUNT(*)+1", DerivationKind.AGGREGATE, SetFunctionType.COUNT, false,
				List.of()), resolved);
	}

	@Test
	public void testResolveWithRowMetadata() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(parse("select ROW_ID from syn1"), SCHEMA_PROVIDER);
		DerivedColumn column = outerColumns(resolver).get(0);

		// call under test
		ResolvedColumn resolved = resolver.resolve(column);

		// A row metadata column is not a column of the schema, so it is not an input.
		assertEquals(new ResolvedColumn(column, "ROW_ID", DerivationKind.IDENTITY, null, false, List.of()), resolved);
	}

	@Test
	public void testResolveColumnReferenceWithUnknownColumn() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(parse("select foo from syn1 where notAColumn > 1"),
				SCHEMA_PROVIDER);
		ColumnReference reference = resolver.getOuterQuery().getTableExpression().getWhereClause()
				.getFirstElementOfType(ColumnReference.class);

		// call under test
		assertEquals(Optional.empty(), resolver.resolve(reference));
	}

	@Test
	public void testResolveWithoutInlinedQuery() throws ParseException {
		// call under test
		ColumnSourceResolver resolver = new ColumnSourceResolver(parse("select foo from syn1"), SCHEMA_PROVIDER);

		assertEquals(Optional.empty(), resolver.getInlinedObjectId());
		assertEquals(Optional.empty(), resolver.getInlinedQuery());
		assertEquals(List.of(), resolver.getInlinedColumns());
		assertEquals(Optional.empty(), resolver.getInlinedColumn("foo"));
	}

	@Test
	public void testResolveWithInlinedCount() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("with syn3 as (" + DEFINING_SQL + ") select foo, n from syn3 where n > 1"), SCHEMA_PROVIDER);
		QuerySpecification inlined = resolver.getInlinedQuery().get();
		List<DerivedColumn> inlinedColumns = inlined.getSelectList().getColumns();
		ResolvedColumn countDefinition = new ResolvedColumn(inlinedColumns.get(2), "n", DerivationKind.AGGREGATE,
				SetFunctionType.COUNT, true, List.of(source(SYN1, S1_ID, null)));
		ColumnReference filterReference = resolver.getOuterQuery().getTableExpression().getWhereClause()
				.getFirstElementOfType(ColumnReference.class);

		// call under test
		Optional<ColumnSource> filterSource = resolver.resolve(filterReference);

		assertEquals(Optional.of(source(VT, VT_COUNT, countDefinition)), filterSource);
		assertSame(inlinedColumns.get(2), filterSource.get().getDefinition().get().getElement());
		assertEquals(List.of(source(SYN1, S1_ID, null)), resolver.getLeafSources(filterSource.get()));
		assertEquals(Optional.of(VT), resolver.getInlinedObjectId());
		assertEquals(Optional.of(countDefinition), resolver.getInlinedColumn("n"));
	}

	@Test
	public void testResolveWithInlinedCastToColumn() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("with syn3 as (" + DEFINING_SQL + ") select s from syn3"), SCHEMA_PROVIDER);
		DerivedColumn outer = outerColumns(resolver).get(0);
		DerivedColumn inlinedCast = resolver.getInlinedQuery().get().getSelectList().getColumns().get(1);
		ResolvedColumn castDefinition = new ResolvedColumn(inlinedCast, "s", DerivationKind.EXPRESSION, null, false,
				List.of(source(SYN1, S1_SCORE, null)));

		// call under test
		ResolvedColumn resolved = resolver.resolve(outer);

		// A cast to a column takes that column's name, which pairs it with the bound column of the same name.
		assertEquals(new ResolvedColumn(outer, "s", DerivationKind.IDENTITY, null, false,
				List.of(source(VT, VT_SCORE, castDefinition))), resolved);
		assertEquals(List.of(source(SYN1, S1_SCORE, null)), resolver.getLeafSources(resolved.getInputs().get(0)));
	}

	@Test
	public void testResolveWithInlinedSelectStar() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("with syn2 as (select * from syn2) select bar from syn2"), SCHEMA_PROVIDER);

		// call under test
		List<String> names = resolver.getInlinedColumns().stream().map(ResolvedColumn::getOutputName).toList();

		assertEquals(List.of("id", "bar"), names);
		assertEquals(List.of(source(SYN2, S2_BAR, null)),
				resolver.getInlinedColumn("bar").get().getInputs());
	}

	@Test
	public void testResolveWithInlinedColumnNotProduced() throws ParseException {
		// The bound schema has a column 's' that this defining SQL does not produce.
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("with syn3 as (select foo, count(distinct id) as n from syn1 group by foo) select s from syn3"),
				SCHEMA_PROVIDER);
		DerivedColumn outer = outerColumns(resolver).get(0);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			resolver.resolve(outer);
		}).getMessage();
		assertEquals("The defining SQL of syn3 does not produce the column 's'", message);
	}

	@Test
	public void testResolveWithInlinedDuplicateName() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(
				parse("with syn3 as (select foo, foo from syn1) select foo from syn3"), SCHEMA_PROVIDER);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			resolver.getInlinedColumn("foo");
		}).getMessage();
		assertEquals("The defining SQL of syn3 produces more than one column named 'foo'", message);
	}

	@Test
	public void testResolveWithColumnOfAnotherQuery() throws ParseException {
		ColumnSourceResolver resolver = new ColumnSourceResolver(parse("select foo from syn1"), SCHEMA_PROVIDER);
		DerivedColumn other = new TableQueryParser("select foo from syn1").querySpecification().getSelectList()
				.getColumns().get(0);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			resolver.resolve(other);
		}).getMessage();
		assertEquals("The query specification is not part of the resolved query", message);
	}

	@Test
	public void testResolveWithoutInlining() throws ParseException {
		QuerySpecification query = new TableQueryParser("select b.bar as b, count(a.id) from syn1 a join syn2 b on (a.id = b.id)")
				.querySpecification();
		TableAndColumnMapper mapper = new TableAndColumnMapper(query, SCHEMA_PROVIDER);
		List<DerivedColumn> columns = query.getSelectList().getColumns();

		// call under test
		List<ResolvedColumn> resolved = columns.stream()
				.map(column -> ColumnSourceResolver.resolveWithoutInlining(column, mapper)).toList();

		assertEquals(List.of(
				new ResolvedColumn(columns.get(0), "b", DerivationKind.IDENTITY, null, false,
						List.of(source(SYN2, S2_BAR, null))),
				new ResolvedColumn(columns.get(1), "COUNT(a.id)", DerivationKind.AGGREGATE, SetFunctionType.COUNT, true,
						List.of(source(SYN1, S1_ID, null)))), resolved);
	}

	@Test
	public void testResolveWithMultipleWithElements() throws ParseException {
		QueryExpression model = parse("with syn3 as (select foo from syn1), syn4 as (select foo from syn1) select foo from syn3");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ColumnSourceResolver(model, SCHEMA_PROVIDER);
		}).getMessage();
		assertEquals("A CTE must have one and only one inner query.", message);
	}

}

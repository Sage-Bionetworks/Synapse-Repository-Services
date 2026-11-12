package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.BooleanOperator;
import org.sagebionetworks.repo.model.table.CohortDefinition;
import org.sagebionetworks.repo.model.table.ColumnCohortFilter;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.FilterGroup;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;

public class CohortQueryValidatorTest {

	private static QueryExpression parse(String sql) throws ParseException {
		return new TableQueryParser(sql).queryExpression();
	}

	private static CohortDefinition cohort(String name, String sql) {
		return new CohortDefinition().setName(name).setQuery(new Query().setSql(sql));
	}

	@Test
	public void testCollectReferencesWithSqlAndNestedFilters() throws ParseException {
		List<QueryFilter> filters = List.of(
				new ColumnSingleValueQueryFilter().setColumnName("a").setOperator(ColumnSingleValueFilterOperator.EQUAL)
						.setValues(List.of("1")),
				new FilterGroup().setOperator(BooleanOperator.OR).setChildren(List.of(
						new ColumnCohortFilter().setColumnName("b").setCohortName("c3"),
						new FilterGroup().setOperator(BooleanOperator.AND).setChildren(List.of(
								new ColumnCohortFilter().setColumnName("b").setCohortName("c1"))))));
		// call under test
		Set<String> names = CohortQueryValidator.collectReferences(
				parse("select * from syn1 where x in cohort(c1) or y not in cohort(c2)"), filters);
		assertEquals(List.of("c1", "c2", "c3"), List.copyOf(names));
	}

	@Test
	public void testCollectReferencesWithNone() throws ParseException {
		// call under test
		Set<String> names = CohortQueryValidator.collectReferences(parse("select * from syn1 where x in (1, 2)"), null);
		assertEquals(Set.of(), names);
	}

	@Test
	public void testCollectCohortFiltersWithNull() {
		// call under test
		assertEquals(List.of(), CohortQueryValidator.collectCohortFilters(null));
	}

	@Test
	public void testValidateDefinitionsWithMatchingReferences() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1)")
				.setAdditionalFilters(List.of(new ColumnCohortFilter().setColumnName("y").setCohortName("c2")))
				.setCohorts(List.of(cohort("c1", "select a from syn2"), cohort("c2", "select b from syn3")));
		// call under test
		CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
	}

	@Test
	public void testValidateDefinitionsWithNoCohorts() throws ParseException {
		Query query = new Query().setSql("select * from syn1");
		// call under test
		CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
	}

	@Test
	public void testValidateDefinitionsWithTooManyCohorts() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1) and y in cohort(c2)")
				.setCohorts(List.of(cohort("c1", "select a from syn2"), cohort("c2", "select b from syn3")));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 1);
		}).getMessage();
		assertEquals("A query may define at most 1 cohorts", message);
	}

	@Test
	public void testValidateDefinitionsWithUnknownReference() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1) and y in cohort(other)")
				.setCohorts(List.of(cohort("c1", "select a from syn2")));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
		}).getMessage();
		assertEquals("Unknown cohort: other", message);
	}

	@Test
	public void testValidateDefinitionsWithReferenceAndNoDefinitions() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1)");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
		}).getMessage();
		assertEquals("Unknown cohort: c1", message);
	}

	@Test
	public void testValidateDefinitionsWithUnusedDefinition() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1)")
				.setCohorts(List.of(cohort("c1", "select a from syn2"), cohort("unused", "select b from syn3")));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
		}).getMessage();
		assertEquals("Cohort 'unused' is defined but never referenced", message);
	}

	@Test
	public void testValidateDefinitionsWithDuplicateName() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1)")
				.setCohorts(List.of(cohort("c1", "select a from syn2"), cohort("c1", "select b from syn3")));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
		}).getMessage();
		assertEquals("Cohort 'c1' is defined more than once", message);
	}

	@Test
	public void testValidateDefinitionsWithInvalidName() throws ParseException {
		Query query = new Query().setSql("select * from syn1")
				.setCohorts(List.of(cohort("1bad name", "select a from syn2")));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
		}).getMessage();
		assertEquals("Cohort name must be a simple identifier (letters, digits and underscores, not starting with a digit)",
				message);
	}

	@Test
	public void testValidateDefinitionsWithNullQuery() throws ParseException {
		Query query = new Query().setSql("select * from syn1 where x in cohort(c1)")
				.setCohorts(List.of(new CohortDefinition().setName("c1")));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.validateDefinitions(parse(query.getSql()), query, 5);
		}).getMessage();
		assertEquals("Cohort 'c1' query is required.", message);
	}

	@Test
	public void testCreateCohortSql() {
		CohortDefinition cohort = cohort("c1",
				"select participant_id from syn2 where file_type has ('txt') and file_type has ('csv')");
		// call under test
		String sql = CohortQueryValidator.createCohortSql(cohort);
		assertEquals("SELECT DISTINCT participant_id FROM syn2 WHERE file_type HAS ( 'csv' ) AND file_type HAS ( 'txt' )", sql);
	}

	@Test
	public void testCreateCohortSqlWithDistinct() {
		// call under test
		String sql = CohortQueryValidator.createCohortSql(cohort("c1", "select distinct pid from syn2"));
		assertEquals("SELECT DISTINCT pid FROM syn2", sql);
	}

	@Test
	public void testCreateCohortSqlWithFiltersAndFacets() {
		CohortDefinition cohort = cohort("c1", "select pid from syn2");
		cohort.getQuery().setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("a")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("1"))));
		// call under test
		String sql = CohortQueryValidator.createCohortSql(cohort);
		// Filters and facets are applied by the query pipeline, not folded into the cohort SQL.
		assertEquals("SELECT DISTINCT pid FROM syn2", sql);
	}

	private static void assertCohortRejected(CohortDefinition cohort, String expectedMessage) {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CohortQueryValidator.createCohortSql(cohort);
		}).getMessage();
		assertEquals(expectedMessage, message);
	}

	@Test
	public void testCreateCohortSqlWithStar() {
		assertCohortRejected(cohort("c1", "select * from syn2"),
				"Cohort 'c1' query must select exactly one column, without an alias, expression or aggregate");
	}

	@Test
	public void testCreateCohortSqlWithMultipleColumns() {
		assertCohortRejected(cohort("c1", "select a, b from syn2"),
				"Cohort 'c1' query must select exactly one column, without an alias, expression or aggregate");
	}

	@Test
	public void testCreateCohortSqlWithAlias() {
		assertCohortRejected(cohort("c1", "select a as b from syn2"),
				"Cohort 'c1' query must select exactly one column, without an alias, expression or aggregate");
	}

	@Test
	public void testCreateCohortSqlWithExpression() {
		assertCohortRejected(cohort("c1", "select a + 1 from syn2"),
				"Cohort 'c1' query must select exactly one column, without an alias, expression or aggregate");
	}

	@Test
	public void testCreateCohortSqlWithAggregate() {
		assertCohortRejected(cohort("c1", "select count(a) from syn2"),
				"Cohort 'c1' query must select exactly one column, without an alias, expression or aggregate");
	}

	@Test
	public void testCreateCohortSqlWithGroupBy() {
		assertCohortRejected(cohort("c1", "select a from syn2 group by a"),
				"Cohort 'c1' query may not use GROUP BY or HAVING");
	}

	@Test
	public void testCreateCohortSqlWithOrderBy() {
		assertCohortRejected(cohort("c1", "select a from syn2 order by a"), "Cohort 'c1' query may not be sorted");
	}

	@Test
	public void testCreateCohortSqlWithSqlLimit() {
		assertCohortRejected(cohort("c1", "select a from syn2 limit 10"), "Cohort 'c1' query may not have a limit or offset");
	}

	@Test
	public void testCreateCohortSqlWithQueryLimit() {
		CohortDefinition cohort = cohort("c1", "select a from syn2");
		cohort.getQuery().setLimit(10L);
		assertCohortRejected(cohort, "Cohort 'c1' query may not have a limit or offset");
	}

	@Test
	public void testCreateCohortSqlWithQueryOffset() {
		CohortDefinition cohort = cohort("c1", "select a from syn2");
		cohort.getQuery().setOffset(10L);
		assertCohortRejected(cohort, "Cohort 'c1' query may not have a limit or offset");
	}

	@Test
	public void testCreateCohortSqlWithQuerySort() {
		CohortDefinition cohort = cohort("c1", "select a from syn2");
		cohort.getQuery().setSort(List.of(new SortItem().setColumn("a")));
		assertCohortRejected(cohort, "Cohort 'c1' query may not be sorted");
	}

	@Test
	public void testCreateCohortSqlWithJoin() {
		assertCohortRejected(cohort("c1", "select t1.a from syn2 t1 join syn3 t2 on (t1.a = t2.a)"),
				"Cohort 'c1' query must query a single table without joins");
	}

	@Test
	public void testCreateCohortSqlWithUnion() {
		assertCohortRejected(cohort("c1", "select a from syn2 union select a from syn3"),
				"Cohort 'c1' query must be a single query without WITH or UNION");
	}

	@Test
	public void testCreateCohortSqlWithNestedCohortDefinitions() {
		CohortDefinition cohort = cohort("c1", "select a from syn2");
		cohort.getQuery().setCohorts(List.of(cohort("c2", "select b from syn3")));
		assertCohortRejected(cohort, "Cohort 'c1' query may not define nested cohorts");
	}

	@Test
	public void testCreateCohortSqlWithCohortReference() {
		assertCohortRejected(cohort("c1", "select a from syn2 where a in cohort(c2)"),
				"Cohort 'c1' query may not reference another cohort");
	}

	@Test
	public void testCreateCohortSqlWithCohortFilter() {
		CohortDefinition cohort = cohort("c1", "select a from syn2");
		cohort.getQuery().setAdditionalFilters(List.of(new ColumnCohortFilter().setColumnName("a").setCohortName("c2")));
		assertCohortRejected(cohort, "Cohort 'c1' query may not reference another cohort");
	}

	@Test
	public void testCreateCohortSqlWithNoSql() {
		assertCohortRejected(new CohortDefinition().setName("c1").setQuery(new Query()), "Cohort 'c1' query.sql is required.");
	}

}

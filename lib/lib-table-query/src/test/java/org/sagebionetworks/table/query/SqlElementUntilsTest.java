package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import com.google.common.collect.Lists;

public class SqlElementUntilsTest {
	
	@Test
	public void testConvertToCount() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1 offset 2");
		QuerySpecification converted = SqlElementUntils.convertToCountQuery(model);
		assertNotNull(converted);
		assertEquals("SELECT COUNT(*) FROM syn123 WHERE foo = 1", converted.toString());
	}

	@Test
	public void testConvertToPaginated() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar");
		QuerySpecification converted = SqlElementUntils.convertToPaginatedQuery(model, 234L, 567L);
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar LIMIT 567 OFFSET 234", converted.toString());
	}

	@Test
	public void testReplacePaginated() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1 offset 2");
		QuerySpecification converted = SqlElementUntils.convertToPaginatedQuery(model, 234L, 567L);
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar LIMIT 567 OFFSET 234", converted.toString());
	}

	@Test
	public void testConvertToSorted() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1");
		SortItem sort1 = new SortItem();
		sort1.setColumn("foo");
		SortItem sort2 = new SortItem();
		sort2.setColumn("zoo");
		sort2.setDirection(SortDirection.ASC);
		SortItem sort3 = new SortItem();
		sort3.setColumn("zaa");
		sort3.setDirection(SortDirection.DESC);
		QuerySpecification converted = SqlElementUntils.convertToSortedQuery(model, Lists.newArrayList(sort1, sort2, sort3));
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY foo ASC, zoo ASC, zaa DESC, bar LIMIT 1", converted.toString());
	}

	@Test
	public void testConvertToSortedEscaped() throws ParseException {
		QuerySpecification model = TableQueryParser
				.parserQuery("select \"foo-bar\", bar from syn123 where \"foo-bar\" = 1 order by bar limit 1");
		SortItem sort1 = new SortItem();
		sort1.setColumn("foo-bar");
		SortItem sort2 = new SortItem();
		sort2.setColumn("zoo");
		sort2.setDirection(SortDirection.ASC);
		SortItem sort3 = new SortItem();
		sort3.setColumn("zaa");
		sort3.setDirection(SortDirection.DESC);
		QuerySpecification converted = SqlElementUntils.convertToSortedQuery(model, Lists.newArrayList(sort1, sort2, sort3));
		assertNotNull(converted);
		assertEquals("SELECT \"foo-bar\", bar FROM syn123 WHERE \"foo-bar\" = 1 ORDER BY \"foo-bar\" ASC, zoo ASC, zaa DESC, bar LIMIT 1",
				converted.toString());
	}

	@Test
	public void testReplaceSorted() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1");
		SortItem sort1 = new SortItem();
		sort1.setColumn("bar");
		sort1.setDirection(SortDirection.DESC);
		SortItem sort2 = new SortItem();
		sort2.setColumn("foo");
		QuerySpecification converted = SqlElementUntils.convertToSortedQuery(model, Lists.newArrayList(sort1, sort2));
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar DESC, foo ASC LIMIT 1", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithoutPagingOverridesNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 1000 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithoutPagingOverrideLimitNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123");
		Long limit = null;
		Long offset = 12L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 1000 OFFSET 12", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithoutPagingOverrideOffsetNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123");
		Long limit = 15L;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 15 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOverridesNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 34 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverridesNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34 offset 12");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 34 OFFSET 12", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverrideLimitNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34 offset 12");
		Long limit = null;
		Long offset = 2L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 32 OFFSET 14", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverrideOffsetNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34 offset 12");
		Long limit = 3L;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 3 OFFSET 12", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverrideInRange() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100 offset 50");
		Long limit = 25L;
		Long offset = 10L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 25 OFFSET 60", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverrideOutOfRange() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100 offset 75");
		Long limit = 50L;
		Long offset = 10L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 50 OFFSET 85", converted.toString());
	}
}

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
		StringBuilder builder = new StringBuilder();
		converted.toSQL(builder, null);
		assertEquals("SELECT COUNT(*) FROM syn123 WHERE foo = 1", builder.toString());
	}

	@Test
	public void testConvertToPaginated() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar");
		QuerySpecification converted = SqlElementUntils.convertToPaginatedQuery(model, 234L, 567L);
		assertNotNull(converted);
		StringBuilder builder = new StringBuilder();
		converted.toSQL(builder, null);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar LIMIT 567 OFFSET 234", builder.toString());
	}

	@Test
	public void testReplacePaginated() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1 offset 2");
		QuerySpecification converted = SqlElementUntils.convertToPaginatedQuery(model, 234L, 567L);
		assertNotNull(converted);
		StringBuilder builder = new StringBuilder();
		converted.toSQL(builder, null);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar LIMIT 567 OFFSET 234", builder.toString());
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
		StringBuilder builder = new StringBuilder();
		converted.toSQL(builder, null);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY foo ASC, zoo ASC, zaa DESC, bar LIMIT 1",
				builder.toString());
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
		StringBuilder builder = new StringBuilder();
		converted.toSQL(builder, null);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar DESC, foo ASC LIMIT 1", builder.toString());
	}
}

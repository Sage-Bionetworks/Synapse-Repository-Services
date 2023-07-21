package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.IndexDescriptionLookup;
import org.sagebionetworks.table.cluster.description.MaterializedViewIndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@ExtendWith(MockitoExtension.class)
public class QueryTranslatorTest {
	
	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private IndexDescriptionLookup mockIndexDescriptionLookup;

	private Map<String, ColumnModel> columnNameToModelMap;
	private List<ColumnModel> tableSchema;

	private static final String DOUBLE_COLUMN = "CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END";
	private static final String STAR_COLUMNS = "_C111_, _C222_, _C333_, _C444_, _C555_, _C666_, " + DOUBLE_COLUMN
			+ ", _C888_, _C999_, _C4242_";

	private ColumnModel cm;
	private List<ColumnModel> schema;
	private String sql;
	private Long userId;
	private IdAndVersion idAndVersion;

	@BeforeEach
	public void before() {
		columnNameToModelMap = Maps.newLinkedHashMap(); // retains order of values
		columnNameToModelMap.put("foo", TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING));
		columnNameToModelMap.put("has space", TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING));
		columnNameToModelMap.put("bar", TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING));
		columnNameToModelMap.put("foo_bar", TableModelTestUtils.createColumn(444L, "foo_bar", ColumnType.STRING));
		columnNameToModelMap.put("Foo", TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING));
		columnNameToModelMap.put("datetype", TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE));
		columnNameToModelMap.put("doubletype", TableModelTestUtils.createColumn(777L, "doubletype", ColumnType.DOUBLE));
		columnNameToModelMap.put("inttype", TableModelTestUtils.createColumn(888L, "inttype", ColumnType.INTEGER));
		columnNameToModelMap.put("has-hyphen", TableModelTestUtils.createColumn(999L, "has-hyphen", ColumnType.STRING));
		columnNameToModelMap.put("has\"quote",
				TableModelTestUtils.createColumn(4242L, "has\"quote", ColumnType.STRING));
		tableSchema = new ArrayList<ColumnModel>(columnNameToModelMap.values());

		cm = new ColumnModel();
		cm.setName("5ormore");
		cm.setColumnType(ColumnType.INTEGER);
		cm.setId("111");
		schema = Lists.newArrayList(cm);
		sql = "select * from syn123";
		userId = 1l;
		idAndVersion = IdAndVersion.parse("syn123");
	}

	@Test
	public void testSelectStarEmtpySchema() throws ParseException {
		tableSchema = new LinkedList<ColumnModel>();
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);

		assertThrows(IllegalArgumentException.class, () -> {
			QueryTranslator.builder("select * from syn123", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		});
	}

	@Test
	public void testSelectStar() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		assertNotNull(translator.getSelectColumns());
		assertEquals(translator.getSelectColumns().size(), 10);
		assertEquals(TableModelUtils.getSelectColumns(this.tableSchema), translator.getSelectColumns());
		assertEquals("syn123", translator.getSingleTableId().get());
	}

	@Test
	public void testSelectStarEscaping() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		translator = QueryTranslator.builder(sql, mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testSelectSingleColumns() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		when(mockSchemaProvider.getColumnModel(any())).thenReturn(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}
	
	@Test
	public void testSqlQueryBuildWithNullSqlContext() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).sqlContext(null).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(SqlContext.query, translator.getSqlContext());
	}
	
	@Test
	public void testSqlQueryBuildWithSqlContextQuery() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).sqlContext(SqlContext.query).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(SqlContext.query, translator.getSqlContext());
	}
	
	@Test
	public void testSqlQueryBuildWithSqlContextBuild() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", mockSchemaProvider, userId)
				.indexDescription(new MaterializedViewIndexDescription(idAndVersion, Collections.emptyList()))
				.sqlContext(SqlContext.build).build();
		assertEquals("SELECT _C111_ FROM T123", translator.getOutputSQL());
		assertEquals(SqlContext.build, translator.getSqlContext());
	}

	@Test
	public void testSelectDoubleQuotedColumn() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("has\"quote"));
		
		QueryTranslator translator = QueryTranslator.builder("select \"has\"\"quote\" from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C4242_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("has\"quote"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}

	@Test
	public void testSelectMultipleColumns() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo, bar from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"),
				columnNameToModelMap.get("bar"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}

	@Test
	public void testSelectDistinct() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		
		QueryTranslator translator = QueryTranslator.builder("select distinct foo, bar from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT DISTINCT _C111_, _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		List<SelectColumn> expectedSelect = Lists.newArrayList(
				TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null),
				TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null));
		assertEquals(expectedSelect, translator.getSelectColumns());
	}

	@Test
	public void selectRowIdAndVersionStar() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionSingleColumn() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionDistinct() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		when(mockSchemaProvider.getColumnModel(any())).thenReturn(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select distinct foo from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionCount() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select count(*) from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionAggregateFunction() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select max(foo) from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionNonAggreageFunction() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select concat('a',foo) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionConstant() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select 'a constant' from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionConstantPlusColumn() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo, 'a constant' from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionArithmeticNoColumns() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select 5 div 2 from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionArithmeticAndColumn() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select 5 div 2, foo from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionArithmeticOfColumns() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select 5 div foo from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionGroupBy() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo, count(*) from syn123 group by foo",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndVersionAggregateFunctionNoGroup() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo, max(bar) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertFalse(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndNonAggregateFunction() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo, DAYOFMONTH(bar) from syn123",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndNonAggregateFunctionColumnRef() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select DAYOFMONTH(bar) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndNonAggregateFunctionOnly() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select DAYOFMONTH('2017-12-12') from syn123",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void selectRowIdAndAs() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select foo as \"cats\" from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertTrue(translator.includesRowIdAndVersion());
	}

	@Test
	public void testSelectConstant() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select 'not a foo' from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT 'not a foo', ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("not a foo", ColumnType.STRING, null)),
				translator.getSelectColumns());
		assertEquals("not a foo", translator.getSelectColumns().get(0).getName());
	}

	@Test
	public void testSelectCountStar() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select count(*) from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectAggregate() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select avg(inttype) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT AVG(_C888_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectAggregateMoreColumns() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("bar"));
		
		QueryTranslator translator = QueryTranslator.builder("select avg(inttype), bar from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT AVG(_C888_), _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(
				Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null),
						TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectGroupByAggregate() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 group by foo", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_ FROM T123 GROUP BY _C111_", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectAggregateMultiple() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select min(foo), max(bar) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT MIN(_C111_), MAX(_C333_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(
				Lists.newArrayList(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null),
						TableModelUtils.createSelectColumn("MAX(bar)", ColumnType.STRING, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testSelectDistinctAggregate() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select count(distinct foo) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(DISTINCT _C111_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(
				Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(DISTINCT foo)", ColumnType.INTEGER, null)),
				translator.getSelectColumns());
	}

	@Test
	public void testWhereSimple() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where foo = 1", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
	}
	
	void setupGetColumns(List<ColumnModel> schema){
		for(ColumnModel cm: schema) {
			when(mockSchemaProvider.getColumnModel(cm.getId())).thenReturn(cm);
		}
	}
	
	void setupGetColumns(ColumnModel...cms){
		for(ColumnModel cm: cms) {
			when(mockSchemaProvider.getColumnModel(cm.getId())).thenReturn(cm);
		}
	}

	@Test
	public void testWhereDoubleFunction() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder(
				"select * from syn123 where isNaN(doubletype) or isInfinity(doubletype)", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE"
						+ " ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' ) "
						+ "OR ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",
				translator.getOutputSQL());
	}

	@Test
	public void testWhereDouble() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where doubletype between 1.0 and 2.0",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C777_ BETWEEN :b0 AND :b1",
				translator.getOutputSQL());
		assertEquals(new Double(1.0), translator.getParameters().get("b0"));
		assertEquals(new Double(2.0), translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereOr() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where foo = 1 or bar = 2",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 OR _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereAnd() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where foo = 1 and bar = 2",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 AND _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereNested() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where (foo = 1 and bar = 2) or foo_bar = 3",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT " + STAR_COLUMNS
						+ ", ROW_ID, ROW_VERSION FROM T123 WHERE ( _C111_ = :b0 AND _C333_ = :b1 ) OR _C444_ = :b2",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
		assertEquals("3", translator.getParameters().get("b2"));
	}

	@Test
	public void testGroupByOne() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 group by foo", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_", translator.getOutputSQL());
	}

	@Test
	public void testGroupByMultiple() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 group by foo, bar", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_, _C333_", translator.getOutputSQL());
	}

	@Test
	public void testOrderByOneNoSpec() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_",
				translator.getOutputSQL());
	}

	@Test
	public void testOrderByOneWithSpec() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo desc", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ DESC",
				translator.getOutputSQL());
	}

	@Test
	public void testOrderByDouble() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 order by doubletype desc",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 ORDER BY _C777_ DESC", translator.getOutputSQL());
	}

	@Test
	public void testOrderByMultipleNoSpec() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo, bar", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_, _C333_",
				translator.getOutputSQL());
	}

	@Test
	public void testOrderByMultipeWithSpec() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 order by foo asc, bar desc",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ ASC, _C333_ DESC",
				translator.getOutputSQL());
	}

	@Test
	public void testLimit() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 limit 100", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0", translator.getOutputSQL());
		assertEquals(100L, translator.getParameters().get("b0"));
	}

	@Test
	public void testLimitAndOffset() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 limit 100 offset 2",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1",
				translator.getOutputSQL());
		assertEquals(100L, translator.getParameters().get("b0"));
		assertEquals(2L, translator.getParameters().get("b1"));
	}

	@Test
	public void testAllParts() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		
		QueryTranslator translator = QueryTranslator.builder(
				"select foo, bar from syn123 where foo_bar >= 1.89e4 order by bar desc limit 10 offset 0",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C444_ >= :b0 ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("18900.0", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testAllPartsWithGrouping() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"),columnNameToModelMap.get("bar"));
		
		QueryTranslator translator = QueryTranslator.builder(
				"select foo, bar from syn123 where foo_bar >= 1.89e4 group by foo order by bar desc limit 10 offset 0",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT _C111_, _C333_ FROM T123 WHERE _C444_ >= :b0 GROUP BY _C111_ ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("18900.0", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testTypeSetFunctionStrings() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select count(*), min(foo), max(foo), count(foo) from syn123",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(*), MIN(_C111_), MAX(_C111_), COUNT(_C111_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null),
				translator.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("MAX(foo)", ColumnType.STRING, null),
				translator.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(foo)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(3));
	}

	@Test
	public void testTypeSetFunctionIntegers() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder(
				"select min(inttype), max(inttype), sum(inttype), avg(inttype), count(inttype) from syn123",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT MIN(_C888_), MAX(_C888_), SUM(_C888_), AVG(_C888_), COUNT(_C888_) FROM T123",
				translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(inttype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(4));
	}

	@Test
	public void testTypeSetFunctionDoubles() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder(
				"select min(doubletype), max(doubletype), sum(doubletype), avg(doubletype), count(doubletype) from syn123",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT MIN(_C777_), MAX(_C777_), SUM(_C777_), AVG(_C777_), COUNT(_C777_) FROM T123",
				translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(doubletype)", ColumnType.DOUBLE, null),
				translator.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(doubletype)", ColumnType.INTEGER, null),
				translator.getSelectColumns().get(4));
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3864() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select doubletype as f1 from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1"
				+ ", ROW_ID, ROW_VERSION" + " FROM T123", translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3864withOrderBy() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select doubletype as f1 from syn123 order by f1",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1"
				+ ", ROW_ID, ROW_VERSION" + " FROM T123 ORDER BY f1", translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3865">3865</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3865() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder(
				"select max(doubletype), min(doubletype) from syn123 order by min(doubletype)",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " MAX(_C777_)" + ", MIN(_C777_) " + "FROM T123 " + "ORDER BY MIN(_C777_)",
				translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3865">3865</a>
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3865andPLFM_3864() throws Exception {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder(
				"select max(doubletype) as f1, min(doubletype) as f2 from syn123 order by f2",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT" + " MAX(_C777_) AS f1" + ", MIN(_C777_) AS f2 " + "FROM T123 " + "ORDER BY f2",
				translator.getOutputSQL());
	}

	/**
	 * We should be throwing 'column a not found' for this case.
	 * 
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3866">3866</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3866() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where foo in (\"a\")",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ IN ( `a` )", translator.getOutputSQL());
	}

	@Test
	public void testPLFM_3867() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			 QueryTranslator.builder("select foo from syn123 where foo = \"a\"",
					mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		}).getMessage();
		assertEquals("Column does not exist: a", message);
	}

	@Test
	public void testTranslateNotIsNaN() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where not isNaN(doubletype)",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE NOT ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",
				translator.getOutputSQL());
	}

	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3869">3869</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3869() throws ParseException {
		
		tableSchema = Lists.newArrayList(TableModelTestUtils.createColumn(123L, "aDouble", ColumnType.DOUBLE));
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select * from syn123 where not isNaN(aDouble)",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT " + "CASE WHEN _DBL_C123_ IS NULL THEN _C123_ ELSE _DBL_C123_ END," + " ROW_ID, ROW_VERSION"
						+ " FROM T123 WHERE NOT ( _DBL_C123_ IS NOT NULL AND _DBL_C123_ = 'NaN' )",
				translator.getOutputSQL());
	}

	/**
	 * The casue of PLFM-3870 was double columns functions were translated as
	 * SUM(CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END) Therefore,
	 * the string 'NaN' was passed to the function and treated as '1'. To fix the
	 * issue we simply use the origial double column for aggregate function, for
	 * example SUM(_C777_)
	 * 
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3870">3870</a>
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testPLFM_3870() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select sum(doubletype) from syn123", mockSchemaProvider,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT SUM(_C777_) FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testTranslateIsNaN() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo from syn123 where not isNaN(doubletype)",
				mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE NOT ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",
				translator.getOutputSQL());
	}

	@Test
	public void testMaxRowSizeBytesSelectStar() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		// the size will include the size of the schema
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		QueryTranslator translator = QueryTranslator.builder("select * from syn123", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
	}

	@Test
	public void testMaxRowSizeBytesCountStar() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		// the size is the size of an integer
		int maxSizeSchema = TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER, null, null);
		QueryTranslator translator = QueryTranslator.builder("select count(*) from syn123", mockSchemaProvider, userId)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
	}

	@Test
	public void testMaxRowsPerPage() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = maxSizeSchema * 2L;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, mockSchemaProvider, maxBytesPerPage,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(new Long(2L), translator.getMaxRowsPerPage());
	}

	@Test
	public void testMaxRowsPerPageMaxBytesSmall() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = 3L;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, mockSchemaProvider, maxBytesPerPage,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(new Long(1L), translator.getMaxRowsPerPage());
	}

	@Test
	public void testMaxRowsPerPageNullMaxBytesPerPage() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(tableSchema);
		
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = null;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, mockSchemaProvider, maxBytesPerPage,
				userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(null, translator.getMaxRowsPerPage());
	}

	@Test
	public void testOverrideLimitAndOffset() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		Long maxBytesPerPage = 10000L;
		QuerySpecification model = new TableQueryParser("select foo from syn123 limit 10 offset 1").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, mockSchemaProvider,
				maxBytesPerPage, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1", translator.getOutputSQL());
		assertEquals(10L, translator.getParameters().get("b0"));
		assertEquals(1L, translator.getParameters().get("b1"));
	}

	@Test
	public void testOverrideLimitAndOffsetNullWithMaxBytesPerPage() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		Long maxBytesPerPage = 1L;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, mockSchemaProvider,
				maxBytesPerPage, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1", translator.getOutputSQL());
		assertEquals(1L, translator.getParameters().get("b0"));
		assertEquals(0L, translator.getParameters().get("b1"));
	}

	@Test
	public void testOverrideNull() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		Long maxBytesPerPage = null;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		QueryTranslator translator = QueryTranslator.builder(model, mockSchemaProvider, maxBytesPerPage, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testPLFM_4161() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		String sql = "select * from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
	}

	@Test
	public void testDefaultType() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// should default to table.
		assertEquals(TableType.table, query.getTableType());
	}

	@Test
	public void testTableDefaultEtag() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// should default to false
		assertFalse(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithoutEtag() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).includeEntityEtag(null).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testViewDefaultEtag() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		// should default to false
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testSelectViewWithEtag() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.includeEntityEtag(true)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION, ROW_ETAG FROM T123", query.getOutputSQL());
		assertTrue(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithEtagFalse() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.includeEntityEtag(false)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testSelectTableWithEtag() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);
		
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}

	@Test
	public void testSelectViewWithEtagAggregate() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		sql = "select count(*) from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new ViewIndexDescription(idAndVersion, TableType.entityview)).build();
		assertEquals("SELECT COUNT(*) FROM T123", query.getOutputSQL());
	}

	/**
	 * This is a test for PLFM-4736.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testAliasGroupByOrderBy() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		sql = "select \"foo\" as \"f\", sum(inttype) as \"i` sum\" from syn123 group by \"f\" order by \"i` sum\" DESC";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_ AS `f`, SUM(_C888_) AS `i`` sum` FROM T123 GROUP BY `f` ORDER BY `i`` sum` DESC",
				query.getOutputSQL());
	}

	@Test
	public void testCurrentUserFunctionInWhereClause() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("inttype"));
		
		sql = "select inttype from syn123 where inttype = CURRENT_USER()";
		Map<String, Object> expectedParameters = Collections.singletonMap("b0", userId);
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C888_, ROW_ID, ROW_VERSION FROM T123 WHERE _C888_ = :b0", query.getOutputSQL());
		assertEquals(expectedParameters, query.getParameters());
	}

	@Test
	public void testCurrentUserFunctionInAggClause() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		sql = "select COUNT(CURRENT_USER()) from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(1) FROM T123", query.getOutputSQL());
	}

	@Test
	public void testCurrentUserFunctionInSelectClause() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		sql = "select CURRENT_USER() from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT 1, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
	}

	/**
	 * Test added for PLFM-6819.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testQueryUnixTimestamp() throws ParseException {
		
		ColumnModel createdOn = new ColumnModel();
		createdOn.setColumnType(ColumnType.DATE);
		createdOn.setName("createdOn");
		createdOn.setId("1");
		schema = Arrays.asList(createdOn);
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		setupGetColumns(schema);

		String sql = "select createdOn, UNIX_TIMESTAMP('2021-06-20 00:00:00')*1000 from syn123 where createdOn > UNIX_TIMESTAMP('2021-06-20 00:00:00')*1000";

		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C1_, UNIX_TIMESTAMP('2021-06-20 00:00:00')*1000, ROW_ID, ROW_VERSION FROM T123 WHERE _C1_ > UNIX_TIMESTAMP('2021-06-20 00:00:00')*:b0",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 1000L);
		assertEquals(expectedParams, query.getParameters());
	}

	@Test
	public void testQueryWithTextMatches() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		sql = "select foo from syn123 WHERE TEXT_MATCHES('some text')";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();

		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE MATCH(ROW_SEARCH_CONTENT) AGAINST(:b0)",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
		assertTrue(query.isIncludeSearch());
	}

	@Test
	public void testTranslateWithJoinWithQueryContext() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		sql = "select * from syn1 join syn2 on (syn1.id = syn2.id) WHERE syn1.foo is not null";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider).sqlContext(SqlContext.query)
					.indexDescription(new TableIndexDescription(idAndVersion)).build();
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}

	@Test
	public void testTranslateWithJoinWithAlowJoinTrue() throws ParseException {
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has\"quote")));
		
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 join syn2 on (syn1.foo = syn2.foo) WHERE syn1.bar = 'some text' order by syn1.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C4242_ "
				+ "FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A0._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}
	
	@Test
	public void testTranslateWithJoinWithAlowJoinTrueWithView() throws ParseException {
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has\"quote")));

		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new ViewIndexDescription(IdAndVersion.parse("syn2"), TableType.entityview));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 join syn2 on (syn1.foo = syn2.foo) WHERE syn1.bar = 'some text' order by syn1.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C4242_, IFNULL(_A1.ROW_BENEFACTOR,-1) " +
						"FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A0._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithQueryContext() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		setupGetColumns(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));

		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new ViewIndexDescription(IdAndVersion.parse("syn2"), TableType.entityview));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn123 WHERE bar = 'some text'";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C333_ = :b0",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithJoinWithAlowJoinTrueWithAlias() throws ParseException {
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has\"quote")));
		
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a join syn2 b on (a.foo = b.foo) WHERE a.bar = 'some text' order by a.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(indexDescription).sqlContext(SqlContext.build).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C4242_ "
				+ "FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A0._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithJoinWithMultipleOfSameTable() throws ParseException {

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));

		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn1")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a join syn1 b on (a.foo = b.foo) WHERE a.bar = 'some text' order by b.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C333_ "
				+ "FROM T1 _A0 JOIN T1 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A1._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithJoinWithMultipleOfSameViewWithBuildContext() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));


		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn1"), TableType.entityview));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a join syn1 b on (a.foo = b.foo) WHERE a.bar = 'some text' order by b.bar";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C333_, IFNULL(_A0.ROW_BENEFACTOR,-1) " +
						"FROM T1 _A0 JOIN T1 _A1 ON ( _A0._C111_ = _A1._C111_ ) WHERE _A0._C333_ = :b0 ORDER BY _A1._C333_",
				query.getOutputSQL());
		assertEquals(ImmutableMap.of("b0", "some text"), query.getParameters());
	}

	@Test
	public void testTranslateWithLeftJoin() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select b.`has space`, sum(a.foo) from syn1 a left join syn2 b on (a.foo = b.foo) group by b.`has space`";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A1._C222_, SUM(_A0._C111_) FROM T1 _A0 LEFT JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ ) GROUP BY _A1._C222_",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithLeftJoinOuter() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn1")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a left outer join syn2 b on (a.foo = b.foo)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C222_ FROM T1 _A0 LEFT OUTER JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithRightJoin() throws ParseException {
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a right join syn2 b on (a.foo = b.foo)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C222_ FROM T1 _A0 RIGHT JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithRightJoinOuter() throws ParseException {

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("has space")));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(IdAndVersion.parse("syn1")),
				new TableIndexDescription(IdAndVersion.parse("syn2")));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(idAndVersion, dependencies);

		sql = "select * from syn1 a right outer join syn2 b on (a.foo = b.foo)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(indexDescription).sqlContext(SqlContext.build).build();
		assertEquals(
				"SELECT _A0._C111_, _A0._C333_, _A1._C111_, _A1._C222_ FROM T1 _A0 RIGHT OUTER JOIN T2 _A1 ON ( _A0._C111_ = _A1._C111_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithIsInfinity() throws ParseException {
		idAndVersion = IdAndVersion.parse("syn1");

		when(mockSchemaProvider.getTableSchema(idAndVersion))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("doubletype")));

		sql = "select * from syn1 a where isInfinity(doubletype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_," + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END,"
						+ " ROW_ID, ROW_VERSION FROM T1 "
						+ "WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithIsNaN() throws ParseException {
		idAndVersion = IdAndVersion.parse("syn1");
		
		when(mockSchemaProvider.getTableSchema(idAndVersion))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("doubletype")));

		sql = "select * from syn1 a where isNaN(doubletype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals(
				"SELECT _C111_," + " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END,"
						+ " ROW_ID, ROW_VERSION FROM T1 "
						+ "WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithDoubleDefiningSql() throws ParseException {

		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("doubletype")));
		setupGetColumns(columnNameToModelMap.get("doubletype"));

		List<IndexDescription> dependencies = Arrays.asList(new ViewIndexDescription(viewId, TableType.dataset));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select doubletype from syn1";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals("SELECT _C777_, IFNULL(ROW_BENEFACTOR,-1) FROM T1", query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithDoubleQuerySql() throws ParseException {
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		IdAndVersion viewId = IdAndVersion.parse("syn1");

		when(mockSchemaProvider.getTableSchema(materializedViewId))
				.thenReturn(List.of(columnNameToModelMap.get("doubletype")));
		setupGetColumns(columnNameToModelMap.get("doubletype"));
		
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(viewId, TableType.dataset));
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		
		// this is a query against a materialized view.
		sql = "select * from syn123";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END, ROW_ID, ROW_VERSION FROM T123",
				query.getOutputSQL());
	}
	
	@Test
	public void testGetSchemaOfSelect() throws ParseException {
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn1")))
		.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("doubletype")));

		sql = "select * from syn1 a where isNaN(doubletype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(new TableIndexDescription(idAndVersion)).build();
		// call under test
		List<ColumnModel> schema = query.getSchemaOfSelect();
		List<ColumnModel> expected = Arrays.asList(
				new ColumnModel().setName("a.foo").setColumnType(ColumnType.STRING).setId(null).setMaximumSize(50L),
				new ColumnModel().setName("a.doubletype").setColumnType(ColumnType.DOUBLE).setId(null)
		);
		assertEquals(expected, schema);
	}
	
	/**
	 * Test added for PLFM-7738.
	 * @throws ParseException
	 */
	@Test
	public void testGetSchemaOfSelectWithUnion() throws ParseException {
		IdAndVersion one = IdAndVersion.parse("syn1");
		ColumnModel a = TableModelTestUtils.createColumn(111L, "a", ColumnType.STRING).setMaximumSize(100L);
		ColumnModel b = TableModelTestUtils.createColumn(222L, "b", ColumnType.STRING).setMaximumSize(50L);
		when(mockSchemaProvider.getTableSchema(one)).thenReturn(List.of(a, b));
		setupGetColumns(a,b);

		IdAndVersion two = IdAndVersion.parse("syn2");
		
		ColumnModel c = TableModelTestUtils.createColumn(333L, "c", ColumnType.STRING).setMaximumSize(40L);
		ColumnModel d = TableModelTestUtils.createColumn(444L, "d", ColumnType.STRING).setMaximumSize(150L);
		when(mockSchemaProvider.getTableSchema(two)).thenReturn(List.of(c, d));
		setupGetColumns(c,d);

		List<IndexDescription> dependencies = List.of(new TableIndexDescription(one), new TableIndexDescription(two));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		sql = "select a, b from syn1 union select c, d from syn2";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.indexDescription(indexDescription).sqlContext(SqlContext.build).build();
		
		// call under test
		List<ColumnModel> schema = query.getSchemaOfSelect();
		List<ColumnModel> expected = List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.STRING).setMaximumSize(100L),
				new ColumnModel().setName("b").setColumnType(ColumnType.STRING).setMaximumSize(150L));
		assertEquals(expected, schema);
	}
	
	@Test
	public void testJoinViewAndTableWithDepenenciesOutOfOrder() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on a.inttype = b.inttype";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C888_, _A1._C888_, IFNULL(_A0.ROW_BENEFACTOR,-1) FROM T1 _A0 JOIN T2 _A1 ON _A0._C888_ = _A1._C888_",
				query.getOutputSQL());
	}
	
	@Test
	public void testJoinSameViewMultipleTimes() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttype = b.inttype) join syn1 c on (b.inttype = c.inttype)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C888_, _A1._C888_, _A2._C888_, IFNULL(_A0.ROW_BENEFACTOR,-1) FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C888_ = _A1._C888_ ) JOIN T1 _A2 ON ( _A1._C888_ = _A2._C888_ )",
				query.getOutputSQL());
	}
	
	@Test
	public void testBuildMaterializedViewWithViewDependencyAndGroupByInDefiningSql() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("foo"),columnNameToModelMap.get("bar")));
		setupGetColumns(columnNameToModelMap.get("foo"));

		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select foo, sum(bar) from syn1 group by foo";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getMessage();
		assertEquals(TableConstants.DEFINING_SQL_WITH_GROUP_BY_ERROR, message);
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithWhereColumnNotFound() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
	
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttype = b.inttype) where a.inttypeWrong = 123";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getLocalizedMessage();
		assertEquals("Column does not exist: a.inttypeWrong", message);
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithJoinColumnNotFound() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order. 
		List<IndexDescription> dependencies = Arrays.asList(
				new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview)
		);

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttypeWrong = b.inttype)";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getLocalizedMessage();
		assertEquals("Column does not exist: a.inttypeWrong", message);
	}

	@Test
	public void testTranslateWithMaterializedViewWithComplexCondition() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));

		// Note: The dependencies are in a different order.
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 a join syn2 b on (a.inttype = b.inttype and a.inttype > 12)";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C888_, _A1._C888_, IFNULL(_A0.ROW_BENEFACTOR,-1) FROM T1 _A0 " +
						"JOIN T2 _A1 ON ( _A0._C888_ = _A1._C888_ AND _A0._C888_ > :b0 )",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 12L);
		assertEquals(expectedParams, query.getParameters());
	}
	
	@Test
	public void testTranslateWithUnionInQueryContext() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));


		// Note: The dependencies are in a different order.
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 where inttype > 1 union select * from syn2 where inttype < 12";
		String message =  assertThrows(IllegalArgumentException.class, () -> {
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
					.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		}).getMessage();
		assertEquals("The UNION keyword is not supported in this context", message);
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithUnionViewAndTable() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion tableId = IdAndVersion.parse("syn2");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		setupGetColumns(columnNameToModelMap.get("inttype"));

		// Note: The dependencies are in a different order.
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(tableId),
				new ViewIndexDescription(viewId, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 where inttype > 1 union select * from syn2 where inttype < 12";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _C888_, IFNULL(ROW_BENEFACTOR,-1) FROM T1 WHERE _C888_ > :b0"
				+ " UNION "
				+ "SELECT _C888_, -1 FROM T2 WHERE _C888_ < :b1",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 1L);
		expectedParams.put("b1", 12L);
		assertEquals(expectedParams, query.getParameters());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithUnionTwoViews() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion view2Id = IdAndVersion.parse("syn2");
	
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		when(mockSchemaProvider.getTableSchema(view2Id)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		setupGetColumns(columnNameToModelMap.get("inttype"));

		// Note: The dependencies are in a different order.
		List<IndexDescription> dependencies = Arrays.asList(new ViewIndexDescription(view2Id, TableType.entityview),
				new ViewIndexDescription(viewId, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 where inttype > 1 union select * from syn2 where inttype < 12";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _C888_, IFNULL(ROW_BENEFACTOR,-1), -1 FROM T1 WHERE _C888_ > :b0 "
				+ "UNION "
				+ "SELECT _C888_, -1, IFNULL(ROW_BENEFACTOR,-1) FROM T2 WHERE _C888_ < :b1",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 1L);
		expectedParams.put("b1", 12L);
		assertEquals(expectedParams, query.getParameters());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithUnionSameViewInEachSideOfUnion() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("inttype")));
		setupGetColumns(columnNameToModelMap.get("inttype"));

		// Note: The dependencies are in a different order.
		List<IndexDescription> dependencies = Arrays.asList(new ViewIndexDescription(viewId, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select * from syn1 where inttype > 1 union select * from syn1 where inttype < 12";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _C888_, IFNULL(ROW_BENEFACTOR,-1) FROM T1 WHERE _C888_ > :b0"
				+ " UNION "
				+ "SELECT _C888_, IFNULL(ROW_BENEFACTOR,-1) FROM T1 WHERE _C888_ < :b1",
				query.getOutputSQL());
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 1L);
		expectedParams.put("b1", 12L);
		assertEquals(expectedParams, query.getParameters());
	}
	
	@Test
	public void testTranslateWithMaterializedViewWithUnionAndJoinMultipleViews() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion view2Id = IdAndVersion.parse("syn2");
		IdAndVersion view3Id = IdAndVersion.parse("syn3");
	
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(columnNameToModelMap.get("foo")));
		when(mockSchemaProvider.getTableSchema(view2Id)).thenReturn(List.of(columnNameToModelMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(view3Id)).thenReturn(List.of(columnNameToModelMap.get("bar")));

		setupGetColumns(columnNameToModelMap.get("bar"));

		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(viewId, TableType.entityview),
				new ViewIndexDescription(view2Id, TableType.entityview),
				new ViewIndexDescription(view3Id, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn4");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "select a.foo from syn1 a join syn2 b on (a.foo = b.`has space`) union select * from syn3 where bar is not null";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		assertEquals(
				"SELECT _A0._C111_, IFNULL(_A0.ROW_BENEFACTOR,-1), IFNULL(_A1.ROW_BENEFACTOR,-1), -1 "
				+ "FROM T1 _A0 JOIN T2 _A1 ON ( _A0._C111_ = _A1._C222_ )"
				+ " UNION "
				+ "SELECT _C333_, -1, -1, IFNULL(ROW_BENEFACTOR,-1) FROM T3 WHERE _C333_ IS NOT NULL",
				query.getOutputSQL());
	}
	
	@Test
	public void testTranslateWithMaterializedViewNestedUnions() throws ParseException {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion view2Id = IdAndVersion.parse("syn2");

		List<IndexDescription> dependencies = Arrays.asList(new ViewIndexDescription(viewId, TableType.entityview),
				new ViewIndexDescription(view2Id, TableType.entityview));

		IdAndVersion materializedViewId = IdAndVersion.parse("syn3");
		IndexDescription indexDescription = new MaterializedViewIndexDescription(materializedViewId, dependencies);

		// this query is used to build the materialized view.
		sql = "(select foo from syn1 union select `has space` from syn2) where foo > 12";

		/*
		 * Note: This query does not currently parse. However, if we do add support for
		 * this type of query, then we need to ensure that the 'where foo > 12' is
		 * properly translated.
		 */
		Throwable cause = assertThrows(IllegalArgumentException.class, () -> {
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
					.sqlContext(SqlContext.build).indexDescription(indexDescription).build();
		}).getCause();
		assertTrue(cause instanceof ParseException);

	}
	
	@Test
	public void testSelectWithSimpleCase() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo")));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select case foo when 'a' then 0 else 2 end as aCase from syn1";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT CASE _C111_ WHEN 'a' THEN 0 ELSE 2 END AS aCase, ROW_ID, ROW_VERSION FROM T1",
				query.getOutputSQL());
	}
	
	@Test
	public void testSelectWithSearchedCase() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		setupGetColumns(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select foo, bar, case "
				+ " when foo > bar then 'foo greater than bar'"
				+ " when foo < bar then 'bar greater than foo'"
				+ " else 'other'"
				+ " end as description from syn1 ";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT _C111_, _C333_, CASE"
				+ " WHEN _C111_ > _C333_ THEN 'foo greater than bar'"
				+ " WHEN _C111_ < _C333_ THEN 'bar greater than foo'"
				+ " ELSE 'other' END AS description, ROW_ID, ROW_VERSION FROM T1",
				query.getOutputSQL());
	}
	
	@Test
	public void testSelectWithNullIf() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select NULLIF(foo,bar) as fb from syn1 ";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT NULLIF(_C111_,_C333_) AS fb, ROW_ID, ROW_VERSION FROM T1",
				query.getOutputSQL());
		List<SelectColumn> select = query.getSelectColumns();
		assertEquals(List.of(new SelectColumn().setName("fb").setColumnType(ColumnType.STRING)), select);
	}
	
	@Test
	public void testCastWithColumnType() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId))
				.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));

		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select cast(foo as STRING) AS someString from syn1 ";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT CAST(_C111_ AS CHAR) AS someString, ROW_ID, ROW_VERSION FROM T1", query.getOutputSQL());
		List<SelectColumn> select = query.getSelectColumns();
		assertEquals(List.of(new SelectColumn().setName("someString").setColumnType(ColumnType.STRING)), select);
	}
	
	@Test
	public void testCastWithColumnId() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId))
				.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getColumnModel("777")).thenReturn(columnNameToModelMap.get("doubletype"));

		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select cast(foo as 777) AS aDouble from syn1 ";
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		assertEquals("SELECT CAST(_C111_ AS DOUBLE) AS aDouble, ROW_ID, ROW_VERSION FROM T1", query.getOutputSQL());
		List<SelectColumn> select = query.getSelectColumns();
		SelectColumn expectedSelect = new SelectColumn().setName("aDouble").setColumnType(ColumnType.DOUBLE)
				.setId("777");
		assertEquals(List.of(expectedSelect), select);
	}
	
	@Test
	public void testVirtualTableCTE() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		when(mockSchemaProvider.getTableSchema(tableId))
				.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(cte))
		.thenReturn(List.of(columnNameToModelMap.get("inttype"), columnNameToModelMap.get("foo_bar")));
		
		when(mockSchemaProvider.getColumnModel("888")).thenReturn(columnNameToModelMap.get("inttype"));
		when(mockSchemaProvider.getColumnModel("444")).thenReturn(columnNameToModelMap.get("foo_bar"));
		
		when(mockIndexDescriptionLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(tableId));

		String definingSql = "select CAST(foo as 888), CAST(bar as 444) from syn1";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = indexDescription.preprocessQuery("select * from syn2 where inttype > 2");
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		
		assertEquals("WITH T2 (_C888_, _C444_) AS "
				+ "(SELECT CAST(_C111_ AS SIGNED), CAST(_C333_ AS CHAR) FROM T1)"
				+ " SELECT _C888_, _C444_ FROM T2 WHERE _C888_ > :b0", query.getOutputSQL());
		List<SelectColumn> select = query.getSelectColumns();
		List<SelectColumn> expectedSelect = List
				.of(new SelectColumn().setName("inttype").setColumnType(ColumnType.INTEGER).setId("888"),
						new SelectColumn().setName("foo_bar").setColumnType(ColumnType.STRING).setId("444"));
		assertEquals(expectedSelect, select);
		
		Map<String, Object> expectedParams = new HashMap<>(4);
		expectedParams.put("b0", 2L);
		assertEquals(expectedParams, query.getParameters());
		assertEquals("syn2", query.getSingleTableId().get());
	}
	
	@Test
	public void testVirtualTableCTEWithComplex() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		when(mockSchemaProvider.getTableSchema(tableId))
				.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(cte))
		.thenReturn(List.of(columnNameToModelMap.get("inttype"), columnNameToModelMap.get("foo_bar")));
		
		when(mockSchemaProvider.getColumnModel("888")).thenReturn(columnNameToModelMap.get("inttype"));
		when(mockSchemaProvider.getColumnModel("444")).thenReturn(columnNameToModelMap.get("foo_bar"));
		
		when(mockIndexDescriptionLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(tableId));

		String definingSql = "select CAST(foo as 888), CAST(bar as 444) from syn1";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = indexDescription.preprocessQuery("select inttype, count(foo_bar) from syn2 where inttype > 2 group by inttype");
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		
		assertEquals("WITH T2 (_C888_, _C444_) AS "
				+ "(SELECT CAST(_C111_ AS SIGNED), CAST(_C333_ AS CHAR) FROM T1)"
				+ " SELECT _C888_, COUNT(_C444_) FROM T2 WHERE _C888_ > :b0 GROUP BY _C888_", query.getOutputSQL());
	}
	
	@Test
	public void testVirtualTableCTEWithUnion() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		when(mockSchemaProvider.getTableSchema(tableId))
				.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(cte))
		.thenReturn(List.of(columnNameToModelMap.get("inttype"), columnNameToModelMap.get("foo_bar")));
				
		when(mockIndexDescriptionLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(tableId));

		String definingSql = "select CAST(foo as 888), CAST(bar as 444) from syn1";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = "select * from syn2 union select * from syn1";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
					.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		}).getMessage();
		assertEquals(TableConstants.UNION_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}
	
	@Test
	public void testVirtualTableCTEWithJoin() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		when(mockSchemaProvider.getTableSchema(tableId))
				.thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		when(mockSchemaProvider.getTableSchema(cte))
		.thenReturn(List.of(columnNameToModelMap.get("inttype"), columnNameToModelMap.get("foo_bar")));
				
		when(mockIndexDescriptionLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(tableId));

		String definingSql = "select CAST(foo as 888), CAST(bar as 444) from syn1";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = indexDescription.preprocessQuery("select * from syn2 a join syn2 b on (a.id= b.id)");
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
					.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}
	
	@Test
	public void testTranslateGroup_concatWithMultipleColumns() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		setupGetColumns(columnNameToModelMap.get("foo"));
		
		QueryTranslator translator = QueryTranslator.builder("select foo, group_concat(distinct bar, foo_bar) from syn123 group by foo", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT _C111_, GROUP_CONCAT(DISTINCT _C333_, _C444_) FROM T123 GROUP BY _C111_", translator.getOutputSQL());

	}
	
	@Test
	public void testTranslateCountDistinctWithMultipleColumns() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(tableSchema);
		
		QueryTranslator translator = QueryTranslator.builder("select count(distinct bar, foo_bar) from syn123", mockSchemaProvider, userId).indexDescription(new TableIndexDescription(idAndVersion)).build();
		assertEquals("SELECT COUNT(DISTINCT _C333_, _C444_) FROM T123", translator.getOutputSQL());

	}
	
	@Test
	public void testVirtualTableWithFileViewSource() {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		
		ColumnModel foo = new ColumnModel().setName("foo").setColumnType(ColumnType.STRING).setMaximumSize(40L).setId("11");
		ColumnModel bar = new ColumnModel().setName("bar").setColumnType(ColumnType.INTEGER).setId("22");
		ColumnModel sumBar = new ColumnModel().setName("sumBar").setColumnType(ColumnType.INTEGER).setId("33");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(foo, bar));
		when(mockSchemaProvider.getTableSchema(cte)).thenReturn(List.of(foo, sumBar));
		setupGetColumns(foo, sumBar);

		when(mockIndexDescriptionLookup.getIndexDescription(viewId)).thenReturn(new ViewIndexDescription(viewId, TableType.entityview));
		
		String definingSql = "select foo, sum(cast(bar as 33)) from syn1 group by foo";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = indexDescription.preprocessQuery("select * from syn2");
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		
		assertEquals("WITH T2 (_C11_, _C33_) AS"
				+ " (SELECT _C11_, SUM(CAST(_C22_ AS SIGNED)) FROM T1 GROUP BY _C11_)"
				+ " SELECT _C11_, _C33_ FROM T2", query.getOutputSQL());
		assertTrue(query.isAggregatedResult());
		
	}
	
	@Test
	public void testSelectWithJsonObject() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select JSON_OBJECT('foo', 1, 'bar', 2) as j from syn1";
		
		QueryTranslator query = QueryTranslator.builder(sql, userId)
			.schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.query)
			.indexDescription(indexDescription).build();
		
		assertEquals("SELECT JSON_OBJECT('foo',1,'bar',2) AS j, ROW_ID, ROW_VERSION FROM T1", query.getOutputSQL());
		
		List<SelectColumn> select = query.getSelectColumns();
		
		assertEquals(List.of(new SelectColumn().setName("j").setColumnType(ColumnType.JSON)), select);
	}
	
	@Test
	public void testSelectWithJsonArray() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select JSON_ARRAY('foo', 1, 'bar', 2) as j from syn1";
		
		QueryTranslator query = QueryTranslator.builder(sql, userId)
			.schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.query)
			.indexDescription(indexDescription).build();
		
		assertEquals("SELECT JSON_ARRAY('foo',1,'bar',2) AS j, ROW_ID, ROW_VERSION FROM T1", query.getOutputSQL());
		
		List<SelectColumn> select = query.getSelectColumns();
		
		assertEquals(List.of(new SelectColumn().setName("j").setColumnType(ColumnType.JSON)), select);
	}
	
	@Test
	public void testVirtualTableWithDefiningClause() {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		
		ColumnModel foo = new ColumnModel().setName("foo").setColumnType(ColumnType.STRING).setMaximumSize(40L).setId("11");
		ColumnModel bar = new ColumnModel().setName("bar").setColumnType(ColumnType.INTEGER).setId("22");
		ColumnModel sumBar = new ColumnModel().setName("sumBar").setColumnType(ColumnType.INTEGER).setId("33");
		
		when(mockSchemaProvider.getTableSchema(viewId)).thenReturn(List.of(foo, bar));
		when(mockSchemaProvider.getTableSchema(cte)).thenReturn(List.of(foo, sumBar));
		setupGetColumns(foo, sumBar);

		when(mockIndexDescriptionLookup.getIndexDescription(viewId)).thenReturn(new ViewIndexDescription(viewId, TableType.entityview));
		
		String definingSql = "select foo, sum(cast(bar as 33)) from syn1 where bar > 1 group by foo";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = indexDescription.preprocessQuery("select * from syn2 defining_where foo = 'apple'");
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		
		assertEquals("WITH T2 (_C11_, _C33_) AS "
				+ "(SELECT _C11_, SUM(CAST(_C22_ AS SIGNED)) FROM T1 WHERE ( _C22_ > :b0 ) AND ( _C11_ = :b1 ) GROUP BY _C11_)"
				+ " SELECT _C11_, _C33_ FROM T2", query.getOutputSQL());
		assertTrue(query.isAggregatedResult());
		
		Map<String, Object> expectedParams = new HashMap<>(2);
		expectedParams.put("b0", 1L);
		expectedParams.put("b1", "apple");
		assertEquals(expectedParams, query.getParameters());
	}
	
	@Test
	public void testVirtualTableWithNonAggregateDefiningSql() {
		IdAndVersion viewId = IdAndVersion.parse("syn1");
		IdAndVersion cte = IdAndVersion.parse("syn2");
		
		ColumnModel foo = new ColumnModel().setName("foo").setColumnType(ColumnType.STRING).setMaximumSize(40L).setId("11");
			
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(List.of(foo));
		setupGetColumns(foo);

		when(mockIndexDescriptionLookup.getIndexDescription(viewId)).thenReturn(new ViewIndexDescription(viewId, TableType.entityview));
		
		String definingSql = "select foo from syn1";
		IndexDescription indexDescription = new VirtualTableIndexDescription(cte, definingSql,  mockIndexDescriptionLookup);

		sql = indexDescription.preprocessQuery("select * from syn2");
		// call under test
		QueryTranslator query = QueryTranslator.builder(sql, userId).schemaProvider(mockSchemaProvider)
				.sqlContext(SqlContext.query).indexDescription(indexDescription).build();
		
		assertEquals("WITH T2 (_C11_) AS (SELECT _C11_ FROM T1) SELECT _C11_ FROM T2", query.getOutputSQL());
		assertFalse(query.includesRowIdAndVersion());
		assertFalse(query.includeEntityEtag());
		assertFalse(query.isAggregatedResult());
	}
		
	@Test
	public void testSelectWithJsonExtract() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select JSON_EXTRACT(foo, '$[0]') as j from syn1 where JSON_EXTRACT(bar, '$.a') > 3";
		
		QueryTranslator query = QueryTranslator.builder(sql, userId)
			.schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.query)
			.indexDescription(indexDescription).build();
		
		assertEquals("SELECT JSON_EXTRACT(_C111_,'$[0]') AS j, ROW_ID, ROW_VERSION FROM T1 WHERE JSON_EXTRACT(_C333_,'$.a') > :b0", query.getOutputSQL());
		
		assertEquals(Map.of("b0", "3"), query.getParameters());
		
		List<SelectColumn> select = query.getSelectColumns();
		
		assertEquals(List.of(new SelectColumn().setName("j").setColumnType(ColumnType.STRING)), select);
	}
		
	@Test
	public void testSelectWithJsonOverlaps() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar")));
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select JSON_OVERLAPS(foo, '[1,2]') as j from syn1 where JSON_OVERLAPS(bar, '[3,4]') IS TRUE";
		
		QueryTranslator query = QueryTranslator.builder(sql, userId)
			.schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.query)
			.indexDescription(indexDescription).build();
		
		assertEquals("SELECT JSON_OVERLAPS(_C111_,'[1,2]') AS j, ROW_ID, ROW_VERSION FROM T1 WHERE JSON_OVERLAPS(_C333_,'[3,4]') IS TRUE", query.getOutputSQL());
		
		List<SelectColumn> select = query.getSelectColumns();
		
		assertEquals(List.of(new SelectColumn().setName("j").setColumnType(ColumnType.BOOLEAN)), select);
	}
	
	@Test
	public void testSelectWithJsonArrayAgg() {
		IdAndVersion tableId = IdAndVersion.parse("syn1");
		
		ColumnModel foo = columnNameToModelMap.get("foo");
		ColumnModel bar = columnNameToModelMap.get("bar");
		
		when(mockSchemaProvider.getTableSchema(tableId)).thenReturn(List.of(foo, bar));
		
		setupGetColumns(bar);
		
		IndexDescription indexDescription = new TableIndexDescription(tableId);

		sql = "select bar, JSON_ARRAYAGG(foo) as j from syn1 group by bar";
		
		QueryTranslator query = QueryTranslator.builder(sql, userId)
			.schemaProvider(mockSchemaProvider)
			.sqlContext(SqlContext.query)
			.indexDescription(indexDescription).build();
		
		assertEquals("SELECT _C333_, JSON_ARRAYAGG(_C111_) AS j FROM T1 GROUP BY _C333_", query.getOutputSQL());
		
		assertTrue(query.isAggregatedResult());
		
		List<SelectColumn> select = query.getSelectColumns();
		
		assertEquals(List.of(new SelectColumn().setName("bar").setColumnType(ColumnType.STRING), new SelectColumn().setName("j").setColumnType(ColumnType.JSON)), select);
	}
		
}

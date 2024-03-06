package org.sagebionetworks.table.cluster.stats;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

import com.google.common.collect.Lists;


// These tests serve as integration tests for specific SQL trees. Although they are integration tests,
// we mock the TableAndColumnMapper as to only test the generators.

@ExtendWith(MockitoExtension.class)
public class StatGeneratorIntegrationTest {

	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private StatGenerator statGenerator;
	
	
	private Long fooMaxSize = 31L;
	private Long barMaxSize = 53L;
	private Long stringListColMaxSize = 97L;
	private Long doubleColMaxSize = 131L;
	private Long integerColMaxSize = 157L;
	
	
	@BeforeEach
	public void before() throws ParseException {
		setupTableAndColumnMapper();
	}
	
	
	@Test
	public void testGenerateWithNull() throws ParseException {
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, statGenerator.generate(null, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGerenateWithColumn() throws ParseException{
		String valueExpression = "foo";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAddition() throws ParseException {
		String valueExpression = "foo+bar";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + barMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithString() throws ParseException {
		String valueExpression = "123";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(3L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcat() throws ParseException {
		String valueExpression = "CONCAT(foo, '12345')";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + 5L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithMultipleStrings() throws ParseException {
		String valueExpression = "CONCAT(foo, '12345', '678')";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + 8L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithMultipleColumns() throws ParseException {
		String valueExpression = "CONCAT(foo, '1', bar)";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + 1L + barMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithNestedConcat() throws ParseException {
		String valueExpression = "CONCAT(CONCAT(foo, '12345'), '678')";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + 8L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithCount() throws ParseException {
		String valueExpression = "CONCAT(foo, '1', COUNT(foo))";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + 1L + ColumnConstants.MAX_INTEGER_BYTES_AS_STRING).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithUnnest() throws ParseException {
		String valueExpression = "CONCAT(foo, '1', UNNEST(stringListCol))";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(fooMaxSize + 1L + stringListColMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAverage() throws ParseException {
		String valueExpression = "AVG(foo)";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_BYTES_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithCount() throws ParseException {
		String valueExpression = "COUNT(*)";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_INTEGER_BYTES_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithUnnest() throws ParseException {
		String valueExpression = "UNNEST(stringListCol)";
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringListColMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	void setupTableAndColumnMapper() throws ParseException {
		List<ColumnModel> schema = Lists.newArrayList(
				TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING).setMaximumSize(fooMaxSize),
				TableModelTestUtils.createColumn(222L, "bar", ColumnType.STRING).setMaximumSize(barMaxSize),
				TableModelTestUtils.createColumn(333L, "stringListCol", ColumnType.STRING_LIST).setMaximumSize(stringListColMaxSize),
				TableModelTestUtils.createColumn(444L, "doubleCol", ColumnType.DOUBLE).setMaximumSize(doubleColMaxSize),
				TableModelTestUtils.createColumn(555L, "integerCol", ColumnType.INTEGER).setMaximumSize(integerColMaxSize));
		
		QuerySpecification model = new TableQueryParser("SELECT foo FROM syn123").queryExpression()
				.getFirstElementOfType(QuerySpecification.class);
	
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		mockTableAndColumnMapper = new TableAndColumnMapper(model, mockSchemaProvider);
	}
	
}

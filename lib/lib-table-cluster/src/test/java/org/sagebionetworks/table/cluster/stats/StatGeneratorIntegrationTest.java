package org.sagebionetworks.table.cluster.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import org.sagebionetworks.table.query.model.RegularIdentifier;

import com.google.common.collect.Lists;

/**
 * These tests serve as integration tests for specific SQL trees. Although they are integration tests,
 * we mock the TableAndColumnMapper as to only test the generators.
 */

@ExtendWith(MockitoExtension.class)
public class StatGeneratorIntegrationTest {

	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private StatGenerator statGenerator;
	
	
	private Long stringColMaxSize = 31L;
	private Long anotherStringColMaxSize = 53L;
	private Long stringListColMaxSize = 67L;
	
	
	@Test
	public void testGenerateWithNullElement() {
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, statGenerator.generate(null, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithNullTableAndColumnMapper() {
		String valueExpression = "123";
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			statGenerator.generate(
					new TableQueryParser(valueExpression).valueExpression(), null);
		}).getMessage();
		
		assertEquals("tableAndColumnMapper is required.", errorMessage);
	}
	
	@Test
	public void testGenerateWithUnimplementedCase() {
		RegularIdentifier unhandledElement = new RegularIdentifier("string"); 
		
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, statGenerator.generate(unhandledElement, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGerenateWithColumn() throws ParseException{
		String valueExpression = "stringCol";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAdditionWithStrings() throws ParseException {
		String valueExpression = "stringCol+anotherStringCol";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAdditionWithIntegers() throws ParseException {
		String valueExpression = "integerCol+anotherIntegerCol";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAdditionWithIntegerPlusDouble() throws ParseException {
		String valueExpression = "integerCol+doubleCol";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAdditionWithDoubles() throws ParseException {
		String valueExpression = "doubleCol+anotherDoubleCol";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING)).build());
		
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
		String valueExpression = "CONCAT(stringCol, '12345')";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize + 5L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithMultipleStrings() throws ParseException {
		String valueExpression = "CONCAT(stringCol, '12345', '678')";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize + 8L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithMultipleColumns() throws ParseException {
		String valueExpression = "CONCAT(stringCol, '1', anotherStringCol)";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize + 1L + anotherStringColMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithNestedConcat() throws ParseException {
		String valueExpression = "CONCAT(CONCAT(stringCol, '12345'), '678')";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize + 8L).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithCount() throws ParseException {
		String valueExpression = "CONCAT(stringCol, '1', COUNT(stringCol))";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize + 1L + ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithConcatWithUnnest() throws ParseException {
		String valueExpression = "CONCAT(stringCol, '1', UNNEST(stringListCol))";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringColMaxSize + 1L + stringListColMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithAverage() throws ParseException {
		String valueExpression = "AVG(stringCol)";
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithCount() throws ParseException {
		String valueExpression = "COUNT(*)";
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING)).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithUnnest() throws ParseException {
		String valueExpression = "UNNEST(stringListCol)";
		
		setupTableAndColumnMapper();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(stringListColMaxSize).build());
		
		assertEquals(expected, statGenerator.generate(
				new TableQueryParser(valueExpression).valueExpression(), mockTableAndColumnMapper));
	}
	
	void setupTableAndColumnMapper() throws ParseException {
		List<ColumnModel> schema = Lists.newArrayList(
				TableModelTestUtils.createColumn(111L, "stringCol", ColumnType.STRING).setMaximumSize(stringColMaxSize),
				TableModelTestUtils.createColumn(222L, "anotherStringCol", ColumnType.STRING).setMaximumSize(anotherStringColMaxSize),
				TableModelTestUtils.createColumn(333L, "stringListCol", ColumnType.STRING_LIST).setMaximumSize(stringListColMaxSize),
				TableModelTestUtils.createColumn(444L, "integerCol", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(555L, "anotherIntegerCol", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(666L, "doubleCol", ColumnType.DOUBLE),
				TableModelTestUtils.createColumn(777L, "anotherDoubleCol", ColumnType.DOUBLE));
				
		
		QuerySpecification model = new TableQueryParser("SELECT foo FROM syn123").queryExpression()
				.getFirstElementOfType(QuerySpecification.class);
	
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		mockTableAndColumnMapper = new TableAndColumnMapper(model, mockSchemaProvider);
	}
	
}

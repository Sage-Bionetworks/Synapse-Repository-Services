package org.sagebionetworks.table.cluster.stats;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;
import org.sagebionetworks.table.query.model.ValueExpressionList;


@ExtendWith(MockitoExtension.class)
public class SetFunctionSpecificationGeneratorUnitTest {

	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private SetFunctionSpecificationGenerator mockSetFunctionSpecificationGenerator;
	
	
	@Test
	public void testGenerateWithLong() throws ParseException {
		SetFunctionSpecification element = new SetFunctionSpecification(true);
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_INTEGER_BYTES_AS_STRING))
				.build());
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithDouble() throws ParseException {
		SetFunctionSpecification element = 
				new SetFunctionSpecification(SetFunctionType.AVG, null, null, null, null);
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_BYTES_AS_STRING))
				.build());
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithMatchesTypeWithLong() throws ParseException {
		ValueExpressionList list = new ValueExpressionList();
		list.addValueExpression(new TableQueryParser("foo").valueExpression());
		
		SetFunctionSpecification element = 
				new SetFunctionSpecification(SetFunctionType.MIN, null, list, null, null);
		
		ColumnModel cm = new ColumnModel()
				.setColumnType(ColumnType.INTEGER)
				.setName("foo")
				.setId("111")
				.setMaximumSize(50L);
		
		when(mockTableAndColumnMapper.lookupColumnReference(any()))
				.thenReturn(Optional.of(new SchemaColumnTranslationReference(cm)));
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_INTEGER_BYTES_AS_STRING))
				.build());
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithMatchesTypeWithDouble() throws ParseException {
		ValueExpressionList list = new ValueExpressionList();
		list.addValueExpression(new TableQueryParser("bar").valueExpression());
		
		SetFunctionSpecification element = 
				new SetFunctionSpecification(SetFunctionType.MIN, null, list, null, null);
		
		ColumnModel cm = new ColumnModel()
				.setColumnType(ColumnType.DOUBLE)
				.setName("bar")
				.setId("222")
				.setMaximumSize(50L);
		
		when(mockTableAndColumnMapper.lookupColumnReference(any()))
				.thenReturn(Optional.of(new SchemaColumnTranslationReference(cm)));
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_BYTES_AS_STRING))
				.build());
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
}

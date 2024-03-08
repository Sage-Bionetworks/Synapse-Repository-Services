package org.sagebionetworks.table.cluster.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ArrayFunctionSpecification;

@ExtendWith(MockitoExtension.class)
public class ArrayFunctionSpecificationGeneratorUnitTest {
	
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private ArrayFunctionSpecificationGenerator mockArrayFunctionSpecificationGenerator;
	
	@Test
	public void testGenerateWithUnnest() throws ParseException {
		ArrayFunctionSpecification element = new TableQueryParser("UNNEST(foo)").arrayFunctionSpecification();
		
		ColumnModel cm = new ColumnModel()
				.setColumnType(ColumnType.STRING)
				.setName("foo")
				.setId("111")
				.setMaximumSize(50L);
		
		when(mockTableAndColumnMapper.lookupColumnReference(element.getColumnReference()))
				.thenReturn(Optional.of(new SchemaColumnTranslationReference(cm)));
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(50L)
				.build());
		
		assertEquals(expected, mockArrayFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithUnnestWithEmptyColumnTranslationReference() throws ParseException {
		ArrayFunctionSpecification element = new TableQueryParser("UNNEST(foo)").arrayFunctionSpecification();
		
		when(mockTableAndColumnMapper.lookupColumnReference(element.getColumnReference()))
				.thenReturn(Optional.empty());
		
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, mockArrayFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
}

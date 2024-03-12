package org.sagebionetworks.table.cluster.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.SetFunctionType;

@ExtendWith(MockitoExtension.class)
public class SetFunctionSpecificationGeneratorUnitTest {

	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private SetFunctionSpecificationGenerator mockSetFunctionSpecificationGenerator;
	
	
	@Test
	public void testGenerateWithLong() throws ParseException {
		SetFunctionSpecification element = new TableQueryParser("COUNT(*)").setFunctionSpecification();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING))
				.build());
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithDouble() throws ParseException {
		SetFunctionSpecification element = new TableQueryParser("AVG(someCol)").setFunctionSpecification();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
				.setMaximumSize(Long.valueOf(ColumnConstants.MAX_DOUBLE_CHARACTERS_AS_STRING))
				.build());
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithUnimplementedCase() throws ParseException {
		SetFunctionSpecification element = new SetFunctionSpecification(SetFunctionType.JSON_ARRAYAGG, null, null, null, null);
		
		Optional<ElementStats> expected = Optional.empty();
		
		assertEquals(expected, mockSetFunctionSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
}

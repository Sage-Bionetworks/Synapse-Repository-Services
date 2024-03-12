package org.sagebionetworks.table.cluster.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.UnsignedValueSpecification;

@ExtendWith(MockitoExtension.class)
public class UnsignedValueSpecificationGeneratorUnitTest {

	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private UnsignedValueSpecificationGenerator unsignedValueSpecificationGenerator;
	
	
	@Test
	public void testGenerate() throws ParseException {
		UnsignedValueSpecification element = new TableQueryParser("12345").unsignedValueSpecification();
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
	            .setMaximumSize(5L)
	            .build());
		
		assertEquals(expected, unsignedValueSpecificationGenerator.generate(element, mockTableAndColumnMapper));
	}
	
}

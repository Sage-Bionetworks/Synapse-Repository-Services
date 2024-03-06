package org.sagebionetworks.table.cluster.stats;

import static org.junit.Assert.assertEquals;
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
import org.sagebionetworks.table.query.model.ColumnReference;


@ExtendWith(MockitoExtension.class)
public class ColumnReferenceGeneratorUnitTest {
	
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private ColumnReferenceGenerator columnReferenceGenerator;

	
	@Test
	public void testGenerate() throws ParseException {
		ColumnReference element = new TableQueryParser("foo").columnReference();
		
		ColumnModel cm = new ColumnModel()
				.setColumnType(ColumnType.STRING)
				.setName("foo")
				.setId("111")
				.setMaximumSize(50L);
		
		when(mockTableAndColumnMapper.lookupColumnReference(element))
				.thenReturn(Optional.of(new SchemaColumnTranslationReference(cm)));
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
	            .setMaximumSize(50L)
	            .build());
		
		assertEquals(expected, columnReferenceGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithEmptyColumnTranslationReference() throws ParseException {
		ColumnReference element = new TableQueryParser("foo").columnReference();
		
		when(mockTableAndColumnMapper.lookupColumnReference(element))
				.thenReturn(Optional.empty());

		Optional<ElementStats> expected = Optional.empty();

		assertEquals(expected, columnReferenceGenerator.generate(element, mockTableAndColumnMapper));
	}
}

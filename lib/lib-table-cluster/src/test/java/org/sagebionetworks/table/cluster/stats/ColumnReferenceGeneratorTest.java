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
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.util.SqlElementUtils;


@ExtendWith(MockitoExtension.class)
public class ColumnReferenceGeneratorTest {
	
	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private TableAndColumnMapper mockTableAndColumnMapper;
	
	@InjectMocks
	private ColumnReferenceGenerator columnReferenceGenerator;

	
	@Test
	public void testGenerate() throws ParseException {
		ColumnReference element = SqlElementUtils.createColumnReference("foo");
		
		ColumnModel cm = new ColumnModel()
				.setColumnType(ColumnType.STRING)
				.setId("111")
				.setName("_C111_")
				.setMaximumSize(50L);
		
		when(mockTableAndColumnMapper.lookupColumnReference(element))
				.thenReturn(Optional.of(new SchemaColumnTranslationReference(cm)));
		
		Optional<ElementStats> expected = Optional.of(ElementStats.builder()
	            .setMaximumSize(50L)
	            .build());
		
		assertEquals(expected, columnReferenceGenerator.generate(element, mockTableAndColumnMapper));
	}
	
	@Test
	public void testGenerateWithNoColumnTranslationReference() throws ParseException {
		ColumnReference element = SqlElementUtils.createColumnReference("foo");
		
		when(mockTableAndColumnMapper.lookupColumnReference(element))
				.thenReturn(Optional.empty());

		Optional<ElementStats> expected = Optional.empty();

		assertEquals(expected, columnReferenceGenerator.generate(element, mockTableAndColumnMapper));
	}
}

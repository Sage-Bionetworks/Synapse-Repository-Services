package org.sagebionetworks.table.cluster.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

@ExtendWith(MockitoExtension.class)
public class ElementsStatsTest {

	@InjectMocks
	private ElementsStats elementsStats;
	
	@Test
	public void testSumStats() throws JSONObjectAdapterException {
		ElementsStats one = ElementsStats.builder()
	            .setMaximumSize(1L)
	            .setMaxListLength(10L)
	            .setDefaultValue("default1")
	            .setFacetType(FacetType.enumeration)
	            .setEnumValues(List.of("enum1"))
	            .setJsonSubColumns(List.of(new JsonSubColumnModel(new JSONObjectAdapterImpl("{key: \"one\"}"))))
	            .build();

		ElementsStats two = ElementsStats.builder()
	            .setMaximumSize(2L)
	            .setMaxListLength(20L)
	            .setDefaultValue("default2")
	            .setFacetType(FacetType.range)
	            .setEnumValues(List.of("enum2"))
	            .setJsonSubColumns(List.of(new JsonSubColumnModel(new JSONObjectAdapterImpl("{key: \"two\"}"))))
	            .build();
		
		
		ElementsStats expected = ElementsStats.builder()
	            .setMaximumSize(3L)
	            .setMaxListLength(20L)
	            .setDefaultValue("default2")
	            .setFacetType(FacetType.range)
	            .setEnumValues(List.of("enum2"))
	            .setJsonSubColumns(List.of(new JsonSubColumnModel(new JSONObjectAdapterImpl("{key: \"two\"}"))))
	            .build();

		assertEquals(expected, ElementsStats.generateSumStats(one, two));
	}
	
	@Test
	public void testAddLongWithNullBothNull() {
		assertEquals(null, ElementsStats.addLongsWithNull(null, null));
	}
	
	@Test
	public void testAddLongWithNullFirstNull() {
		assertEquals(Long.valueOf(123), ElementsStats.addLongsWithNull(null, 123L));
	}
	
	@Test
	public void testAddLongWithNullSecondNull() {
		assertEquals(Long.valueOf(123), ElementsStats.addLongsWithNull(123L, null));
	}
	
	@Test
	public void testAddLongWithNullNeitherNull() {
		assertEquals(Long.valueOf(4), ElementsStats.addLongsWithNull(3L, 1L));
	}
	
	@Test
	public void testFirstNonNullBothNull() {
		assertNull(ElementsStats.lastNonNull(null, null));
	}
	
	@Test
	public void testFirstNonNullFirstNull() {
		assertEquals(Long.valueOf(1), ElementsStats.lastNonNull(Long.valueOf(1), null));
	}
	
	@Test
	public void testFirstNonNullSecondNull() {
		assertEquals(Long.valueOf(2), ElementsStats.lastNonNull(null, Long.valueOf(2)));
	}
	
	@Test
	public void testFirstNonNullNeitherNull() {
		assertEquals(Long.valueOf(2), ElementsStats.lastNonNull(Long.valueOf(1), Long.valueOf(2)));
	}
}

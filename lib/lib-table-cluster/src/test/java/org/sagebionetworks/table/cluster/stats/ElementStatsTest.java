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
public class ElementStatsTest {

	@InjectMocks
	private ElementStats elementStats;
	
	@Test
	public void testGenerateSumStats() throws JSONObjectAdapterException {
		ElementStats one = ElementStats.builder()
	            .setMaximumSize(1L)
	            .setMaxListLength(10L)
	            .setDefaultValue("default1")
	            .setFacetType(FacetType.enumeration)
	            .setEnumValues(List.of("enum1"))
	            .setJsonSubColumns(List.of(new JsonSubColumnModel(new JSONObjectAdapterImpl("{key: \"one\"}"))))
	            .build();

		ElementStats two = ElementStats.builder()
	            .setMaximumSize(2L)
	            .setMaxListLength(20L)
	            .setDefaultValue("default2")
	            .setFacetType(FacetType.range)
	            .setEnumValues(List.of("enum2"))
	            .setJsonSubColumns(List.of(new JsonSubColumnModel(new JSONObjectAdapterImpl("{key: \"two\"}"))))
	            .build();
		
		
		ElementStats expected = ElementStats.builder()
	            .setMaximumSize(3L)
	            .setMaxListLength(20L)
	            .setDefaultValue("default2")
	            .setFacetType(FacetType.range)
	            .setEnumValues(List.of("enum2"))
	            .setJsonSubColumns(List.of(new JsonSubColumnModel(new JSONObjectAdapterImpl("{key: \"two\"}"))))
	            .build();

		assertEquals(expected, ElementStats.generateSumStats(one, two));
	}
	
	@Test
	public void testAddLongWithNullBothNull() {
		assertEquals(null, ElementStats.addLongsWithNull(null, null));
	}
	
	@Test
	public void testAddLongWithNullFirstNull() {
		assertEquals(Long.valueOf(123), ElementStats.addLongsWithNull(null, 123L));
	}
	
	@Test
	public void testAddLongWithNullSecondNull() {
		assertEquals(Long.valueOf(123), ElementStats.addLongsWithNull(123L, null));
	}
	
	@Test
	public void testAddLongWithNullNeitherNull() {
		assertEquals(Long.valueOf(4), ElementStats.addLongsWithNull(3L, 1L));
	}
	
	@Test
	public void testFirstNonNullBothNull() {
		assertNull(ElementStats.lastNonNull(null, null));
	}
	
	@Test
	public void testFirstNonNullFirstNull() {
		assertEquals(Long.valueOf(1), ElementStats.lastNonNull(Long.valueOf(1), null));
	}
	
	@Test
	public void testFirstNonNullSecondNull() {
		assertEquals(Long.valueOf(2), ElementStats.lastNonNull(null, Long.valueOf(2)));
	}
	
	@Test
	public void testFirstNonNullNeitherNull() {
		assertEquals(Long.valueOf(2), ElementStats.lastNonNull(Long.valueOf(1), Long.valueOf(2)));
	}
}

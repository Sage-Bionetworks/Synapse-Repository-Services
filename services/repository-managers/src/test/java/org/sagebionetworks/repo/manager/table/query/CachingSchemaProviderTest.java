package org.sagebionetworks.repo.manager.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.SchemaProvider;

@ExtendWith(MockitoExtension.class)
public class CachingSchemaProviderTest {

	@Mock
	SchemaProvider mockSchemaProvider;
	
	@InjectMocks
	CachingSchemaProvider cacheProvider;
	
	@Test
	public void testCache() {
		IdAndVersion idOne = IdAndVersion.parse("syn1.1");
		List<ColumnModel> oneSchema = List.of(new ColumnModel().setName("one"));
		when(mockSchemaProvider.getTableSchema(idOne)).thenReturn(oneSchema);
		
		IdAndVersion idTwo = IdAndVersion.parse("syn123");
		List<ColumnModel> twoSchema = List.of(new ColumnModel().setName("two"));
		when(mockSchemaProvider.getTableSchema(idTwo)).thenReturn(twoSchema);

		// call under test
		List<ColumnModel> result = cacheProvider.getTableSchema(idOne);
		assertEquals(oneSchema, result);
		// call under test
		result = cacheProvider.getTableSchema(idOne);
		assertEquals(oneSchema, result);
		// call under test
		result = cacheProvider.getTableSchema(idTwo);
		assertEquals(twoSchema, result);
		// call under test
		result = cacheProvider.getTableSchema(idTwo);
		assertEquals(twoSchema, result);
		
		verify(mockSchemaProvider, times(2)).getTableSchema(any());
		verify(mockSchemaProvider, times(1)).getTableSchema(idOne);
		verify(mockSchemaProvider, times(1)).getTableSchema(idTwo);
	}
	
	@Test
	public void testCacheWithNullProvider() {
		
		mockSchemaProvider = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new CachingSchemaProvider(mockSchemaProvider);
		}).getMessage();
		assertEquals("toWrap is required.", message);
	}
}

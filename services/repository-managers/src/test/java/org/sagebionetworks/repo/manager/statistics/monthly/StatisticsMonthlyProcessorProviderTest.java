package org.sagebionetworks.repo.manager.statistics.monthly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyProcessorProviderTest {

	@Mock
	private StatisticsMonthlyProcessor mockProcessor1;
	
	@Mock
	private StatisticsMonthlyProcessor mockProcessor2;

	private StatisticsMonthlyProcessorProvider provider;

	@Test
	public void testGetProviderResolved() {
		when(mockProcessor1.getSupportedType()).thenReturn(StatisticsObjectType.PROJECT);

		List<StatisticsMonthlyProcessor> processors = Collections.singletonList(mockProcessor1);

		provider = new StatisticsMonthlyProcessorProviderImpl(processors);

		// Call under test
		List<StatisticsMonthlyProcessor> result = provider.getProcessors(StatisticsObjectType.PROJECT);

		assertEquals(processors, result);
	}
	
	@Test
	public void testGetProviderMultiple() {
		when(mockProcessor1.getSupportedType()).thenReturn(StatisticsObjectType.PROJECT);
		when(mockProcessor2.getSupportedType()).thenReturn(StatisticsObjectType.PROJECT);

		List<StatisticsMonthlyProcessor> processors = ImmutableList.of(mockProcessor1, mockProcessor2);

		provider = new StatisticsMonthlyProcessorProviderImpl(processors);

		// Call under test
		List<StatisticsMonthlyProcessor> result = provider.getProcessors(StatisticsObjectType.PROJECT);

		assertEquals(processors, result);
	}


	@Test
	public void testGetProviderunresolved() {
		List<StatisticsMonthlyProcessor> processors = Collections.emptyList();

		provider = new StatisticsMonthlyProcessorProviderImpl(processors);

		// Call under test
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			provider.getProcessors(StatisticsObjectType.PROJECT);
		});
	}
	

}

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

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyProcessorProviderUnitTest {

	@Mock
	private StatisticsMonthlyProcessor mockProcessor;

	private StatisticsMonthlyProcessorProvider provider;

	@Test
	public void testGetProviderResolved() {
		when(mockProcessor.getSupportedType()).thenReturn(StatisticsObjectType.PROJECT);

		List<StatisticsMonthlyProcessor> processors = Collections.singletonList(mockProcessor);

		provider = new StatisticsMonthlyProcessorProviderImpl(processors);

		// Call under test
		StatisticsMonthlyProcessor processor = provider.getMonthlyProcessor(StatisticsObjectType.PROJECT);

		assertEquals(mockProcessor, processor);
	}

	@Test
	public void testGetProviderunresolved() {
		List<StatisticsMonthlyProcessor> processors = Collections.emptyList();

		provider = new StatisticsMonthlyProcessorProviderImpl(processors);

		// Call under test
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			provider.getMonthlyProcessor(StatisticsObjectType.PROJECT);
		});
	}

}

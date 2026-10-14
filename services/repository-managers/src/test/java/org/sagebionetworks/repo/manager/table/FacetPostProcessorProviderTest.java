package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;

@ExtendWith(MockitoExtension.class)
public class FacetPostProcessorProviderTest {

	@Mock
	private FacetPostProcessor mockRoundingProcessor;
	@Mock
	private FacetPostProcessor mockNoiseProcessor;

	@Test
	public void testGetProcessor() {
		when(mockRoundingProcessor.getAlgorithm()).thenReturn(FacetPostProcessingAlgorithm.ROUNDING);
		when(mockNoiseProcessor.getAlgorithm()).thenReturn(FacetPostProcessingAlgorithm.NOISE);
		FacetPostProcessorProvider provider = new FacetPostProcessorProvider(List.of(mockRoundingProcessor, mockNoiseProcessor));

		// call under test
		assertSame(mockRoundingProcessor, provider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING));
		assertSame(mockNoiseProcessor, provider.getProcessor(FacetPostProcessingAlgorithm.NOISE));
	}

	@Test
	public void testGetProcessorWithNullAlgorithm() {
		when(mockRoundingProcessor.getAlgorithm()).thenReturn(FacetPostProcessingAlgorithm.ROUNDING);
		FacetPostProcessorProvider provider = new FacetPostProcessorProvider(List.of(mockRoundingProcessor));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getProcessor(null);
		}).getMessage();
		assertEquals("algorithm is required.", message);
	}

	@Test
	public void testGetProcessorWithUnregisteredAlgorithm() {
		when(mockRoundingProcessor.getAlgorithm()).thenReturn(FacetPostProcessingAlgorithm.ROUNDING);
		// A provider that only knows the ROUNDING processor must fail clearly for NOISE.
		FacetPostProcessorProvider provider = new FacetPostProcessorProvider(List.of(mockRoundingProcessor));

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getProcessor(FacetPostProcessingAlgorithm.NOISE);
		});
	}

}

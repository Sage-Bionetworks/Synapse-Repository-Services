package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

@ExtendWith(MockitoExtension.class)
public class AggregateDataConfigurationValidatorTest {

	@Mock
	private FacetPostProcessorProvider mockFacetPostProcessorProvider;

	@Mock
	private FacetPostProcessor mockFacetPostProcessor;

	@Mock
	private TableManagerSupport mockTableManagerSupport;

	@InjectMocks
	private AggregateDataConfigurationValidator validator;

	private FacetRoundingParameters parameters;
	private AggregateDataConfiguration configuration;

	@BeforeEach
	public void before() {
		parameters = new FacetRoundingParameters().setRoundTo(5L);
		configuration = new AggregateDataConfiguration().setSuppressionThreshold(10L)
				.setFacetPostProcessingConfig(new FacetPostProcessingConfig()
						.setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING).setParameters(parameters));
	}

	@Test
	public void testValidateWithValidConfiguration() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);

		// call under test
		validator.validate(configuration);

		verify(mockFacetPostProcessor).validateParameters(parameters);
	}

	@Test
	public void testValidateWithNullConfiguration() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(null);
		}).getMessage();
		assertEquals("aggregateDataConfiguration is required.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithNullThreshold() {
		// the generated setter rejects null for a required property, so the threshold is simply never set
		configuration = new AggregateDataConfiguration()
				.setFacetPostProcessingConfig(configuration.getFacetPostProcessingConfig());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.suppressionThreshold is required.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithZeroThreshold() {
		configuration.setSuppressionThreshold(0L);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.suppressionThreshold must be greater than zero.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithNegativeThreshold() {
		configuration.setSuppressionThreshold(-1L);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.suppressionThreshold must be greater than zero.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithNullFacetPostProcessingConfig() {
		configuration.setFacetPostProcessingConfig(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.facetPostProcessingConfig is required.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithNullAlgorithm() {
		configuration.getFacetPostProcessingConfig().setAlgorithm(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.facetPostProcessingConfig.algorithm is required.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithNullParameters() {
		configuration.getFacetPostProcessingConfig().setParameters(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.facetPostProcessingConfig.parameters is required.", message);
		verifyNoInteractions(mockFacetPostProcessorProvider);
	}

	@Test
	public void testValidateWithInvalidParameters() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		doThrow(new IllegalArgumentException("roundTo must be a positive value")).when(mockFacetPostProcessor)
				.validateParameters(parameters);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("roundTo must be a positive value", message);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testValidateWithApprovedVirtualTableIds() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		when(mockTableManagerSupport.getTableType(IdAndVersion.parse("syn123"))).thenReturn(TableType.virtualtable);
		when(mockTableManagerSupport.getTableType(IdAndVersion.parse("syn456"))).thenReturn(TableType.virtualtable);
		configuration.setApprovedVirtualTableIds(List.of("syn123", "syn456"));

		// call under test
		validator.validate(configuration);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));
		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn456"));
	}

	@Test
	public void testValidateWithEmptyApprovedVirtualTableIds() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		configuration.setApprovedVirtualTableIds(Collections.emptyList());

		// call under test
		validator.validate(configuration);

		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testValidateWithNullApprovedVirtualTableIds() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		configuration.setApprovedVirtualTableIds(null);

		// call under test
		validator.validate(configuration);

		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testValidateWithNullApprovedVirtualTableId() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		configuration.setApprovedVirtualTableIds(Arrays.asList((String) null));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.approvedVirtualTableIds[] is required.", message);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testValidateWithVersionedApprovedVirtualTableId() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		configuration.setApprovedVirtualTableIds(List.of("syn123.4"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.approvedVirtualTableIds must not include a version: syn123.4", message);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testValidateWithNonVirtualTableApprovedId() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		when(mockTableManagerSupport.getTableType(IdAndVersion.parse("syn123"))).thenReturn(TableType.virtualtable);
		when(mockTableManagerSupport.getTableType(IdAndVersion.parse("syn456"))).thenReturn(TableType.materializedview);
		configuration.setApprovedVirtualTableIds(List.of("syn123", "syn456"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		}).getMessage();
		assertEquals("aggregateDataConfiguration.approvedVirtualTableIds must only include VirtualTables: syn456", message);
	}

	@Test
	public void testValidateWithMalformedApprovedVirtualTableId() {
		when(mockFacetPostProcessorProvider.getProcessor(FacetPostProcessingAlgorithm.ROUNDING)).thenReturn(mockFacetPostProcessor);
		configuration.setApprovedVirtualTableIds(List.of("not-an-id"));

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(configuration);
		});
		verifyNoInteractions(mockTableManagerSupport);
	}

}

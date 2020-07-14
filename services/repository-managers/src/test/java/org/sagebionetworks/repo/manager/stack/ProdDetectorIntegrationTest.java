package org.sagebionetworks.repo.manager.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProdDetectorIntegrationTest {
	
	private static final String PROD_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";
	private static final String STACK_VERSION = "testing";

	@Autowired
	private SimpleHttpClient httpClient;
	
	@Autowired
	private LoggerProvider logProvider;
	
	// We mock the configuration so that we can replace the prod/staging endpoints
	@Mock
	private StackConfiguration stackConfiguration;
	
	// Let the normal injection go through to test the wiring
	@Autowired
	private ProdDetector prodDetector;
	
	@BeforeEach
	public void before() {
		assertNotNull(prodDetector);
	}
	
	@Test
	public void testWithProd() {
		when(stackConfiguration.getStackInstance()).thenReturn(STACK_VERSION);
		when(stackConfiguration.getRepositoryServiceProdEndpoint()).thenReturn(PROD_ENDPOINT);
		
		ProdDetectorImpl prodDetector = new ProdDetectorImpl(httpClient, stackConfiguration, logProvider);
		prodDetector.init();
				
		Optional<Boolean> result = prodDetector.isProductionStack();
		
		assertEquals(Optional.of(Boolean.FALSE), result);
	}
	
	@Test
	public void testWithException() {
		when(stackConfiguration.getRepositoryServiceProdEndpoint()).thenReturn(PROD_ENDPOINT + "/wrongEndpoint");
		
		ProdDetectorImpl prodDetector = new ProdDetectorImpl(httpClient, stackConfiguration, logProvider);
		prodDetector.init();
				
		Optional<Boolean> result = prodDetector.isProductionStack();
		
		assertFalse(result.isPresent());
		
	}
	
}

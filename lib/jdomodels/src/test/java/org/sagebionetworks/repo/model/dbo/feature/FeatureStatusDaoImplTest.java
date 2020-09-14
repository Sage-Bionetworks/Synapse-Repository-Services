package org.sagebionetworks.repo.model.dbo.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.feature.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class FeatureStatusDaoImplTest {
	
	@Autowired
	private FeatureStatusDao featureStatusDao;

	@BeforeEach
	public void before() {
		featureStatusDao.clear();
	}
	
	@AfterEach
	public void after() {
		featureStatusDao.clear();
	}
	
	@Test
	public void testIsEnabledWithoutRecords() {
		// Call under test
		Optional<Boolean> result = featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void testIsEnabledWithEnabled() {
		
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, true);
		
		// Call under test
		Optional<Boolean> result = featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION);
		
		assertTrue(result.isPresent());
		assertTrue(result.get());
	}
	
	@Test
	public void testIsEnabledWithDisabled() {
		
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, false);
		
		// Call under test
		Optional<Boolean> result = featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION);
		
		assertTrue(result.isPresent());
		assertFalse(result.get());
	}
	
	@Test
	public void testDisabledFeature() {
		
		Optional<Boolean> result = featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION);

		assertFalse(result.isPresent());
		
		// Enable
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, true);
		
		assertTrue(featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION).get());

		// Disable
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, false);
		
		assertFalse(featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION).get());

		// Re-Enable
		featureStatusDao.setFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION, true);
		
		assertTrue(featureStatusDao.isFeatureEnabled(Feature.DATA_ACCESS_AUTO_REVOCATION).get());
	}
	
}

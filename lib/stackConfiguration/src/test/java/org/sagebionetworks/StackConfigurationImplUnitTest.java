package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *  To test methods in StackConfigurationImpl that are not directly related to the stack
 *
 *  This class is missing a lot of tests! Please add them :)
 */
@RunWith(MockitoJUnitRunner.class)
public class StackConfigurationImplUnitTest {
	
	StackConfigurationImpl config;

	@Mock
	ConfigurationProperties mockProperties;
	
	@Before
	public void before() {
		this.config = new StackConfigurationImpl(mockProperties);
	}

	@Test
	public void testGetDoiPrefixOnProd() {
		String stackName = "prod";
		String prefix = "a prefix";
		when(mockProperties.getProperty(StackConstants.STACK_PROPERTY_NAME)).thenReturn(stackName); // config.isProduction() will return true
		String doiPrefixPropertyName = "org.sagebionetworks.doi.prefix";
		when(mockProperties.getProperty(DOI_PREFIX_PROPERTY)).thenReturn(prefix);

		// Call under test
		String actualPrefix = config.getDoiPrefix();

		assertEquals(prefix, actualPrefix);
		verify(mockProperties).getProperty(StackConstants.STACK_PROPERTY_NAME);
		verify(mockProperties, never()).getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		verify(mockProperties).getProperty(doiPrefixPropertyName);

	}

	@Test
	public void testGetDoiPrefixNotOnProd() {
		String stackPropertyName = "testNotProd";
		String stackInstancePropertyName = "testStack";
		String prefix = "a prefix";
		when(mockProperties.getProperty(StackConstants.STACK_PROPERTY_NAME)).thenReturn(stackPropertyName); // config.isProduction() will return false
		when(mockProperties.getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME)).thenReturn(stackInstancePropertyName);
		String doiPrefixPropertyName = "org.sagebionetworks.doi.prefix";
		when(mockProperties.getProperty(doiPrefixPropertyName)).thenReturn(prefix);

		String expectedPrefix = prefix + "/" + stackInstancePropertyName;

		// Call under test
		String actualPrefix = config.getDoiPrefix();

		assertEquals(expectedPrefix, actualPrefix);
		verify(mockProperties).getProperty(StackConstants.STACK_PROPERTY_NAME);
		verify(mockProperties).getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
		verify(mockProperties).getProperty(doiPrefixPropertyName);
	}
	
	@Test
	public void testGetSharedS3BackupBucket() {
		String stackName = "prod";
		when(mockProperties.getProperty(StackConstants.STACK_PROPERTY_NAME)).thenReturn(stackName);
		when(mockProperties.getProperty("org.sagebionetworks.shared.s3.backup.bucket")).thenReturn(".bucket");
		assertEquals("prod.bucket", config.getSharedS3BackupBucket());
	}


	private static final String DOI_PREFIX_PROPERTY = "org.sagebionetworks.doi.prefix";
}

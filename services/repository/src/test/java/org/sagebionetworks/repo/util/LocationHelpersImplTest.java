/**
 * 
 */
package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LocationHelpersImplTest {

	@Autowired
	LocationHelper helper;

	private static final String INTEGRATION_TEST_READ_ONLY_USER_ID = "integration.test@sagebase.org";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#createS3Url(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateS3Url() throws Exception {

		String url = helper.createS3Url(INTEGRATION_TEST_READ_ONLY_USER_ID,
				"/test/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz",
				"33183779e53ce0cfc35f59cc2a762cbd", "application/binary");
		assertNotNull(url);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#getS3Url(java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetS3Url() throws Exception {

		String url = helper.getS3Url(INTEGRATION_TEST_READ_ONLY_USER_ID,
				"/test/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertNotNull(url);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#getS3HeadUrl(java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetS3HeadUrl() throws Exception {

		String url = helper.getS3HeadUrl(INTEGRATION_TEST_READ_ONLY_USER_ID,
				"/test/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertNotNull(url);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testGetS3KeyFromS3Url() throws Exception {
		String s3Key = helper
				.getS3KeyFromS3Url("https://s3.amazonaws.com/" + StackConfiguration.getS3Bucket() + "/4678/0.0.0/mskcc_prostate_cancer.phenotype.zip?Expires=1314977993&AWSAccessKeyId=AKIAIYIHFAWJF4R4QKKQ&Signature=MYh%2BgZLdKKMOyIZGq7fDL%2BSvFJ4%3D");
		assertEquals("/4678/0.0.0/mskcc_prostate_cancer.phenotype.zip", s3Key);
	}
}

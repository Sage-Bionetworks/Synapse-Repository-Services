/**
 * 
 */
package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	private static final Long TEST_USER_ID = 2938475L;
	private static final Long OTHER_TEST_USER_ID = 9378952L;

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
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#presignS3PUTUrl(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateS3Url() throws Exception {

		String url = helper.presignS3PUTUrl(TEST_USER_ID,
				"/9876/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz",
				"33183779e53ce0cfc35f59cc2a762cbd", "application/binary");
		assertNotNull(url);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#presignS3GETUrl(java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetS3Url() throws Exception {

		String url = helper.presignS3GETUrl(TEST_USER_ID,
				"/9876/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertNotNull(url);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#presignS3HEADUrl(java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetS3HeadUrl() throws Exception {

		String url = helper.presignS3HEADUrl(TEST_USER_ID,
				"/9123123123/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

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
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testGetEntityIdFromS3Url() throws Exception {
		assertEquals(KeyFactory.keyToString(123L), helper.getEntityIdFromS3Url("/123/blahblah"));
		assertEquals(KeyFactory.keyToString(123L), helper.getEntityIdFromS3Url("123/blahblah"));
		assertEquals(KeyFactory.keyToString(123L), helper.getEntityIdFromS3Url("https://s3.amazonaws.com/" + StackConfiguration.getS3Bucket() + "/123/blahblah"));
	}
	
	@Test
	public void testCache() throws Exception {
		
		String getUrl = helper.presignS3GETUrl(TEST_USER_ID, "/123/foo.zip", 6);
		String headUrl = helper.presignS3HEADUrl(TEST_USER_ID, "/123/foo.zip", 6);
		assertFalse(getUrl.equals(headUrl));

		// If 2 of the 6 seconds have elapsed, the urls are still "fresh enough" to reuse
		Thread.sleep(1000);  
		
		// Test Cache Hits
		assertTrue(getUrl.equals(helper.presignS3GETUrl(TEST_USER_ID, "/123/foo.zip", 6)));
		assertTrue(headUrl.equals(helper.presignS3HEADUrl(TEST_USER_ID, "/123/foo.zip", 6)));
		
		// Test Cache Misses due to different users
		assertFalse(getUrl.equals(helper.presignS3GETUrl(OTHER_TEST_USER_ID, "/123/foo.zip", 6)));
		assertFalse(headUrl.equals(helper.presignS3HEADUrl(OTHER_TEST_USER_ID, "/123/foo.zip", 6)));

		// Test Cache Misses due to different s3Keys
		assertFalse(getUrl.equals(helper.presignS3GETUrl(TEST_USER_ID, "/123/foo.tgz", 6)));
		assertFalse(headUrl.equals(helper.presignS3HEADUrl(TEST_USER_ID, "/123/foo.tgz", 6)));

		// Now that 4+ of the 6 seconds have elapsed, the urls are NOT "fresh enough" to reuse
		Thread.sleep(3000);  
		
		// Test Cache Misses due to timing
		assertFalse(getUrl.equals(helper.presignS3GETUrl(TEST_USER_ID, "/123/foo.zip", 6)));
		assertFalse(headUrl.equals(helper.presignS3HEADUrl(TEST_USER_ID, "/123/foo.zip", 6)));
	}
	
}

/**
 * 
 */
package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

	private static final Logger log = Logger
			.getLogger(LocationHelpersImplTest.class.getName());

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
	 * {@link org.sagebionetworks.repo.util.LocationHelpersImpl#createS3Url(java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateS3Url() throws Exception {

		String url = helper.createS3Url(
				LocationHelpersImpl.INTEGRATION_TEST_READ_ONLY_USER_ID,
				"/test/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz",
				"33183779e53ce0cfc35f59cc2a762cbd");

		// TODO find a more direct way to go from hex to base64
		byte[] encoded = Base64.encodeBase64(Hex
				.decodeHex("33183779e53ce0cfc35f59cc2a762cbd".toCharArray()));
		String base64Md5 = new String(encoded, "ASCII");
		String localFilepath = "/Users/deflaux/platform/trunk/tools/tcgaWorkflow/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz";
		log.info("curl -v -X PUT -H Content-Type:application/binary " 
				+ " -H Content-MD5:" + base64Md5
				+ " -H x-amz-acl:bucket-owner-full-control "
				+ " --data-binary @" + localFilepath + " '" + url + "'");

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

		String url = helper.getS3Url(
				LocationHelpersImpl.INTEGRATION_TEST_READ_ONLY_USER_ID,
				"/test/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		String localFilepath = "/var/tmp/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz";
		log.info("curl -v -o " + localFilepath + " '" + url + "'");

		assertNotNull(url);
	}

}

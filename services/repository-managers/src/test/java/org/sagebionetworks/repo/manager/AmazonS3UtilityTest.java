package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AmazonS3UtilityTest {
	
	@Autowired
	AmazonS3Utility s3Utility;
	
	private String keyToDelete;
	
	@Before
	public void before() throws Exception {
		this.keyToDelete = null;
	}
	
	@After
	public void after() throws Exception {
		if (keyToDelete!=null && s3Utility!=null) {
			s3Utility.deleteFromS3(keyToDelete);
			this.keyToDelete = null;
		}
	}

	@Test
	public void testRoundTrip() {
		String content = "Now is the time of every good man to come to the aid of his party.";
		String testKey = "org_sagebionetworks_repo_manager_AmazonS3UtilityTest";
		s3Utility.uploadStringToS3File(testKey, content, null);
		this.keyToDelete = testKey;
		String retrieved = s3Utility.downloadFromS3ToString(testKey);
		assertEquals(content, retrieved);
	}

}

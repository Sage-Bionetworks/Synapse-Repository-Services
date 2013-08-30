package org.sagebionetworks.logging.s3;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This is a an integration test for the log sweeper.
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:log-sweeper.spb.xml" })
public class LogSweeperTest {

	@Autowired
	LogSweeperFactory logSweeperFactory;
	@Autowired
	AmazonS3Client s3Client;
	
	@Before
	public void before(){
		// Make sure the bucket and the local directory start off empty
	}
	
	@Test
	public void test(){
		List<String> keys = logSweeperFactory.sweepLogs();
		System.out.println(keys);
	}
}

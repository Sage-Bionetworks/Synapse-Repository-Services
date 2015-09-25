package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynapseEmailServiceImplTest {
	
	private static String BUCKET = null;
	
	private String s3KeyToDelete;

	@Autowired
	private SynapseEmailService sesClient;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	@BeforeClass
	public static void before() throws Exception {
		BUCKET = StackConfiguration.getS3Bucket();
	}
	
	@After
	public void after() throws Exception {
		S3TestUtils.doDeleteAfter(s3Client);
	}

	@Test
	public void testWriteToFile() throws Exception {
		String to = UUID.randomUUID().toString()+"@foo.bar";
		s3KeyToDelete = to+".json";
		assertFalse(S3TestUtils.doesFileExist(BUCKET, s3KeyToDelete, s3Client, 2000L));
		S3TestUtils.addObjectToDelete(BUCKET, s3KeyToDelete);
		SendRawEmailRequest emailRequest = new SendRawEmailRequest();
		emailRequest.setDestinations(Collections.singletonList(to));
		Message message = new Message();

		RawMessage rawMessage = new RawMessage();
		rawMessage.setData(ByteBuffer.wrap("my dog has fleas".getBytes()));
		emailRequest.setRawMessage(rawMessage);
		emailRequest.setSource("me@foo.bar");
		sesClient.sendRawEmail(emailRequest);
		assertTrue(S3TestUtils.doesFileExist(BUCKET, s3KeyToDelete, s3Client, 60000L));
	}

}

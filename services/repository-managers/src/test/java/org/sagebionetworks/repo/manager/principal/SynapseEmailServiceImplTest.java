package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynapseEmailServiceImplTest {
	
	private static String BUCKET = null;
	
	private String s3KeyToDelete;

	@Autowired
	private SynapseEmailService sesClient;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	@BeforeClass
	public static void before() throws Exception {
		BUCKET = StackConfigurationSingleton.singleton().getS3Bucket();
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
		Content content = Content.builder().data("my dog has fleas").build();
		Body body = Body.builder().text(content).build();
		Message message = Message.builder().body(body).build();
		Destination destination = Destination.builder().toAddresses(Collections.singletonList(to)).build();
		SendEmailRequest emailRequest = SendEmailRequest.builder()
				.destination(destination)
				.message(message)
				.source("me@foo.bar")
				.build();
		sesClient.sendEmail(emailRequest);
		assertTrue(S3TestUtils.doesFileExist(BUCKET, s3KeyToDelete, s3Client, 60000L));
	}

}

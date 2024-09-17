package org.sagebionetworks.aws.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

public class SynapsePropertyCredentialProviderTest {
	
	@Test
	public void testWithCredentials() {
		Properties props=new Properties();
		String key = "theKey";
		String id = "theId";
		props.put("org.sagebionetworks.stack.iam.key", key);
		props.put("org.sagebionetworks.stack.iam.id", id);
		SynapsePropertyCredentialProvider provider = new SynapsePropertyCredentialProvider("testing", props);
		
		// call under test
		AwsCredentials cred = provider.resolveCredentials();
		AwsBasicCredentials expected = AwsBasicCredentials.create(id, key);
		assertEquals(expected, cred);
	}
	
	@Test
	public void testWithCredentialsWithTrim() {
		Properties props=new Properties();
		String key = "theKey";
		String id = "theId";
		props.put("org.sagebionetworks.stack.iam.key", key+"\n");
		props.put("org.sagebionetworks.stack.iam.id", ""+id+" ");
		SynapsePropertyCredentialProvider provider = new SynapsePropertyCredentialProvider("testing", props);
		
		// call under test
		AwsCredentials cred = provider.resolveCredentials();
		AwsBasicCredentials expected = AwsBasicCredentials.create(id, key);
		assertEquals(expected, cred);
	}

	@Test
	public void testWithNullProperites() {
		SynapsePropertyCredentialProvider provider = new SynapsePropertyCredentialProvider("testing", null);
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			provider.resolveCredentials();
		}).getMessage();
		assertEquals("No properties for name: testing", message);
	}
	
	@Test
	public void testWithEmptyProperites() {
		SynapsePropertyCredentialProvider provider = new SynapsePropertyCredentialProvider("testing", new Properties());
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			provider.resolveCredentials();
		}).getMessage();
		assertEquals("No properties for name: testing", message);
	}
	
	
	@Test
	public void testWithNullId() {
		Properties props=new Properties();
		String key = "theKey";
		String id = "theId";
		props.put("org.sagebionetworks.stack.iam.key", key);
		SynapsePropertyCredentialProvider provider = new SynapsePropertyCredentialProvider("testing", props);
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			provider.resolveCredentials();
		}).getMessage();
		assertEquals("No properties for name: testing", message);
	}
	
	@Test
	public void testWithNullKey() {
		Properties props=new Properties();
		String key = "theKey";
		String id = "theId";
		props.put("org.sagebionetworks.stack.iam.id", id);
		SynapsePropertyCredentialProvider provider = new SynapsePropertyCredentialProvider("testing", props);
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			provider.resolveCredentials();
		}).getMessage();
		assertEquals("No properties for name: testing", message);
	}
	

}

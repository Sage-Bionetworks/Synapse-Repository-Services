package org.sagebionetworks.aws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.ParameterProviderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

@RunWith(MockitoJUnitRunner.class)
public class ParameterProviderImplTest {

	@Mock
	AWSSimpleSystemsManagement ssmClient;

	@Mock
	AWSKMS keyManagementClient;

	@Mock
	StackConfiguration config;

	@Captor
	ArgumentCaptor<GetParameterRequest> getParameterRequestCaptor;

	ParameterProviderImpl provider;

	@Before
	public void before() {

		when(config.getStack()).thenReturn("dev");
		when(config.getStackInstance()).thenReturn("test");

		when(ssmClient.getParameter(any(GetParameterRequest.class)))
				.thenReturn(new GetParameterResult().withParameter(new Parameter().withValue("value")));

		provider = new ParameterProviderImpl();
		ReflectionTestUtils.setField(provider, "ssmClient", ssmClient);
		ReflectionTestUtils.setField(provider, "keyManagementClient", keyManagementClient);
		ReflectionTestUtils.setField(provider, "config", config);
	}

	@Test
	public void testCreateFullKeyName() {
		// call under test
		String fullKey = provider.createFullKeyName("org.sagebionetworks.key");
		assertEquals("dev.test.org.sagebionetworks.key", fullKey);
	}

	@Test
	public void testStringToByteBuffer() {
		String input = "input string";
		// call under test
		ByteBuffer buffer = ParameterProviderImpl.stringToByteBuffer(input);
		String clone = ParameterProviderImpl.byteBufferToString(buffer);
		assertEquals(input, clone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetValueNull() {
		// call under test
		provider.getValue(null);
	}

	@Test
	public void testGetValue() {
		String key = "someKey";
		String value = provider.getValue(key);
		assertEquals("value", value);;
		verify(ssmClient).getParameter(getParameterRequestCaptor.capture());
		GetParameterRequest request = getParameterRequestCaptor.getValue();
		assertEquals("dev.test.someKey", request.getName());
	}
}

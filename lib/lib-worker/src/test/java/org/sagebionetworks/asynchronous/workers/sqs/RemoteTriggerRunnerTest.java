package org.sagebionetworks.asynchronous.workers.sqs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.remote.RemoteTriggerRunner;
import org.sagebionetworks.asynchronous.workers.remote.RemoteTriggerWorkerStackConfiguration;
import org.sagebionetworks.asynchronous.workers.remote.SQSMessageProvider;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.common.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class RemoteTriggerRunnerTest {

	@Mock
	private StackConfiguration mockStackConfig;
	
	@Mock
	private RemoteTriggerWorkerStackConfiguration mockTriggerConfig;
	
	@Mock
	private SynapseAdminClient mockClient;
	
	@Mock
	private SQSMessageProvider mockMessageProvider;
	
	@Mock
	private ProgressCallback mockCallback;

	private RemoteTriggerRunner runner;
	
	private static final String QUEUE = "queue";
	private static final String KEY = "key";
	private static final String SECRET = "secret";
	
	@BeforeEach
	public void before() {

		when(mockTriggerConfig.getQueueName()).thenReturn(QUEUE);
		when(mockTriggerConfig.getMessageProvider()).thenReturn(mockMessageProvider);
		
		runner = new RemoteTriggerRunner(mockTriggerConfig, mockStackConfig, mockClient);
		
		verify(mockTriggerConfig).getQueueName();
		verify(mockTriggerConfig).getMessageProvider();
	}
	
	@Test
	public void testRun() throws Exception {
		String messageBody = "someMessage";
		
		when(mockMessageProvider.getMessageBody(any())).thenReturn(messageBody);
		when(mockStackConfig.getServiceAuthKey(any())).thenReturn(KEY);
		when(mockStackConfig.getServiceAuthSecret(any())).thenReturn(SECRET);
		
		// Call under test
		runner.run(mockCallback);
		
		verify(mockMessageProvider).getMessageBody(mockCallback);
		verify(mockStackConfig).getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		verify(mockStackConfig).getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);
		verify(mockClient).setBasicAuthorizationCredentials(KEY, SECRET);
		verify(mockClient).sendSQSMessage(QUEUE, messageBody);
		verify(mockClient).removeAuthorizationHeader();	
	}
	
	@Test
	public void testRunThrowing() throws Exception {
		String messageBody = "someMessage";
		
		when(mockMessageProvider.getMessageBody(any())).thenReturn(messageBody);
		when(mockStackConfig.getServiceAuthKey(any())).thenReturn(KEY);
		when(mockStackConfig.getServiceAuthSecret(any())).thenReturn(SECRET);
		
		doThrow(SynapseClientException.class).when(mockClient).sendSQSMessage(any(), any());
		
		// Call under test
		runner.run(mockCallback);
		
		verify(mockMessageProvider).getMessageBody(mockCallback);
		verify(mockStackConfig).getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		verify(mockStackConfig).getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);
		verify(mockClient).setBasicAuthorizationCredentials(KEY, SECRET);
		verify(mockClient).sendSQSMessage(QUEUE, messageBody);
		verify(mockClient).removeAuthorizationHeader();	
	}
	
}

package org.sagebionetworks.dynamo.workers;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.dynamo.workers.sqs.DynamoQueueWorker;
import org.sagebionetworks.file.worker.PreviewWorker;
import org.sagebionetworks.repo.manager.dynamo.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.ObjectType;

import com.amazonaws.services.sqs.model.Message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DynamoQueueWorkerTest {
	
	NodeTreeUpdateManager mockUpdateManager;
	Consumer mockConsumer;
	WorkerLogger mockWorkerLogger;
	List<Message> inputList;
	
	@Before
	public void before() {
		Assume.assumeTrue(StackConfiguration.singleton().getDynamoEnabled());
		mockUpdateManager = Mockito.mock(NodeTreeUpdateManager.class);
		mockConsumer = Mockito.mock(Consumer.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		inputList = new LinkedList<Message>();
	}

	@Test
	public void testSkipNonEntityMsgs() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("id");
		chgMsg.setObjectType(ObjectType.EVALUATION);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		inputList.add(msg);
		DynamoQueueWorker worker = new DynamoQueueWorker(inputList, mockUpdateManager, mockConsumer, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		assertNotNull(processedMsgs);
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
	}
	
	@Test
	public void testWorkerLoggerCalled() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("id");
		chgMsg.setObjectType(ObjectType.ENTITY);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		inputList.add(msg);
		RuntimeException e = new RuntimeException("Error in create");
		Mockito.doThrow(e).when(mockUpdateManager).create(chgMsg.getObjectId(), chgMsg.getParentId(), chgMsg.getTimestamp());
		DynamoQueueWorker worker = new DynamoQueueWorker(inputList, mockUpdateManager, mockConsumer, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		verify(mockWorkerLogger).logWorkerFailure(DynamoQueueWorker.class, chgMsg, e, true);
		assertNotNull(processedMsgs);
		assertEquals(0, processedMsgs.size());
	}

}

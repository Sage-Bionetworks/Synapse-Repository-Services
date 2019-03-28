package org.sagebionetworks.repo.manager.audit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.audit.dao.ObjectRecordBatch;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class ObjectRecordQueueImplTest {

	@Mock
	ObjectRecordDAO mockObjectRecordDAO;

	ObjectRecordQueueImpl queue;

	ObjectRecord one;
	ObjectRecord two;
	ObjectRecord three;
	ObjectRecord four;

	ObjectRecordBatch batchOne;
	ObjectRecordBatch batchTwo;
	ObjectRecordBatch batchThree;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		queue = new ObjectRecordQueueImpl();
		ReflectionTestUtils.setField(queue, "objectRecordDAO",
				mockObjectRecordDAO);

		one = new ObjectRecord();
		one.setTimestamp(new Long(1));

		two = new ObjectRecord();
		two.setTimestamp(new Long(2));

		three = new ObjectRecord();
		three.setTimestamp(new Long(3));

		four = new ObjectRecord();
		four.setTimestamp(new Long(4));

		batchOne = new ObjectRecordBatch(Lists.newArrayList(one, two),
				"typeOne");
		batchTwo = new ObjectRecordBatch(Lists.newArrayList(three), "typeTwo");
		batchThree = new ObjectRecordBatch(Lists.newArrayList(four), "typeOne");
	}

	@Test
	public void testPushAndFire() throws IOException {
		// push all three batches to the queue
		queue.pushObjectRecordBatch(batchOne);
		queue.pushObjectRecordBatch(batchTwo);
		queue.pushObjectRecordBatch(batchThree);

		// simulate timer fired
		queue.timerFired();
		// two batches should be sent to S3
		verify(mockObjectRecordDAO).saveBatch(
				Lists.newArrayList(one, two, four), "typeOne");
		verify(mockObjectRecordDAO).saveBatch(Lists.newArrayList(three),
				"typeTwo");
	}

	@Test
	public void testNoPushWithFire() throws IOException {
		// timer fired with nothing pushed
		queue.timerFired();
		// no save batches.
		verify(mockObjectRecordDAO, never()).saveBatch(
				anyListOf(ObjectRecord.class), anyString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPushNull() {
		queue.pushObjectRecordBatch(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPushNoType() {
		ObjectRecordBatch batch = new ObjectRecordBatch(Lists.newArrayList(one,
				two), null);
		queue.pushObjectRecordBatch(batch);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testPushNullList() {
		ObjectRecordBatch batch = new ObjectRecordBatch(null, "typeOne");
		queue.pushObjectRecordBatch(batch);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testPushEmptyList() {
		ObjectRecordBatch batch = new ObjectRecordBatch(new LinkedList<ObjectRecord>(), "typeOne");
		queue.pushObjectRecordBatch(batch);
	}
	
	@Test
	public void testPushFailed() throws IOException{
		// setup a failure
		when(mockObjectRecordDAO.saveBatch(anyListOf(ObjectRecord.class), anyString())).thenThrow(new IOException("Something went wrong"));
		// add to the queue.
		queue.pushObjectRecordBatch(batchOne);
		assertEquals(1, queue.getQueueSize());
		queue.timerFired();
		assertEquals(0, queue.getQueueSize());
	}
}

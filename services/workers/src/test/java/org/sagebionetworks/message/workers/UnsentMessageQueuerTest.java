package org.sagebionetworks.message.workers;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.UnsentMessageRange;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UnsentMessageQueuerTest {

	@Autowired
	private UnsentMessageQueuer unsentMessageQueuer;
	
	@Autowired
	private MessageQueue unsentMessageQueue;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	@Autowired
	private UnsentMessageQueuerTestHelper unsentMessageQueuerTestHelper;
	
	@Before
	public void setup() throws Exception {
		unsentMessageQueuer.setApproxRangeSize(1000L);
		changeDAO.deleteAllChanges();
	}

	@After
	public void teardown() throws Exception {
		changeDAO.deleteAllChanges();
	}
	
	@Test
	public void testRangeAllQueued() throws Exception {
		// Create a ton of messages (~50% gaps)
		List<ChangeMessage> batch = unsentMessageQueuerTestHelper.createList(10000, ObjectType.ENTITY, 1234, 5678);
		batch = changeDAO.replaceChange(batch);
		
		// All these should fall within the range that is queued up
		Set<Long> changeNumbers = unsentMessageQueuerTestHelper.convertBatchToRange(batch);
		
		// The range size must be adjustable, so make sure a variety of values work
		long[] rangeSizes = new long[] { 17L, 83L, 299L, 1000L, 7331L };
		for (int r = 0; r < rangeSizes.length; r++) {
			// Get the elements that would be queued
			unsentMessageQueuer.setApproxRangeSize(rangeSizes[r]);
			List<SendMessageBatchRequestEntry> queued = unsentMessageQueuer.buildRangeBatch();
			
			// Convert the queued elements into ranges
			List<UnsentMessageRange> ranges = new ArrayList<UnsentMessageRange>();
			for (int i = 0; i < queued.size(); i++) {
				ranges.add(extractMessageBody(queued.get(i)));
			}
			
			// Make sure the entire range of change numbers is accounted for
			Set<Long> outaRange = new HashSet<Long>(changeNumbers);
			removeRangeFromSet(outaRange, ranges);
			assertTrue("Not queued: " + outaRange, outaRange.isEmpty());
		}
	}
	
	private UnsentMessageRange extractMessageBody(SendMessageBatchRequestEntry message) {
		JSONObject object;
		try {
			object = new JSONObject(message.getMessageBody());
			JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(object);
			return new UnsentMessageRange(adapter);
		} catch (JSONException e) {
			throw new RuntimeException("Could not parse the message: " + message.getMessageBody());
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException("Could not convert the message: " + message.getMessageBody());
		}
	}
	
	@Test
	public void testRoundtrip() throws Exception {
		// Create a sizable number of messages (~10% gaps)
		List<ChangeMessage> batch = unsentMessageQueuerTestHelper.createList(5000, ObjectType.ENTITY, 12345, 67890);
		batch = changeDAO.replaceChange(batch);

		// All these should fall within the range that is queued up
		Set<Long> changeNumbers = unsentMessageQueuerTestHelper.convertBatchToRange(batch);
		
		unsentMessageQueuerTestHelper.emptyQueue(unsentMessageQueue.getQueueUrl());
		unsentMessageQueuer.run();
		List<UnsentMessageRange> ranges = unsentMessageQueuerTestHelper.emptyQueue(unsentMessageQueue.getQueueUrl());
		
		// Make sure the entire range of change numbers is accounted for
		removeRangeFromSet(changeNumbers, ranges);
		assertTrue("Not queued: " + changeNumbers, changeNumbers.isEmpty());
	}
	
	private void removeRangeFromSet(Set<Long> outaRange, List<UnsentMessageRange> ranges) {
		for (int i = 0; i < ranges.size(); i++) {
			UnsentMessageRange range = ranges.get(i);
			long lower = range.getLowerBound();
			long upper = range.getUpperBound();
			for (Long j = lower; j <= upper; j++) {
				outaRange.remove(j);
			}
		}
	}
}

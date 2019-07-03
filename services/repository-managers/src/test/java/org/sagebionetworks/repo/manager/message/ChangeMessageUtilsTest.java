package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ChangeMessageUtilsTest {

	@Test
	public void testMaxMessages() throws Exception {
		// Calculate the size in bytes for a change messages string.
		ChangeMessage change = new ChangeMessage();
		change.setChangeNumber(Long.MAX_VALUE);
		change.setChangeType(ChangeType.UPDATE);
		change.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		change.setObjectId("" + Long.MAX_VALUE);
		change.setTimestamp(new Date(292278993));
		ChangeMessages messages = new ChangeMessages();
		messages.setList(Arrays.asList(change));
		String json = EntityFactory.createJSONStringForEntity(messages);
		int jsonSizeInBytes = json.getBytes("UTF-8").length;
		System.out.println("ChangeMessages JSON size in bytes: "
				+ jsonSizeInBytes);
		int bytesForMaxMessages = jsonSizeInBytes * ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE;
		assertTrue(
				"The MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE is too large. The resulting SQS message body would be: "+bytesForMaxMessages+" bytes",
				bytesForMaxMessages < ChangeMessageUtils.MAX_SQS_MESSAGES_SIZE_BYTES);
	}

	@Test
	public void testGroupMessagesByObjectTypeAndPartitionEachGroup() {
		ChangeMessage one = new ChangeMessage();
		one.setObjectType(ObjectType.ENTITY);
		one.setObjectId("one");
		ChangeMessage two = new ChangeMessage();
		two.setObjectType(ObjectType.FILE);
		two.setObjectId("two");
		ChangeMessage three = new ChangeMessage();
		three.setObjectType(ObjectType.ENTITY);
		three.setObjectId("three");
		ChangeMessage four = new ChangeMessage();
		four.setObjectType(ObjectType.PRINCIPAL);
		four.setObjectId("four");
		ChangeMessage five = new ChangeMessage();
		five.setObjectType(ObjectType.ENTITY);
		five.setObjectId("five");
		List<ChangeMessage> batch = Arrays.asList(one, two, three, four, five);

		Map<ObjectType, List<List<ChangeMessage>>> groups = ChangeMessageUtils
				.groupByObjectTypeAndPartitionEachGroup(batch, 2);
		assertNotNull(groups);
		assertEquals(3, groups.size());
		// entity should have two
		List<List<ChangeMessage>> groupPartitions = groups
				.get(ObjectType.ENTITY);
		// There are a total three entity messages. With a partition size of 2,
		// there should be two in the first partition and one in the last.
		assertEquals(2, groupPartitions.size());
		assertEquals(Arrays.asList(one, three), groupPartitions.get(0));
		assertEquals(Arrays.asList(five), groupPartitions.get(1));
		// file should have one
		groupPartitions = groups.get(ObjectType.FILE);
		assertEquals(1, groupPartitions.size());
		assertEquals(Arrays.asList(two), groupPartitions.get(0));
		// principal should have one.
		groupPartitions = groups.get(ObjectType.PRINCIPAL);
		assertEquals(1, groupPartitions.size());
		assertEquals(Arrays.asList(four), groupPartitions.get(0));
	}

}

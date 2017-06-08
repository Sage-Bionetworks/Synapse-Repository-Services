package org.sagebionetworks.repo.manager.message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;

import com.google.common.collect.Lists;

public class ChangeMessageUtils {

	/**
	 * See:
	 * http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/
	 * API_SendMessage.html The limit is 256KB (262,144 bytes) according to the
	 * above docs.
	 */
	public static final int MAX_SQS_MESSAGES_SIZE_BYTES = 262 * 1000;

	/**
	 * The maximum number of change messages that can be written to a single
	 * Amazon SQS messages body.
	 */
	public static final int MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE = 500;
	
	/**
	 * The maximum number of IDs that can be written to a single Amazon SQL message body.
	 */
	public static final int MAX_NUMBER_OF_ID_MESSAGES_PER_SQS_MESSAGE = 1000;

	/**
	 * Given an unbounded list of ChangeMessages, first group all change
	 * messages by ObjectType. Then partition each group such that each sub-list
	 * is less than or equal to the max partition size.
	 * 
	 * @param batch
	 * @param maxPartitionSize
	 * @return
	 */
	public static Map<ObjectType, List<List<ChangeMessage>>> groupByObjectTypeAndPartitionEachGroup(
			List<ChangeMessage> batch, int maxPartitionSize) {
		// Group all messages by type
		Map<ObjectType, List<ChangeMessage>> typeGroupMap = new HashMap<ObjectType, List<ChangeMessage>>();
		for (ChangeMessage change : batch) {
			ObjectType type = change.getObjectType();
			if (type == null) {
				throw new IllegalArgumentException("Type cannot be null");
			}
			List<ChangeMessage> group = typeGroupMap.get(type);
			if (group == null) {
				group = new LinkedList<ChangeMessage>();
				typeGroupMap.put(type, group);
			}
			group.add(change);
		}
		// Partition each group.
		Map<ObjectType, List<List<ChangeMessage>>> results = new HashMap<ObjectType, List<List<ChangeMessage>>>();
		for (ObjectType groupType : typeGroupMap.keySet()) {
			List<ChangeMessage> group = typeGroupMap.get(groupType);
			results.put(groupType, Lists.partition(group, maxPartitionSize));
		}
		return results;
	}

}

package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.message.ChangeType;

public class BroadcastMessageBuilderUtil {

	public static String getAction(ChangeType changeType) {
		if(ChangeType.CREATE == changeType){
			return "created";
		}else if(ChangeType.UPDATE == changeType){
			return "updated";
		}else{
			return "removed";
		}
	}

	/**
	 * Truncate a string to the given max length if needed.
	 * @param toTruncate
	 * @param maxLength
	 * @return
	 */
	public static String truncateString(String toTruncate, int maxLength){
		if(toTruncate.length() <= maxLength){
			return toTruncate;
		}else{
			return toTruncate.substring(0, maxLength)+"...";
		}
	}
}

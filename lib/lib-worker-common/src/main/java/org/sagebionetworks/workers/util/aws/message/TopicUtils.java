package org.sagebionetworks.workers.util.aws.message;

import java.util.List;

public class TopicUtils {

	/**
	 * Check to see if the policyString already give permission to all topics.
	 * This method assumes that the policyString was returned from AWS SQS client
	 * and has a valid format. 
	 * 
	 * @param policyString
	 * @param topicsToSubscribe
	 * @return 	true if all topics appears in the policyString,
	 * 			false otherwise.
	 */
	public static boolean containsAllTopics(String policyString,
			List<String> topicsToSubscribe) {
		for (String topic : topicsToSubscribe){
			if (policyString.indexOf(topic) < 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Generate the SourceArn string from a list of topics
	 * 
	 * @param topicsArns
	 * @throws IllegalArgumentException
	 * @return the string that represents the topicsArns
	 */
	public static String generateSourceArn(List<String> topicsArns) {
		if (topicsArns == null || topicsArns.size() < 1) {
			throw new IllegalArgumentException();
		}
		
		char quote = '"';
		String comma = ", ";
		
		if (topicsArns.size() == 1) {
			return quote + topicsArns.get(0) + quote;
		}
		String sourceArn = "[ " + quote + topicsArns.get(0) + quote;
		for (int i = 1; i < topicsArns.size(); i++) {
			sourceArn += comma + quote + topicsArns.get(i) + quote;
		}
		sourceArn += " ]";
		return sourceArn;
	}
}

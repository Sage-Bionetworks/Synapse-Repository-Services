package org.sagebionetworks.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.aws.MultiFactorAuthenticationCredentialProvider;
import org.sagebionetworks.repo.model.message.ChangeMessage;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import au.com.bytecode.opencsv.CSVWriter;;

/**
 * This class will purge the give SQS queue and record each deleted message to a
 * CVS file.
 *
 */
public class PurgeAndRecordSqsQueue {

	public static void main(String[] args) throws IOException {
		if (args == null || args.length < 1) {
			throw new IllegalArgumentException(
					"args[0]=mfaDeviceArn, args[1]=queueName, args[2]=queueOwnerAWSAccountId");
		}

		String mfaDeviceArn = args[0];
		String queueName = args[1];
		String queueOwnerAWSAccountId = args[2];

		AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1)
				.withCredentials(new MultiFactorAuthenticationCredentialProvider(mfaDeviceArn)).build();
		// lookup the queue url
		GetQueueUrlResult urlRequest = sqsClient.getQueueUrl(
				new GetQueueUrlRequest().withQueueName(queueName).withQueueOwnerAWSAccountId(queueOwnerAWSAccountId));
		String queueUrl = urlRequest.getQueueUrl();
		System.out.println("Reading messages from queue: " + queueUrl);

		File temp = File.createTempFile("PurgeAndRecordSqsQueue", ".csv");
		System.out.println("Writing results to: " + temp.getAbsolutePath());
		
		try (FileOutputStream fos = new FileOutputStream(temp);
				Writer writer = new OutputStreamWriter(fos, "UTF-8");
				CSVWriter csv = new CSVWriter(writer);) {
			// start with a header
			String[] line = new String[] { "objectId", "objectType", "changeTimestamp", "changeNumber", "objectEtag",
					"changeType", "userId" };
			csv.writeNext(line);

			// keep reading messages until no more messages are avaiable.
			int count = 0;
			while (true) {
				ReceiveMessageResult rmr = sqsClient
						.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10));
				if (rmr.getMessages() == null || rmr.getMessages().isEmpty()) {
					System.out.println("Received an empty message");
					return;
				}
//				if (count > 1) {
//					System.out.println("Reached max count");
//					return;
//				}

				for (Message message : rmr.getMessages()) {
					// extract all of the changes messages for this message
					List<ChangeMessage> batch = MessageUtils.extractChangeMessageBatch(message);
					// log each message
					for (ChangeMessage change : batch) {
						line[0] = change.getObjectId();
						line[1] = change.getObjectType().name();
						line[2] = "" + change.getTimestamp().getTime();
						line[3] = "" + change.getChangeNumber();
						line[5] = "" + change.getChangeType().name();
						line[6] = "" + change.getUserId();

						csv.writeNext(line);

						message.getAttributes();
						csv.flush();
					}
					sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
					count++;
					System.out.println("Message count: " + count);
				}
				System.out.println("Writing results to: " + temp.getAbsolutePath());
			}
		}

	}

}

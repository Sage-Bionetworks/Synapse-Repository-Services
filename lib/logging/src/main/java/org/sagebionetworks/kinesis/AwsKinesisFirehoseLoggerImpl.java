package org.sagebionetworks.kinesis;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.BufferingHints;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DestinationDescription;
import com.amazonaws.services.kinesisfirehose.model.ExtendedS3DestinationDescription;
import com.amazonaws.services.kinesisfirehose.model.ExtendedS3DestinationUpdate;
import com.amazonaws.services.kinesisfirehose.model.UpdateDestinationRequest;
import com.amazonaws.services.kinesisfirehose.model.UpdateDestinationResult;

@Component
public class AwsKinesisFirehoseLoggerImpl implements AwsKinesisFirehoseLogger {

	private AwsKinesisFirehoseBatchClient kinesisFirehoseBatchClient;
	
	private AwsKinesisLogRecordSerializer kinesisRecordSerializer;
	
	private AmazonKinesisFirehose kinesisFirehoseClient;
	
	private String stack;
	
	private String instance;

	@Autowired
	public AwsKinesisFirehoseLoggerImpl(AwsKinesisFirehoseBatchClient kinesisFirehoseBatchClient,  AwsKinesisLogRecordSerializer kinesisRecordSerializer, AmazonKinesisFirehose kinesisFirehoseClient) {
		this.kinesisFirehoseBatchClient = kinesisFirehoseBatchClient;
		this.kinesisRecordSerializer = kinesisRecordSerializer;
		this.kinesisFirehoseClient = kinesisFirehoseClient;
	}
	
	@Autowired
	public void configure(StackConfiguration stackConfiguration) {
		this.stack = stackConfiguration.getStack();
		this.instance = stackConfiguration.getStackInstance();
	}

	@Override
	public void logBatch(String kinesisDataStreamSuffix, List<? extends AwsKinesisLogRecord> logRecords) {
		ValidateArgument.required(kinesisDataStreamSuffix, "The kinesisDataStreamSuffix");
		ValidateArgument.required(logRecords, "The logRecords");
		
		if (logRecords.isEmpty()) {
			return;
		}
		
		// Override the stack and instance if needed
		logRecords.forEach( record-> {
			if (record.getStack() == null) {
				record.withStack(stack);
			}
			if (record.getInstance() == null) {
				record.withInstance(instance);
			}
		});

		AwsKinesisRecordIterator recordIterator = new AwsKinesisRecordIterator(logRecords, kinesisRecordSerializer, AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT);

		String streamName = kinesisStreamName(kinesisDataStreamSuffix);
		
		List<AwsKinesisRecord> batch = new LinkedList<>();
		
		int batchSize = 0;
		
		while (recordIterator.hasNext()) {
			AwsKinesisRecord next = recordIterator.next();
			
			if (batchSize + next.size() > AwsKinesisFirehoseConstants.REQUEST_SIZE_LIMIT || batch.size() == AwsKinesisFirehoseConstants.PUT_BATCH_MAX_RECORD_LIMIT) {
				kinesisFirehoseBatchClient.sendBatch(streamName, batch);
				batch = new LinkedList<>();
				batchSize = 0;
			}

			// Always add to the next batch
			batch.add(next);
			batchSize += next.size();
			
		}
		
		if (!batch.isEmpty()) {
			kinesisFirehoseBatchClient.sendBatch(streamName, batch);
		}
		
	}
	
	@Override
	public int updateKinesisDeliveryTime(String kinesisDataStreamSuffix, int intervalInSeconds) {
		ValidateArgument.required(kinesisDataStreamSuffix, "The kinesisDataStreamSuffix");
		
		String kinesisStreamName = kinesisStreamName(kinesisDataStreamSuffix);
		
		DescribeDeliveryStreamRequest streamDescriptionRequest = new DescribeDeliveryStreamRequest()
				.withDeliveryStreamName(kinesisStreamName);
		
		DescribeDeliveryStreamResult streamDescription = kinesisFirehoseClient.describeDeliveryStream(streamDescriptionRequest);
		
		List<DestinationDescription> destinations = streamDescription.getDeliveryStreamDescription().getDestinations();
		
		if (destinations.isEmpty()) {
			throw new IllegalArgumentException("The stream " + kinesisStreamName + " does not have any destination");
		}
		DestinationDescription destinationDescription = destinations.iterator().next();
		ExtendedS3DestinationDescription s3Destination = destinationDescription.getExtendedS3DestinationDescription();
		
		if (s3Destination == null) {
			throw new IllegalArgumentException("The stream " + kinesisStreamName + " destination must be an S3 destination");
		}
		
		int currentInverval = s3Destination.getBufferingHints().getIntervalInSeconds();
		
		UpdateDestinationRequest updateDestinationRequest = new UpdateDestinationRequest()
				.withDeliveryStreamName(kinesisStreamName)
				.withCurrentDeliveryStreamVersionId(streamDescription.getDeliveryStreamDescription().getVersionId())
				.withDestinationId(destinationDescription.getDestinationId())
				.withExtendedS3DestinationUpdate(new ExtendedS3DestinationUpdate()
						.withBufferingHints(new BufferingHints().withSizeInMBs(s3Destination.getBufferingHints().getSizeInMBs())
						.withIntervalInSeconds(intervalInSeconds))
				);
		
		kinesisFirehoseClient.updateDestination(updateDestinationRequest);
		
		while (!s3Destination.getBufferingHints().getIntervalInSeconds().equals(intervalInSeconds)) {
			s3Destination = kinesisFirehoseClient.describeDeliveryStream(streamDescriptionRequest).getDeliveryStreamDescription().getDestinations().iterator().next().getExtendedS3DestinationDescription();
		}
		
		return currentInverval;
	}
	
	private String kinesisStreamName(String kinesisDataStreamSuffix) {
		return stack + instance + kinesisDataStreamSuffix;
	}

}

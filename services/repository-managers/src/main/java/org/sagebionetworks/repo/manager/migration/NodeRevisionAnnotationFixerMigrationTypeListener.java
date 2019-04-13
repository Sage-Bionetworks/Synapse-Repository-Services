package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.migration.MigrationType.NODE_REVISION;

import java.util.List;
import java.util.StringJoiner;

import com.amazonaws.services.sqs.AmazonSQSClient;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeRevisionAnnotationFixerMigrationTypeListener implements MigrationTypeListener{
	public static final String QUEUE_BASE_NAME = "TEMPORARY_ANNOTATION_FIX_WORKER_QUEUE";
	public static final int SQS_MESSAGE_MAX_BYTES = 262144;

	@Autowired
	AmazonSQSClient sqsClient;

	@Autowired
	StackConfiguration stackConfiguration;

	String queueUrl;

	int maxBytesPerBatch;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if(type != NODE_REVISION){
			return;
		}

		StringJoiner stringJoiner = createStringJoiner();
		for(D change: delta){
			if(!(change instanceof DBORevision)){
				continue;
			}
			DBORevision dboRevision = (DBORevision) change;
			String message = dboRevision.getOwner() + ";" + dboRevision.getRevisionNumber();

			// Since we area only joining numeric values as strings (ASCII) and ASCII delimiters,
			// it should be safe to assume that String.length() === byte size.
			// We can also safely assume that in this case a single message will never >= SQS_MESSAGE_MAX_BYTES
			if(stringJoiner.length() + message.length() < maxBytesPerBatch) {
				stringJoiner.add(message);
			}else { //if message would exceed length. send out current message and build up a new string joiner
				sqsClient.sendMessage(queueUrl, stringJoiner.toString());
				stringJoiner = createStringJoiner();
				stringJoiner.add(message);
			}
		}

		if(stringJoiner.length() > 0) {
			sqsClient.sendMessage(queueUrl, stringJoiner.toString());
		}

	}

	private static StringJoiner createStringJoiner(){
		return new StringJoiner("\n");
	}

	public void setMaxBytesPerBatch(int maxBytesPerBatch) {
		this.maxBytesPerBatch = maxBytesPerBatch;
	}

	public void init(){
		this.maxBytesPerBatch = SQS_MESSAGE_MAX_BYTES;
		queueUrl = sqsClient.createQueue(stackConfiguration.getQueueName(QUEUE_BASE_NAME)).getQueueUrl();
	}
}

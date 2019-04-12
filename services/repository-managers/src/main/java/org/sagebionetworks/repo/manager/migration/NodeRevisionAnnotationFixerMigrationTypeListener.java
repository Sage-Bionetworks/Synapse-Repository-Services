package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.migration.MigrationType.NODE_REVISION;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQSClient;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeRevisionAnnotationFixerMigrationTypeListener implements MigrationTypeListener{
	public static final String QUEUE_BASE_NAME = "TEMPORARY_ANNOTATION_FIX_WORKER_QUEUE";

	@Autowired
	AmazonSQSClient sqsClient;

	@Autowired
	StackConfiguration stackConfiguration;

	String queueUrl;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if(type != NODE_REVISION){
			return;
		}

		for(D change: delta){
			if(!(change instanceof DBORevision)){
				continue;
			}
			DBORevision dboRevision = (DBORevision) change;
			sqsClient.sendMessage(queueUrl, dboRevision.getOwner() + ";" + dboRevision.getRevisionNumber());
		}
	}

	public void init(){
		queueUrl = sqsClient.createQueue(stackConfiguration.getQueueName(QUEUE_BASE_NAME)).getQueueUrl();
	}
}

package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
public class GridReplicaConnectionManagerImpl implements GridReplicaConnectionManager {

	private static final Logger log = LogManager.getLogger(GridReplicaConnectionManagerImpl.class);

	private final GridDao gridDao;
	private final InternalReplicaToHubEventPublisher publisher;
	private final SnsClient snsClient;
	private final String topicArn;
	private final TransactionSynchronizationProxy transactionSynchronizationProxy;

	public GridReplicaConnectionManagerImpl(GridDao gridDao, InternalReplicaToHubEventPublisher publisher,
			SnsClient snsClient, @Qualifier("gridReplicaChangeTopicArn") String gridReplicaChangeTopicArn,
			TransactionSynchronizationProxy transactionSynchronizationProxy) {
		this.gridDao = gridDao;
		this.publisher = publisher;
		this.snsClient = snsClient;
		this.topicArn = gridReplicaChangeTopicArn;
		this.transactionSynchronizationProxy = transactionSynchronizationProxy;
	}

	@WriteTransaction
	@Override
	public GridReplica createReplica(Long userId, String sessionId, boolean isAgent, EventSource source) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(source, "source");
		return gridDao.createReplica(userId, sessionId, isAgent, source);
	}

	@WriteTransaction
	@Override
	public GridReplica createReplicaAndConnect(Long userId, String sessionId, boolean isAgent, EventSource source) {
		GridReplica replica = createReplica(userId, sessionId, isAgent, source);
		publishConnectEvent(userId, sessionId, replica.getReplicaId(), source);
		return replica;
	}

	void publishConnectEvent(Long userId, String sessionId, Long replicaId, EventSource source) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(source, "source");
		publisher.publishEventAfterCommit(
				new EventContext(EventType.CONNECT, source, UUID.randomUUID().toString()),
				JsonRxMessageType.Notification, "connection",
				new Connection().setGridSessionId(GridUtils.gridSessionIdAsLong(sessionId))
						.setReplicaId(replicaId).setUserId(userId));
	}

	@Override
	public void publishSchemaChangedEvent(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");
		if (transactionSynchronizationProxy.isSynchronizationActive()) {
			transactionSynchronizationProxy.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));
				}
			});
		} else {
			sendChangesToTopic(ReplicaChangeSet.fromSchemaChange(sessionId));
		}
	}

	@Override
	public void sendChangesToTopic(ReplicaChangeSet changeSet) {
		log.info("Publishing replica change set to topic: {} changeSet: {}", topicArn, changeSet);
		snsClient.publish(PublishRequest.builder().targetArn(topicArn).message(changeSet.toJson()).build());
	}
}

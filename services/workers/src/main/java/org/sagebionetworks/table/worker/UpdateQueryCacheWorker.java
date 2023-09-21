package org.sagebionetworks.table.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.QueryCacheManager;
import org.sagebionetworks.repo.model.table.QueryCacheHitEvent;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class UpdateQueryCacheWorker implements TypedMessageDrivenRunner<QueryCacheHitEvent> {

	private ConnectionFactory connectionFactory;
	private QueryCacheManager queryCache;

	@Autowired
	public UpdateQueryCacheWorker(ConnectionFactory connectionFactory, QueryCacheManager queryCache) {
		super();
		this.connectionFactory = connectionFactory;
		this.queryCache = queryCache;
	}

	@Override
	public Class<QueryCacheHitEvent> getObjectClass() {
		return QueryCacheHitEvent.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, QueryCacheHitEvent event)
			throws RecoverableMessageException, Exception {
		TableIndexDAO tableIndexDao = connectionFactory.getFirstConnection();
		queryCache.refreshCachedQuery(tableIndexDao, event.getQueryRequestHash());
	}

}

package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ReplicatedEvent;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ReplicationToViewManagerImpl implements ReplicationToViewManager {

	public static final int MAX_CALLS_PER_RUN = 1000;

	private static final Log LOG = LogFactory.getLog(ReplicationToViewManagerImpl.class);

	private final TableIndexConnectionFactory connectionFactory;
	private final TableManagerSupport tableManagerSupport;
	private final int viewUpdateVisibilityTimeoutSeconds;

	public ReplicationToViewManagerImpl(TableIndexConnectionFactory connectionFactory,
			TableManagerSupport tableManagerSupport, int viewUpdateVisibilityTimeoutSeconds) {
		super();
		this.connectionFactory = connectionFactory;
		this.tableManagerSupport = tableManagerSupport;
		this.viewUpdateVisibilityTimeoutSeconds = viewUpdateVisibilityTimeoutSeconds;
	}

	@Override
	public void objectReplicated(ReplicatedEvent event) {
		ReplicationType objectType = ReplicationType.matchType(event.getReplicatedObjectType()).get();
		TableIndexManager indexManger = connectionFactory.connectToFirstIndex();
		indexManger.getViewsIntersectionForPath(event.getPathIds(), objectType).forEachRemaining(viewId -> {
			LOG.info(String.format("View: syn%d matched for event: %s", viewId, event));
			indexManger.setViewAsNeedsUpdate(viewId, viewUpdateVisibilityTimeoutSeconds);
		});
	}

	/**
	 * Helper to extract a list of longs from the provided JSON array string.
	 * 
	 * @param json
	 * @return
	 */
	static List<Long> parseJSONArray(String json) {
		if (json == null) {
			return Collections.emptyList();
		}
		JSONArray array = new JSONArray(json);
		List<Long> results = new ArrayList<>(array.length());
		for (int i = 0; i < array.length(); i++) {
			results.add(array.getLong(i));
		}
		return results;
	}

	@Override
	public void consumeVisibleViewUpdates() {
		TableIndexManager indexManger = connectionFactory.connectToFirstIndex();

		for (int i = 0; i < MAX_CALLS_PER_RUN; i++) {
			boolean consumed = indexManger.consumeFirstVisibleViewUpdate(viewId -> {
				LOG.info(String.format("Triggering an update for view: syn%d", viewId));
				try {
					tableManagerSupport.triggerIndexUpdate(IdAndVersion.newBuilder().setId(viewId).build());
				} catch (NotFoundException e) {
					// skip views that are not found.
				} catch (Throwable e) {
					LOG.error("Failed to trigger view update:", e);
				}
			});
			if (!consumed) {
				return;
			}
		}
	}

}

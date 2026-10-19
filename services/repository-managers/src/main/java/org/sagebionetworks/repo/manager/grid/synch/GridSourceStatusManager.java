package org.sagebionetworks.repo.manager.grid.synch;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Reports whether the source of a grid session holds changes the grid has not
 * picked up yet, so that a client can prompt the user to synchronize.
 */
@Service
public class GridSourceStatusManager {

	private final GridManager gridManager;
	private final EntityManager entityManager;
	private final GridRecordSetSourceStatusManager recordSetSourceStatusManager;

	public GridSourceStatusManager(GridManager gridManager, EntityManager entityManager,
			GridRecordSetSourceStatusManager recordSetSourceStatusManager) {
		this.gridManager = gridManager;
		this.entityManager = entityManager;
		this.recordSetSourceStatusManager = recordSetSourceStatusManager;
	}

	/**
	 * Compare the source of a grid session against the state the grid was last
	 * synchronized to. What counts as a change is specific to the type of the
	 * source; only a RecordSet source is supported. See
	 * {@link org.sagebionetworks.repo.model.grid.GridSourceStatus} for what the
	 * reported status does and does not promise.
	 *
	 * @param user      the caller, who must have access to both the session and
	 *                  READ access on the source
	 * @param sessionId the grid session to report on
	 * @return the status of the session's source
	 * @throws IllegalArgumentException if the session's source type is not
	 *                                  supported
	 * @throws NotFoundException        if the session has no source
	 */
	public GridSourceStatus getGridSourceStatus(UserInfo user, String sessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(sessionId, "sessionId");

		GridSession session = gridManager.getGridSession(user, sessionId);
		if (session.getSourceEntityId() == null) {
			throw new NotFoundException("Grid session: " + sessionId + " does not have a source");
		}

		Entity source = entityManager.getEntity(user, session.getSourceEntityId());
		if (source instanceof RecordSet recordSet) {
			return recordSetSourceStatusManager.getGridSourceStatus(user, session, recordSet);
		}
		throw new IllegalArgumentException(
				"Grid source status is not available for a grid session with a source of type: "
						+ source.getClass().getSimpleName());
	}
}

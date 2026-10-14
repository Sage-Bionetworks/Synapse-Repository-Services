package org.sagebionetworks.repo.manager.grid.synch;

import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
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
	private final GridRecordSetSourceStatusManager recordSetSourceStatusManager;

	public GridSourceStatusManager(GridManager gridManager,
			GridRecordSetSourceStatusManager recordSetSourceStatusManager) {
		this.gridManager = gridManager;
		this.recordSetSourceStatusManager = recordSetSourceStatusManager;
	}

	/**
	 * Compare the source of a grid session against the state the grid was last
	 * synchronized to. What counts as a change is specific to the type of the
	 * source; only a RecordSet source is supported.
	 * <p>
	 * A result reporting no changes does not promise that a synchronization would
	 * be a no-op. See the status of each source type for what its comparison does
	 * and does not cover.
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
		GridSource source = gridManager.getSessionSource(sessionId)
				.orElseThrow(() -> new NotFoundException("Grid session: " + sessionId + " does not have a source"));

		return switch (source.getType()) {
		case recordset -> recordSetSourceStatusManager.getGridSourceStatus(user, session);
		default -> throw new IllegalArgumentException(
				"Grid source status is not available for a grid session with a source of type: " + source.getType());
		};
	}
}

package org.sagebionetworks.repo.manager.grid.synch;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.entity.RecordSetRowParsing;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceChangeType;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.stereotype.Service;

/**
 * Compares a grid session against its source RecordSet. The comparisons here are
 * specific to a RecordSet and do not generalize to other source types.
 */
@Service
public class GridRecordSetSourceStatusManager {

	private final EntityManager entityManager;
	private final FileHandleManager fileHandleManager;

	public GridRecordSetSourceStatusManager(EntityManager entityManager, FileHandleManager fileHandleManager) {
		this.entityManager = entityManager;
		this.fileHandleManager = fileHandleManager;
	}

	/**
	 * Compare the source RecordSet of a grid session against the revision the grid
	 * was last synchronized to. See
	 * {@link org.sagebionetworks.repo.model.grid.GridSourceStatus} for what the
	 * reported status does and does not promise.
	 *
	 * @param user    the caller, who must have READ access on the source
	 * @param session the grid session to report on
	 * @param current the current revision of the session's source RecordSet
	 * @return the status of the session's source
	 */
	public GridSourceStatus getGridSourceStatus(UserInfo user, GridSession session, RecordSet current) {
		// An EnumSet emits its members in declaration order, so the reported changes are
		// deterministic regardless of the order the comparisons below run in.
		Set<GridSourceChangeType> changes = EnumSet.noneOf(GridSourceChangeType.class);

		// Re-binding a schema does not create a new RecordSet version, so this is
		// compared independently of the version numbers.
		if (SessionSchema.of(entityManager.findBoundSchema(current.getId())).differsFrom(session)) {
			changes.add(GridSourceChangeType.JSON_SCHEMA);
		}
		addRevisionChanges(user, session, current, changes);

		return new GridSourceStatus().setGridSessionId(session.getSessionId()).setSourceEntityId(current.getId())
				.setSessionSourceVersionNumber(session.getSourceEntityVersionNumber())
				.setCurrentSourceVersionNumber(current.getVersionNumber()).setChanges(new ArrayList<>(changes));
	}

	private void addRevisionChanges(UserInfo user, GridSession session, RecordSet current,
			Set<GridSourceChangeType> changes) {
		Long baselineVersion = session.getSourceEntityVersionNumber();
		if (baselineVersion == null) {
			changes.add(GridSourceChangeType.NO_BASELINE);
			return;
		}
		// A saved revision is immutable, so a grid synchronized to the current revision
		// cannot differ from it in anything that revision carries. This is the steady
		// state of a polling client, so it skips reading the baseline.
		if (baselineVersion.equals(current.getVersionNumber())) {
			return;
		}
		RecordSet baseline;
		try {
			baseline = entityManager.getEntityForVersion(user, current.getId(), baselineVersion, RecordSet.class);
		} catch (NotFoundException e) {
			// A baseline revision deleted after the grid synchronized to it leaves nothing
			// to compare against, rather than being an error.
			changes.add(GridSourceChangeType.BASELINE_MISSING);
			return;
		}
		addVersionChanges(changes, baseline, current);
	}

	/**
	 * Each version of a RecordSet carries its own data file handle, CSV descriptor
	 * and upsert key, so the two versions can be compared without reading a CSV.
	 * <p>
	 * None of the three can be edited in place: changing the upsert key or the
	 * descriptor forces a new revision, and a version's data file can be swapped
	 * only for a file with the same MD5. That is why matching MD5s mean the rows
	 * are unchanged even though the file handle ids differ.
	 */
	private void addVersionChanges(Set<GridSourceChangeType> changes, RecordSet baseline, RecordSet current) {
		if (!Objects.equals(baseline.getDataFileHandleId(), current.getDataFileHandleId()) && !fileHandleManager
				.isMatchingMD5(baseline.getDataFileHandleId(), current.getDataFileHandleId())) {
			changes.add(GridSourceChangeType.DATA);
		}
		RecordSetRowParsing baselineParsing = RecordSetRowParsing.of(baseline);
		RecordSetRowParsing currentParsing = RecordSetRowParsing.of(current);
		if (currentParsing.isCsvDescriptorChangedFrom(baselineParsing)) {
			changes.add(GridSourceChangeType.CSV_DESCRIPTOR);
		}
		if (currentParsing.isUpsertKeyChangedFrom(baselineParsing)) {
			changes.add(GridSourceChangeType.UPSERT_KEY);
		}
	}

}

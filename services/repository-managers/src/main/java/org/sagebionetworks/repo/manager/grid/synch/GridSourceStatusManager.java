package org.sagebionetworks.repo.manager.grid.synch;

import static org.sagebionetworks.repo.manager.grid.create.RecordSetCreateGridHandler.DEFAULT_RECORD_SET_CSV_DESCRIPTOR;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceChangeType;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Reports whether the source RecordSet of a grid session has changes the grid
 * has not picked up yet, so that a client can prompt the user to PULL.
 */
@Service
public class GridSourceStatusManager {

	private final GridManager gridManager;
	private final EntityManager entityManager;
	private final FileHandleManager fileHandleManager;

	public GridSourceStatusManager(GridManager gridManager, EntityManager entityManager,
			FileHandleManager fileHandleManager) {
		this.gridManager = gridManager;
		this.entityManager = entityManager;
		this.fileHandleManager = fileHandleManager;
	}

	/**
	 * Compare the current version of a grid session's source RecordSet against
	 * the version the grid was created from or last synchronized to. Only the
	 * metadata of the two versions is compared; no CSV is read. When no changes
	 * are reported, a PULL would change nothing. When changes are reported, a
	 * PULL may still change nothing.
	 *
	 * @param user      the caller, who must have access to the grid session and
	 *                  READ access on its source
	 * @param sessionId the grid session to report on
	 * @return the status of the session's source
	 * @throws IllegalArgumentException if the session's source is not a RecordSet
	 */
	public GridSourceStatus getGridSourceStatus(UserInfo user, String sessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(sessionId, "sessionId");

		GridSession session = gridManager.getGridSession(user, sessionId);
		Entity source = entityManager.getEntity(user, session.getSourceEntityId());
		if (!(source instanceof RecordSet current)) {
			throw new IllegalArgumentException(
					"Grid source status is only available for a grid session with a RecordSet source");
		}

		// An EnumSet keeps the reported changes in declaration order, independent of
		// the order of the comparisons below.
		Set<GridSourceChangeType> changes = EnumSet.noneOf(GridSourceChangeType.class);

		// A schema binding change does not create a new version, so it is compared
		// regardless of the version numbers.
		if (!Objects.equals(findBoundSchemaId(current.getId()), session.getGridJsonSchema$Id())) {
			changes.add(GridSourceChangeType.JSON_SCHEMA);
		}

		Long baselineVersionNumber = session.getSourceEntityVersionNumber();
		if (baselineVersionNumber == null) {
			changes.add(GridSourceChangeType.NO_BASELINE);
		} else {
			findVersion(user, current.getId(), baselineVersionNumber).ifPresentOrElse(
					baseline -> addVersionChanges(changes, baseline, current),
					() -> changes.add(GridSourceChangeType.BASELINE_MISSING));
		}

		return new GridSourceStatus().setGridSessionId(session.getSessionId()).setSourceEntityId(current.getId())
				.setBaselineVersionNumber(baselineVersionNumber).setCurrentVersionNumber(current.getVersionNumber())
				.setHasChanges(!changes.isEmpty()).setChanges(new ArrayList<>(changes));
	}

	/**
	 * Each version of a RecordSet records its own data file handle, CSV descriptor
	 * and upsert key. A version's data can only be swapped for a file with the same
	 * MD5, so different file handles with matching MD5s hold the same data.
	 */
	private void addVersionChanges(Set<GridSourceChangeType> changes, RecordSet baseline, RecordSet current) {
		if (!Objects.equals(baseline.getDataFileHandleId(), current.getDataFileHandleId())
				&& !fileHandleManager.isMatchingMD5(baseline.getDataFileHandleId(), current.getDataFileHandleId())) {
			changes.add(GridSourceChangeType.DATA);
		}
		if (!Objects.equals(csvDescriptorOrDefault(baseline), csvDescriptorOrDefault(current))) {
			changes.add(GridSourceChangeType.CSV_DESCRIPTOR);
		}
		if (!Objects.equals(baseline.getUpsertKey(), current.getUpsertKey())) {
			changes.add(GridSourceChangeType.UPSERT_KEY);
		}
	}

	private String findBoundSchemaId(String recordSetId) {
		return entityManager.findBoundSchema(recordSetId)
				.map(binding -> binding.getJsonSchemaVersionInfo().get$id()).orElse(null);
	}

	private static CsvTableDescriptor csvDescriptorOrDefault(RecordSet recordSet) {
		return Optional.ofNullable(recordSet.getCsvDescriptor()).orElse(DEFAULT_RECORD_SET_CSV_DESCRIPTOR);
	}

	/**
	 * A baseline version that was deleted after the grid synchronized to it leaves
	 * nothing to compare against, which is reported rather than treated as an error.
	 */
	private Optional<RecordSet> findVersion(UserInfo user, String recordSetId, Long versionNumber) {
		try {
			return Optional.of(entityManager.getEntityForVersion(user, recordSetId, versionNumber, RecordSet.class));
		} catch (NotFoundException e) {
			return Optional.empty();
		}
	}
}

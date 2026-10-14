package org.sagebionetworks.repo.manager.grid.synch;

import static org.sagebionetworks.repo.manager.grid.create.RecordSetCreateGridHandler.DEFAULT_RECORD_SET_CSV_DESCRIPTOR;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceChangeType;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
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
	 * was last synchronized to.
	 * <p>
	 * This compares the metadata of the two revisions rather than diffing their
	 * rows, so neither answer predicts exactly what a synchronization would do.
	 * When it reports changes, a synchronization may still change nothing: a CSV
	 * re-saved with different bytes but identical rows is reported as a change.
	 * When it reports no changes, a synchronization would leave the grid's rows
	 * unchanged, but it always re-validates them against the currently bound JSON
	 * schema, whose contents can have changed under an unchanged $id.
	 *
	 * @param user    the caller, who must have READ access on the source
	 * @param session the grid session to report on, already authorized
	 * @return the status of the session's source
	 */
	public GridSourceStatus getGridSourceStatus(UserInfo user, GridSession session) {
		RecordSet current = entityManager.getEntity(user, session.getSourceEntityId(), RecordSet.class);

		// An EnumSet emits its members in declaration order, so the reported changes are
		// deterministic regardless of the order the comparisons below run in.
		Set<GridSourceChangeType> changes = EnumSet.noneOf(GridSourceChangeType.class);

		// Re-binding a schema does not create a new RecordSet version, so this is
		// compared independently of the version numbers. Note that a schema bound
		// without a semantic version keeps the same $id across its versions, so a
		// newly published version of it is invisible here.
		if (!Objects.equals(findBoundSchemaId(current.getId()), session.getGridJsonSchema$Id())) {
			changes.add(GridSourceChangeType.JSON_SCHEMA);
		}

		Long baselineVersion = session.getSourceEntityVersionNumber();
		if (baselineVersion == null) {
			changes.add(GridSourceChangeType.NO_BASELINE);
		} else {
			Optional<RecordSet> baseline = findVersion(user, current.getId(), baselineVersion);
			if (baseline.isEmpty()) {
				changes.add(GridSourceChangeType.BASELINE_MISSING);
			} else {
				addVersionChanges(changes, baseline.get(), current);
			}
		}

		return new GridSourceStatus().setGridSessionId(session.getSessionId()).setSourceEntityId(current.getId())
				.setHasChanges(!changes.isEmpty()).setChanges(new ArrayList<>(changes));
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
	 * A baseline version that no longer exists (deleted after the grid synchronized
	 * to it) leaves nothing to compare against, rather than being an error.
	 */
	private Optional<RecordSet> findVersion(UserInfo user, String recordSetId, Long versionNumber) {
		try {
			return Optional.of(entityManager.getEntityForVersion(user, recordSetId, versionNumber, RecordSet.class));
		} catch (NotFoundException e) {
			return Optional.empty();
		}
	}
}

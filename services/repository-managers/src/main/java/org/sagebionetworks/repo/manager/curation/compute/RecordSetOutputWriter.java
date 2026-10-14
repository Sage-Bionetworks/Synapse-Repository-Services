package org.sagebionetworks.repo.manager.curation.compute;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.stereotype.Service;

/**
 * Shared persistence for compute tasks that produce a CSV destined for a RecordSet. Stores the CSV
 * (already uploaded as a Synapse file handle) as a new version of the destination RecordSet. Used by
 * the sample sheet and RecordSet generation sub-workers so the version handling stays identical
 * between them. The JSON Schema is bound to the RecordSet by the data manager ahead of execution and
 * the binding is entity-scoped, so a new version inherits it without re-binding.
 */
@Service
public class RecordSetOutputWriter {

	private final EntityManager entityManager;

	public RecordSetOutputWriter(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Resolves the $id of the JSON Schema bound to the given RecordSet. The data manager binds the
	 * target schema to the destination RecordSet ahead of execution; the generated CSV must conform to
	 * it.
	 *
	 * @param user        The user performing the read; access is checked as this user.
	 * @param recordSetId The destination RecordSet entity ID.
	 * @return The bound schema's $id.
	 * @throws org.sagebionetworks.repo.web.NotFoundException if no schema is bound to the RecordSet.
	 */
	public String getBoundSchemaId(UserInfo user, String recordSetId) {
		return entityManager.getBoundSchema(user, recordSetId).getJsonSchemaVersionInfo().get$id();
	}

	/**
	 * Store the given file handle as a new version of the destination RecordSet.
	 *
	 * @param user             The user performing the write; all access is checked as this user.
	 * @param recordSetId      The destination RecordSet entity ID.
	 * @param dataFileHandleId The file handle ID of the generated CSV.
	 */
	public void storeCsvAsNewRecordSetVersion(UserInfo user, String recordSetId, String dataFileHandleId) {
		// Store the generated CSV as a new version of the destination RecordSet, preserving its
		// data-manager-configured properties (name, parent, upsertKey). Clear the version label and
		// comment so the DAO assigns a unique label for the new version (see NodeDAOImpl.createNewVersion);
		// reusing the existing label collides on UNIQUE_REVISION_LABEL.
		RecordSet recordSet = entityManager.getEntity(user, recordSetId, RecordSet.class);
		recordSet.setDataFileHandleId(dataFileHandleId);
		recordSet.setVersionLabel(null);
		recordSet.setVersionComment(null);
		entityManager.updateEntity(user, recordSet, true, null);
	}
}

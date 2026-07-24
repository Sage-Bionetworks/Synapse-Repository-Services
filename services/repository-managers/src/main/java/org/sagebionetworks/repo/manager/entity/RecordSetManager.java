package org.sagebionetworks.repo.manager.entity;

import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.service.metadata.EntityEvent;

public interface RecordSetManager {


    /**
     * Validates RecordSet-specific fields. This step will also clear the internally-controlled
     * `validationSummary` and `validationFileHandleId` fields unless the provided EntityEvent
     * has `skipSanitation` set to `true`.
     * @param entity
     * @param event
     */
    public void validateRecordSet(RecordSet entity, EntityEvent event);

    /**
     * Infers the column schema from the RecordSet's bound JSON Schema and
     * binds it both to this revision's immutable snapshot (T{id}_{v}) and
     * to the entity-level default that unversioned queries read (T{id}).
     * Finally, a message is sent to trigger rebuilding the index.
     * <p>
     * If no bound JSON Schema is present, then the RecordSet will not be indexed.
     */
    public void inferSchemaAndBindToIndex(UserInfo userInfo, RecordSet entity);

    /**
     * Mutates the passed RecordSet to fill in the `validationSummary` property.
     * @param entity
     */
    public void updateWithValidationResults(RecordSet entity);
}

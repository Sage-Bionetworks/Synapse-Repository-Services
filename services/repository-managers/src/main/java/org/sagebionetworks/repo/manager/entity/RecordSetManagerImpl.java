package org.sagebionetworks.repo.manager.entity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.service.metadata.EntityEvent;
import org.sagebionetworks.repo.service.metadata.EventType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class RecordSetManagerImpl implements RecordSetManager {

    private final ColumnModelManager columnModelManager;
    private final NodeDAO nodeDao;
    private final TableManagerSupport tableManagerSupport;
    private final EntitySchemaValidationResultDao validationResultDao;
    private final RecordSetSchemaResolver recordSetSchemaResolver;

    @Inject
    public RecordSetManagerImpl(ColumnModelManager columnModelManager, NodeDAO nodeDao,
                                TableManagerSupport tableManagerSupport, EntitySchemaValidationResultDao validationResultDao,
                                RecordSetSchemaResolver recordSetSchemaResolver) {
        this.columnModelManager = columnModelManager;
        this.nodeDao = nodeDao;
        this.tableManagerSupport = tableManagerSupport;
        this.validationResultDao = validationResultDao;
        this.recordSetSchemaResolver = recordSetSchemaResolver;
    }

    @Override
    public void validateRecordSet(RecordSet entity, EntityEvent event) {
        if (EventType.CREATE == event.getType() || EventType.UPDATE == event.getType()) {
            ValidateArgument.requiredNotEmpty(entity.getUpsertKey(), "The upsertKey");

            if (entity.getCsvDescriptor() != null) {
                ValidateArgument.requirement(Boolean.TRUE.equals(entity.getCsvDescriptor().getIsFirstLineHeader()), "The csvDescriptor.isFirstLineHeader must be true.");
            }

            if (!event.skipSanitization()) {
                // The validation summary and file are server-controlled and only set by the
                // grid session export job. A client must not be able to set them directly, so
                // they are stripped here. Trusted internal callers (the exporter) skip
                // sanitization via EntityService.updateEntity(..., skipSanitization=true).
                entity.setValidationSummary(null);
                entity.setValidationFileHandleId(null);
            }
        }
    }

    @Override
    @WriteTransaction
    public void inferSchemaAndBindToIndex(UserInfo userInfo, RecordSet entity) {
        // Only index RecordSets that have a bound JSON Schema. The columns are derived from the bound schema.
        Optional<JsonSchema> jsonSchema = recordSetSchemaResolver.getBoundValidationSchema(entity.getId());
        if (jsonSchema.isPresent()) {
            List<ColumnModel> schema = RecordSetSchemaResolver.getJsonSchemaColumns(jsonSchema.get());
            if (schema.isEmpty()) {
                throw new IllegalArgumentException("Cannot determine the column model schema from the JSON Schema. At least one property must be present.");
            }

            List<ColumnModel> persistedColumns = columnModelManager.createColumnModels(userInfo, schema);
            List<String> columnIds = persistedColumns.stream().map(ColumnModel::getId).collect(Collectors.toList());

            Long id = KeyFactory.stringToKey(entity.getId());
            Long versionNumber = nodeDao.getCurrentRevisionNumber(KeyFactory.keyToString(id));
            IdAndVersion versionedKey = IdAndVersion.newBuilder().setId(id).setVersion(versionNumber).build();
            // Versioned binding preserves the schema for this specific snapshot.
            columnModelManager.bindColumnsToVersionOfObject(columnIds, versionedKey);
            // Default binding serves queries against "syn123" (no version). The provider
            // always fires for the current revision, so this is always correct here.
            columnModelManager.bindColumnsToDefaultVersionOfObject(columnIds, entity.getId());

            // Finally, trigger rebuilding the index.
            triggerIndexRebuild(entity);
        }
    }

    void triggerIndexRebuild(RecordSet entity) {
        // We fire two triggers per create/update:
        //   1. A versioned trigger so this specific revision's immutable
        //      snapshot T{id}_{v} gets built
        //   2. A versionless trigger so the entity-level alias T{id} (the
        //      target for "select * from syn123") gets rebuilt and the
        //      unversioned TableStatus flips to PROCESSING
        // The worker dedupes via isIndexWorkRequired, so a successful build
        // for one message short-circuits the other.
        Long id = KeyFactory.stringToKey(entity.getId());
        Long versionNumber = nodeDao.getCurrentRevisionNumber(KeyFactory.keyToString(id));
        IdAndVersion versionedKey = IdAndVersion.newBuilder().setId(id).setVersion(versionNumber).build();
        IdAndVersion entityKey = IdAndVersion.newBuilder().setId(id).build();
        tableManagerSupport.setTableToProcessingAndTriggerUpdate(versionedKey);
        tableManagerSupport.setTableToProcessingAndTriggerUpdate(entityKey);
    }

    @Override
    public void updateWithValidationResults(RecordSet entity) {
        Long recordSetId = KeyFactory.stringToKey(entity.getId());
        Long recordSetVersion = entity.getVersionNumber();

        validationResultDao
                .getRecordSetValidationSummaryStatistics(recordSetId, recordSetVersion)
                .ifPresent(entity::setValidationSummary);
    }


}

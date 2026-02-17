package org.sagebionetworks.repo.manager.grid.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridAuthorizationManager;
import org.sagebionetworks.repo.manager.grid.SnapshotRowHandler;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.stereotype.Service;

@Service
public class QueryCreateGridHandler implements CreateGridHandler {

	private final GridDao gridDao;
	private final TableQueryManager tableQueryManager;
	private final EntityManager entityManager;
	private final JsonSchemaManager schemaManager;
	private final JsonSchemaValidationManager jsonSchemaValidationManager;
	private final GridAuthorizationManager gridAuthorizationManager;
	private final FileProvider fileProvider;
	private final SynapseS3Client synapseS3Client;
	private final StackConfiguration stackConfig;

	public QueryCreateGridHandler(GridDao gridDao, EntityManager entityManager, TableQueryManager tableQueryManager,
								  JsonSchemaManager schemaManager, JsonSchemaValidationManager jsonSchemaValidationManager,
								  GridAuthorizationManager gridAuthorizationManager, FileProvider fileProvider,
								  SynapseS3Client synapseS3Client, StackConfiguration stackConfig) {
		super();
		this.gridDao = gridDao;
		this.entityManager = entityManager;
		this.tableQueryManager = tableQueryManager;
		this.schemaManager = schemaManager;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
		this.gridAuthorizationManager = gridAuthorizationManager;
		this.fileProvider = fileProvider;
		this.synapseS3Client = synapseS3Client;
		this.stackConfig = stackConfig;
	}

	@Override
	public boolean canCreate(CreateGridRequest request) {
		return request.getInitialQuery() != null;
	}

	@Override
	public CreateGridHandlerResult createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request,
			SnapshotStore snapshotStore) {
		try {
			Query initialQuery = request.getInitialQuery();
			/*
			 * The first query will determine the size of each row and fetch a row sample
			 * that we can use to determine the schema.
			 */
			QueryResultBundle pre = tableQueryManager.querySinglePage(callback, user,
					new Query().setSql(initialQuery.getSql()).setLimit(1L),
					new QueryOptions().withReturnMaxRowsPerPage(true).withRunQuery(true).withReturnSelectColumns(true));
			RowSet rowSet = pre.getQueryResult().getQueryResults();
			String tableId = rowSet.getTableId();

			Optional<String> schemaIdOp = getSchemaId(user, tableId, rowSet.getRows());
			Long maxRowSizeBytes = getMaxRowSizeBytes(pre.getMaxRowsPerPage());

			GridSession session = gridDao.createGridSession(new CreateGridSession().setUserId(user.getId())
					.setSourceId(tableId).setSchemaId(schemaIdOp.orElse(null)).setOwner(request.getOwnerPrincipalId()));
			GridReplica replica = gridDao.createReplica(user.getId(), session.getSessionId(), false,
					EventSource.INTERNAL);

			// Always include the entity etag so it is included in the grid metadata. The
			// etag can be used to merge the
			// grid data back into a Synapse Table or View
			initialQuery.setIncludeEntityEtag(true);

			final Optional<JsonSchema> validationSchema = schemaIdOp.map(schemaManager::getValidationSchema);

			final List<String> columnsRequiredBySchema = validationSchema
					.map(JsonSchema::getRequired)
					.orElse(new ArrayList<>());

			final Map<String, Integer> columnNameToIndex = new HashMap<>();
			List<SelectColumn> selectColumns = pre.getSelectColumns();
			for (int i = 0; i < selectColumns.size(); i++) {
				columnNameToIndex.put(selectColumns.get(i).getName(), i);
			}
			final List<Integer> columnsRequiredBySchemaIndices = selectColumns.stream()
					.filter(cm -> columnsRequiredBySchema.contains(cm.getName()))
					.map(cm -> columnNameToIndex.get(cm.getName()))
					.collect(Collectors.toList());
			
			// ensure only rows are added that the owner can see.
			UserInfo sessionOwner = gridAuthorizationManager.getRowLevelFilterUserInfo(user, session.getSessionId());

			// The second query is a full query to build all of the patches from the query
			// results.
			tableQueryManager.runQueryAsStream(callback, sessionOwner, initialQuery, t -> {
				List<ColumnModel> schema = t.getMainQuery().getTranslator().getSchemaOfSelect();
				return new SnapshotRowHandler(snapshotStore, session.getSessionId(), replica.getReplicaId(), schema,
						columnsRequiredBySchemaIndices, fileProvider, user.getId(),	jsonSchemaValidationManager,
						validationSchema.orElse(null));
			}, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE);
			return new CreateGridHandlerResult().setGridSession(session).setGridReplica(replica);
		} catch (LockUnavilableException | TableUnavailableException e) {
			callback.updateProgress("Waiting for table/view to become available...", 1L, 100L);
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	Optional<String> getSchemaId(UserInfo user, String tableId, List<Row> rows) {
		if (EntityType.entityview.equals(entityManager.getEntityType(tableId)) && rows != null && rows.size() > 0) {
			String firstRowId = KeyFactory.keyToString(rows.get(0).getRowId());
			try {
				JsonSchemaObjectBinding binding = entityManager.getBoundSchema(user, firstRowId);
				return Optional.of(binding.getJsonSchemaVersionInfo().get$id());
			} catch (NotFoundException e) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	/**
	 * Calculate the maximum size of a row given the maximum number of rows per
	 * page. Note: This is a function of the
	 * {@link TableQueryManager#getMaxBytesPerRequest()}.
	 * 
	 * @param maxRowsPerPage
	 * @return
	 */
	Long getMaxRowSizeBytes(Long maxRowsPerPage) {
		if (maxRowsPerPage <= 1L) {
			return Long.MAX_VALUE;
		}
		return this.tableQueryManager.getMaxBytesPerRequest() / maxRowsPerPage;
	}

}

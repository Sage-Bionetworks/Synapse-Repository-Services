package org.sagebionetworks.repo.manager.grid.create;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.PatchRowHandler;
import org.sagebionetworks.repo.manager.grid.PatchStore;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
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
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.stereotype.Service;

@Service
public class QueryCreateGridHandler implements CreateGridHandler {

	private final GridDao gridDao;
	private final TableQueryManager tableQueryManager;
	private final EntityManager entityManager;

	public QueryCreateGridHandler(GridDao gridDao, EntityManager entityManager, TableQueryManager tableQueryManager) {
		super();
		this.gridDao = gridDao;
		this.entityManager = entityManager;
		this.tableQueryManager = tableQueryManager;
	}

	@Override
	public boolean canCreate(CreateGridRequest request) {
		return request.getInitialQuery() != null;
	}

	@Override
	public CreateGridHandlerResult createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request,
			PatchStore patchStore) {
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
					.setSourceId(tableId).setSchemaId(schemaIdOp.orElse(null)));
			GridReplica replica = gridDao.createReplica(user.getId(), session.getSessionId(), false,
					EventSource.INTERNAL);

			// Always include the entity etag so it is included in the grid metadata. The
			// etag can be used to merge the
			// grid data back into a Synapse Table or View
			initialQuery.setIncludeEntityEtag(true);

			// The second query is a full query to build all of the patches from the query
			// results.
			tableQueryManager.runQueryAsStream(callback, user, initialQuery, t -> {
				List<ColumnModel> schema = t.getMainQuery().getTranslator().getSchemaOfSelect();
				return new PatchRowHandler(patchStore, session.getSessionId(), replica.getReplicaId(), schema,
						maxRowSizeBytes);
			});
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

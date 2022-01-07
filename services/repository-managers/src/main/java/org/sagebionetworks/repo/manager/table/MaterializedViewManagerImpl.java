package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterializedViewManagerImpl implements MaterializedViewManager {
	
	private static final long PAGE_SIZE_LIMIT = 1000;

	final private MaterializedViewDao dao;
	final private ColumnModelManager columModelManager;
	final private TableManagerSupport tableMangerSupport;
	final private TransactionalMessenger messagePublisher;
	
	@Autowired
	public MaterializedViewManagerImpl(MaterializedViewDao dao, ColumnModelManager columModelManager, TableManagerSupport tableMangerSupport, TransactionalMessenger messagePublisher) {
		this.dao = dao;
		this.columModelManager = columModelManager;
		this.tableMangerSupport = tableMangerSupport;
		this.messagePublisher = messagePublisher;
	}
	
	@Override
	public void validate(MaterializedView materializedView) {
		ValidateArgument.required(materializedView, "The materialzied view");		

		getQuerySpecification(materializedView.getDefiningSQL()).getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);
		
	}
	
	@Override
	@WriteTransaction
	public void registerSourceTables(IdAndVersion idAndVersion, String definingSql) {
		ValidateArgument.required(idAndVersion, "The id of the materialized view");
		
		QuerySpecification querySpecification = getQuerySpecification(definingSql);
		
		Set<IdAndVersion> newSourceTables = getSourceTableIds(querySpecification);
		Set<IdAndVersion> currentSourceTables = dao.getSourceTablesIds(idAndVersion);
		
		if (!newSourceTables.equals(currentSourceTables)) {
			Set<IdAndVersion> toDelete = new HashSet<>(currentSourceTables);
			
			toDelete.removeAll(newSourceTables);
			
			dao.deleteSourceTablesIds(idAndVersion, toDelete);
			dao.addSourceTablesIds(idAndVersion, newSourceTables);
		}
		
		bindSchemaToView(idAndVersion, querySpecification);
		tableMangerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Override
	public void refreshDependentMaterializedViews(IdAndVersion tableId) {
		ValidateArgument.required(tableId, "The tableId");
		
		PaginationIterator<IdAndVersion> idsItereator = new PaginationIterator<IdAndVersion>(
			(long limit, long offset) -> dao.getMaterializedViewIdsPage(tableId, limit, offset),
			PAGE_SIZE_LIMIT);
		
		idsItereator.forEachRemaining(id -> {
			
			// No need to update a snapshot
			if (id.getVersion().isPresent()) {
				return;
			}
			
			ChangeMessage change = new ChangeMessage()
					.setObjectType(ObjectType.MATERIALIZED_VIEW)
					.setObjectId(id.getId().toString())
					.setChangeType(ChangeType.UPDATE);
			
			messagePublisher.sendMessageAfterCommit(change);
		});
		
	}
	
	/**
	 * Extract the schema from the defining query and bind the results to the provided materialized view.
	 * 
	 * @param idAndVersion
	 * @param definingQuery
	 */
	void bindSchemaToView(IdAndVersion idAndVersion, QuerySpecification definingQuery) {
		SqlQuery sqlQuery = new SqlQueryBuilder(definingQuery).schemaProvider(columModelManager).allowJoins(true)
				.build();
		// create each column as needed.
		List<String> schemaIds = sqlQuery.getSchemaOfSelect().stream()
				.map(c -> columModelManager.createColumnModel(c).getId()).collect(Collectors.toList());
		columModelManager.bindColumnsToVersionOfObject(schemaIds, idAndVersion);
	}
	
	static QuerySpecification getQuerySpecification(String definingSql) {
		ValidateArgument.requiredNotBlank(definingSql, "The definingSQL of the materialized view");
		try {
			return TableQueryParser.parserQuery(definingSql);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
		
	static Set<IdAndVersion> getSourceTableIds(QuerySpecification querySpecification) {
		Set<IdAndVersion> sourceTableIds = new HashSet<>();
		
		for (TableNameCorrelation table : querySpecification.createIterable(TableNameCorrelation.class)) {
			sourceTableIds.add(IdAndVersion.parse(table.getTableName().toSql()));
		}
		
		return sourceTableIds;
	}

	@Override
	public List<String> getSchemaIds(IdAndVersion idAndVersion) {
		return columModelManager.getColumnIdsForTable(idAndVersion);
	}

}

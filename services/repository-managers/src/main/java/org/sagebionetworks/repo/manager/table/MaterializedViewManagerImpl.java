package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterializedViewManagerImpl implements MaterializedViewManager {

	final private MaterializedViewDao dao;
	final private ColumnModelManager columModelManager;
	
	@Autowired
	public MaterializedViewManagerImpl(MaterializedViewDao dao, ColumnModelManager columModelManager) {
		this.dao = dao;
		this.columModelManager = columModelManager;
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
		
		if (newSourceTables.equals(currentSourceTables)) {
			return;
		}
		
		Set<IdAndVersion> toDelete = new HashSet<>(currentSourceTables);
		
		toDelete.removeAll(newSourceTables);
		
		dao.deleteSourceTablesIds(idAndVersion, toDelete);
		dao.addSourceTablesIds(idAndVersion, newSourceTables);
		
	}
	
	void bindSchemaToView(IdAndVersion idAndVersion, QuerySpecification querySpecification) {
		Long userId = -1L;
		SqlQuery sqlQuery = new SqlQueryBuilder(querySpecification, userId).schemaProvider(columModelManager).allowJoins(true).build();
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

}

package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class VirtualTableManagerImpl implements VirtualTableManager {

	private final ColumnModelManager columModelManager;
	private final TableManagerSupport tableManagerSupport;

	public VirtualTableManagerImpl(ColumnModelManager columModelManager, TableManagerSupport tableManagerSupport) {
		this.columModelManager = columModelManager;
		this.tableManagerSupport = tableManagerSupport;
	}

	@Override
	public void validate(VirtualTable virtualTable) {
		ValidateArgument.required(virtualTable, "The virtualTable");

		String definingSql = virtualTable.getDefiningSQL();

		ValidateArgument.requiredNotBlank(definingSql, "The definingSQL of the virtual table");
		String id = virtualTable.getId() != null? virtualTable.getId() : "syn1";
		// This constructor will do deeper validation...
		new VirtualTableIndexDescription(KeyFactory.idAndVersion(id, virtualTable.getVersionNumber()),
				definingSql, tableManagerSupport);
	}

	@Override
	public void validateDefiningSql(String definingSql) {
		ValidateArgument.required(definingSql, "The definingSQL of the virtual table");
		validateSqlAndGetSchema(definingSql);
	}

	List<ColumnModel> validateSqlAndGetSchema(String definingSql) {
		ValidateArgument.required(definingSql, "The definingSQL of the virtual table");

		try {
			QuerySpecification querySpec = new TableQueryParser(definingSql).querySpecificationEOF();
			List<IdAndVersion> definingIds = querySpec.stream(TableNameCorrelation.class)
					.map(s -> IdAndVersion.parse(s.toSql())).collect(Collectors.toList());
			if(definingIds.size() != 1) {
				throw new IllegalArgumentException("The defining SQL can only reference one table/view");
			}
			IndexDescription definingIndexDescription = tableManagerSupport.getIndexDescription(definingIds.get(0));
			
			QueryTranslator sqlQuery = QueryTranslator.builder().sql(definingSql)
					.schemaProvider(tableManagerSupport).sqlContext(SqlContext.query).indexDescription(definingIndexDescription)
					.build();

			return sqlQuery.getSchemaOfSelect();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public List<String> getSchemaIds(IdAndVersion idAndVersion) {
		return columModelManager.getColumnIdsForTable(idAndVersion);
	}

	@Override
	public void registerDefiningSql(IdAndVersion id, String definingSQL) {
		ValidateArgument.required(id, "table Id");
		ValidateArgument.required(definingSQL, "definingSQL");
		
		List<String> schemaIds = validateSqlAndGetSchema(definingSQL).stream()
				.map(c -> columModelManager.createColumnModel(c).getId()).collect(Collectors.toList());

		columModelManager.bindColumnsToVersionOfObject(schemaIds, id);
	}

}

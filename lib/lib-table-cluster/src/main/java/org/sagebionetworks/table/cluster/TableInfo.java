package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An immutable representation of table information from a SQL query.
 *
 */
public class TableInfo {

	private final String originalTableName;
	private final IdAndVersion tableIdAndVersion;
	private final String tableAlias;
	private final String translatedTableName;
	private final List<ColumnModel> tableSchema;

	public TableInfo(TableNameCorrelation tableNameCorrelation, List<ColumnModel> schema) {
		ValidateArgument.required(tableNameCorrelation, "TableNameCorrelation");
		originalTableName = tableNameCorrelation.getTableName().toSql();
		tableIdAndVersion = IdAndVersion.parse(originalTableName);
		tableAlias = tableNameCorrelation.getTableAlias().orElse(null);
		translatedTableName = SQLUtils.getTableNameForId(tableIdAndVersion, TableType.INDEX);
		this.tableSchema = schema;
	}

	/**
	 * The table name as it appears in the original query.
	 * 
	 * @return the originalTableName
	 */
	public String getOriginalTableName() {
		return originalTableName;
	}

	/**
	 * The IdAndVersion of the original table.
	 */
	public IdAndVersion getTableIdAndVersion() {
		return tableIdAndVersion;
	}

	/**
	 * Table alias will only exist if the original SQL included a table alias.
	 * 
	 * @return the tableAlias
	 */
	public Optional<String> getTableAlias() {
		return Optional.ofNullable(tableAlias);
	}

	/**
	 * The translated name of this table. For example: syn123.3 -> T123_3
	 * 
	 * @return the translatedTableName
	 */
	public String getTranslatedTableName() {
		return translatedTableName;
	}

	/**
	 * Get the schema associated with this table.
	 * @return the tableSchema
	 */
	public List<ColumnModel> getTableSchema() {
		return tableSchema;
	}

}

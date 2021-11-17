package org.sagebionetworks.table.cluster;

import java.util.Optional;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
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

	public TableInfo(TableNameCorrelation tableNameCorrelation) {
		ValidateArgument.required(tableNameCorrelation, "TableNameCorrelation");
		originalTableName = tableNameCorrelation.getTableName().toSql();
		tableIdAndVersion = IdAndVersion.parse(originalTableName);
		tableAlias = tableNameCorrelation.getTableAlias().orElse(null);
		translatedTableName = SQLUtils.getTableNameForId(tableIdAndVersion, TableType.INDEX);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((originalTableName == null) ? 0 : originalTableName.hashCode());
		result = prime * result + ((tableAlias == null) ? 0 : tableAlias.hashCode());
		result = prime * result + ((tableIdAndVersion == null) ? 0 : tableIdAndVersion.hashCode());
		result = prime * result + ((translatedTableName == null) ? 0 : translatedTableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TableInfo)) {
			return false;
		}
		TableInfo other = (TableInfo) obj;
		if (originalTableName == null) {
			if (other.originalTableName != null) {
				return false;
			}
		} else if (!originalTableName.equals(other.originalTableName)) {
			return false;
		}
		if (tableAlias == null) {
			if (other.tableAlias != null) {
				return false;
			}
		} else if (!tableAlias.equals(other.tableAlias)) {
			return false;
		}
		if (tableIdAndVersion == null) {
			if (other.tableIdAndVersion != null) {
				return false;
			}
		} else if (!tableIdAndVersion.equals(other.tableIdAndVersion)) {
			return false;
		}
		if (translatedTableName == null) {
			if (other.translatedTableName != null) {
				return false;
			}
		} else if (!translatedTableName.equals(other.translatedTableName)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "TableInfo [originalTableName=" + originalTableName + ", tableIdAndVersion=" + tableIdAndVersion
				+ ", tableAlias=" + tableAlias + ", translatedTableName=" + translatedTableName + "]";
	}

}

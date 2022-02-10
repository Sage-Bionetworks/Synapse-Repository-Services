package org.sagebionetworks.table.cluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.cluster.columntranslation.RowMetadataColumnTranslationReference;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An immutable representation of table information from a SQL query.
 *
 */
public class TableInfo implements ColumnLookup {

	private final String originalTableName;
	private final IdAndVersion tableIdAndVersion;
	private final String tableAlias;
	private final String translatedTableName;
	private final List<ColumnModel> tableSchema;
	private final List<ColumnTranslationReference> translationReferences;
	private final int tableIndex;
	private final String translatedTableAlias;

	public TableInfo(TableNameCorrelation tableNameCorrelation, int tableIndex, List<ColumnModel> schema) {
		ValidateArgument.required(tableNameCorrelation, "TableNameCorrelation");
		originalTableName = tableNameCorrelation.getTableName().toSql();
		tableIdAndVersion = IdAndVersion.parse(originalTableName);
		tableAlias = tableNameCorrelation.getTableAlias().orElse(null);
		translatedTableName = SQLUtils.getTableNameForId(tableIdAndVersion, TableType.INDEX);
		this.translatedTableAlias = SQLUtils.getTableAliasForIndex(tableIndex);
		this.tableSchema = schema;
		this.tableIndex = tableIndex;
		this.translationReferences = schema.stream().map(c-> new SchemaColumnTranslationReference(c)).collect(Collectors.toList());
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

	/**
	 * The index of the table as listed in the from clause.  The first table in the from clause will be
	 * index = 0 while the last table will be index = n-1.
	 * @return the tableIndex
	 */
	public int getTableIndex() {
		return tableIndex;
	}

	/**
	 * @return the translatedTableAlias
	 */
	public String getTranslatedTableAlias() {
		return translatedTableAlias;
	}

	/**
	 * Attempt to match the given ColumnReference to a column of this table.
	 * @param columnReference
	 * @return
	 */
	@Override
	public Optional<ColumnTranslationReference> lookupColumnReference(ColumnReference columnReference) {
		if (columnReference == null) {
			return Optional.empty();
		}
		Optional<ColumnName> lhsOptional = columnReference.getNameLHS();
		if (lhsOptional.isPresent()) {
			String unquotedLHS = lhsOptional.get().toSqlWithoutQuotes();
			// if we have a LHS it must match either the table name or table alias.
			if (!unquotedLHS.equals(originalTableName) && !unquotedLHS.equals(tableAlias)
					&& !unquotedLHS.equals(translatedTableAlias) && !unquotedLHS.equals(translatedTableName)) {
				return Optional.empty();
			}
		}
		String rhs = columnReference.getNameRHS().toSqlWithoutQuotes();
		Optional<ColumnTranslationReference> optional = translationReferences.stream()
				.filter(t -> rhs.equals(t.getTranslatedColumnName()) || rhs.equals(t.getUserQueryColumnName()))
				.findFirst();
		if (optional.isPresent()) {
			return optional;
		} else {
			// attempt to match to row metadata
			return RowMetadataColumnTranslationReference.lookupColumnReference(rhs);
		}
	}
	
	/**
	 * Does the passed TableNameCorrelation match this TableInfo?
	 * 
	 * @param tableNameCorrelation
	 * @return
	 */
	public boolean isMatch(TableNameCorrelation tableNameCorrelation) {
		String tableNameSql = tableNameCorrelation.getTableName().toSql();
		if (originalTableName.equals(tableNameSql) || translatedTableName.equals(tableNameSql)) {
			if (getTableAlias().equals(tableNameCorrelation.getTableAlias())
					|| translatedTableAlias.equals(tableNameCorrelation.getTableAlias().orElse(null))) {
				return true;
			}	
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(originalTableName, tableAlias, tableIdAndVersion, tableIndex, tableSchema,
				translatedTableName, translationReferences);
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
		return Objects.equals(originalTableName, other.originalTableName)
				&& Objects.equals(tableAlias, other.tableAlias)
				&& Objects.equals(tableIdAndVersion, other.tableIdAndVersion) && tableIndex == other.tableIndex
				&& Objects.equals(tableSchema, other.tableSchema)
				&& Objects.equals(translatedTableName, other.translatedTableName)
				&& Objects.equals(translationReferences, other.translationReferences);
	}

	@Override
	public String toString() {
		return "TableInfo [originalTableName=" + originalTableName + ", tableIdAndVersion=" + tableIdAndVersion
				+ ", tableAlias=" + tableAlias + ", translatedTableName=" + translatedTableName + ", tableSchema="
				+ tableSchema + ", translationReferences=" + translationReferences + ", tableIndex=" + tableIndex + "]";
	}

}

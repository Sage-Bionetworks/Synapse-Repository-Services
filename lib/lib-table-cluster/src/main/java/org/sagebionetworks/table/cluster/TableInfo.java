package org.sagebionetworks.table.cluster;

import java.util.List;
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

	public TableInfo(TableNameCorrelation tableNameCorrelation, List<ColumnModel> schema) {
		ValidateArgument.required(tableNameCorrelation, "TableNameCorrelation");
		originalTableName = tableNameCorrelation.getTableName().toSql();
		tableIdAndVersion = IdAndVersion.parse(originalTableName);
		tableAlias = tableNameCorrelation.getTableAlias().orElse(null);
		translatedTableName = SQLUtils.getTableNameForId(tableIdAndVersion, TableType.INDEX);
		this.tableSchema = schema;
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
			if (!unquotedLHS.equals(originalTableName) && !unquotedLHS.equals(tableAlias)) {
				return Optional.empty();
			}
		}
		String rhs = columnReference.getNameRHS().toSqlWithoutQuotes();
		Optional<ColumnTranslationReference> optional = translationReferences.stream()
				.filter(t -> rhs.equals(t.getTranslatedColumnName()) || rhs.equals(t.getUserQueryColumnName()))
				.findFirst();
		if(optional.isPresent()) {
			return optional;
		}else {
			// attempt to match to row metadata
			return RowMetadataColumnTranslationReference.lookupColumnReference(rhs);
		}
	}

}

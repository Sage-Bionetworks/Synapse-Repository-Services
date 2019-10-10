package org.sagebionetworks.table.cluster.columntranslation;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * ColumnTranslationReference derived from a ColumnModel that was associated with a schema
 */
class SchemaColumnTranslationReference implements ColumnTranslationReference{
	private final ColumnType columnType;
	private final String userQueryColumnName;
	private final String translatedColumnName;

	public SchemaColumnTranslationReference(ColumnModel columnModel){
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.required(columnModel.getColumnType(), "columnModel.type");
		ValidateArgument.requiredNotBlank(columnModel.getId(), "columnModel.id");
		ValidateArgument.requiredNotBlank(columnModel.getName(), "columnModel.name");
		this.columnType = columnModel.getColumnType();
		this.userQueryColumnName = columnModel.getName();
		this.translatedColumnName = SQLUtils.getColumnNameForId(columnModel.getId());
	}

	@Override
	public ColumnType getColumnType() {
		return columnType;
	}

	@Override
	public String getUserQueryColumnName() {
		return userQueryColumnName;
	}

	@Override
	public String getTranslatedColumnName() {
		return translatedColumnName;
	}
}

package org.sagebionetworks.table.cluster.columntranslation;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * ColumnTranslationReference derived from a ColumnModel that was associated with a schema
 */
public class SchemaColumnTranslationReference implements ColumnTranslationReference{
	private final ColumnType columnType;
	private final String userQueryColumnName;
	private final String translatedColumnName;
	private final String id;

	public SchemaColumnTranslationReference(ColumnModel columnModel){
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.required(columnModel.getColumnType(), "columnModel.type");
		ValidateArgument.requiredNotBlank(columnModel.getId(), "columnModel.id");
		ValidateArgument.requiredNotBlank(columnModel.getName(), "columnModel.name");
		this.columnType = columnModel.getColumnType();
		this.id = columnModel.getId();
		this.translatedColumnName = SQLUtils.getColumnNameForId(columnModel.getId());
		this.userQueryColumnName = columnModel.getName();
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


	/**
	 * Id of the column
	 * @return Id of the column
	 */
	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SchemaColumnTranslationReference reference = (SchemaColumnTranslationReference) o;
		return columnType == reference.columnType &&
				Objects.equals(userQueryColumnName, reference.userQueryColumnName) &&
				Objects.equals(translatedColumnName, reference.translatedColumnName) &&
				Objects.equals(id, reference.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnType, userQueryColumnName, translatedColumnName, id);
	}
}

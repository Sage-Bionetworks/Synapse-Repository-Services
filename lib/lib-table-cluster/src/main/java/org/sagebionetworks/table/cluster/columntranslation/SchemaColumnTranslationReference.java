package org.sagebionetworks.table.cluster.columntranslation;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * ColumnTranslationReference derived from a ColumnModel that was associated with a schema
 */
public class SchemaColumnTranslationReference implements ColumnTranslationReference {
	private final ColumnType columnType;
	private final String userQueryColumnName;
	private final String translatedColumnName;
	private final String id;
	private final Long maximumSize;
	private final Long maximumListLength;
	private final FacetType facetType;
	private final String defaultValue;
	private final List<JsonSubColumnModel> jsonSubColumns;

	public SchemaColumnTranslationReference(ColumnModel columnModel){
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.required(columnModel.getColumnType(), "columnModel.type");
		ValidateArgument.requiredNotBlank(columnModel.getId(), "columnModel.id");
		ValidateArgument.requiredNotBlank(columnModel.getName(), "columnModel.name");
		this.columnType = columnModel.getColumnType();
		this.id = columnModel.getId();
		this.translatedColumnName = SQLUtils.getColumnNameForId(columnModel.getId());
		this.userQueryColumnName = columnModel.getName();
		this.maximumSize = columnModel.getMaximumSize();
		this.maximumListLength = columnModel.getMaximumListLength();
		this.facetType = columnModel.getFacetType();
		this.defaultValue = columnModel.getDefaultValue();
		this.jsonSubColumns = columnModel.getJsonSubColumns();
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

	@Override
	public Long getMaximumSize() {
		return this.maximumSize;
	}
	/**
	 * Id of the column
	 * @return Id of the column
	 */
	public String getId() {
		return id;
	}
	
	@Override
	public Long getMaximumListLength() {
		return this.maximumListLength;
	}
	
	@Override
	public FacetType getFacetType() {
		return this.facetType;
	}

	@Override
	public String getDefaultValues() {
		return this.defaultValue;
	}
	
	@Override
	public List<JsonSubColumnModel> getJsonSubColumns() {
		return this.jsonSubColumns;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnType, defaultValue, facetType, id, jsonSubColumns, maximumListLength, maximumSize, translatedColumnName,
				userQueryColumnName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SchemaColumnTranslationReference)) {
			return false;
		}
		SchemaColumnTranslationReference other = (SchemaColumnTranslationReference) obj;
		return columnType == other.columnType && Objects.equals(defaultValue, other.defaultValue) && facetType == other.facetType
				&& Objects.equals(id, other.id) && Objects.equals(jsonSubColumns, other.jsonSubColumns)
				&& Objects.equals(maximumListLength, other.maximumListLength) && Objects.equals(maximumSize, other.maximumSize)
				&& Objects.equals(translatedColumnName, other.translatedColumnName)
				&& Objects.equals(userQueryColumnName, other.userQueryColumnName);
	}

	@Override
	public String toString() {
		return "SchemaColumnTranslationReference [columnType=" + columnType + ", userQueryColumnName=" + userQueryColumnName
				+ ", translatedColumnName=" + translatedColumnName + ", id=" + id + ", maximumSize=" + maximumSize + ", maximumListLength="
				+ maximumListLength + ", facetType=" + facetType + ", defaultValue=" + defaultValue + ", jsonSubColumns=" + jsonSubColumns
				+ "]";
	}

}

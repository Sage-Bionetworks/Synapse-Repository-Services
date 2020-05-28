package org.sagebionetworks.repo.manager.table.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DefaultField;
import org.sagebionetworks.repo.model.table.HasViewObjectType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * DTO used to define the default columns for a view that can be defained
 * through the {@link ObjectField} or standard {@link ColumnModel}s.
 * <p>
 * The embedded {@link #builder(ViewObjectType)} should be used in order to
 * build a {@link DefaultColumnModel}.
 * 
 * @author Marco Marasca
 */
public class DefaultColumnModel implements HasViewObjectType {

	private final ViewObjectType objectType;
	private final List<ObjectField> defaultFields;
	private final List<ColumnModel> customFields;

	DefaultColumnModel(ViewObjectType objectType, List<ObjectField> defaultFields,
			List<ColumnModel> customFields) {
		this.objectType = objectType;
		this.defaultFields = defaultFields;
		this.customFields = customFields;
	}

	@Override
	public ViewObjectType getObjectType() {
		return objectType;
	}

	/**
	 * @return The default object fields included
	 */
	public List<ObjectField> getDefaultFields() {
		return defaultFields;
	}

	/**
	 * @return The custom fields included
	 */
	public List<ColumnModel> getCustomFields() {
		return customFields;
	}

	/**
	 * @param columnName The column name
	 * @return An optional with the column model in the custom fields that matches
	 *         the given column name
	 */
	public Optional<ColumnModel> findCustomFieldByColumnName(String columnName) {
		ValidateArgument.requiredNotBlank(columnName, "columnName");
		if (customFields == null || customFields.isEmpty()) {
			return Optional.empty();
		}
		return customFields.stream()
				.filter((model) -> columnName.equals(model.getName()))
				.findFirst();
	}

	/**
	 * @param objectType The type of view object
	 * @return A builder for the default column model
	 */
	public static DefaultColumnModelBuilder builder(ViewObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");
		return new DefaultColumnModelBuilder(objectType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(customFields, defaultFields);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DefaultColumnModel other = (DefaultColumnModel) obj;
		return Objects.equals(customFields, other.customFields) && Objects.equals(defaultFields, other.defaultFields);
	}

	public static class DefaultColumnModelBuilder {

		private final ViewObjectType objectType;
		private List<ObjectField> defaultFields = new ArrayList<>();
		private List<ColumnModel> customFields = new ArrayList<>();

		private DefaultColumnModelBuilder(ViewObjectType objectType) {
			this.objectType = objectType;
		}

		public DefaultColumnModelBuilder withObjectField(ObjectField... fields) {
			if (fields != null) {
				for (ObjectField field : fields) {
					defaultFields.add(field);
				}
			}
			return this;
		}

		public DefaultColumnModelBuilder withCustomField(DefaultField... columns) {
			if (columns != null) {
				for (DefaultField column : columns) {
					customFields.add(map(column));
				}
			}
			return this;
		}

		private ColumnModel map(DefaultField column) {
			ValidateArgument.requiredNotBlank(column.getColumnName(), "columnName");
			ValidateArgument.required(column.getColumnType(), "columnType");

			ColumnModel columnModel = new ColumnModel();
			columnModel.setName(column.getColumnName());
			columnModel.setColumnType(column.getColumnType());
			columnModel.setMaximumSize(column.getMaximumSize());
			columnModel.setEnumValues(column.getEnumValues());
			columnModel.setFacetType(column.getFacetType());

			return columnModel;
		}

		public DefaultColumnModel build() {
			ValidateArgument.requirement(defaultFields != null && !defaultFields.isEmpty(),
					"At least one objectField must be defined");
			return new DefaultColumnModel(objectType, defaultFields, customFields);
		}

	}

}

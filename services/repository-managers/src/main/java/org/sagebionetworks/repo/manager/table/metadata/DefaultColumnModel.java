package org.sagebionetworks.repo.manager.table.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.HasViewObjectType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * DTO used to define the default columns for a view that can be defained
 * through the {@link ObjectField} or standard {@link ColumnModel}s.
 * <p>
 * The embedded {@link #builder(ViewObjectType)} should be used in order to build a
 * {@link DefaultColumnModel}.
 * 
 * @author Marco Marasca
 */
public class DefaultColumnModel implements HasViewObjectType {

	private final ViewObjectType objectType;
	private final List<ObjectField> defaultFields;
	private final List<ColumnModel> customFields;

	public DefaultColumnModel(ViewObjectType objectType, List<ObjectField> defaultFields,
			List<ColumnModel> customFields) {
		this.objectType = objectType;
		this.defaultFields = defaultFields;
		this.customFields = customFields;
	}

	@Override
	public ViewObjectType getObjectType() {
		return objectType;
	}

	public List<ObjectField> getDefaultFields() {
		return defaultFields;
	}

	public List<ColumnModel> getCustomFields() {
		return customFields;
	}

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

		public DefaultColumnModelBuilder withColumnModel(ColumnModel... columnModels) {
			if (columnModels != null) {
				for (ColumnModel model : columnModels) {
					customFields.add(model);
				}
			}
			return this;
		}

		public DefaultColumnModel build() {
			ValidateArgument.requirement(defaultFields != null && !defaultFields.isEmpty(),
					"At least one objectField must be defined");
			return new DefaultColumnModel(objectType, defaultFields, customFields);
		}

	}

}

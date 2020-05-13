package org.sagebionetworks.repo.manager.table.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ObjectField;

public class DefaultColumnModel {

	private final List<ObjectField> defaultFields;
	private final List<ColumnModel> customFields;

	public DefaultColumnModel(List<ObjectField> defaultFields, List<ColumnModel> customFields) {
		this.defaultFields = defaultFields;
		this.customFields = customFields;
	}

	public List<ObjectField> getDefaultFields() {
		return defaultFields;
	}

	public List<ColumnModel> getCustomFields() {
		return customFields;
	}

	public static DefaultColumnModelBuilder builder() {
		return new DefaultColumnModelBuilder();
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

		private List<ObjectField> defaultFields = new ArrayList<>();
		private List<ColumnModel> customFields = new ArrayList<>();

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
			return new DefaultColumnModel(defaultFields, customFields);
		}

	}

}

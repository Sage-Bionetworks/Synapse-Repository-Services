package org.sagebionetworks.table.cluster.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;

public class ObjectFieldModelResolverImpl implements ObjectFieldModelResolver {

	private ObjectFieldTypeMapper fieldTypeProvider;

	public ObjectFieldModelResolverImpl(ObjectFieldTypeMapper fieldTypeProvider) {
		this.fieldTypeProvider = fieldTypeProvider;
	}
	
	@Override
	public List<ColumnModel> getAllColumnModels() {
		List<ColumnModel> models = new ArrayList<>();
		for (ObjectField field : ObjectField.values()) {
			models.add(getColumnModel(field));
		}
		return models;
	}

	@Override
	public ColumnModel getColumnModel(ObjectField field) {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName(field.name());
		columnModel.setMaximumSize(field.getSize());
		columnModel.setFacetType(field.getFacetType());

		// The type of column depends on the object type
		ColumnType columnType = resolveColumnType(field);

		columnModel.setColumnType(columnType);

		return columnModel;
	}

	@Override
	public Optional<ObjectField> findMatch(ColumnModel columnModel) {
		ObjectField match = null;
		for (ObjectField field : ObjectField.values()) {
			if (isMatch(field, columnModel)) {
				match = field;
				break;
			}
		}
		return Optional.ofNullable(match);
	}

	boolean isMatch(ObjectField field, ColumnModel model) {
		// name must match
		if (!field.name().equals(model.getName())) {
			return false;
		}
		ColumnType fieldType = resolveColumnType(field);
		// type must match
		if (!fieldType.equals(model.getColumnType())) {
			return false;
		}
		// size must be greater than or equal
		if (field.getSize() != null) {
			if (model.getMaximumSize() == null) {
				return false;
			}
			if (model.getMaximumSize() < field.getSize()) {
				return false;
			}
		}
		// name and type match, and size is than greater or equal
		return true;
	}

	ColumnType resolveColumnType(ObjectField field) {
		if (field.getColumnType() != null) {
			return field.getColumnType();
		}
		switch (field) {
		case id:
			return fieldTypeProvider.getIdColumnType();
		case parentId:
			return fieldTypeProvider.getParentIdColumnType();
		case benefactorId:
			return fieldTypeProvider.getBenefactorIdColumnType();
		default:
			throw new IllegalStateException("Cannot resolve the columnType mapped to the " + field.name() + " field");
		}
	}

}

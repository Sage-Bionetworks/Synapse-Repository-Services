package org.sagebionetworks.repo.manager.table.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultColumnModelMapperImpl implements DefaultColumnModelMapper {

	private MetadataIndexProviderFactory metadataIndexProviderFactory;
	private ObjectFieldModelResolverFactory objectFieldModelResolverFactory;
	private ColumnModelManager columnModelManager;

	@Autowired
	public DefaultColumnModelMapperImpl(MetadataIndexProviderFactory metadataIndexProviderFactory,
			ObjectFieldModelResolverFactory objectFieldModelResolverFactory, ColumnModelManager columnModelManager) {
		this.metadataIndexProviderFactory = metadataIndexProviderFactory;
		this.objectFieldModelResolverFactory = objectFieldModelResolverFactory;
		this.columnModelManager = columnModelManager;
	}

	@Override
	public List<ColumnModel> map(DefaultColumnModel defaultColumns) {
		ValidateArgument.required(defaultColumns, "defaultColumns");
		ValidateArgument.required(defaultColumns.getObjectType(), "defaultColumns.objectType");
		ValidateArgument.requiredNotEmpty(defaultColumns.getDefaultFields(), "defaultColumns.defaultFields");
		
		ObjectFieldModelResolver fieldResolver = getObjectFieldResolver(defaultColumns.getObjectType());
		
		List<ColumnModel> defaultFields = map(defaultColumns.getDefaultFields(), fieldResolver);
		List<ColumnModel> customFields = defaultColumns.getCustomFields();

		if (customFields == null || customFields.isEmpty()) {
			customFields = Collections.emptyList();
		}

		List<ColumnModel> columnModels = new ArrayList<>(defaultFields.size() + customFields.size());

		columnModels.addAll(defaultFields);
		columnModels.addAll(customFields);
		
		return create(columnModels);
	}

	@Override
	public List<ColumnModel> getColumnModels(ViewObjectType objectType, ObjectField ...fields) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(fields, "fields");
		
		if (fields.length == 0) {
			return Collections.emptyList();
		}
		
		ObjectFieldModelResolver fieldResolver = getObjectFieldResolver(objectType);
		
		List<ColumnModel> models = map(Arrays.asList(fields), fieldResolver);
		
		return create(models);
	}
	
	private List<ColumnModel> create(List<ColumnModel> models) {
		return models.stream()
			.map(columnModelManager::createColumnModel)
			.collect(Collectors.toList());
	}
	
	private List<ColumnModel> map(List<ObjectField> fields, ObjectFieldModelResolver fieldModelResolver) {
		return fields.stream().map(fieldModelResolver::getColumnModel).collect(Collectors.toList());
	}
	
	private ObjectFieldModelResolver getObjectFieldResolver(ViewObjectType objectType) {
		ObjectFieldTypeMapper fieldTypeMapper = metadataIndexProviderFactory.getMetadataIndexProvider(objectType);
		return objectFieldModelResolverFactory.getObjectFieldModelResolver(fieldTypeMapper);
	}

}

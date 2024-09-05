package org.sagebionetworks.repo.service.metadata;

import java.util.*;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataProviderFactoryImpl implements MetadataProviderFactory {

	private Map<EntityType, EntityProvider<? extends Entity>> metadataProviders;

	@Autowired
	void metadataProviders(
			ProjectMetadataProvider projectProvider, 
			FolderMetadataProvider folderProvider,
			FileEntityMetadataProvider fileProvider, 
			TableEntityMetadataProvider tableProvider,
			EntityViewMetadataProvider entityViewProvider, 
			ExternalDockerRepoValidator dockerProvider,
			SubmissionViewMetadataProvider submissionViewProvider, 
			DatasetMetadataProvider datasetProvider,
			DatasetCollectionMetadataProvider datasetCollectionProvider,
			MaterializedViewMetadataProvider materializedViewProvider,
			VirtualTableMetadataProvider virtualTableProvider) {
		metadataProviders = new HashMap<EntityType, EntityProvider<? extends Entity>>();
		metadataProviders.put(EntityType.project, projectProvider);
		metadataProviders.put(EntityType.folder, folderProvider);
		metadataProviders.put(EntityType.file, fileProvider);
		metadataProviders.put(EntityType.table, tableProvider);
		metadataProviders.put(EntityType.entityview, entityViewProvider);
		metadataProviders.put(EntityType.dockerrepo, dockerProvider);
		metadataProviders.put(EntityType.submissionview, submissionViewProvider);
		metadataProviders.put(EntityType.dataset, datasetProvider);
		metadataProviders.put(EntityType.datasetcollection, datasetCollectionProvider);
		metadataProviders.put(EntityType.materializedview, materializedViewProvider);
		metadataProviders.put(EntityType.virtualtable, virtualTableProvider);
	}

	@Override
	public Optional<EntityProvider<? extends Entity>> getMetadataProvider(EntityType type) {
		return Optional.ofNullable(metadataProviders.get(type));
	}
}

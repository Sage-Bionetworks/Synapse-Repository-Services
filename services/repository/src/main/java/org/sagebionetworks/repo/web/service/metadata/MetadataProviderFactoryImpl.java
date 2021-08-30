package org.sagebionetworks.repo.web.service.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataProviderFactoryImpl implements MetadataProviderFactory {

	private Map<EntityType, List<EntityProvider<? extends Entity>>> metadataProviders;

	@Autowired
	void metadataProviders(ProjectMetadataProvider projectProvider, FolderMetadataProvider folderProvider,
			FileEntityMetadataProvider fileProvider, TableEntityMetadataProvider tableProvider,
			EntityViewMetadataProvider entityViewProvider, ExternalDockerRepoValidator dockerProvider,
			SubmissionViewMetadataProvider submissionViewProvider, DatasetMetadataProvider datasetProvider) {
		metadataProviders = new HashMap<EntityType, List<EntityProvider<? extends Entity>>>();
		metadataProviders.put(EntityType.project, Collections.singletonList(projectProvider));
		metadataProviders.put(EntityType.folder, Collections.singletonList(folderProvider));
		metadataProviders.put(EntityType.file, Collections.singletonList(fileProvider));
		metadataProviders.put(EntityType.table, Collections.singletonList(tableProvider));
		metadataProviders.put(EntityType.entityview, Collections.singletonList(entityViewProvider));
		metadataProviders.put(EntityType.dockerrepo, Collections.singletonList(dockerProvider));
		metadataProviders.put(EntityType.submissionview, Collections.singletonList(submissionViewProvider));
		metadataProviders.put(EntityType.dataset, Collections.singletonList(datasetProvider));
	}

	@Override
	public List<EntityProvider<? extends Entity>> getMetadataProvider(EntityType type) {
		return metadataProviders.get(type);
	}
}

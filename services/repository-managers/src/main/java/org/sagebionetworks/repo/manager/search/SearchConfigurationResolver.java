package org.sagebionetworks.repo.manager.search;

import java.util.Optional;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.search.SearchConfigurationDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.table.SearchConfigBinding;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.springframework.stereotype.Service;

@Service
public class SearchConfigurationResolver {

	private final SearchConfigurationDao searchConfigurationDao;
	private final NodeDAO nodeDAO;

	public SearchConfigurationResolver(SearchConfigurationDao searchConfigurationDao, NodeDAO nodeDAO) {
		this.searchConfigurationDao = searchConfigurationDao;
		this.nodeDAO = nodeDAO;
	}

	/**
	 * Resolves the effective SearchConfiguration for a SearchIndex entity.
	 * Resolution order:
	 * 1. Explicit searchConfigurationId on the entity
	 * 2. Search configuration binding from the entity hierarchy (walks up ancestors)
	 * 3. null (platform defaults)
	 *
	 * @param searchConfigurationId The explicit search configuration ID, or null
	 * @param parentId The parent entity ID for hierarchy binding lookup
	 * @return The resolved SearchConfiguration, or empty if platform defaults should be used
	 */
	public Optional<SearchConfiguration> resolve(String searchConfigurationId, String parentId) {
		// 1. Explicit configuration
		if (searchConfigurationId != null && !searchConfigurationId.isEmpty()) {
			return searchConfigurationDao.get(searchConfigurationId);
		}
		// 2. Binding table hierarchy walk
		if (parentId == null || parentId.isEmpty()) {
			return Optional.empty();
		}
		Long parentNodeId = KeyFactory.stringToKey(parentId);
		Optional<Long> boundEntityId = nodeDAO.getEntityIdOfFirstBoundSearchConfig(parentNodeId);
		if (boundEntityId.isPresent()) {
			Optional<SearchConfigBinding> binding = searchConfigurationDao.getSearchConfigBindingForObject(
					boundEntityId.get(), "entity");
			if (binding.isPresent()) {
				return searchConfigurationDao.get(binding.get().getSearchConfigurationId());
			}
		}
		// 3. No configuration -- use platform defaults
		return Optional.empty();
	}
}

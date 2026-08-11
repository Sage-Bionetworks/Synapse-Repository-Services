package org.sagebionetworks.repo.manager.search;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SearchConfigurationDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.table.BindSearchConfigToEntityRequest;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.ListSearchConfigurationsRequest;
import org.sagebionetworks.repo.model.search.table.ListSearchConfigurationsResponse;
import org.sagebionetworks.repo.model.search.table.SearchConfigBinding;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class SearchConfigurationManagerImpl implements SearchConfigurationManager {

	private static final String ENTITY_OBJECT_TYPE = "entity";

	private final SearchConfigurationDao searchConfigurationDao;
	private final AccessControlListDAO aclDao;
	private final OrganizationDao organizationDao;
	private final ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	private final TextAnalyzerDao textAnalyzerDao;
	private final NodeDAO nodeDAO;
	private final EntityAuthorizationManager entityAuthorizationManager;

	public SearchConfigurationManagerImpl(SearchConfigurationDao searchConfigurationDao, AccessControlListDAO aclDao,
			OrganizationDao organizationDao,
			ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao, TextAnalyzerDao textAnalyzerDao, NodeDAO nodeDAO,
			EntityAuthorizationManager entityAuthorizationManager) {
		this.searchConfigurationDao = searchConfigurationDao;
		this.aclDao = aclDao;
		this.organizationDao = organizationDao;
		this.columnAnalyzerOverrideDao = columnAnalyzerOverrideDao;
		this.textAnalyzerDao = textAnalyzerDao;
		this.nodeDAO = nodeDAO;
		this.entityAuthorizationManager = entityAuthorizationManager;
	}

	@Override
	@WriteTransaction
	public SearchConfiguration create(UserInfo user, SearchConfiguration request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getOrganizationName(), "organizationName");
		ValidateArgument.requiredNotBlank(request.getName(), "name");
		SearchResourceConstants.validateResourceName(request.getName());

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException("Only Sage Bionetworks employees can manage search configurations.");
		}
		if (!user.isAdmin()) {
			aclDao.canAccess(user, resolveOrganizationId(request.getOrganizationName()), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE)
				.checkAuthorizationOrElseThrow();
		}

		validateReferencedNames(request);

		return searchConfigurationDao.create(user.getId(), request);
	}

	@Override
	public SearchConfiguration get(UserInfo user, String id) {
		ValidateArgument.requiredNotBlank(id, "id");

		return searchConfigurationDao.get(id)
			.orElseThrow(() -> new NotFoundException("A search configuration with the given id does not exist."));
	}

	@Override
	@WriteTransaction
	public SearchConfiguration update(UserInfo user, SearchConfiguration request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getId(), "id");
		ValidateArgument.requiredNotBlank(request.getOrganizationName(), "organizationName");
		ValidateArgument.requiredNotBlank(request.getName(), "name");
		SearchResourceConstants.validateResourceName(request.getName());

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException("Only Sage Bionetworks employees can manage search configurations.");
		}
		SearchConfiguration existing = searchConfigurationDao.get(request.getId())
			.orElseThrow(() -> new NotFoundException("A search configuration with the given id does not exist."));

		if (!existing.getOrganizationName().equals(request.getOrganizationName())) {
			throw new IllegalArgumentException(SearchResourceConstants.ORG_NAME_IMMUTABLE_MSG);
		}
		if (!existing.getName().equals(request.getName())) {
			throw new IllegalArgumentException(SearchResourceConstants.NAME_IMMUTABLE_MSG);
		}

		if (!user.isAdmin()) {
			aclDao.canAccess(user, resolveOrganizationId(existing.getOrganizationName()), ObjectType.ORGANIZATION, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
		}

		validateReferencedNames(request);

		return searchConfigurationDao.update(user.getId(), request);
	}

	@Override
	@WriteTransaction
	public SearchConfigBinding bindSearchConfigToEntity(UserInfo user, BindSearchConfigToEntityRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getEntityId(), "entityId");
		ValidateArgument.requiredNotBlank(request.getSearchConfigurationId(), "searchConfigurationId");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException("Only Sage Bionetworks employees can bind search configurations.");
		}

		Long entityId = KeyFactory.stringToKey(request.getEntityId());
		Long searchConfigId = Long.parseLong(request.getSearchConfigurationId());

		// Verify the user has UPDATE permission on the entity, resolving the benefactor ACL.
		entityAuthorizationManager.hasAccess(user, request.getEntityId(), ACCESS_TYPE.UPDATE)
			.checkAuthorizationOrElseThrow();

		// A search configuration can only be bound to a Project or Folder so that entities within
		// that container inherit the binding.
		EntityType entityType = nodeDAO.getNodeTypeById(request.getEntityId());
		if (!NodeUtils.isProjectOrFolder(entityType)) {
			throw new IllegalArgumentException("A search configuration can only be bound to a Project or Folder.");
		}

		// Verify search config exists
		searchConfigurationDao.get(request.getSearchConfigurationId())
			.orElseThrow(() -> new NotFoundException("A search configuration with the given id does not exist."));

		searchConfigurationDao.bindSearchConfigToObject(searchConfigId, entityId, ENTITY_OBJECT_TYPE, user.getId());

		return searchConfigurationDao.getSearchConfigBindingForObject(entityId, ENTITY_OBJECT_TYPE)
			.orElseThrow(() -> new IllegalStateException("Failed to bind search configuration to entity."));
	}

	@Override
	public SearchConfigBinding getSearchConfigBinding(UserInfo user, String entityId) {
		ValidateArgument.requiredNotBlank(entityId, "entityId");

		Long nodeId = KeyFactory.stringToKey(entityId);
		Long firstBoundEntityId = nodeDAO.getEntityIdOfFirstBoundSearchConfig(nodeId)
			.orElseThrow(() -> new NotFoundException(
					"No search configuration binding found for entity '" + entityId + "' or any of its ancestors."));

		return searchConfigurationDao.getSearchConfigBindingForObject(firstBoundEntityId, ENTITY_OBJECT_TYPE)
			.orElseThrow(() -> new IllegalStateException("Binding not found after hierarchy walk."));
	}

	@Override
	@WriteTransaction
	public void clearSearchConfigBinding(UserInfo user, String entityId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.requiredNotBlank(entityId, "entityId");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException("Only Sage Bionetworks employees can clear search configuration bindings.");
		}

		Long nodeId = KeyFactory.stringToKey(entityId);

		// Verify the user has UPDATE permission on the entity, resolving the benefactor ACL.
		entityAuthorizationManager.hasAccess(user, entityId, ACCESS_TYPE.UPDATE)
			.checkAuthorizationOrElseThrow();

		searchConfigurationDao.clearSearchConfigBinding(nodeId, ENTITY_OBJECT_TYPE);
	}

	@Override
	public ListSearchConfigurationsResponse list(UserInfo user, ListSearchConfigurationsRequest request) {
		ValidateArgument.required(request, "request");

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());

		List<SearchConfiguration> page;
		if (request.getOrganizationName() == null) {
			page = searchConfigurationDao.listAll(nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		} else {
			page = searchConfigurationDao.list(request.getOrganizationName(),
				nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		}

		return new ListSearchConfigurationsResponse()
			.setResults(page)
			.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}

	/**
	 * Walk every analyzer / override binding on the SearchConfiguration. Each binding is
	 * either a {@code $ref} reference to a saved row or an inline literal:
	 * <ul>
	 *   <li>For a {@code $ref}: validate the qualified-name format and verify the target
	 *       row exists.</li>
	 *   <li>For an inline literal: convert into the typed POJO so the schema-shape check
	 *       runs at create / update time, then recursively validate any nested {@code $ref}
	 *       bindings (a ColumnAnalyzerOverride entry's {@code analyzer} can itself be a
	 *       reference).</li>
	 * </ul>
	 * Any unresolved reference or malformed inline literal raises {@link IllegalArgumentException}
	 * so the API rejects the request synchronously rather than letting the failure surface
	 * during an async index build.
	 */
	private void validateReferencedNames(SearchConfiguration config) {
		List<String> textAnalyzerRefs = new ArrayList<>();
		List<String> overrideRefs = new ArrayList<>();
		collectTextAnalyzerRef(config.getDefaultAnalyzer(), "defaultAnalyzer", textAnalyzerRefs);
		if (config.getColumnAnalyzerOverrides() != null) {
			for (Object entry : config.getColumnAnalyzerOverrides()) {
				String ref = SearchOpaqueJsonUtil.readRef(entry);
				if (ref != null) {
					SearchResourceConstants.validateQualifiedNameFormat(ref, "columnAnalyzerOverrides");
					overrideRefs.add(ref);
				} else {
					ColumnAnalyzerOverride inline = SearchOpaqueJsonUtil.toInline(entry, ColumnAnalyzerOverride.class);
					if (inline != null && inline.getOverrides() != null) {
						for (ColumnAnalyzerOverrideEntry e : inline.getOverrides()) {
							collectTextAnalyzerRef(e.getAnalyzer(),
								"columnAnalyzerOverrides[].overrides[].analyzer", textAnalyzerRefs);
						}
					}
				}
			}
		}
		if (!textAnalyzerRefs.isEmpty()) {
			List<String> missing = textAnalyzerDao.findNonExistentNames(textAnalyzerRefs);
			if (!missing.isEmpty()) {
				throw new IllegalArgumentException("The following text analyzer name(s) do not exist: " + missing);
			}
		}
		if (!overrideRefs.isEmpty()) {
			List<String> missing = columnAnalyzerOverrideDao.findNonExistentNames(overrideRefs);
			if (!missing.isEmpty()) {
				throw new IllegalArgumentException("The following column analyzer override name(s) do not exist: " + missing);
			}
		}
	}

	/**
	 * If the binding is a {@code $ref}, validate qname format and append to the ref-name
	 * collector for batched existence checking. Otherwise round-trip the inline analyzer
	 * literal (a bare OpenSearch {@code settings.analysis} block) through the typed
	 * deserializer so the analyzer shape is checked at create / update time.
	 */
	private static void collectTextAnalyzerRef(Object binding, String fieldName, List<String> refs) {
		if (binding == null) {
			return;
		}
		String ref = SearchOpaqueJsonUtil.readRef(binding);
		if (ref != null) {
			SearchResourceConstants.validateQualifiedNameFormat(ref, fieldName);
			refs.add(ref);
			return;
		}
		// Inline analyzer literal: a bare OpenSearch settings.analysis block. Round-trip
		// through the OpenSearch typed deserializer so curator-supplied JSON is rejected at
		// create / update time if its analyzer / token-filter shape is malformed. The
		// null resolver rejects any nested $ref entries: refs inside an inline-literal slot
		// are not a supported feature — curators wanting reusable analyzers (or synonyms)
		// save a TextAnalyzer / SynonymSet and bind by qname.
		SearchOpaqueJsonUtil.toInlineAnalyzerSettings(binding, qname -> null);
	}

	private String resolveOrganizationId(String organizationName) {
		return organizationDao.getOrganizationByName(organizationName).getId();
	}
}

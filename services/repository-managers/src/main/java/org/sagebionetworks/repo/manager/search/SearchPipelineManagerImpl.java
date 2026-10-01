package org.sagebionetworks.repo.manager.search;

import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.search.SearchPipelineDao;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesRequest;
import org.sagebionetworks.repo.model.search.table.ListNamedSearchPipelinesResponse;
import org.sagebionetworks.repo.model.search.table.NamedSearchPipeline;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class SearchPipelineManagerImpl implements SearchPipelineManager {

	private static final String MSG_UNAUTHORIZED = "Only Sage Bionetworks employees can manage search pipelines.";
	private static final String MSG_NOT_FOUND = "A search pipeline with the given id does not exist.";

	private final SearchPipelineDao searchPipelineDao;
	private final AccessControlListDAO aclDao;
	private final OrganizationDao organizationDao;

	public SearchPipelineManagerImpl(SearchPipelineDao searchPipelineDao,
			AccessControlListDAO aclDao, OrganizationDao organizationDao) {
		this.searchPipelineDao = searchPipelineDao;
		this.aclDao = aclDao;
		this.organizationDao = organizationDao;
	}

	@Override
	@WriteTransaction
	public NamedSearchPipeline create(UserInfo user, NamedSearchPipeline request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getOrganizationName(), "organizationName");
		ValidateArgument.requiredNotBlank(request.getName(), "name");
		SearchResourceConstants.validateResourceName(request.getName());
		SearchDslValidator.validateSavedSearchPipeline(request.getSettings(), "settings");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}
		if (!user.isAdmin()) {
			aclDao.canAccess(user, resolveOrganizationId(request.getOrganizationName()), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE)
				.checkAuthorizationOrElseThrow();
		}

		return searchPipelineDao.create(user.getId(), request);
	}

	@Override
	public NamedSearchPipeline get(UserInfo user, String id) {
		ValidateArgument.requiredNotBlank(id, "id");

		return searchPipelineDao.get(id).orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));
	}

	@Override
	@WriteTransaction
	public NamedSearchPipeline update(UserInfo user, NamedSearchPipeline request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getId(), "id");
		ValidateArgument.requiredNotBlank(request.getOrganizationName(), "organizationName");
		ValidateArgument.requiredNotBlank(request.getName(), "name");
		SearchDslValidator.validateSavedSearchPipeline(request.getSettings(), "settings");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}
		NamedSearchPipeline existing = searchPipelineDao.get(request.getId())
			.orElseThrow(() -> new NotFoundException(MSG_NOT_FOUND));

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

		return searchPipelineDao.update(user.getId(), request);
	}

	@Override
	public ListNamedSearchPipelinesResponse list(UserInfo user, ListNamedSearchPipelinesRequest request) {
		ValidateArgument.required(request, "request");

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());

		List<NamedSearchPipeline> page;
		if (request.getOrganizationName() == null) {
			page = searchPipelineDao.listAll(nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		} else {
			page = searchPipelineDao.list(request.getOrganizationName(),
				nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		}

		return new ListNamedSearchPipelinesResponse()
			.setResults(page)
			.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}

	private String resolveOrganizationId(String organizationName) {
		return organizationDao.getOrganizationByName(organizationName).getId();
	}
}

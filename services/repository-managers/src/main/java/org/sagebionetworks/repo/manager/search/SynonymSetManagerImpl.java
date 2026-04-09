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
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class SynonymSetManagerImpl implements SynonymSetManager {

	private static final String MSG_UNAUTHORIZED = "Only Sage Bionetworks employees can manage synonym sets.";

	private final SynonymSetDao synonymSetDao;
	private final AccessControlListDAO aclDao;
	private final OrganizationDao organizationDao;

	public SynonymSetManagerImpl(SynonymSetDao synonymSetDao,
			AccessControlListDAO aclDao, OrganizationDao organizationDao) {
		this.synonymSetDao = synonymSetDao;
		this.aclDao = aclDao;
		this.organizationDao = organizationDao;
	}

	@Override
	@WriteTransaction
	public SynonymSet create(UserInfo user, SynonymSet request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getOrganizationName(), "organizationName");
		ValidateArgument.requiredNotBlank(request.getName(), "name");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}
		if (!user.isAdmin()) {
			aclDao.canAccess(user, resolveOrganizationId(request.getOrganizationName()), ObjectType.ORGANIZATION, ACCESS_TYPE.CREATE)
				.checkAuthorizationOrElseThrow();
		}

		return synonymSetDao.create(user.getId(), request);
	}

	@Override
	public SynonymSet get(UserInfo user, String id) {
		ValidateArgument.requiredNotBlank(id, "id");

		return synonymSetDao.get(id)
			.orElseThrow(() -> new NotFoundException("A synonym set with the given id does not exist."));
	}

	@Override
	@WriteTransaction
	public SynonymSet update(UserInfo user, SynonymSet request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requiredNotBlank(request.getId(), "id");
		ValidateArgument.requiredNotBlank(request.getOrganizationName(), "organizationName");
		ValidateArgument.requiredNotBlank(request.getName(), "name");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}
		SynonymSet existing = synonymSetDao.get(request.getId())
			.orElseThrow(() -> new NotFoundException("A synonym set with the given id does not exist."));

		if (!existing.getOrganizationName().equals(request.getOrganizationName())) {
			throw new IllegalArgumentException("The organizationName cannot be changed.");
		}

		if (!user.isAdmin()) {
			aclDao.canAccess(user, resolveOrganizationId(existing.getOrganizationName()), ObjectType.ORGANIZATION, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
		}

		return synonymSetDao.update(user.getId(), request);
	}

	@Override
	@WriteTransaction
	public void delete(UserInfo user, String id) {
		ValidateArgument.required(user, "user");
		ValidateArgument.requiredNotBlank(id, "id");

		AuthorizationUtils.disallowAnonymous(user);
		if (!AuthorizationUtils.isSageEmployeeOrAdmin(user)) {
			throw new UnauthorizedException(MSG_UNAUTHORIZED);
		}

		SynonymSet existing = synonymSetDao.get(id)
			.orElseThrow(() -> new NotFoundException("A synonym set with the given id does not exist."));

		if (!user.isAdmin()) {
			aclDao.canAccess(user, resolveOrganizationId(existing.getOrganizationName()), ObjectType.ORGANIZATION, ACCESS_TYPE.DELETE)
				.checkAuthorizationOrElseThrow();
		}

		synonymSetDao.delete(id);
	}

	@Override
	public ListSynonymSetsResponse list(UserInfo user, ListSynonymSetsRequest request) {
		ValidateArgument.required(request, "request");

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());

		List<SynonymSet> page;
		if (request.getOrganizationName() == null) {
			page = synonymSetDao.listAll(nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		} else {
			page = synonymSetDao.list(request.getOrganizationName(),
				nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		}

		return new ListSynonymSetsResponse()
			.setResults(page)
			.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}

	private String resolveOrganizationId(String organizationName) {
		return organizationDao.getOrganizationByName(organizationName).getId();
	}
}

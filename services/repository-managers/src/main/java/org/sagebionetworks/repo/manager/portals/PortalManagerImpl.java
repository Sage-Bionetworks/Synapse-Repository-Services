package org.sagebionetworks.repo.manager.portals;

import java.util.List;

import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserPortalPermissions;
import org.sagebionetworks.repo.model.dbo.portals.DBOPortal;
import org.sagebionetworks.repo.model.dbo.portals.PortalDao;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsResponse;
import org.sagebionetworks.repo.model.portals.Portal;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class PortalManagerImpl implements PortalManager {
		
	private AccessControlListDAO aclDao;
	private PortalDao portalsDao;

	public PortalManagerImpl(AccessControlListDAO aclDao, PortalDao portalsDao) {
		this.aclDao = aclDao;
		this.portalsDao = portalsDao;
	}

	@Override
	@WriteTransaction
	public Portal createPortal(UserInfo user, CreateOrUpdatePortalRequest request) {
		validateCreateOrUpdateRequest(user, request);
		
		if (!AuthorizationUtils.isPortalManagerOrAdmin(user)) {
			throw new UnauthorizedException("You are not authorized to perform this operation.");
		}
		
		Portal portal = portalsDao.createPortal(user.getId(), request.getName(), request.getUrl());
				
		aclDao.create(AccessControlListUtil.createACL(portal.getId().toString(), user, DEFAULT_PERMISSIONS, portal.getCreatedOn()), ObjectType.PORTAL);
		
		return portal;
	}

	@Override
	public Portal getPortal(String portalId) {
		ValidateArgument.required(portalId, "The portalId");
		
		return portalsDao.getPortal(portalId).orElseThrow(() -> new NotFoundException("A portal with the given id does not exist."));
	}
	
	@Override
	@WriteTransaction
	public Portal updatePortal(UserInfo user, String portalId, CreateOrUpdatePortalRequest request) {
		ValidateArgument.required(portalId, "The portalId");
		
		validateCreateOrUpdateRequest(user, request);
		
		if (!AuthorizationUtils.isPortalManagerOrAdmin(user)) {
			aclDao.canAccess(user, portalId, ObjectType.PORTAL, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();
		}
		
		return portalsDao.updatePortal(user.getId(), getPortal(portalId).getId(), request.getName(), request.getUrl());
	}
	
	@Override
	@WriteTransaction
	public void deletePortal(UserInfo user, String portalId) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(portalId, "The portalId");
		
		if (!AuthorizationUtils.isPortalManagerOrAdmin(user)) {
			aclDao.canAccess(user, portalId, ObjectType.PORTAL, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();
		}
		
		portalsDao.deletePortal(getPortal(portalId).getId());
	}

	@Override
	public ListPortalsResponse listPortals(ListPortalsRequest request) {
		ValidateArgument.required(request, "The request");
		
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		
		List<Portal> page = portalsDao.getPortalPage(nextPageToken.getLimitForQuery(), nextPageToken.getOffset());
		
		return new ListPortalsResponse()
			.setPage(page)
			.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}	

	@Override
	public AccessControlList getPortalAcl(String portalId) {
		ValidateArgument.required(portalId, "The portalId");
		
		return aclDao.getAcl(portalId, ObjectType.PORTAL).orElseThrow(() -> new NotFoundException("Could not find an ACL for the portal with the given id."));
	}

	@Override
	@WriteTransaction
	public AccessControlList updatePortalAcl(UserInfo user, String portalId, AccessControlList acl) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(portalId, "The portalId");
		ValidateArgument.required(acl, "The acl");
		
		acl.setId(portalId);
		
		// Makes sure the user is not revoking their own permissions
		PermissionsManagerUtils.validateACLContent(acl, user, Long.valueOf(portalId));
		
		if (!AuthorizationUtils.isPortalManagerOrAdmin(user)) {
			aclDao.canAccess(user, portalId, ObjectType.PORTAL, ACCESS_TYPE.CHANGE_PERMISSIONS).checkAuthorizationOrElseThrow();
		}
		
		aclDao.update(acl, ObjectType.PORTAL);

		return getPortalAcl(portalId);
	}
	
	@Override
	public UserPortalPermissions getUserPortalPermissions(UserInfo user, String portalId) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(portalId, "The portalId");
		
		boolean canMintDoi = canMintDoi(user, portalId).isAuthorized();
		
		return new UserPortalPermissions()
			.setCanMintDoi(canMintDoi);
	}
	
	@Override
	public AuthorizationStatus canMintDoi(UserInfo user, String portalId) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(portalId, "The portalId");

		// Any user on the Synapse.org portal can mint a DOIs as long as they have edit permissions on the underlying entity.
		if (DBOPortal.SYNAPSE_PORTAL_ID.toString().equals(portalId) || AuthorizationUtils.isPortalManagerOrAdmin(user)) {
			return AuthorizationStatus.authorized();
		} else {
			return aclDao.canAccess(user, portalId, ObjectType.PORTAL, ACCESS_TYPE.UPDATE);
		}
	}
	
	private static void validateCreateOrUpdateRequest(UserInfo user, CreateOrUpdatePortalRequest request) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		ValidateArgument.requiredNotBlank(request.getName(), "The name");
		ValidateArgument.validUrl(request.getUrl(), "The url");
	}

}

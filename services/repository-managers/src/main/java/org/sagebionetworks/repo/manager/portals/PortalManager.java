package org.sagebionetworks.repo.manager.portals;

import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserPortalPermissions;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsResponse;
import org.sagebionetworks.repo.model.portals.Portal;

public interface PortalManager {
	
	Set<ACCESS_TYPE> DEFAULT_PERMISSIONS = Set.of(ACCESS_TYPE.CREATE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.CHANGE_PERMISSIONS);
	
	Portal createPortal(UserInfo user, CreateOrUpdatePortalRequest request);

	Portal getPortal(String portalId);
	
	Portal updatePortal(UserInfo user, String portalId, CreateOrUpdatePortalRequest request);
	
	void deletePortal(UserInfo user, String portalId);	

	ListPortalsResponse listPortals(ListPortalsRequest request);	

	AccessControlList getPortalAcl(String portalId);

	AccessControlList updatePortalAcl(UserInfo user, String portalId, AccessControlList acl);
	
	AuthorizationStatus canMintDoi(UserInfo user, String portalId);
	
	UserPortalPermissions getUserPortalPermissions(UserInfo user, String portalId);
	
}

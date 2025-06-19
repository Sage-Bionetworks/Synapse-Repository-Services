package org.sagebionetworks.repo.service.portals;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.auth.UserPortalPermissions;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsResponse;
import org.sagebionetworks.repo.model.portals.Portal;

public interface PortalService {

	Portal createPortal(Long userId, CreateOrUpdatePortalRequest request);

	Portal getPortal(String portalId);

	ListPortalsResponse listPortals(ListPortalsRequest request);

	Portal updatePortal(Long userId, String portalId, CreateOrUpdatePortalRequest request);

	void deletePortal(Long userId, String portalId);
	
	AccessControlList getPortalAcl(String portalId);

	AccessControlList updatePortalAcl(Long userId, String portalId, AccessControlList acl);

	UserPortalPermissions getUserPortalPermissions(Long userId, String portalId);
}

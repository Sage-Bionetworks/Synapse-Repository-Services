package org.sagebionetworks.repo.service.portals;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.portals.PortalManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserPortalPermissions;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsResponse;
import org.sagebionetworks.repo.model.portals.Portal;
import org.springframework.stereotype.Service;

@Service
public class PortalServiceImpl implements PortalService {

	private UserManager userManager;
	private PortalManager portalsManager;
	
	public PortalServiceImpl(UserManager userManager, PortalManager portalsManager) {
		this.userManager = userManager;
		this.portalsManager = portalsManager;
	}

	@Override
	public Portal createPortal(Long userId, CreateOrUpdatePortalRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return portalsManager.createPortal(user, request);
	}

	@Override
	public Portal getPortal(String portalId) {
		return portalsManager.getPortal(portalId);
	}

	@Override
	public ListPortalsResponse listPortals(ListPortalsRequest request) {
		return portalsManager.listPortals(request);
	}

	@Override
	public Portal updatePortal(Long userId, String portalId, CreateOrUpdatePortalRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return portalsManager.updatePortal(user, portalId, request);
	}
	
	@Override
	public void deletePortal(Long userId, String portalId) {
		UserInfo user = userManager.getUserInfo(userId);
		portalsManager.deletePortal(user, portalId);
	}

	@Override
	public AccessControlList getPortalAcl(String portalId) {
		return portalsManager.getPortalAcl(portalId);
	}

	@Override
	public AccessControlList updatePortalAcl(Long userId, String portalId, AccessControlList acl) {
		UserInfo user = userManager.getUserInfo(userId);
		return portalsManager.updatePortalAcl(user, portalId, acl);
	}
	
	@Override
	public UserPortalPermissions getUserPortalPermissions(Long userId, String portalId) {
		UserInfo user = userManager.getUserInfo(userId);
		return portalsManager.getUserPortalPermissions(user, portalId);
	}

}

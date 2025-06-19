package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.UserPortalPermissions;
import org.sagebionetworks.repo.model.portals.CreateOrUpdatePortalRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsRequest;
import org.sagebionetworks.repo.model.portals.ListPortalsResponse;
import org.sagebionetworks.repo.model.portals.Portal;
import org.sagebionetworks.repo.service.portals.PortalService;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides APIs to manage Synapse portals, generally managed by Synapse administrators.
 */
@ControllerInfo(displayName = "Portals Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class PortalController {

	private PortalService portalService;
	
	public PortalController(PortalService portalsService) {
		this.portalService = portalsService;
	}

	/**
	 * Allows to register a new Synapse portal, this service is only available to Synapse (or Portal) administrators.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.PORTAL }, method = RequestMethod.POST)
	public @ResponseBody Portal createPortal(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody CreateOrUpdatePortalRequest request) {
		return portalService.createPortal(userId, request);
	}

	/**
	 * Fetches the details of the Portal with the given id. Any user can perform this operation.
	 * 
	 * @param userId
	 * @param portalId
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_ID }, method = RequestMethod.GET)
	public @ResponseBody Portal getPortal(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "portalId") String portalId) {
		return portalService.getPortal(portalId);
	}

	/**
	 * Fetches a page of portals registered with Synapse, sorted by creation time. Any user can perform this operation.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_LIST }, method = RequestMethod.POST)
	public @ResponseBody ListPortalsResponse listPortals(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody ListPortalsRequest request) {
		return portalService.listPortals(request);
	}

	/**
	 * Updates the details of a Synapse portal. Only Synapse (or Portal) administrators or users with the UPDATE permission on the portal are allowed to perform this operation. 
	 * 
	 * @param userId
	 * @param portalId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_ID }, method = RequestMethod.PUT)
	public @ResponseBody Portal updatePortal(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "portalId") String portalId, @RequestBody CreateOrUpdatePortalRequest request) {
		return portalService.updatePortal(userId, portalId, request);
	}
	
	/**
	 * Deletes a Synapse portal. Only Synapse administrators or users with the DELETE permission on the portal are allowed to perform this operation.
	 * 
	 * @param userId
	 * @param portalId
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_ID }, method = RequestMethod.DELETE)
	public void deletePortal(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "portalId") String portalId) {
		portalService.deletePortal(userId, portalId);
	}
	
	/**
	 * Fetches the ACL currently assigned to the Synapse portal with the provided id. Any user can perform this operation. 
	 * 
	 * @param userId
	 * @param portalId
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_ACL }, method = RequestMethod.GET)
	public @ResponseBody AccessControlList getPortalAcl(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "portalId", required = true) String portalId) {
		return portalService.getPortalAcl(portalId);
	}
	
	/**
	 * Updates the ACL for a Synapse portal. Only Synapse administrators or users with the CHANGE_PERMISSION permission on the portal are allowed to perform this operation.
	 * 
	 * @param userId
	 * @param portalId
	 * @param acl
	 * @return
	 */
	@RequiredScope({view, modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_ACL }, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList updatePortalAcl(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "portalId", required = true) String portalId, @RequestBody AccessControlList acl) {
		return portalService.updatePortalAcl(userId, portalId, acl);
	}
	
	/**
	 * Returns the permissions that the user has on the portal with the given id. Any user can perform this operation.
	 * 
	 * @param userId
	 * @param portalId
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PORTAL_PERMISSIONS }, method = RequestMethod.GET)
	public @ResponseBody UserPortalPermissions getuserPortalPermissions(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable(value = "portalId", required = true) String portalId) {
		return portalService.getUserPortalPermissions(userId, portalId);
	}
}

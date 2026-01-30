package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;
import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.service.ServiceProvider;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerInfo(displayName="Realm Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class RealmController {

	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * List the IDs of the current realms.
	 * @return list of the IDs of the existing realms
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.REALM_LIST, method = RequestMethod.GET)
	@ResponseBody
	public RealmIdList listRealmIds() {
		return serviceProvider.getRealmService().listRealmIds();
	}

	/**
	 * Retrieve a realm by its ID
	 * @param realmId
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.REALM_ID, method = RequestMethod.GET)
	@ResponseBody
	public Realm getRealm(@PathVariable(ID_PATH_VARIABLE) String realmId) {
		return serviceProvider.getRealmService().getRealm(realmId);
	}
	/**
	 * Retrieve the principals for a realm
	 * @param realmId
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.REALM_ID_PRINCIPALS, method = RequestMethod.GET)
	@ResponseBody
	public RealmPrincipal getRealmPrincipalsGivenId(@PathVariable(ID_PATH_VARIABLE) String realmId) {
		return serviceProvider.getRealmService().getRealmPrincipals(realmId);
	}	
	
	/**
	 * Retrieve the principals for a realm
	 * @param realmId
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.REALM_PRINCIPALS, method = RequestMethod.GET)
	@ResponseBody
	public RealmPrincipal getRealmPrincipals(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId) {
		return serviceProvider.getRealmService().getRealmPrincipals(userId);
	}
}

package org.sagebionetworks.web.client.widget.sharing;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclAccessType;
import org.sagebionetworks.web.shared.users.AclEntry;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AccessControlListEditor implements AccessControlListEditorView.Presenter {
	
	private static final String ETAG = "etag";
	private static final String ACL_RESOURCE_ACCESS = "resourceAccess";
	private static final String ACL_ENTRY_ACCESS_TYPE = "accessType";
	private static final String ACL_ENTRY_PRINCIPAL_ID = "groupName";
	private static final String ACL_ENTRY_CREATEDBY = "createdBy";
	private static final String ACL_ENTRY_RESOURCE_ID = "id";

	
	private AccessControlListEditorView view;
	private NodeServiceAsync nodeService;
	private UserAccountServiceAsync userAccountService;
	private NodeType nodeType;
	private String nodeId;
	private JSONObject originalAcl;
	private List<AclPrincipal> principals;
	private PlaceChanger placeChanger;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;

	
	@Inject
	public AccessControlListEditor(AccessControlListEditorView view, NodeServiceAsync nodeService, UserAccountServiceAsync userAccountService, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController) {
		this.view = view;
		this.nodeService = nodeService;
		this.userAccountService = userAccountService;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		view.setPresenter(this);
	}	

    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }
	
	public void setResource(final NodeType type, final String id) {						
		this.nodeType = type;
		this.nodeId = id;
	}
	
	public Widget asWidget() {
		view.setPresenter(this);
		view.showLoading();
		nodeService.getAllUsersAndGroups(new AsyncCallback<List<AclPrincipal>>() {
			@Override
			public void onSuccess(final List<AclPrincipal> usersAndGroupsList) {
				principals = usersAndGroupsList;
				nodeService.getNodeAclJSON(nodeType, nodeId, new AsyncCallback<String>() {		
					@Override
					public void onSuccess(String result) {
						try {
							nodeModelCreator.validate(result);
						} catch (RestServiceException ex) {
							if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
								onFailure(null);							
							}
							return;
						}					
						originalAcl = JSONParser.parseStrict(result).isObject();				
						final List<AclEntry> entries = createAclEntries(originalAcl);
						boolean isInherited = false;
						
						if(originalAcl.containsKey(ACL_ENTRY_RESOURCE_ID) && !nodeId.equals(originalAcl.get(ACL_ENTRY_RESOURCE_ID).isString().stringValue())) {
							isInherited = true;
						}
						view.setAclDetails(entries, principals, isInherited);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage("Sharing settings unavailable.");
					}
				});				
			}

			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Unable to retrieve Users and Groups. Please try reloading the page.");
			}					
		});
		return view.asWidget();
	}	
	
	@Override
	public void createAcl() {
		// create acl with current user as the administrator (owner)
		UserData currentUser = authenticationController.getLoggedInUser();
		List<AclAccessType> accessList = AclUtils.getAclAccessTypes(PermissionLevel.CAN_ADMINISTER);
		nodeService.createAcl(nodeType, nodeId, currentUser.getEmail(), accessList, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result) {
				try {
					nodeModelCreator.validate(result);
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
						onFailure(null);
					}
					return;
				}	
				view.showLoading();
				refresh();
			}
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Creation of local sharing settings failed. Please try again.");
			}
		});
	}
	
	@Override
	public void addAccess(AclPrincipal principal, PermissionLevel permissionLevel) {
		if(principal != null && permissionLevel != null) {
			// TODO : check if access conflicts or overlaps!!
			
			// create new Resource Access entry
			JSONObject newResourceAccess = new JSONObject();
			newResourceAccess.put(ACL_ENTRY_PRINCIPAL_ID, new JSONString(principal.getName()));
			
			List<AclAccessType> accessList = AclUtils.getAclAccessTypes(permissionLevel);
			JSONArray jsonList = new JSONArray();
			for(int i=0; i<accessList.size(); i++) {
				jsonList.set(i, new JSONString(accessList.get(i).toString()));
			}
			newResourceAccess.put(ACL_ENTRY_ACCESS_TYPE, jsonList);
			
			// add new entry to list
			JSONObject newAcl = new JSONObject(originalAcl.getJavaScriptObject()); // clone
			JSONArray resourceAccessList = newAcl.get(ACL_RESOURCE_ACCESS).isArray();
			resourceAccessList.set(resourceAccessList.size(), newResourceAccess);			
			newAcl.put(ACL_RESOURCE_ACCESS, resourceAccessList);
			
			// persist
			String etag = originalAcl.get(ETAG).isString().stringValue();
			nodeService.updateAcl(nodeType, nodeId, newAcl.toString(), etag, new AsyncCallback<String>() {				
				@Override
				public void onSuccess(String result) {
					try {
						nodeModelCreator.validate(result);
					} catch (RestServiceException ex) {
						if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
							onFailure(null);
						}
						return;
					}					
					refresh();					
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage("Addition failed. Please try again.");
				}
			});
		}
	}

	@Override
	public void changeAccess(AclEntry aclEntry, PermissionLevel permissionLevel) {
		if(aclEntry != null && permissionLevel != null) {
			Integer resourceIndex = getResourceAccessIndex(aclEntry.getPrincipal());
			if(resourceIndex != null) {
				// add replace permissions to list
				JSONObject newAcl = new JSONObject(originalAcl.getJavaScriptObject()); // clone
				JSONArray resourceAccessList = newAcl.get(ACL_RESOURCE_ACCESS).isArray();			
				JSONObject newResourceAccess = resourceAccessList.get(resourceIndex).isObject();
				
				// update access
				List<AclAccessType> accessList = AclUtils.getAclAccessTypes(permissionLevel);
				JSONArray jsonList = new JSONArray();
				for(int i=0; i<accessList.size(); i++) {
					jsonList.set(i, new JSONString(accessList.get(i).toString()));
				}
				newResourceAccess.put(ACL_ENTRY_ACCESS_TYPE, jsonList);				
				resourceAccessList.set(resourceAccessList.size(), newResourceAccess);
				newAcl.put(ACL_RESOURCE_ACCESS, resourceAccessList);
				
				// persist
				String etag = originalAcl.get(ETAG).isString().stringValue();
				nodeService.updateAcl(nodeType, nodeId, newAcl.toString(), etag, new AsyncCallback<String>() {
					@Override
					public void onSuccess(String result) {
						try {
							nodeModelCreator.validate(result);
						} catch (RestServiceException ex) {
							if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
								onFailure(null);							
							}
							return;
						}					
						refresh();					
					}
	
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage("Change failed. Please try again.");
						
					}	
				});
			} else {
				view.showErrorMessage("An error occured. Please try reloading the page.");
			}

		}		
	}
	
	@Override
	public void removeAccess(AclEntry aclEntry) {
		if(aclEntry != null) {
			Integer resourceIndex = getResourceAccessIndex(aclEntry.getPrincipal());
			if(resourceIndex != null) {
				// add replace permissions to list
				JSONObject newAcl = new JSONObject(originalAcl.getJavaScriptObject()); // clone
				JSONArray resourceAccessList = newAcl.get(ACL_RESOURCE_ACCESS).isArray();
				
				// copy over old values and skip removed one
				JSONArray newResourceAccessList = new JSONArray();
				int count = 0;
				for(int i=0; i<resourceAccessList.size(); i++) {
					if(i == resourceIndex) continue;
					newResourceAccessList.set(count, resourceAccessList.get(i));
					count++;
				}
				// replace list with new one
				newAcl.put(ACL_RESOURCE_ACCESS, newResourceAccessList);
				
				// persist
				String etag = originalAcl.get(ETAG).isString().stringValue();
				nodeService.updateAcl(nodeType, nodeId, newAcl.toString(), etag, new AsyncCallback<String>() {
					@Override
					public void onSuccess(String result) {
						try {
							nodeModelCreator.validate(result);
						} catch (RestServiceException ex) {
							if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
								onFailure(null);
							}
							return;
						}					
						refresh();					
					}
	
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage("Remove failed. Please try again.");						
					}	
				});
			} else {
				view.showErrorMessage("An error occured. Please try reloading the page.");
			}

		}		
	}

	@Override
	public void deleteAcl() {
		// delete this ACL		
		nodeService.deleteAcl(nodeType, nodeId, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result) {
				try {
					nodeModelCreator.validate(result);
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
						onFailure(null);
					}
					return;
				}	
				view.showLoading();
				refresh();				
			}
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Creation of local sharing settings failed. Please try again.");
			}
		});
	}


	/*
	 * Private Methods
	 */
	
	private void refresh() {		
		nodeService.getNodeAclJSON(nodeType, nodeId, new AsyncCallback<String>() {		
			@Override
			public void onSuccess(String result) {
				try {
					nodeModelCreator.validate(result);
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
						onFailure(null);
					}
					return;
				}					
				originalAcl = JSONParser.parseStrict(result).isObject();				
				final List<AclEntry> entries = createAclEntries(originalAcl);
				boolean isInherited = false;
				
				if(originalAcl.containsKey(ACL_ENTRY_RESOURCE_ID) && !nodeId.equals(originalAcl.get(ACL_ENTRY_RESOURCE_ID).isString().stringValue())) {
					isInherited = true;
				}
				view.setAclDetails(entries, principals, isInherited);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Sharing settings unavailable.");
			}
		});				
	}
	
	private Integer getResourceAccessIndex(AclPrincipal principal) {
		JSONArray resouceAccessList = originalAcl.get(ACL_RESOURCE_ACCESS).isArray();
		
		for(int i=0; i<resouceAccessList.size(); i++) {
			if(resouceAccessList.get(i) != null) {				
				JSONObject resourceAccess = resouceAccessList.get(i).isObject();
				if(resourceAccess.get(ACL_ENTRY_PRINCIPAL_ID).isString().stringValue().equals(principal.getName()))
					return i;
			}
		}		
		return null;
	}
	
	private List<AclEntry> createAclEntries(JSONObject acl) {
		List<AclEntry> entries = new ArrayList<AclEntry>();

		if(acl.containsKey(ACL_RESOURCE_ACCESS)) {
			JSONArray accesses = acl.get(ACL_RESOURCE_ACCESS).isArray();
			for(int i=0; i<accesses.size(); i++) {
				JSONObject accessObj = accesses.get(i).isObject();				
				if(accessObj.containsKey(ACL_ENTRY_PRINCIPAL_ID) 
						&& accessObj.containsKey(ACL_ENTRY_ACCESS_TYPE)) {
						String principalId = accessObj.get(ACL_ENTRY_PRINCIPAL_ID).isString().stringValue();
						AclPrincipal principal = getPrincipalById(principalId);
						List<AclAccessType> accessTypes = extractAccessTypes(accessObj.get(ACL_ENTRY_ACCESS_TYPE).isArray());						
						if(principal == null || accessTypes == null) continue; // skip if malformed
						boolean isOwner = false;
						if(originalAcl.containsKey(ACL_ENTRY_CREATEDBY) && originalAcl.get(ACL_ENTRY_CREATEDBY).isString().stringValue().equals(principal.getName()))
							isOwner = true;
						entries.add(new AclEntry(principal, accessTypes, isOwner));												
				}
			}
		}
		
		return entries;
	}

	private AclPrincipal getPrincipalById(String principalId) {		
		if(principals != null) {
			// TODO : linear search could be optimized
			for(AclPrincipal principal : principals) {
				if(principal.getName().equals(principalId))
					return principal;
			}
		}
		return null;
	}

	private List<AclAccessType> extractAccessTypes(JSONArray accessList) {
		List<AclAccessType> list = new ArrayList<AclAccessType>();
		for(int i=0; i<accessList.size(); i++) {			
			AclAccessType accessType = AclUtils.getAclAccessType(accessList.get(i).isString().stringValue());
			if(accessType != null) 
				list.add(accessType);				
		}
		return list;
	}

}

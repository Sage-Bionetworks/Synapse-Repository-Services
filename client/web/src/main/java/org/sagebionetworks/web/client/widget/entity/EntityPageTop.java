package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityPageTop implements EntityPageTopView.Presenter, SynapseWidgetPresenter  {
		
	private EntityPageTopView view;
	private NodeServiceAsync nodeService;
	private PlaceChanger placeChanger;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	private Entity entity;
	private String entityTypeDisplay; 
	private HandlerManager handlerManager = new HandlerManager(this);
	private boolean isAdmin = false;
	private boolean canEdit = false;
	
	@Inject
	public EntityPageTop(EntityPageTopView view, NodeServiceAsync service, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, GlobalApplicationState globalApplicationState) {
		this.view = view;
		this.nodeService = service;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		view.setPresenter(this);
	}	

    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }

    public void setEntity(Entity entity) {
    	this.entity = entity;		
		if(entity != null) {			
			UserData currentUser = authenticationController.getLoggedInUser();
			if(currentUser != null) {
				AclUtils.getHighestPermissionLevel(DisplayUtils.getNodeTypeForEntity(entity), entity.getId(), nodeService, new AsyncCallback<PermissionLevel>() {
					@Override
					public void onSuccess(PermissionLevel result) {
						isAdmin = false;
						canEdit = false;
						if(result == PermissionLevel.CAN_EDIT) {
							canEdit = true;
						} else if(result == PermissionLevel.CAN_ADMINISTER) {
							canEdit = true;
							isAdmin = true;
						}
						sendDetailsToView(isAdmin, canEdit);
					}
					
					@Override
					public void onFailure(Throwable caught) {				
						view.showErrorMessage(DisplayConstants.ERROR_GETTING_PERMISSIONS_TEXT);
						isAdmin = false;
						canEdit = false;
						sendDetailsToView(isAdmin, canEdit);
					}			
				});
			} else {
				// because this is a public page, they can view
				isAdmin = false;
				canEdit = false;
				sendDetailsToView(isAdmin, canEdit);
			}
		}		
	}

	@SuppressWarnings("unchecked")
	public void clearState() {
		view.clear();
		// remove handlers
		handlerManager = new HandlerManager(this);		
		this.entity = null;		
	}
    
	@Override
	public Widget asWidget() {
		if(entity != null) {
			return asWidget(entity);
		} 
		return null;
	}	
	
	public Widget asWidget(Entity entity) {
		view.setPresenter(this);				
		return view.asWidget();
	}
	
	@Override
	public PlaceChanger getPlaceChanger() {
		return globalApplicationState.getPlaceChanger();
	}

	@Override
	public void refresh() {
		// TODO : tell consumer to refresh?
		sendDetailsToView(isAdmin, canEdit);
	}	

	@Override
	public void fireEntityUpdatedEvent() {
		handlerManager.fireEvent(new EntityUpdatedEvent());
	}
	
	@SuppressWarnings("unchecked")
	public void addEntityUpdatedHandler(EntityUpdatedHandler handler) {		
		handlerManager.addHandler(EntityUpdatedEvent.getType(), handler);
	}

	/*
	 * Private Methods
	 */
	private void sendDetailsToView(boolean isAdmin, boolean canEdit) {
		entityTypeDisplay = DisplayUtils.getEntityTypeDisplay(entity);
		view.setEntityDetails(entity, isAdmin, canEdit);
	}

}

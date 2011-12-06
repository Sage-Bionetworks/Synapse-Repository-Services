package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.Lookup;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

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
		// TODO : get admin status!
		isAdmin = true;
		canEdit = true;
		sendDetailsToView(isAdmin, canEdit);
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
		view.clear();
		
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
	public void delete() {
		final String parentId = entity.getParentId();
				
		nodeService.deleteAcl(DisplayUtils.getNodeTypeForEntity(entity), entity.getId(), new AsyncCallback<String>() {			
			@Override
			public void onSuccess(String result) {
				try {
					nodeModelCreator.validate(result);
					view.showInfo(entityTypeDisplay + " Deleted", "The " + entityTypeDisplay + " was successfully deleted."); 
					// Go to entity's parent
					placeChanger.goTo(new Lookup(parentId));
				} catch (RestServiceException ex) {					
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {					
						onFailure(null);					
					} 
					return;
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showEntityDeleteFailure();			
			}
		});
	}

	/*
	 * Private Methods
	 */
	private void sendDetailsToView(boolean isAdmin, boolean canEdit) {
		entityTypeDisplay = DisplayUtils.getEntityTypeDisplay(entity);
		view.setEntityDetails(entity, isAdmin, canEdit);
	}
}

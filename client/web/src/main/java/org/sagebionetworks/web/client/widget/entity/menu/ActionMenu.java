package org.sagebionetworks.web.client.widget.entity.menu;

import java.util.Set;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.place.Lookup;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ActionMenu implements ActionMenuView.Presenter, SynapseWidgetPresenter {
	
	private ActionMenuView view;
	private PlaceChanger placeChanger;
	private NodeServiceAsync nodeService;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private HandlerManager handlerManager = new HandlerManager(this);
	private Entity entity;
	private EntityTypeProvider entityTypeProvider;
	
	@Inject
	public ActionMenu(ActionMenuView view, NodeServiceAsync nodeService, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, EntityTypeProvider entityTypeProvider) {
		this.view = view;
		this.nodeService = nodeService;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.entityTypeProvider = entityTypeProvider;
		
		view.setPresenter(this);
	}	
	
	public Widget asWidget(Entity entity, boolean isAdministrator, boolean canEdit) {		
		view.setPresenter(this);
		this.entity = entity; 		
		
		// Get EntityType
		EntityType entityType = entityTypeProvider.getEntityTypeForEntity(entity);
		
		view.createMenu(entity, entityType, isAdministrator, canEdit);
		return view.asWidget();
	}

	@SuppressWarnings("unchecked")
	public void clearState() {
		view.clear();
		// remove handlers
		handlerManager = new HandlerManager(this);		
		this.entity = null;		
	}

	/**
	 * Does nothing. Use asWidget(Entity)
	 */
	@Override
	public Widget asWidget() {
		return null;
	}

    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }
    
	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}
	
	@Override
	public void fireEntityUpdatedEvent() {
		handlerManager.fireEvent(new EntityUpdatedEvent());
	}
	
	@SuppressWarnings("unchecked")
	public void addEntityUpdatedHandler(EntityUpdatedHandler handler) {
		handlerManager.addHandler(EntityUpdatedEvent.getType(), handler);
	}

	@Override
	public void deleteEntity() {
		final String parentId = entity.getParentId();
		final String entityTypeDisplay = DisplayUtils.getEntityTypeDisplay(entity);
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
				view.showErrorMessage(DisplayConstants.ERROR_ENTITY_DELETE_FAILURE);			
			}
		});
	}	
	
	/*
	 * Private Methods
	 */
}

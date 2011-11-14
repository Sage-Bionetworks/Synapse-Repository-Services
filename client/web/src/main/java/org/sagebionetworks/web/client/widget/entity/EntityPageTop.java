package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

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

}

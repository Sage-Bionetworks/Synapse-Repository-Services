package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.EntityView;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class EntityPresenter extends AbstractActivity implements EntityView.Presenter {
		
	private Synapse place;
	private EntityView view;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	private AuthenticationController authenticationController;
	private NodeServiceAsync nodeService;
	private SynapseClientAsync synapseClient;
	private NodeModelCreator nodeModelCreator;
	private String entityId;
	
	@Inject
	public EntityPresenter(EntityView view,
			GlobalApplicationState globalApplicationState,
			AuthenticationController authenticationController,
			NodeServiceAsync nodeService, SynapseClientAsync synapseClient, NodeModelCreator nodeModelCreator) {
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.authenticationController = authenticationController;
		this.nodeService = nodeService;
		this.synapseClient = synapseClient;
		this.nodeModelCreator = nodeModelCreator;
		this.placeChanger = globalApplicationState.getPlaceChanger();
	
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(Synapse place) {
		this.place = place;
		this.view.setPresenter(this);		
		
		// token maps directly to entity id
		this.entityId = place.toToken();
		
		refresh();
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
    public String mayStop() {
        view.clear();
        return null;
    }
	
	public void refresh() {
		synapseClient.getEntity(entityId, new AsyncCallback<EntityWrapper>() {
			@Override
			public void onSuccess(EntityWrapper result) {
				Entity entity = null;
				try {
					entity = nodeModelCreator.createEntity(result);
					view.setEntity(entity);
				} catch (RestServiceException ex) {					
					if(!DisplayUtils.handleServiceException(ex, globalApplicationState.getPlaceChanger(), authenticationController.getLoggedInUser())) {					
						onFailure(null);					
					} 
					return;
				}				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage(DisplayConstants.ERROR_UNABLE_TO_LOAD);
			}			
		});

	}
	
}

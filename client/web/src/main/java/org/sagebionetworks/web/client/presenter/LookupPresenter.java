package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.Lookup;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.LookupView;
import org.sagebionetworks.web.shared.EntityTypeResponse;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LookupPresenter extends AbstractActivity implements LookupView.Presenter {
		
	private Lookup place;
	private LookupView view;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	private NodeServiceAsync nodeService;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	
	@Inject
	public LookupPresenter(LookupView view,
			GlobalApplicationState globalApplicationState,
			NodeServiceAsync nodeService, NodeModelCreator nodeModelCreator,
			AuthenticationController authenticationController) {
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.nodeService = nodeService;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		
		view.setPresenter(this);		
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(Lookup place) {
		this.place = place;
		this.view.setPresenter(this);
	
		view.clear();		
		String entityId = place.toToken();
		doLookupEntity(entityId);
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	/**
	 * looks up the entity via id and forwards the user to it
	 * @param entityId
	 */
	public void doLookupEntity(final String entityId) {
		view.showLooking(entityId);
		nodeService.getNodeType(entityId, new AsyncCallback<String>() {
			
			@Override
			public void onSuccess(String result) {
				try {
					EntityTypeResponse etr = nodeModelCreator.createEntityTypeResponse(result);
					String typeString = etr.getType().substring(1); // remove leading "/"
					try {
						NodeType type = NodeType.valueOf(typeString.toUpperCase());
						goToEntity(type, etr.getId());
					} catch (IllegalArgumentException ex) {
						view.showUnknownType(typeString, entityId);
					}
				} catch (RestServiceException ex) {
					DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser());
					onFailure(null);
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showLookupFailed(entityId);
			}
		});
	}

	/**
	 * Forwards the given entity to its page in the portal
	 * @param type
	 * @param id
	 */
	public void goToEntity(NodeType type, String id) {
		view.showForwarding();
		Place place = new Synapse(id);
		view.doneLooking();
		if(placeChanger != null) {
			placeChanger.goTo(place);
		}
	}

	@Override
    public String mayStop() {
        view.clear();
        return null;
    }

}

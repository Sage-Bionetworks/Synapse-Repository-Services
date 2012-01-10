package org.sagebionetworks.web.client.widget.breadcrumb;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Breadcrumb implements BreadcrumbView.Presenter, SynapseWidgetPresenter {

	private BreadcrumbView view;
	private SynapseClientAsync synapseClient;
	private GlobalApplicationState globalApplicationState;
	private AuthenticationController authenticationController;
	private NodeModelCreator nodeModelCreator;
	private EntityTypeProvider entityTypeProvider;

	@Inject
	public Breadcrumb(BreadcrumbView view, SynapseClientAsync synapseClient,
			GlobalApplicationState globalApplicationState,
			AuthenticationController authenticationController,
			NodeModelCreator nodeModelCreator, EntityTypeProvider entityTypeProvider) {
		this.view = view;
		this.synapseClient = synapseClient;
		this.globalApplicationState = globalApplicationState;
		this.authenticationController = authenticationController;
		this.nodeModelCreator = nodeModelCreator;
		this.entityTypeProvider = entityTypeProvider;

		view.setPresenter(this);	
	}
	
	/**
	 * Create Breadcrumbs for an Entity
	 * @param entity
	 * @return
	 */
	public Widget asWidget(Entity entity) {		
		view.setPresenter(this);
		EntityType entityType = entityTypeProvider.getEntityTypeForEntity(entity);
		synapseClient.getEntityPath(entity.getId(), entityType.getUrlPrefix(), new AsyncCallback<EntityWrapper>() {
			
			@Override
			public void onSuccess(EntityWrapper result) {
				EntityPath entityPath = null;
				try {
					// exchange root for home
					List<LinkData> links = new ArrayList<LinkData>();					
					links.add(new LinkData("Home", new Home(DisplayUtils.DEFAULT_PLACE_TOKEN)));
					String current = null;
					
					// get and add paths
					entityPath = nodeModelCreator.createEntityPath(result);
					if(entityPath != null) {
						List<EntityHeader> path = entityPath.getPath();
						if(path != null) {
							// create link data for each path element except for the first (root) and last (current)
							for(int i=1; i<path.size()-1; i++) {
								EntityHeader element = path.get(i);
								links.add(new LinkData(element.getName(), new Synapse(element.getId())));
							}						
							// set current as name of last path element
							current = path.get(path.size()-1).getName();
						}						
					}
					view.setLinksList(links, current);
				} catch (RestServiceException ex) {					
					if(!DisplayUtils.handleServiceException(ex, globalApplicationState.getPlaceChanger(), authenticationController.getLoggedInUser())) {					
						onFailure(null);					
					} 
					return;
				}				

			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error occured loading the breadcrumbs");
			}
		});		
		return view.asWidget();
	}

	/**
	 * Not used
	 */
	@Override
	public Widget asWidget() {
		// TODO Auto-generated method stub
		return null;
	}	

	
	@Override
	public void setPlaceChanger(PlaceChanger placeChanger) {		
	}

	@Override
	public void goTo(Place place) {
		globalApplicationState.getPlaceChanger().goTo(place);
	}

}

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.AnalysesHome;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.AnalysisView;
import org.sagebionetworks.web.client.view.ProjectView;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class AnalysisPresenter extends AbstractActivity implements AnalysisView.Presenter {
		
	private org.sagebionetworks.web.client.place.Analysis place;
	private AnalysisView view;
	private String analysisId;
	private NodeServiceAsync nodeService;
	private PlaceChanger placeChanger;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	
	@Inject
	public AnalysisPresenter(AnalysisView view, NodeServiceAsync service, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, GlobalApplicationState globalApplicationState){
		this.view = view;
		this.nodeService = service;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		
		// Set the presenter on the view
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}
	
	public void setPlace(org.sagebionetworks.web.client.place.Analysis place) {
		this.place = place;
		this.analysisId = place.toToken();
		view.setPresenter(this);
		
		// load the analysis given in the Analysis Place		
		loadFromServer();
	}
	

	@Override
	public void refresh() {
		loadFromServer();
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
	public void delete() {
		nodeService.deleteNode(NodeType.ANALYSIS, analysisId, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				view.showInfo("Analysis Deleted", "The analysis was successfully deleted.");
				placeChanger.goTo(new AnalysesHome(DisplayUtils.DEFAULT_PLACE_TOKEN));
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Analysis delete failed.");
			}
		});
	}
	
	
	/*
	 * Protected Methods
	 */
	protected void loadFromServer() {
		// Fetch the data about this analysis from the server
		nodeService.getNodeJSON(NodeType.ANALYSIS, this.analysisId, new AsyncCallback<String>() {
			
			@Override
			public void onSuccess(String result) {
				Analysis analysis = null;
				try {
					analysis = nodeModelCreator.createAnalysis(result);
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
						onFailure(null);
					}
					return;
				}				
				setAnalysis(analysis);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setAnalysis(null);
				view.showErrorMessage("An error retrieving the Analysis occured. Please try reloading the page.");				
			}
		});
	}
	
	/**
	 * Sends the analysis elements to the view
	 * @param analysis
	 */
	protected void setAnalysis(final Analysis analysis) {
		if (analysis != null) {			
			UserData currentUser = authenticationController.getLoggedInUser();
			if(currentUser != null) {
				AclUtils.getHighestPermissionLevel(NodeType.ANALYSIS, analysis.getId(), nodeService, new AsyncCallback<PermissionLevel>() {
					@Override
					public void onSuccess(PermissionLevel result) {
						boolean isAdministrator = false;
						boolean canEdit = false;
						if(result == PermissionLevel.CAN_EDIT) {
							canEdit = true;
						} else if(result == PermissionLevel.CAN_ADMINISTER) {
							canEdit = true;
							isAdministrator = true;
						} 
						setAnalysisDetails(analysis, isAdministrator, canEdit);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage(DisplayConstants.ERROR_GETTING_PERMISSIONS_TEXT);
						setAnalysisDetails(analysis, false, false);
					}			
				});		
			} else {
				// because this is a public page, they can view
				setAnalysisDetails(analysis, false, false);
			}
		} 	
	}

	
	private void setAnalysisDetails(final Analysis analysis,
			boolean isAdministrator, boolean canEdit) {
		view.setAnalysisDetails(analysis.getId(), analysis.getName(),
				analysis.getDescription(), analysis.getCreatedBy(),
				analysis.getCreatedOn(), isAdministrator, canEdit);
	}	
	
	@Override
    public String mayStop() {
        view.clear();
        return null;
    }
	
}

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.ProjectView;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.Project;
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

public class ProjectPresenter extends AbstractActivity implements ProjectView.Presenter {
		
	private org.sagebionetworks.web.client.place.Project place;
	private ProjectView view;
	private String projectId;
	private NodeServiceAsync nodeService;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	
	@Inject
	public ProjectPresenter(ProjectView view, NodeServiceAsync service, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController){
		this.view = view;
		this.nodeService = service;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		
		// Set the presenter on the view
		this.view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		this.placeController = DisplayUtils.placeController;
		this.placeChanger = new PlaceChanger() {			
			@Override
			public void goTo(Place place) {
				placeController.goTo(place);
			}
		};		
		// Install the view
		panel.setWidget(view);
	}
	
	public void setPlace(org.sagebionetworks.web.client.place.Project place) {
		this.place = place;
		this.projectId = place.toToken();
		
		// load the project given in the Project Place		
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
		nodeService.deleteNode(NodeType.PROJECT, projectId, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				view.showInfo("Project Deleted", "The project was successfully deleted.");
				placeChanger.goTo(new ProjectsHome(DisplayUtils.DEFAULT_PLACE_TOKEN));
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Project delete failed.");
			}
		});
	}
	
	
	/*
	 * Protected Methods
	 */
	protected void loadFromServer() {
		// Fetch the data about this project from the server
		nodeService.getNodeJSON(NodeType.PROJECT, this.projectId, new AsyncCallback<String>() {
			
			@Override
			public void onSuccess(String result) {
				Project project = null;
				try {
					project = nodeModelCreator.createProject(result);
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger)) {
						onFailure(null);
					}
					return;
				}				
				setProject(project);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setProject(null);
				view.showErrorMessage("An error retrieving the Project occured. Please try reloading the page.");				
			}
		});
	}
	
	/**
	 * Sends the project elements to the view
	 * @param project
	 */
	protected void setProject(final Project project) {
		if (project != null) {			
			UserData currentUser = authenticationController.getLoggedInUser();
			if(currentUser != null) {
				AclUtils.getHighestPermissionLevel(NodeType.PROJECT, project.getId(), nodeService, new AsyncCallback<PermissionLevel>() {
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
						setProjectDetails(project, isAdministrator, canEdit);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage(DisplayConstants.ERROR_GETTING_PERMISSIONS_TEXT);
						setProjectDetails(project, false, false);
					}			
				});		
			} else {
				// because this is a public page, they can view
				setProjectDetails(project, false, false);
			}
		} 	
	}

	
	private void setProjectDetails(final Project project,
			boolean isAdministrator, boolean canEdit) {
		view.setProjectDetails(project.getId(), project.getName(),
				project.getDescription(), project.getCreator(),
				project.getCreationDate(), project.getStatus(), isAdministrator, canEdit);
	}	
	
}

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.services.ProjectServiceAsync;
import org.sagebionetworks.web.client.view.ProjectView;
import org.sagebionetworks.web.shared.Project;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ProjectPresenter extends AbstractActivity implements ProjectView.Presenter {
		
	private org.sagebionetworks.web.client.place.Project place;
	private ProjectView view;
	private String projectId;
	private ProjectServiceAsync service;
	
	@Inject
	public ProjectPresenter(ProjectView view, ProjectServiceAsync service){
		this.view = view;
		this.service = service;
		
		// Set the presenter on the view
		this.view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// load the project given in the Project Place		
		loadFromServer();
		
		// Install the view
		panel.setWidget(view);
	}
	
	public void setPlace(org.sagebionetworks.web.client.place.Project place) {
		this.place = place;
		this.projectId = place.toToken();
	}
	
	
	/*
	 * Protected Methods
	 */
	protected void loadFromServer() {
		// Fetch the data about this project from the server
		service.getProject(this.projectId, new AsyncCallback<Project>() {
			
			@Override
			public void onSuccess(Project result) {
				setProject(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setProject(null);
				view.showErrorMessage("An error retrieving the Dataset occured. Please try reloading the page.");				
			}
		});
	}
	
	/**
	 * Sends the project elements to the view
	 * @param project
	 */
	protected void setProject(Project project) {
		view.setProjectDetails(
				project.getId(),
				project.getName(), 
				project.getDescription(),
				project.getCreator(),
				project.getCreationDate(),
				project.getStatus());
	}
	
	
	/*
	 * Private Methods
	 */
	
	
}

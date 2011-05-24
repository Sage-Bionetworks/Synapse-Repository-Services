package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.view.ProjectsHomeView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ProjectsHomePresenter extends AbstractActivity implements ProjectsHomeView.Presenter {
		
	private ProjectsHome place;
	private ProjectsHomeView view;	
	
	@Inject
	public ProjectsHomePresenter(ProjectsHomeView view){
		this.view = view;
		// Set the presenter on the view
		this.view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(ProjectsHome place) {
		this.place = place;
	}

}

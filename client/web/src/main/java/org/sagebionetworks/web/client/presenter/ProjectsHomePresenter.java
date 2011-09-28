package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.view.ProjectsHomeView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ProjectsHomePresenter extends AbstractActivity implements ProjectsHomeView.Presenter {
		
	private ProjectsHome place;
	private ProjectsHomeView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	
	@Inject
	public ProjectsHomePresenter(ProjectsHomeView view, GlobalApplicationState globalApplicationState){
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(ProjectsHome place) {
		this.place = place;
		view.setPresenter(this);
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
	
}

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayUtils;
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

	
	@Inject
	public ProjectsHomePresenter(ProjectsHomeView view){
		this.view = view;
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
		// Set the presenter on the view
		this.view.setPresenter(this);

		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(ProjectsHome place) {
		this.place = place;
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

}

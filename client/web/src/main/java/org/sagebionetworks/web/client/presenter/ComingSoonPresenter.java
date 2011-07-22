package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.ComingSoon;
import org.sagebionetworks.web.client.view.ComingSoonView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ComingSoonPresenter extends AbstractActivity implements ComingSoonView.Presenter {
		
	private ComingSoon place;
	private ComingSoonView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;

	
	@Inject
	public ComingSoonPresenter(ComingSoonView view){
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

	public void setPlace(ComingSoon place) {
		this.place = place;
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

}

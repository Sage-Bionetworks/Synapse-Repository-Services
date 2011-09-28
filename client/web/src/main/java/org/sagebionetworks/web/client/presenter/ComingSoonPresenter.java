package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.ComingSoon;
import org.sagebionetworks.web.client.view.ComingSoonView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ComingSoonPresenter extends AbstractActivity implements ComingSoonView.Presenter {
		
	private ComingSoon place;
	private ComingSoonView view;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;

	
	@Inject
	public ComingSoonPresenter(ComingSoonView view, GlobalApplicationState globalApplicationState){
		this.globalApplicationState = globalApplicationState;
		this.view = view;
		this.placeChanger = globalApplicationState.getPlaceChanger();

		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(ComingSoon place) {
		this.place = place;
		this.view.setPresenter(this);
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

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.view.DatasetsHomeView;
import org.sagebionetworks.web.shared.SearchParameters.FromType;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class DatasetsHomePresenter extends AbstractActivity implements DatasetsHomeView.Presenter {

	private DatasetsHome place;
	private DatasetsHomeView view;
	private DynamicTablePresenter dynamicPresenter;
	
	@Inject
	public DatasetsHomePresenter(DatasetsHomeView view, DynamicTablePresenter dynamicPresenter){
		this.view = view;
		this.dynamicPresenter = dynamicPresenter;
		// Setting the type determines the default columns
		this.dynamicPresenter.setType(FromType.dataset);
		// Set the presenter on the view
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Let the dynamic presenter know we are starting.
		dynamicPresenter.start();
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(DatasetsHome place) {
		this.place = place;
	}

}

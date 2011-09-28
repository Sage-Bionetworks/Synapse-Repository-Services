package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.Layer;
import org.sagebionetworks.web.client.place.PhenoEdit;
import org.sagebionetworks.web.client.view.PhenoEditView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class PhenoEditPresenter extends AbstractActivity implements PhenoEditView.Presenter {
		
	private PhenoEdit place;
	private PhenoEditView view;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	private String layerId;
	
	@Inject
	public PhenoEditPresenter(PhenoEditView view, GlobalApplicationState globalApplicationState){
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

	public void setPlace(PhenoEdit place) {
		this.place = place;
		view.setPresenter(this);
		
		layerId = place.toToken();
		view.setEditorDetails(layerId, "layerName", "layerLink", "datasetLink");
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
	public void goBackToLayer() {
		placeChanger.goTo(new Layer(layerId, null, false));
	}

	@Override
    public String mayStop() {
        view.clear();
        return null;
    }

}

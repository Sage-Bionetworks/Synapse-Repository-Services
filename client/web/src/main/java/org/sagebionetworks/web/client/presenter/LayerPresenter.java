package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.view.LayerView;
import org.sagebionetworks.web.shared.Layer;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LayerPresenter extends AbstractActivity implements DatasetView.Presenter{
	

	private org.sagebionetworks.web.client.place.Layer place;
	private DatasetServiceAsync service;
	private LayerView view;
	private String datasetId;
	private String layerId;
	private Layer model;
	
	
	/**
	 * Everything is injected via Guice.
	 * @param view
	 * @param datasetService
	 */
	@Inject
	public LayerPresenter(LayerView view, DatasetServiceAsync datasetService) {
		this.view = view;
		this.service = datasetService;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// First refresh from the server
		refreshFromServer();		
		// add the view to the panel
		panel.setWidget(view);		
	}

	public void refreshFromServer() {
		// Fetch the data about this dataset from the server
		service.getLayer(this.datasetId, this.layerId, new AsyncCallback<Layer>() {
			
			@Override
			public void onSuccess(Layer result) {
				setLayer(result);				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setLayer(null);
				view.showErrorMessage(caught.getMessage());				
			}
		});
		
	}

	protected void setLayer(Layer result) {
		view.setLayerRow(new LayerRow(result));
	}

	public void setPlace(org.sagebionetworks.web.client.place.Layer place) {
		this.place = place;
		this.layerId = place.toToken();
	}

	@Override
	public void licenseAccepted() {
		// TODO Auto-generated method stub
	}

	@Override
	public void logDownload() {
		// TODO Auto-generated method stub
	}

}

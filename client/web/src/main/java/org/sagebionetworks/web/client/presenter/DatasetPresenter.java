package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.SearchParameters.FromType;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class DatasetPresenter extends AbstractActivity implements DatasetView.Presenter{
	

	private org.sagebionetworks.web.client.place.Dataset place;
	private DatasetServiceAsync service;
	private DynamicTablePresenter dynamicTablePresenter;
	private DatasetView view;
	private String datasetId;
	private Dataset model;
	
	
	/**
	 * Everything is injected via Guice.
	 * @param view
	 * @param datasetService
	 */
	@Inject
	public DatasetPresenter(DatasetView view, DatasetServiceAsync datasetService, DynamicTablePresenter dynamicTablePresenter) {
		this.view = view;
		this.service = datasetService;
		this.dynamicTablePresenter = dynamicTablePresenter;
		// Setting the type determines the default columns
		this.dynamicTablePresenter.setType(FromType.layers);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// First refresh from the server
		refreshFromServer();
		
		// Let the dynamic presenter know we are starting.
		dynamicTablePresenter.start();
		// add the view to the panel
		panel.setWidget(view);
		
	}

	public void refreshFromServer() {
		// Fetch the data about this dataset from the server
		service.getDataset(this.datasetId, new AsyncCallback<Dataset>() {
			
			@Override
			public void onSuccess(Dataset result) {
				setDataset(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setDataset(null);
				view.showErrorMessage(caught.getMessage());				
			}
		});
		
	}

	protected void setDataset(Dataset result) {
		this.model = result;
		view.setDatasetRow(new DatasetRow(result));
	}

	public void setPlace(org.sagebionetworks.web.client.place.Dataset place) {
		this.place = place;
		this.datasetId = place.toToken();
	}

}

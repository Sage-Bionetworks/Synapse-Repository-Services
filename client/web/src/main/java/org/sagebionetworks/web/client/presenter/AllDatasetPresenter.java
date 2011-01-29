package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.place.AllDatasets;
import org.sagebionetworks.web.client.view.AllDatasetsView;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class AllDatasetPresenter extends AbstractActivity implements AllDatasetsView.Presenter {
	
	private static Logger logger = Logger.getLogger(AllDatasetPresenter.class.getName());
	
	private AllDatasetsView view;
	private AllDatasets place;
	private DatasetServiceAsync service;
	
	private String sortKey = null;
	private boolean ascending = false;
	
	// This keeps track of which page we are on.
	private int paginationOffest = 0;
	private int paginationLength = 10;

	/**
	 * We let GIN inject all of this object's dependencies.
	 * @param place
	 * @param clientFactory
	 */
	@Inject
	public AllDatasetPresenter( AllDatasetsView view, DatasetServiceAsync datasetService) {
		this.service = datasetService;
		this.view = view;
		// let the view know this is its dependency.
		this.view.setPresenter(this);
	}
	
	public void setPlace(AllDatasets place){
		this.place = place;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// First refresh the data from the server.
		refreshFromServer();

		// Let the view know this will be its presenter
		view.setPresenter(this);
		// Add the view to the main container
		panel.setWidget(view.asWidget());
		
	}
	
	/**
	 * Fetch the latest data from the server.
	 */
	protected void refreshFromServer(){
		// Fetch this page from the server
		this.service.getAllDatasets(paginationOffest+1, paginationLength, sortKey, ascending, new AsyncCallback<PaginatedDatasets>() {
			
			@Override
			public void onSuccess(PaginatedDatasets result) {
				setPaginationData(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				// First clear the data
				setPaginationData(new PaginatedDatasets());
				// Show an error in the view
				view.showErrorMessage(caught.getMessage());
			}
		});
	}
	
	/**
	 * Public for test.
	 * @param result
	 */
	public void setPaginationData(PaginatedDatasets result) {
		// Create the dataset rows and pass them to the view
		List<Dataset> datasets = result.getResults();
		List<DatasetRow> rows = new ArrayList<DatasetRow>(datasets.size());
		for(Dataset dataset: datasets){
			rows.add(new DatasetRow(dataset));
		}
		// Pass the results to the view
		view.setDatasetRows(rows, paginationOffest, paginationLength, result.getTotalNumberOfResults(), sortKey, ascending);
	}

	@Override
	public void pageTo(int start, int length) {
		this.paginationOffest = start;
		this.paginationLength = length;
		refreshFromServer();
	}

	@Override
	public void toggleSort(String columnKey) {
		// We need to resychn
		sortKey = columnKey;
		ascending = !ascending;
		refreshFromServer();
	}

}

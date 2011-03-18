package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.widget.licensebox.LicenceServiceAsync;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class DatasetPresenter extends AbstractActivity implements DatasetView.Presenter{	

	private org.sagebionetworks.web.client.place.Dataset place;
	private DatasetServiceAsync service;
	private LicenceServiceAsync licenseService;
	private DatasetView view;
	private String datasetId;
	private Dataset model;
	private final static String licenseAgreementText = "<p>Copyright 2011 Sage Bionetworks</p><p>Licensed under the Apache License, Version 2.0 (the \"License\");you may not use this file except in compliance with the License.You may obtain a copy of the License at</p><p>&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0</p><p>Unless required by applicable law or agreed to in writing, softwaredistributed under the License is distributed on an \"AS IS\" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.See the License for the specific language governing permissions andlimitations under the License.</p>";
	
	/**
	 * Everything is injected via Guice.
	 * @param view
	 * @param datasetService
	 */
	@Inject
	public DatasetPresenter(DatasetView view, DatasetServiceAsync datasetService, LicenceServiceAsync licenseService) {
		this.view = view;
		this.view.setPresenter(this);
		this.service = datasetService;
		this.licenseService = licenseService;
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

		// check if license agreement has been accepted
		// TODO !!!! use real username !!!!
		licenseService.hasAccepted("GET-USERNAME", result.getUri(), new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {				
				view.showErrorMessage("Dataset downloading unavailable. Please try reloading the page.");
				view.disableLicensedDownloads(true);
			}

			@Override
			public void onSuccess(Boolean hasAccepted) {								
				view.requireLicenseAcceptance(!hasAccepted);
				// TODO !!!! get actual license agreement text (and citation if needed) !!!!
				LicenseAgreement agreement = new LicenseAgreement();
				agreement.setLicenseHtml(licenseAgreementText);
				view.setLicenseAgreement(agreement);
				
				// set download files
				// TODO : !!!! get real downloads from dataset object !!!!		
				List<FileDownload> downloads = new ArrayList<FileDownload>();
				downloads.add(new FileDownload("http://google.com", "Lusis Dataset", "3f37dba446d160543ab5732f04726fe0"));
				view.setDatasetDownloads(downloads);
			}
		});
	}

	public void setPlace(org.sagebionetworks.web.client.place.Dataset place) {
		this.place = place;
		this.datasetId = place.toToken();
	}

	@Override
	public void licenseAccepted() {
		// TODO : !!!! get real username !!!!
		licenseService.acceptLicenseAgreement("GET-USERNAME", model.getUri(), new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				// tell the user that they will need to accept this again in the future?
			}

			@Override
			public void onSuccess(Boolean licenseAccepted) {
				// tell the user that they will need to accept this again in the future (if licenseAccepted == false)?
			}
		});		
	}

	@Override
	public void logDownload() {		
	}
}

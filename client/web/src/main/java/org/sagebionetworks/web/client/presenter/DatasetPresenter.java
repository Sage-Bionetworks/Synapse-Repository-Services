package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.shared.Agreement;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EULA;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

@SuppressWarnings("unused")
public class DatasetPresenter extends AbstractActivity implements DatasetView.Presenter{	

	private org.sagebionetworks.web.client.place.Dataset place;
	private PlaceController placeController; 
	private PlaceChanger placeChanger;
	private NodeServiceAsync nodeService;
	private boolean hasAcceptedLicenseAgreement;
	private LicenceServiceAsync licenseService;
	private LicenseAgreement licenseAgreement;
	private DatasetView view;
	private String datasetId;
	private Dataset model;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	
	/**
	 * Everything is injected via Guice.
	 * @param view
	 * @param datasetService
	 */
	@Inject
	public DatasetPresenter(DatasetView view, NodeServiceAsync nodeService, LicenceServiceAsync licenseService, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, GlobalApplicationState globalApplicationState) {
		this.view = view;
		this.view.setPresenter(this);
		this.nodeService = nodeService;
		this.licenseService = licenseService;		
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		
		this.hasAcceptedLicenseAgreement = false;
	}

	/**
	 * Initial start of this presenter upon instantiation
	 */
	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		this.placeController = globalApplicationState.getPlaceController();		
		this.placeChanger = new PlaceChanger() {			
			@Override
			public void goTo(Place place) {
				placeController.goTo(place);
			}
		};
		// add the view to the panel
		panel.setWidget(view);
		
	}
	
	/**
	 * Called when focus is brought to this presenter in the UI
	 * @param place
	 */
	public void setPlace(org.sagebionetworks.web.client.place.Dataset place) {		
		this.place = place;
		this.datasetId = place.toToken(); 
		
		refreshFromServer();
	}

	/**
	 * Calls the service and refreshes all page objects
	 */
	public void refreshFromServer() {
		// Fetch the data about this dataset from the server
		nodeService.getNodeJSON(NodeType.DATASET, this.datasetId, new AsyncCallback<String>() {			
			@Override
			public void onSuccess(String result) {
				Dataset resultDataset = null;
				try {
					resultDataset = nodeModelCreator.createDataset(result);
				} catch (RestServiceException ex) {					
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {					
						onFailure(null);					
					} 
					return;
				}
				setDataset(resultDataset);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setDataset(null);
				view.showErrorMessage("An error retrieving the Dataset occured. Please try reloading the page.");				
			}
		});
		
	}

	
	@Override
	public void licenseAccepted() {
		if(!hasAcceptedLicenseAgreement && licenseAgreement != null) {
			Agreement agreement = new Agreement();							
			agreement.setEulaId(licenseAgreement.getEulaId());
			agreement.setName("SynapseWeb Agreement");
			agreement.setDatasetId(model.getParentId());
			
			nodeService.createNode(NodeType.AGREEMENT, agreement.toJson(), new AsyncCallback<String>() {
				@Override
				public void onSuccess(String result) {
					// agreement saved
				}
	
				@Override
				public void onFailure(Throwable caught) {
					view.showInfo("Error", DisplayConstants.ERROR_FAILED_PERSIST_AGREEMENT_TEXT);
				}
			});
		}
	}

	@Override
	public void refresh() {
		refreshFromServer();
	}

	@Override
	public void goTo(Place place) {
		this.placeChanger.goTo(place);
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
	public boolean downloadAttempted() {
		if(authenticationController.getLoggedInUser() != null) {
			return true;
		} else {
			view.showInfo("Login Required", "Please Login to download data.");			
			placeChanger.goTo(new LoginPlace(DisplayUtils.DEFAULT_PLACE_TOKEN));
		}
		return false;
	}

	@Override
	public void delete() {
		nodeService.deleteNode(NodeType.DATASET, datasetId, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				view.showInfo("Dataset Deleted", "The dataset was successfully deleted.");
				placeChanger.goTo(new ProjectsHome(DisplayUtils.DEFAULT_PLACE_TOKEN));
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Dataset delete failed.");
			}
		});
	}
	
	/*
	 * Protected & Private Methods
	 */
	protected void setDataset(final Dataset dataset) {
		this.model = dataset;
		if(dataset != null) {
			UserData currentUser = authenticationController.getLoggedInUser();
			if(currentUser != null) {
				AclUtils.getHighestPermissionLevel(NodeType.PROJECT, dataset.getId(), nodeService, new AsyncCallback<PermissionLevel>() {
					@Override
					public void onSuccess(PermissionLevel result) {
						boolean isAdministrator = false;
						boolean canEdit = false;
						if(result == PermissionLevel.CAN_EDIT) {
							canEdit = true;
						} else if(result == PermissionLevel.CAN_ADMINISTER) {
							canEdit = true;
							isAdministrator = true;
						}
						setDatasetDetails(dataset, isAdministrator, canEdit);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage(DisplayConstants.ERROR_GETTING_PERMISSIONS_TEXT);
						// because this is a public page, they can view 
						setDatasetDetails(dataset, false, false);						
					}			
				});
			} else {
				// because this is a public page, they can view
				setDatasetDetails(dataset, false, false);
			}
		} 				
	}

	protected void setDatasetDetails(Dataset result,
			final boolean isAdministrator, final boolean canEdit) {		
		nodeService.getNodeAnnotationsJSON(NodeType.DATASET, model.getId(), new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result) {
				Annotations annotations = null;
				try {
					annotations = nodeModelCreator.createAnnotations(result);
					Map<String,List<Long>> longAnnotations = annotations.getLongAnnotations();
					Map<String,List<String>> stringAnnotations = annotations.getStringAnnotations();
					Map<String,List<Date>> dateAnnotations = annotations.getDateAnnotations();
					List<String> diseases = stringAnnotations.containsKey("Disease") ? stringAnnotations.get("Disease") : new ArrayList<String>();
					List<String> institutions = stringAnnotations.containsKey("Institution") ? stringAnnotations.get("Institution") : new ArrayList<String>();
					List<String> postingRestrictionList = stringAnnotations.containsKey("Posting_Restriction") ? stringAnnotations.get("Posting_Restriction") : new ArrayList<String>();
					List<String> species = stringAnnotations.containsKey("Species") ? stringAnnotations.get("Species") : new ArrayList<String>();
					List<String> types = stringAnnotations.containsKey("Type") ? stringAnnotations.get("Type") : new ArrayList<String>();
					List<String> tissueTumorList = stringAnnotations.containsKey("Tissue_Tumor") ? stringAnnotations.get("Tissue_Tumor") : new ArrayList<String>();
					List<String> citationList = stringAnnotations.containsKey("citation") ? stringAnnotations.get("citation") : new ArrayList<String>();
					List<Long> nSamplesList = longAnnotations.containsKey("Number_of_Samples") ? longAnnotations.get("Number_of_Samples") : new ArrayList<Long>();
					List<Long> nDownloadsList = longAnnotations.containsKey("number_of_downloads") ? longAnnotations.get("number_of_downloads") : new ArrayList<Long>();
					List<Long> nFollowersList = longAnnotations.containsKey("number_of_followers") ? longAnnotations.get("number_of_followers") : new ArrayList<Long>();
					List<Long> pubmedIdList = longAnnotations.containsKey("pubmed_id") ? longAnnotations.get("pubmed_id") : new ArrayList<Long>();
					List<Date> lastModifiedDateList = dateAnnotations.containsKey("dateAnnotations") ? dateAnnotations.get("dateAnnotations") : new ArrayList<Date>();
												
									
					int studySize = 0;
					if(longAnnotations.containsKey("Number_of_Samples")) {
						studySize = longAnnotations.get("Number_of_Samples").get(0).intValue();
					}
					String postingRestriction = postingRestrictionList.size() > 0 ? postingRestrictionList.get(0) : "";
					int nOtherPublications = 0; // TODO : get number of other pubs
					int nSamples = nSamplesList.size() > 0 ? nSamplesList.get(0).intValue() : 0;
					int nFollowers = nFollowersList.size() > 0 ? nFollowersList.get(0).intValue() : 0;
					int nDownloads = nDownloadsList.size() > 0 ? nDownloadsList.get(0).intValue() : 0;
					Integer pubmedId = pubmedIdList.size() > 0 ? pubmedIdList.get(0).intValue() : null;
					Date lastModifiedDate = lastModifiedDateList.size() > 0 ? lastModifiedDateList.get(0) : null;
					String citation = citationList.size() > 0 ? citationList.get(0) : "";
					String tissueTumor = tissueTumorList.size() > 0 ? tissueTumorList.get(0) : "";
					
					String referencePublicationUrl = pubmedId != null ? "http://www.ncbi.nlm.nih.gov/pubmed/" + pubmedId : null;
					
					 view.setDatasetDetails(model.getId(),
					 model.getName(),
					 model.getDescription(),
					 diseases.toArray(new String[diseases.size()]), 
					 species.toArray(new String[species.size()]),
					 studySize,
					 tissueTumor,
					 types.toArray(new String[types.size()]),
					 citation, 
					 referencePublicationUrl,
					 nOtherPublications,
					 "#ComingSoon:0", // TODO : change this to be real
					 model.getCreationDate(),
					 model.getReleaseDate(),
					 lastModifiedDate,
					 model.getCreator(),
					 institutions.toArray(new String[institutions.size()]),  
					 nFollowers,
					 "#ComingSoon:0", // TODO : view followers url, change this to be real
					 postingRestriction,
					 "#ComingSoon:0", // TODO : release notes url. change this to be real
					 model.getStatus(),
					 model.getVersion(),
					 nSamples,
					 nDownloads,
					 citation,
					 pubmedId, 
					 isAdministrator, 
					 canEdit);
					 
					 // set the license agreement
					 setLicenseAgreement();
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
						onFailure(null);
					}
					return;
				}									
			}


			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error retrieving Dataset details occured. Please try reloading the page.");
			}

		});				
	}	
	
	protected void setLicenseAgreement() {
		final String eulaId = model.getEulaId();
		UserData currentUser = authenticationController.getLoggedInUser();
		if(eulaId != null && currentUser != null) {			
			// now query to see if user has accepted the agreement			
			licenseService.hasAccepted(currentUser.getEmail(), eulaId, model.getId(), new AsyncCallback<Boolean>() {
				@Override
				public void onSuccess(Boolean hasAccepted) {
					view.requireLicenseAcceptance(!hasAccepted);
		
					// load license agreement (needed for viewing even if hasAccepted)
					nodeService.getNodeJSON(NodeType.EULA, eulaId, new AsyncCallback<String>() {
						@Override
						public void onSuccess(String eulaJson) {
							EULA eula = null;
							try {
								eula = nodeModelCreator.createEULA(eulaJson);
							} catch (RestServiceException ex) {
								DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser());
								onFailure(null);								
								return;
							}
							if(eula != null) {
								// set licence agreement text
								licenseAgreement = new LicenseAgreement();				
								licenseAgreement.setLicenseHtml(eula.getAgreement());
								licenseAgreement.setEulaId(eulaId);
								view.setLicenseAgreement(licenseAgreement);
							} else {
								setDownloadFailure();
							}
						}
						
						@Override
						public void onFailure(Throwable caught) {
							setDownloadFailure();
						}									
					});
					
					// load download locations
					loadDownloadLocations();				
					
				}
				
				@Override
				public void onFailure(Throwable caught) {
					setDownloadFailure();
				}
			});									
		}
	}

	protected void setDownloadFailure() {
		view.showErrorMessage("Dataset downloading unavailable. Please try reloading the page.");	
		view.disableLicensedDownloads(true);
	}	

	protected void loadDownloadLocations() {
		// TODO Implement (Nicole)		
	}

	
}



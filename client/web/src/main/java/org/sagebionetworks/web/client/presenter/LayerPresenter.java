package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.PhenoEdit;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.LayerView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.shared.Agreement;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EULA;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LayerPresenter extends AbstractActivity implements LayerView.Presenter {	

	private org.sagebionetworks.web.client.place.Layer place;
	private NodeServiceAsync nodeService;
	private PlaceChanger placeChanger;
	private LayerView view;
	private String layerId;	
	private Boolean showDownload;
	private Layer layerModel;
	private boolean hasAcceptedLicenseAgreement;
	private LicenceServiceAsync licenseService;
	private LicenseAgreement licenseAgreement;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private boolean iisAdministrator;
	private boolean ccanEdit;
	private GlobalApplicationState globalApplicationState;
	
	/**
	 * Everything is injected via Guice.
	 * @param view
	 * @param datasetService
	 */
	@Inject
	public LayerPresenter(LayerView view, NodeServiceAsync nodeService, LicenceServiceAsync licenseService, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, GlobalApplicationState globalApplicationState) {
		this.view = view;
		this.nodeService = nodeService;
		this.licenseService = licenseService;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		this.hasAcceptedLicenseAgreement = false;
		
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// add the view to the panel
		panel.setWidget(view);		
	}

	/**
	 * Called when this presenter gets focus from AppActivityManager
	 * @param place
	 */
	public void setPlace(org.sagebionetworks.web.client.place.Layer place) {
		this.place = place;
		this.layerId = place.getLayerId();		
		this.showDownload = place.getDownload();
		view.setPresenter(this);
		refresh();
	}	

	@Override
	public void licenseAccepted() {		
		if(!hasAcceptedLicenseAgreement && licenseAgreement != null) {
			UserData user = authenticationController.getLoggedInUser();
			Agreement agreement = new Agreement();							
			agreement.setEulaId(licenseAgreement.getEulaId());
			agreement.setDatasetId(layerModel.getParentId());
			
			nodeService.createNode(NodeType.AGREEMENT, agreement.toJson(), new AsyncCallback<String>() {
				@Override
				public void onSuccess(String result) {
					view.showInfo("Saved", "Agreement acceptance saved.");
				}
	
				@Override
				public void onFailure(Throwable caught) {
					view.showInfo("Error", DisplayConstants.ERROR_FAILED_PERSIST_AGREEMENT_TEXT);
				}
			});
		}
	}

	/**
	 * Reloads all asynchronous data from the server	
	 */
	@Override
	public void refresh() {
		view.clear();		
		// Fetch the data about this dataset from the server
		nodeService.getNodeJSON(NodeType.LAYER, this.layerId, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String layerJson) {							
				try {
					layerModel = nodeModelCreator.createLayer(layerJson);					
				} catch (RestServiceException ex) {
					if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {
						onFailure(null);
					}
					return;
				}					
				// Load calls that required the model 
				loadLicenseAgreement(layerModel, showDownload);
				loadPermissionLevel(layerModel);
				loadDownloadLocations(layerModel, showDownload); 
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error occured retrieving this Layer. Please try reloading the page.");
			}			
		});		
				
		loadLayerPreview();		
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
			if(placeChanger != null) {
				placeChanger.goTo(new LoginPlace(DisplayUtils.DEFAULT_PLACE_TOKEN));
			}
		}
		return false;
	}

	@Override
	public void delete() {
		nodeService.deleteNode(NodeType.LAYER, layerId, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				view.showInfo("Layer Deleted", "The layer was successfully deleted.");
				if(placeChanger != null) {
					placeChanger.goTo(new ProjectsHome(DisplayUtils.DEFAULT_PLACE_TOKEN));
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Layer delete failed.");
			}
		});
	}

	
	/*
	 * Private Asynchronous load methods
	 */
	
	/**
	 * Loads the Permission levels for this user  
	 * @param model Layer model object
	 */
	public void loadPermissionLevel(final Layer model) {
		if(model != null) {			
			UserData currentUser = authenticationController.getLoggedInUser();
			if(currentUser != null) {
				AclUtils.getHighestPermissionLevel(NodeType.LAYER, model.getId(), nodeService, new AsyncCallback<PermissionLevel>() {
					@Override
					public void onSuccess(PermissionLevel result) {
						iisAdministrator = false;
						ccanEdit = false;
						if(result == PermissionLevel.CAN_EDIT) {
							ccanEdit = true;
						} else if(result == PermissionLevel.CAN_ADMINISTER) {
							ccanEdit = true;
							iisAdministrator = true;
						}
						setLayerDetails(model, iisAdministrator, ccanEdit);
					}
					
					@Override
					public void onFailure(Throwable caught) {				
						view.showErrorMessage(DisplayConstants.ERROR_GETTING_PERMISSIONS_TEXT);
						iisAdministrator = false;
						ccanEdit = false;
						setLayerDetails(model, iisAdministrator, ccanEdit);
					}			
				});
			} else {
				// because this is a public page, they can view
				iisAdministrator = false;
				ccanEdit = false;
				setLayerDetails(model, iisAdministrator, ccanEdit);
			}
		}
		
	}
	
	/**
	 * Load the Preview for this layer from the server
	 */
	public void loadLayerPreview() {				
		// get the preview string to get file header order, then get the previewAsData
		nodeService.getNodePreview(NodeType.LAYER, this.layerId, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String pagedResultString) {
				LayerPreview layerPreview = null;				
				try {
					PagedResults  pagedResult = nodeModelCreator.createPagedResults(pagedResultString);
					if(pagedResult != null) {
						List<String> results = pagedResult.getResults();
						if(results.size() > 0) {
							layerPreview = nodeModelCreator.createLayerPreview(results.get(0));
						} else {
							view.showLayerPreviewUnavailable();
							onFailure(null);
							return;
						}					
					}
				} catch (RestServiceException ex) {
					DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser());
					onFailure(null);					
					return;
				}				

				if(layerPreview != null) {
					final LayerPreview layerPreviewFinal = layerPreview;
					// get column display order, if possible from the layer preview
					final List<String> columnDisplayOrder = layerPreviewFinal.getHeaders();
					final Map<String, String> columnDescriptions = new HashMap<String, String>();
					final Map<String, String> columnUnits = new HashMap<String, String>();
	
					nodeService.getNodeAnnotationsJSON(NodeType.LAYER, layerId, new AsyncCallback<String>() {

						@Override
						public void onFailure(Throwable caught) {
							// set the table even if we can't get the annotations
							callSetLayerPreviewTable();
						}

						@Override
						public void onSuccess(String annotationJsonString) {

							// get columns descriptions from service
							try {
								Annotations annotations = nodeModelCreator.createAnnotations(annotationJsonString);
								Map<String, List<String>> strAnnotations = annotations.getStringAnnotations();
								for(String annotKey : strAnnotations.keySet()) {
									// process descriptions									
									if(annotKey.startsWith(DisplayUtils.LAYER_COLUMN_DESCRIPTION_KEY_PREFIX)) {
										String colName = annotKey.substring(DisplayUtils.LAYER_COLUMN_DESCRIPTION_KEY_PREFIX.length());
										List<String> values = strAnnotations.get(annotKey);
										if(values != null && values.size() > 0) {										
											columnDescriptions.put(colName, values.get(0));
											continue; // skip the units test
										}
									}

									// process units									
									if(annotKey.startsWith(DisplayUtils.LAYER_COLUMN_UNITS_KEY_PREFIX)) {
										String colName = annotKey.substring(DisplayUtils.LAYER_COLUMN_UNITS_KEY_PREFIX.length());
										List<String> values = strAnnotations.get(annotKey);
										if(values != null && values.size() > 0) {										
											columnUnits.put(colName, values.get(0));
										}
									}
								}
							} catch (RestServiceException ex) {
								DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser());
							}												
							callSetLayerPreviewTable();
						}
						
						private void callSetLayerPreviewTable() {
							// append units onto description
							for(String key : columnUnits.keySet()) {
								String units = columnUnits.get(key);
								columnDescriptions.put(key, columnDescriptions.get(key) + " (" + units + ")");
							}		
							
							view.setLayerPreviewTable(layerPreviewFinal.getRows(), columnDisplayOrder, columnDescriptions, columnUnits);														
						}
					});
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				// continue
				view.showLayerPreviewUnavailable();
			}
		});		

	}

	/**
	 * Sets the Layer model details into the view
	 * @param model The Layer model object
	 * @param isAdministrator Allows Administrator menus to be displayed
	 * @param canEdit Allows Edit menus to be displayed
	 */
	private void setLayerDetails(Layer model, boolean isAdministrator, boolean canEdit) {
		// process the layer and send values to view
		if(model != null) {
			view.setLayerDetails(model.getId(), 
								 model.getName(), 
							 	 model.getProcessingFacility(), 
								 model.getQcBy(), "#ComingSoon:0",
								 "qc_script.R", "#ComingSoon:0",
								 model.getQcDate(),
								 model.getDescription(),
								 5,
								 Integer.MAX_VALUE, // TODO : get total number of rows in layer
								 "Public", // TODO : replace with security object
								 "<a href=\"#Dataset:"+ model.getParentId() +"\">Dataset</a>", // TODO : have dataset name included in layer metadata
								 model.getPlatform(), 
								 isAdministrator, 
								 canEdit,
								 model.getType());
		}
	}

	/**
	 * Loads the License Agreement
	 * @param model Layer model object
	 */
	public void loadLicenseAgreement(final Layer model, final Boolean showDownload) {		
		// get Dataset to get its EULA id
		if(model != null) {
			nodeService.getNodeJSON(NodeType.DATASET, model.getParentId(), new AsyncCallback<String>() {
				@Override
				public void onSuccess(String datasetJson) {
					Dataset dataset = null;
					try {
						dataset = nodeModelCreator.createDataset(datasetJson);
					} catch (RestServiceException ex) {
						DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser());
						onFailure(null);					
						return;
					}
					if(dataset != null) {
						// Dataset found, get EULA id if exists
						final String eulaId = dataset.getEulaId();
						if(eulaId == null) {
							// No EULA id means that this has open downloads
							view.requireLicenseAcceptance(false);
							licenseAgreement = null;
							view.setLicenseAgreement(licenseAgreement);						
						} else {
							// EULA required
							// now query to see if user has accepted the agreement
							UserData currentUser = authenticationController.getLoggedInUser();
							if(currentUser == null && showDownload) {
								view.showInfo(DisplayConstants.ERROR_TITLE_LOGIN_REQUIRED, DisplayConstants.ERROR_LOGIN_REQUIRED);
								if(placeChanger != null) {
									placeChanger.goTo(new LoginPlace(DisplayUtils.DEFAULT_PLACE_TOKEN));
								}
								return;
							}
							
							// Check to see if the license has already been accepted
							licenseService.hasAccepted(currentUser.getEmail(), eulaId, model.getParentId(), new AsyncCallback<Boolean>() {
								@Override
								public void onSuccess(final Boolean hasAccepted) {
									hasAcceptedLicenseAgreement = hasAccepted;
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
												showDownloadLoadFailure();
											}
										}
										
										@Override
										public void onFailure(Throwable caught) {
											showDownloadLoadFailure();
										}									
									});
								}
								
								@Override
								public void onFailure(Throwable caught) {
									showDownloadLoadFailure();								
								}
							});									
						}
					} else {
						showDownloadLoadFailure();
					}
				} 
	
				@Override
				public void onFailure(Throwable caught) {
					showDownloadLoadFailure();
				}
			});
		}
	}
	
	/**
	 * Loads the download locations for the given Layer 
	 * @param model Layer model object
	 */
	public void loadDownloadLocations(final Layer model, final Boolean showDownload) {
		view.showDownloadsLoading();
		if(model != null) {
			nodeService.getNodeLocations(NodeType.LAYER, model.getId(), new AsyncCallback<String>() {
				@Override
				public void onSuccess(String pagedResultString) {				
					List<FileDownload> downloads = new ArrayList<FileDownload>();						
					try {							
						PagedResults pagedResult = nodeModelCreator.createPagedResults(pagedResultString);
						if(pagedResult != null) {
							List<String> results = pagedResult.getResults();
							for(String fileDownloadString : results) {
								DownloadLocation downloadLocation = nodeModelCreator.createDownloadLocation(fileDownloadString);								
								if(downloadLocation != null && downloadLocation.getPath() != null) { 
									FileDownload dl = new FileDownload(downloadLocation.getPath(), "Download " + model.getName(), downloadLocation.getMd5sum(), downloadLocation.getContentType());
									downloads.add(dl);
								}	
							}
						}
					} catch (ForbiddenException ex) {
						// if user hasn't signed an existing use agreement, a ForbiddenException is thrown. Do not alert user to this 
						onFailure(null);
					} catch (RestServiceException ex) {
						DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser());
						onFailure(null);					
						return;
					}				
					view.setLicensedDownloads(downloads);
					
					// show download if requested
					if(showDownload != null && showDownload == true) {
						if(downloadAttempted()) {
							view.showDownload();
						}
					}
				}
				@Override
				public void onFailure(Throwable caught) {				
					view.setDownloadUnavailable();				
				}
			});
		}
	}

	@Override
	public void openPhenoTypeEditor() {
		placeChanger.goTo(new PhenoEdit(layerId));
	}
	
	/*
	 * Private Methods
	 */
	private void showDownloadLoadFailure() {
		view.showErrorMessage("Dataset downloading unavailable. Please try reloading the page.");
		view.setDownloadUnavailable();
		view.disableLicensedDownloads(true);
	}

}



package org.sagebionetworks.web.client.widget.licenseddownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Licensed Downloader Presenter
 * @author dburdick
 *
 */
public class LicensedDownloader implements LicensedDownloaderView.Presenter, SynapseWidgetPresenter {
	
	private LicensedDownloaderView view;
	private NodeServiceAsync nodeService;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	private LicenceServiceAsync licenseService;
	private SynapseClientAsync synapseClient;
	private JSONObjectAdapter jsonObjectAdapterProvider;

	private boolean requireLicenseAcceptance;
	private AsyncCallback<Void> licenseAcceptedCallback;	
	private boolean hasAcceptedLicenseAgreement;
	private LicenseAgreement licenseAgreement;
	
	@Inject
	public LicensedDownloader(LicensedDownloaderView view,
			NodeServiceAsync nodeService, LicenceServiceAsync licenseService,
			NodeModelCreator nodeModelCreator,
			AuthenticationController authenticationController,
			GlobalApplicationState globalApplicationState,
			SynapseClientAsync synapseClient, 
			JSONObjectAdapter jsonObjectAdapter) {
		this.view = view;
		this.nodeService = nodeService;
		this.licenseService = licenseService;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		this.synapseClient = synapseClient;
		this.jsonObjectAdapterProvider = jsonObjectAdapter;
		
		view.setPresenter(this);		
	}

	/*
	 * Public methods
	 */		

	/**
	 * Use with your own download button/link. 
	 * @param entity
	 * @param showDownloadLocations
	 */
	public void configureHeadless(Entity entity, boolean showDownloadLocations) {
		view.setPresenter(this);
		loadLicenseAgreement(entity);
		loadDownloadLocations(entity, showDownloadLocations);		
	}
	
	/**
	 * Returns a standard download button
	 * @param entity
	 * @param showDownloadLocations
	 * @return
	 */
	public Widget asWidget(Entity entity, boolean showDownloadLocations) {
		configureHeadless(entity, showDownloadLocations);
		return view.asWidget();
	}	
	
	/**
	 * does nothing use asWidget(Entity entity) 
	 */
	@Override
	public Widget asWidget() { 
		return null;
	}	

		
	/**
	 * Loads the License Agreement
	 * @param model Layer model object
	 */
	public void loadLicenseAgreement(final Entity entity) {
		if(entity != null) {
			// find the EULA id, be it in this entity or higher in its parents
			findEulaId(entity, new AsyncCallback<String>() {
				@Override
				public void onSuccess(final String eulaId) {
					if(eulaId == null) {
						// No EULA id means that this has open downloads
						licenseAgreement = null;	
						setLicenseAgreement(licenseAgreement);
					} else {
						// EULA required
						// now query to see if user has accepted the agreement
						UserData currentUser = authenticationController.getLoggedInUser();
						if(currentUser == null) {								
							view.setUnauthorizedDownloads();								
							return;
						}  
						
						// Check to see if the license has already been accepted
						licenseService.hasAccepted(currentUser.getEmail(), eulaId, entity.getParentId(), new AsyncCallback<Boolean>() {
							@Override
							public void onSuccess(final Boolean hasAccepted) {
								hasAcceptedLicenseAgreement = hasAccepted;
								setRequireLicenseAcceptance(!hasAccepted);

								// load license agreement (needed for viewing even if hasAccepted)
								nodeService.getNodeJSON(NodeType.EULA, eulaId, new AsyncCallback<String>() {
									@Override
									public void onSuccess(String eulaJson) {
										Eula eula = null;
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
											setLicenseAgreement(licenseAgreement);
										} else {
											view.showDownloadFailure();
										}
									}
									
									@Override
									public void onFailure(Throwable caught) {
										view.showDownloadFailure();
									}									
								});
							}
							
							@Override
							public void onFailure(Throwable caught) {
								view.showDownloadFailure();								
							}
						});									
					}
				}

				@Override
				public void onFailure(Throwable caught) {
					view.showDownloadFailure();
				}
			});
		}
	}
	
	
	/**
	 * Loads the download locations for the given Layer 
	 * @param entity Layer model object
	 */
	public void loadDownloadLocations(final Entity entity, final Boolean showDownloadLocations) {		
		if(entity != null) {
			view.showDownloadsLoading();		
			if(entity instanceof Locationable) {
				Locationable locationable = (Locationable)entity;
				List<LocationData> locations = locationable.getLocations();				
				if(locations != null && locations.size() > 0) {
					this.view.setDownloadLocations(locations);
					
					// show download if requested
					if(showDownloadLocations != null && showDownloadLocations == true) {
						if(downloadAttempted()) {
							showWindow();
						} else {
							view.setUnauthorizedDownloads();
						}
					}			
				} else {
					view.setNoDownloads();
				}
			} else {
				view.setNoDownloads();
			}
		}
	}
			
	// TODO : this is not needed
	@Override
	public void setPlaceChanger(PlaceChanger placeChanger) {
		this.placeChanger = placeChanger;		
	}	
	
	private void setDownloadUnavailable() {
		this.view.setDownloadUrls(null);
		// TODO : more?
	}		

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

	/**
	 * Recursively look up the entity hierarchy until we find a eula id
	 * @param root
	 * @param callback
	 */
	private void findEulaId(Entity root, final AsyncCallback<String> callback) {
		if(root == null) {
			callback.onFailure(null);
			return;			
		}
		try {
			if(root.getJSONSchema() == null) {
				callback.onFailure(null);
				return;
			}				
			ObjectSchema schema = new ObjectSchema(jsonObjectAdapterProvider.createNew(root.getJSONSchema()));
			Map<String, ObjectSchema> properties = schema.getProperties();
			if(properties.containsKey(DisplayUtils.ENTITY_EULA_ID_KEY)) {
				// this Entity contains a eula field
				JSONObjectAdapter rootObj = jsonObjectAdapterProvider.createNew();
				root.writeToJSONObject(rootObj);
				// We have found a parent entity with a eula, and regardless of what it holds we have a success condition
				String eulaId = null;
				if(rootObj.has(DisplayUtils.ENTITY_EULA_ID_KEY)) {
					eulaId = rootObj.getString(DisplayUtils.ENTITY_EULA_ID_KEY);
				} 
				callback.onSuccess(eulaId);			
			} else {
				// look up parent
				String parentId = root.getParentId();
				if(parentId != null) {
					synapseClient.getEntity(parentId, new AsyncCallback<EntityWrapper>() {						
						@Override
						public void onSuccess(EntityWrapper result) {							
							Entity parentEntity = null;
							try {
								// Recurse with parent 
								parentEntity = nodeModelCreator.createEntity(result);
								findEulaId(parentEntity, callback);
							} catch (RestServiceException ex) {					
								DisplayUtils.handleServiceException(ex, globalApplicationState.getPlaceChanger(), authenticationController.getLoggedInUser());					
								onFailure(null);													 							
							}											
						}
						@Override
						public void onFailure(Throwable caught) {
							callback.onFailure(caught);
						}
					});
				} else {
					// hit root and didn't find a eula
					callback.onFailure(null);
				}
			}			
		} catch (JSONObjectAdapterException e) {
			callback.onFailure(null);
		}
	}
		
	public void showWindow() {
		this.view.showWindow();
	}
	
	public void hideWindow() {
		this.view.hideWindow();
	}
		
	public void setLicenseAgreement(LicenseAgreement agreement) {
		if (agreement != null) {
			if (agreement.getCitationHtml() != null) {
				view.setCitationHtml(agreement.getCitationHtml());
			}
			view.setLicenseHtml(agreement.getLicenseHtml());
		} else {
			this.setRequireLicenseAcceptance(false);
		}
	}
	
	@Deprecated
	public void setDownloadUrls(List<FileDownload> downloads) {		
		this.view.setDownloadUrls(downloads);
	}
	
	public void showLoading() {
		this.view.showDownloadsLoading();
	}
		
	public void clear() {
		view.clear();
		this.licenseAcceptedCallback = null;		
	}
	
	public void setRequireLicenseAcceptance(boolean requireLicenseAcceptance) {
		this.requireLicenseAcceptance = requireLicenseAcceptance;
		this.view.setLicenceAcceptanceRequired(requireLicenseAcceptance);
	}
	
	@Deprecated
	public void setLicenseAcceptedCallback(AsyncCallback<Void> callback) {
		this.licenseAcceptedCallback = callback;
	}
	
	@Override
	public void setLicenseAccepted() {		
		// send out to using class to let know of acceptance
		licenseAcceptedCallback.onSuccess(null);
		// allow the view to skip the license agreement now and show the download view
		setRequireLicenseAcceptance(false);
		showWindow();		
	}

}

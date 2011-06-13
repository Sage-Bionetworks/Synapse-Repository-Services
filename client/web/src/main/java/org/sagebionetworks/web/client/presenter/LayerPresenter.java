package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.view.LayerView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LayerPresenter extends AbstractActivity implements LayerView.Presenter{	

	private org.sagebionetworks.web.client.place.Layer place;
	private DatasetServiceAsync service;
	private NodeServiceAsync nodeService;
	private LayerView view;
	private String layerId;	
	private String datasetId;
	private Boolean showDownload;
	private Layer model;
	private LayerPreview layerPreview;
	private LicenceServiceAsync licenseService;
	private final static String licenseAgreementText = "<p><b><larger>Copyright 2011 Sage Bionetworks</larger></b><br/><br/></p><p>Licensed under the Apache License, Version 2.0 (the \"License\"). You may not use this file except in compliance with the License. You may obtain a copy of the License at<br/><br/></p><p>&nbsp;&nbsp;<a href=\"http://www.apache.org/licenses/LICENSE-2.0\" target=\"new\">http://www.apache.org/licenses/LICENSE-2.0</a><br/><br/></p><p>Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions andlimitations under the License.<br/><br/></p><p><strong><a name=\"definitions\">1. Definitions</a></strong>.<br/><br/></p> <p>\"License\" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.<br/><br/></p> <p>\"Licensor\" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.<br/><br/></p> <p>\"Legal Entity\" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity. For the purposes of this definition, \"control\" means (i) the power, direct or indirect, to cause the direction or management of such entity, whether by contract or otherwise, or (ii) ownership of fifty percent (50%) or more of the outstanding shares, or (iii) beneficial ownership of such entity.<br/><br/></p> <p>\"You\" (or \"Your\") shall mean an individual or Legal Entity exercising permissions granted by this License.<br/><br/></p> <p>\"Source\" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.<br/><br/></p> <p>\"Object\" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.<br/><br/></p> <p>\"Work\" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work (an example is provided in the Appendix below).<br/><br/></p> <p>\"Derivative Works\" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship. For the purposes of this License, Derivative Works shall not include works that remain separable from, or merely link (or bind by name) to the interfaces of, the Work and Derivative Works thereof.<br/><br/></p> <p>\"Contribution\" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner. For the purposes of this definition, \"submitted\" means any form of electronic, verbal, or written communication sent to the Licensor or its representatives, including but not limited to communication on electronic mailing lists, source code control systems, and issue tracking systems that are managed by, or on behalf of, the Licensor for the purpose of discussing and improving the Work, but excluding communication that is conspicuously marked or otherwise designated in writing by the copyright owner as \"Not a Contribution.\"<br/><br/></p> <p>\"Contributor\" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by Licensor and subsequently incorporated within the Work.<br/><br/></p>";	
	
	/**
	 * Everything is injected via Guice.
	 * @param view
	 * @param datasetService
	 */
	@Inject
	public LayerPresenter(LayerView view, DatasetServiceAsync datasetService, NodeServiceAsync nodeService, LicenceServiceAsync licenseService) {
		this.view = view;
		this.service = datasetService;
		this.nodeService = nodeService;
		this.licenseService = licenseService;
		
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// First refresh from the server
		refreshFromServer();		
		// add the view to the panel
		panel.setWidget(view);		
	}

	public void refreshFromServer() {
		view.clear();
		// Fetch the data about this dataset from the server
		nodeService.getNodeJSON(NodeType.LAYER, this.layerId, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String layerJson) {
				Layer layer = new Layer(JSONParser.parseStrict(layerJson).isObject());
				setLayer(layer);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error occured retrieving this Layer. Please try reloading the page.");
			}			
		});
		
		// get the preview string to get file header order, then get the previewAsData
		nodeService.getNodePreview(NodeType.LAYER, layerId, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String pagedResult) {
				JSONObject pagedObject = JSONParser.parseStrict(pagedResult).isObject();
				if(pagedObject != null && pagedObject.containsKey("results")) {
					JSONArray resultList = pagedObject.get("results").isArray();
					if(resultList != null && resultList.size() > 0) {						
						LayerPreview layerPreview = new LayerPreview(resultList.get(0).isObject());
						setLayerPreview(layerPreview);					
					}
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				view.showLayerPreviewUnavailable();				
			}

		});		
	}


	public void setPlace(org.sagebionetworks.web.client.place.Layer place) {
		this.place = place;
		this.layerId = place.getLayerId();
		this.datasetId = place.getDatasetId();
		this.showDownload = place.getDownload();
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

	
	/*
	 * Protected Methods
	 */
	
	protected void setLayer(final Layer layer) {
		this.model = layer;
		
		// process the layer and send values to view
		view.setLayerDetails(model.getId(), 
							 model.getName(), 
						 	 model.getProcessingFacility(), 
							 model.getQcBy(), "#",
							 "qc_script.R", "#",
							 model.getQcDate(),
							 model.getDescription(),
							 5,
							 Integer.MAX_VALUE, // TODO : get total number of rows in layer
							 "Public", // TODO : replace with security object
							 "<a href=\"#Dataset:"+ this.datasetId +"\">Dataset</a>", // TODO : have dataset name included in layer metadata
							 model.getPlatform());

		// see if license is required for doanload
		licenseService.hasAccepted("GET-USERNAME", model.getUri(), new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {				
				view.showErrorMessage("Dataset downloading unavailable. Please try reloading the page.");
				view.setDownloadUnavailable();
				view.disableLicensedDownloads(true);
			}

			@Override
			public void onSuccess(Boolean hasAccepted) {								
				view.requireLicenseAcceptance(!hasAccepted);
				// TODO !!!! get actual license agreement text (and citation if needed) !!!!
				LicenseAgreement agreement = new LicenseAgreement();
				agreement.setLicenseHtml(licenseAgreementText);
				view.setLicenseAgreement(agreement);
				
				nodeService.getNodeLocations(NodeType.LAYER, layerId, new AsyncCallback<String>() {
					@Override
					public void onSuccess(String pagedResult) {
						JSONObject pagedObject = JSONParser.parseStrict(pagedResult).isObject();
						List<FileDownload> downloads = new ArrayList<FileDownload>();
						if(pagedObject != null && pagedObject.containsKey("results")) {
							JSONArray resultList = pagedObject.get("results").isArray();
							if(resultList != null) {
								for(int i=0; i<resultList.size(); i++) {
									if(resultList.get(i).isObject() != null) {										
										DownloadLocation downloadLocation = new DownloadLocation(resultList.get(i).isObject());
										if(downloadLocation != null && downloadLocation.getPath() != null) {
											FileDownload dl = new FileDownload(downloadLocation.getPath(), "Download " + model.getName(), downloadLocation.getMd5sum());
											downloads.add(dl);
										}	
									}
								}								
							}
						}
						view.setLicensedDownloads(downloads);
						
						// show download if requested
						if(showDownload != null && showDownload == true) {
							view.showDownload();
						}
					}
					@Override
					public void onFailure(Throwable caught) {
						view.setDownloadUnavailable();						
					}
				});
				
//				// get download link
//				service.getLayerDownloadLocation(datasetId, layerId, new AsyncCallback<DownloadLocation>() {
//					@Override
//					public void onSuccess(DownloadLocation downloadLocation) {
//						
//						if(downloadLocation != null && downloadLocation.getPath() != null) {
//							FileDownload dl = new FileDownload(downloadLocation.getPath(), "Download " + model.getName(), downloadLocation.getMd5sum());
//							downloads.add(dl);
//						}
//						view.setLicensedDownloads(downloads);
//						
//						// show download if requested
//						if(showDownload != null && showDownload == true) {
//							view.showDownload();
//						}
//					}
//
//					@Override
//					public void onFailure(Throwable caught) {					
//						view.setDownloadUnavailable();
//					}
//				});
			}
		});

		
	}
	
	protected void setLayerPreview(LayerPreview preview) {
		this.layerPreview = preview;

		
		// get column display order, if possible from the layer preview
		List<String> columnDisplayOrder = preview.getHeaders();

		// TODO : get columns descriptions from service
		Map<String, String> columnDescriptions = getTempColumnDescriptions();
		Map<String, String> columnUnits = getTempColumnUnits();
		
		// append units onto description
		for(String key : columnUnits.keySet()) {
			String units = columnUnits.get(key);
			columnDescriptions.put(key, columnDescriptions.get(key) + " (" + units + ")");
		}		
		
		view.setLayerPreviewTable(preview.getRows(), columnDisplayOrder, columnDescriptions, columnUnits);

	}
	
	private Map<String, String> getTempColumnUnits() {
		Map<String,String> units = new LinkedHashMap<String, String>();
		
		units.put("predxbxpsa","ng/mL");
		units.put("age","years");
		units.put("pre_treatment_psa","ng/mL");
		units.put("bcr_freetime","years");
		units.put("survtime","months");
		units.put("nomogram_pfp_postrp","probability");
		units.put("nomogram_nomopred_extra_capsular_extension","probability");
		units.put("nomogram_nomopred_lni","probability");
		units.put("nomogram_nomopred_ocd","probability");
		units.put("nomogram_nomopred_seminal_vesicle_invasion","probability");
		
		return units;
	}

	private Map<String, String> getTempColumnDescriptions() {
		Map<String,String> descriptions = new LinkedHashMap<String, String>();
		
		descriptions.put("sample_type","Type of sample that was profiled. One of: Benign=benign tumor ,CELL LINE=cancer cell line , MET=metastatic tumor, PRIMARY=primary tumor or XENOGRAFT. ");
		descriptions.put("metastatic_site","Site in the body where metastatic tumor was detected.");
		descriptions.put("ethnicity","ethnicity of the individual");
		descriptions.put("predxbxpsa","PSA prior to diagnostic biopsy");
		descriptions.put("age","Age at diagnosis");
		descriptions.put("clinical_primary_gleason","clinical gleason score of the majority of the tumor");
		descriptions.put("clinical_secondary_gleason","clinical gleason score of the minority of the tumor");
		descriptions.put("clinical_gleason_score","clinical tertiary gleason score");
		descriptions.put("pre_treatment_psa","Prostate specific antigen level prior to treatment");
		descriptions.put("clinical_tnm_stage_t","Cancer stage based on the Tumor, Node, Metastasis scoring system");
		descriptions.put("neoadjradtx","neo-adjuvant treatment");
		descriptions.put("chemotx","chemotherapy treatment");
		descriptions.put("hormtx","Hormone treatment");
		descriptions.put("radtxtype","radiation treatment type");
		descriptions.put("rp_type","Surgical treatment. RP=radical prostatectomy, LP=laproscopic prostatectomy, SALRP=Salvage radical prostatectomy");
		descriptions.put("sms","somatostatin");
		descriptions.put("extra_capsular_extension","extra-capsular extension: cancer extending beyond the prostate capsule");
		descriptions.put("seminal_vesicle_invasion","seminal vesicle invasion. Either \"positive\" or \"negative\"");
		descriptions.put("tnm_stage_n","TNM \"N\" stage");
		descriptions.put("number_nodes_removed","Number of cancerous nodes removed");
		descriptions.put("number_nodes_positive","number node-positive nodes");
		descriptions.put("pathologic_tnm_stage_t","pathologic TNM \"T\" stage");
		descriptions.put("pathologic_primary_gleason","pathalogic gleason score of the majority of the tumor");
		descriptions.put("pathologic_secondary_gleason","pathalagic gleason score of the minority of the tumor");
		descriptions.put("pathologic_gleason_score","pathalogic tertiary gleason score");
		descriptions.put("bcr_freetime","elapsed time before bichemical recurrance");
		descriptions.put("bcr_event","was a biochemical recurrance event detected");
		descriptions.put("metsevent","metastatic event");
		descriptions.put("survtime","Survival time post-diagnosis");
		descriptions.put("event","Cause of death");
		descriptions.put("nomogram_pfp_postrp","nomogram progression-free probability post radical prostatectomy");
		descriptions.put("nomogram_nomopred_extra_capsular_extension","probability of extra capsular extension");
		descriptions.put("nomogram_nomopred_lni","probability of lymph node involvement");
		descriptions.put("nomogram_nomopred_ocd","probability of organ confined disease");
		descriptions.put("nomogram_nomopred_seminal_vesicle_invasion","probability of seminal vesicle invasion");
		descriptions.put("copy_number_cluster","copy number cluster size");
		descriptions.put("expression_array_tissue_source","Source of tissue used for expression profiling");		

		return descriptions;
	}

	@Override
	public void refresh() {
		refreshFromServer();
	}

}










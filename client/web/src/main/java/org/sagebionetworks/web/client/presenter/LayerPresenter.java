package org.sagebionetworks.web.client.presenter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.view.LayerView;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LayerPresenter extends AbstractActivity implements LayerView.Presenter{
	

	private org.sagebionetworks.web.client.place.Layer place;
	private DatasetServiceAsync service;
	private LayerView view;
	private String datasetId;
	private String layerId;
	private Layer model;
	private LayerPreview layerPreview;
	private TableResults layerPreviewAsMap;
	
	
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
		view.clear();
		// Fetch the data about this dataset from the server
		service.getLayer(this.datasetId, this.layerId, new AsyncCallback<Layer>() {			
			@Override
			public void onSuccess(Layer result) {
				setLayer(result);				
			}
			
			@Override
			public void onFailure(Throwable caught) {				
				view.showErrorMessage(caught.getMessage());				
			}
		});
		
		// TODO : Best Practice here? Maybe chat w/ Nicole about including header in previewAsMap
		// get the preview string to get file header order, then get the previewAsData
		final String localDatasetId = this.datasetId;
		final String localLayerId = this.layerId;
		service.getLayerPreview(localDatasetId, localLayerId, new AsyncCallback<LayerPreview>() {
			
			@Override
			public void onSuccess(LayerPreview layerPreviewResult) {												
				setLayerPreview(layerPreviewResult);
				
				// retrieve the map of values
				service.getLayerPreviewMap(localDatasetId, localLayerId, new AsyncCallback<TableResults>() {
					
					@Override
					public void onSuccess(TableResults result) {
						setLayerPreviewAsMap(result);								
					}
					
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage("Layer's data preview is unavailable at the moment.");				
						//Log.error(caught.getMessage());
					}
				});
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("Layer's data preview is unavailable.");
				
			}
		});
		
		
	}


	public void setPlace(org.sagebionetworks.web.client.place.Layer place) {
		this.place = place;
		this.layerId = place.getLayerId();
		this.datasetId = place.getDatasetId();
	}

	protected void setLayer(Layer layer) {
		// process the layer and send values to view
		view.setLayerDetails(layer.getName(), 
						 	 layer.getProcessingFacility(), 
							 layer.getQcBy(), "#",
							 "qc_script.R", "#",
							 layer.getQcDate(),
							 layer.getDescription(),
							 5,
							 Integer.MAX_VALUE, // TODO : get total number of rows in layer
							 "Public"); // TODO : replace with security object 
	}
	
	protected void setLayerPreview(LayerPreview preview) {
		this.layerPreview = preview;
	}

	protected void setLayerPreviewAsMap(TableResults previewMap) {
		this.layerPreviewAsMap = previewMap;
		
		// get column display order, if possible from the layer preview
		String[] columnDisplayOrder;
		if(layerPreview != null) {
			String tabPreviewString = layerPreview.getPreview();
			tabPreviewString = tabPreviewString.replaceAll("\r\n.*", ""); // delete all but first line
			tabPreviewString = tabPreviewString.replaceAll("\n.*", ""); // delete all but first line
			columnDisplayOrder = tabPreviewString.split("\t");
		} else {			
			List<Map<String,Object>> rows = layerPreviewAsMap.getRows();
			Map<String,Object> map1 = rows.get(0);
			Set<String> keyset = map1.keySet();
			columnDisplayOrder = keyset.toArray(new String[keyset.size()]);
		}
		
		// TODO : get columns descriptions from service
		Map<String, String> columnDescriptions = getTempColumnDescriptions();
		Map<String, String> columnUnits = getTempColumnUnits();
		
		// append units onto description
		for(String key : columnUnits.keySet()) {
			String units = columnUnits.get(key);
			columnDescriptions.put(key, columnDescriptions.get(key) + " (" + units + ")");
		}		
		
		view.setLayerPreviewTable(layerPreviewAsMap, columnDisplayOrder, columnDescriptions, columnUnits);
	}
	
	private Map<String, String> getTempColumnUnits() {
		Map<String,String> units = new LinkedHashMap<String, String>();
		
		units.put("sample", "ng/mL");
		
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
		
		descriptions.put("sample","Type of sample that was profiled. One of: Benign=benign tumor, CELL LINE=cancer cell line, MET=metastatic tumor, PRIMARY=primary tumor or XENOGRAFT.");
		
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
}










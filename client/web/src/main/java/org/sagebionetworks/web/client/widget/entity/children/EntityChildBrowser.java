package org.sagebionetworks.web.client.widget.entity.children;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.HasPreviews;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.WhereCondition;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityChildBrowser implements EntityChildBrowserView.Presenter, SynapseWidgetPresenter {
	
	private EntityChildBrowserView view;
	private PlaceChanger placeChanger;
	private NodeServiceAsync nodeService;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	private HandlerManager handlerManager = new HandlerManager(this);
	private Entity entity;
	private EntityTypeProvider entityTypeProvider;

	private LayerPreview layerPreview; 
	PreviewData previewData;
	
	@Inject
	public EntityChildBrowser(EntityChildBrowserView view, NodeServiceAsync nodeService, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, EntityTypeProvider entityTypeProvider) {
		this.view = view;
		this.nodeService = nodeService;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		this.entityTypeProvider = entityTypeProvider;
				
		previewData = new PreviewData();
		view.setPresenter(this);
	}	
	
	public Widget asWidget(Entity entity, boolean canEdit) {		
		view.setPresenter(this);
		this.entity = entity; 		
		
		// Get EntityType
		EntityType entityType = entityTypeProvider.getEntityTypeForEntity(entity);
		view.createBrowser(entity, entityType, canEdit);
		 
		// load preview if has previews
		if(entity instanceof HasPreviews) {
			loadPreview();
		}
		
		return view.asWidget();
	}

	@SuppressWarnings("unchecked")
	public void clearState() {
		view.clear();
		// remove handlers
		handlerManager = new HandlerManager(this);		
		this.entity = null;		
	}

	/**
	 * Does nothing. Use asWidget(Entity)
	 */
	@Override
	public Widget asWidget() {
		return null;
	}

    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }
    
	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}
		
	@SuppressWarnings("unchecked")
	public void addEntityUpdatedHandler(EntityUpdatedHandler handler) {
		handlerManager.addHandler(EntityUpdatedEvent.getType(), handler);
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<WhereCondition> getProjectContentsWhereContidions() {
		final List<WhereCondition> where = new ArrayList<WhereCondition>();
		where.add(new WhereCondition(DisplayUtils.ENTITY_PARENT_ID_KEY, WhereOperator.EQUALS, entity.getId()));
		return where;
	}

	@Override
	public List<EntityType> getContentsSkipTypes() {
		// Get EntityType
		EntityType entityType = entityTypeProvider.getEntityTypeForEntity(entity);
		
		List<EntityType> ignore = new ArrayList<EntityType>();
		// ignore self type children 
		ignore.add(entityType); 

		// ignore locations
		// TODO : locations will be going away. remove this then
		for(EntityType type : entityTypeProvider.getEntityTypes()) {
			if("location".equals(type.getName())) {
				ignore.add(type);
			}
		}
		
		return ignore;
	}

	@Override
	public LocationData getMediaLocationData() {
		LocationData location = new LocationData();
		if(entity instanceof Layer && ((Layer)entity).getType() == LayerTypeNames.M) {			
			List<LocationData> locations = ((Layer)entity).getLocations();
			if(locations != null && locations.size() > 0) {
				location = locations.get(0); // send the first location
			}			 				
		}
		return location;
	}
	
	/**
	 * Load the Preview for this layer from the server
	 */
	public void loadPreview() {			
		// Treat preview of Layers of Media type specially
		if(entity instanceof Layer && ((Layer)entity).getType() == LayerTypeNames.M) {			
			return;
		}
		
		// get the preview string to get file header order, then get the previewAsData
		nodeService.getNodePreview(DisplayUtils.getNodeTypeForEntity(entity), entity.getId(), new AsyncCallback<String>() {
			@Override
			public void onSuccess(String pagedResultString) {
				layerPreview = null;				
				try {
					PagedResults  pagedResult = nodeModelCreator.createPagedResults(pagedResultString);
					if(pagedResult != null) {
						List<String> results = pagedResult.getResults();
						if(results.size() > 0) {
							layerPreview = nodeModelCreator.createLayerPreview(results.get(0));
						} else {
							layerPreview = null;
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
					// get column display order, if possible from the layer preview
	
					nodeService.getNodeAnnotationsJSON(DisplayUtils.getNodeTypeForEntity(entity), entity.getId(), new AsyncCallback<String>() {

						@Override
						public void onFailure(Throwable caught) {
							// set the table even if we can't get the annotations
							callSetLayerPreviewTable();
						}

						@Override
						public void onSuccess(String annotationJsonString) {
							
							previewData.setColumnDisplayOrder(layerPreview.getHeaders());
							previewData.setColumnDescriptions(new HashMap<String, String>());
							previewData.setColumnUnits(new HashMap<String, String>());							
							final Map<String, String> columnDescriptions = previewData.getColumnDescriptions();
							final Map<String, String> columnUnits = previewData.getColumnUnits();	

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
							Map<String, String> columnDescriptions = previewData.getColumnDescriptions();
							Map<String, String> columnUnits = previewData.getColumnUnits();	
							// append units onto description
							for(String key : columnUnits.keySet()) {
								String units = columnUnits.get(key);
								columnDescriptions.put(key, columnDescriptions.get(key) + " (" + units + ")");
							}		
							if(layerPreview != null && layerPreview.getRows() != null && layerPreview.getRows().size() > 0 ) {
								previewData.setRows(layerPreview.getRows());
							}
							view.setPreviewTable(previewData);
						}
					});
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				// continue
				layerPreview = null;
				view.setPreviewTable(null);
			}
		});		

	}
		
	/*
	 * Private Methods
	 */
}

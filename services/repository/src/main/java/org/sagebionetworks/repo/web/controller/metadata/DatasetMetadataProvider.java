package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class DatasetMetadataProvider implements TypeSpecificMetadataProvider<Dataset>{
	
	@Autowired
	LayerTypeCountCache layerTypeCountCache;

	/**
	 * This should add the url to this datasets annotations.  And a link to this datasets layers
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Override
	public void addTypeSpecificMetadata(Dataset entity,	HttpServletRequest request, UserInfo user, EventType eventType) throws DatastoreException, NotFoundException, UnauthorizedException {
		if(entity == null) throw new IllegalArgumentException("Entity cannot be null");
		if(entity.getId() == null) throw new IllegalArgumentException("Entity.id cannot be null");
		// We need to set the hasClinical, hasExpression, and hasGenetic
		// Note: this addresses bug PLFM-185
		// Count the clinical layers
		long count = layerTypeCountCache.getCountFor(entity.getId(), LayerTypeNames.C, user);
		entity.setHasClinicalData(count > 0);
		// Count the Expression layers
		count = layerTypeCountCache.getCountFor(entity.getId(), LayerTypeNames.E, user);
		entity.setHasExpressionData(count > 0);
		// Count the Expression layers
		count = layerTypeCountCache.getCountFor(entity.getId(), LayerTypeNames.G, user);
		entity.setHasGeneticData(count > 0);
	}
	
	/**
	 * Create a query that selects the layer children of this dataset.
	 * @param parentId
	 * @return
	 */
	public static BasicQuery createChildrenLayerQuery(String parentId){
		BasicQuery query = new BasicQuery();
		// We want all children
		query.setFrom(ObjectType.layer);
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COL_PARENT_ID), Compartor.EQUALS, Long.parseLong(parentId)));
		return query;
	}
	
	/**
	 * Build a query to find the Clinical layers of the given dataset.
	 * @param datasetId
	 * @return
	 */
	public static BasicQuery createHasClinicalQuery(String datasetId){
		// Start with the children query
		BasicQuery query = createChildrenLayerQuery(datasetId);
		// Now add a filter for the clinical layer type.
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COLUMN_LAYER_TYPE), Compartor.EQUALS, InputDataLayer.LayerTypeNames.C.name()));
		return query;
	}
	
	/**
	 * Build a query to find the expression layers of the given dataset.
	 * @param datasetId
	 * @return
	 */
	public static BasicQuery createHasExpressionQuery(String datasetId){
		// Start with the children query
		BasicQuery query = createChildrenLayerQuery(datasetId);
		// Now add a filter for the clinical layer type.
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COLUMN_LAYER_TYPE), Compartor.EQUALS, InputDataLayer.LayerTypeNames.E.name()));
		return query;
	}
	
	/**
	 * Build a query to find the expression layers of the given dataset.
	 * @param datasetId
	 * @return
	 */
	public static BasicQuery createHasGeneticQuery(String datasetId){
		// Start with the children query
		BasicQuery query = createChildrenLayerQuery(datasetId);
		// Now add a filter for the clinical layer type.
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COLUMN_LAYER_TYPE), Compartor.EQUALS, InputDataLayer.LayerTypeNames.G.name()));
		return query;
	}


	/**
	 * Make sure version is not null
	 */
	@Override
	public void validateEntity(Dataset entity, EventType eventType) {
		if(entity.getVersion() == null){
			entity.setVersion("1.0.0");
		}
	}

	@Override
	public void entityDeleted(Dataset entity) {
		// Clear the cache for this dataset
		if(entity != null){
			layerTypeCountCache.clearCacheFor(entity.getId());
		}
	}

}

package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides layer specific metadata.
 * 
 * @author jmhill
 *
 */
public class InputDataLayerMetadataProvider implements TypeSpecificMetadataProvider<InputDataLayer> {
	
	@Autowired
	LayerTypeCountCache layerTypeCountCache;

	@Override
	public void addTypeSpecificMetadata(InputDataLayer entity,	HttpServletRequest request, UserInfo user, EventType eventType) {
		// Only clear the cache for a CREATE or UPDATE event. (See http://sagebionetworks.jira.com/browse/PLFM-232)
		if(EventType.CREATE == eventType || EventType.UPDATE == eventType){
			clearCountsForLayer(entity);			
		}
	}

	/**
	 * Helper for clearing the cache.
	 * @param entity
	 */
	private void clearCountsForLayer(InputDataLayer entity) {
		if(entity != null){
			if(entity.getParentId() != null){
				if(entity.getType() != null){
					// Clear any cached counts for this layer
					layerTypeCountCache.clearCacheFor(entity.getParentId(), LayerTypeNames.valueOf(entity.getType()));
				}
			}
		}
	}

	@Override
	public void validateEntity(InputDataLayer entity, EventType eventType) {
		if(entity.getVersion() == null){
			entity.setVersion("1.0.0");
		}
		if(entity.getType() == null){
			throw new IllegalArgumentException("Layer.type cannot be null");
		}
		if(entity.getParentId() == null){
			throw new IllegalArgumentException("Layer.parentId cannot be null");
		}
	}

	@Override
	public void entityDeleted(InputDataLayer entity) {
		// Clear the counts for this entity.
		clearCountsForLayer(entity);
	}

}

package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Layer.LayerTypeNames;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides layer specific metadata.
 * 
 * @author jmhill
 *
 */
public class LayerMetadataProvider implements TypeSpecificMetadataProvider<Layer> {
	
	@Autowired
	LayerTypeCountCache layerTypeCountCache;

	@Override
	public void addTypeSpecificMetadata(Layer entity,	HttpServletRequest request, UserInfo user, EventType eventType) {
		// Only clear the cache for a CREATE or UPDATE event. (See http://sagebionetworks.jira.com/browse/PLFM-232)
		if(EventType.CREATE == eventType || EventType.UPDATE == eventType){
			clearCountsForLayer(entity);			
		}
	}

	/**
	 * Helper for clearing the cache.
	 * @param entity
	 */
	private void clearCountsForLayer(Layer entity) {
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
	public void validateEntity(Layer entity, EntityEvent event) {
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
	public void entityDeleted(Layer entity) {
		// Clear the counts for this entity.
		clearCountsForLayer(entity);
	}

}

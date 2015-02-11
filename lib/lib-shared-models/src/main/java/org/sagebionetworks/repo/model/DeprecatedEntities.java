package org.sagebionetworks.repo.model;

import org.sagebionetworks.bridge.model.Community;

/**
 * Helper to identify deprecated entity types.
 * 
 * @author jhill
 *
 */
public class DeprecatedEntities {
	
	/**
	 * Is the given entity deprecated?
	 * @param entity
	 * @return
	 */
	public static <T extends Entity> boolean isDeprecated(T entity){
		if(entity instanceof Locationable){
			return true;
		}
		if(entity instanceof Summary){
			return true;
		}
		if(entity instanceof Preview){
			return true;
		}
		if(entity instanceof HasSteps){
			return true;
		}
		if(entity instanceof Step){
			return true;
		}
		if(entity instanceof Page){
			return true;
		}
		if(entity instanceof Community){
			return true;
		}
		return false;
	}

}

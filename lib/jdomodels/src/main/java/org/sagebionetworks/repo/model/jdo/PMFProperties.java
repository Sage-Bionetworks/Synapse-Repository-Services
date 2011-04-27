package org.sagebionetworks.repo.model.jdo;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;

/**
 * This class exists to load the Persistence Manager Factory configuration properties
 * and make them available to Spring.
 * @author jmhill
 *
 */
public class PMFProperties extends Properties implements InitializingBean {

	/**
	 * Spring will call this method after creating the object.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		// First load the configuration properties
		Map<Object, Object> map = PMFConfigUtils.getPersitanceManagerProperties();
		// Add all of the properties from the map
		if(map != null){
			Iterator<Object> it = map.keySet().iterator();
			while(it.hasNext()){
				Object key = it.next();
				Object value = map.get(key);
				this.put(key, value);
			}
		}
	}

}

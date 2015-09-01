package org.sagebionetworks.auth;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.DomainType;

public class DomainTypeUtils {

	public static DomainType valueOf(String valueOf) {
		if (StringUtils.isBlank(valueOf)) {
			return DomainType.SYNAPSE;
		} 
		try {
			return DomainType.valueOf(valueOf);	
		} catch(Throwable t) {
			return DomainType.SYNAPSE;
		}
	}
	
}

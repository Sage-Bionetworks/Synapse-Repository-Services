package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.element.ElementList;

public class Prerelease extends ElementList<PrereleaseIdentifier> {

	public static final String DELIMITER  = ".";
	
	public Prerelease() {
		super(DELIMITER);
	}

}

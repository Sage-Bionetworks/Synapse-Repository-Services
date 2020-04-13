package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.element.ElementList;

public class Build extends ElementList<BuildIdentifier> {

	public static final String DELIMITER  = ".";
	
	public Build() {
		super(DELIMITER);
	}

}

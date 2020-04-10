package org.sagebionetworks.schema.id;

import org.sagebionetworks.schema.element.ElementList;
import org.sagebionetworks.schema.semantic.version.AlphanumericIdentifier;

public class DotSeparatedAlphaNumeric extends ElementList<AlphanumericIdentifier> {
	
	public static final String DELIMITER  = ".";

	public DotSeparatedAlphaNumeric() {
		super(DELIMITER);
	}

}

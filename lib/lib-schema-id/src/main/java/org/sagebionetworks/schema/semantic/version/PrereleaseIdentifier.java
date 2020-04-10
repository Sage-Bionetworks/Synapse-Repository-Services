package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.element.SimpleBranch;

/**
 * A simple branch between AlphanumericIdentifier | NumericIdentifier
 *
 */
public class PrereleaseIdentifier extends SimpleBranch {

	public PrereleaseIdentifier(AlphanumericIdentifier alphanumericIdentifier) {
		super(alphanumericIdentifier);
	}

	public PrereleaseIdentifier(NumericIdentifier numericIdentifier) {
		super(numericIdentifier);
	}
}

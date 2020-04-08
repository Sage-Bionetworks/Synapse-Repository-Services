package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.Element;

public class Prerelease extends Element {

	DotSeparatedPrereleaseIdentifiers dotSeparatedPrereleaseIdentifiers;

	@Override
	public void toString(StringBuilder builder) {
		dotSeparatedPrereleaseIdentifiers.toString(builder);
	}

}

package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.Element;

public class DotSeparatedPrereleaseIdentifiers extends Element {

	private PrereleaseIdentifier prereleaseIdentifier;
	private DotSeparatedPrereleaseIdentifiers dotSeparatedPrereleaseIdentifiers;
	
	
	
	public DotSeparatedPrereleaseIdentifiers(PrereleaseIdentifier prereleaseIdentifier,
			DotSeparatedPrereleaseIdentifiers dotSeparatedPrereleaseIdentifiers) {
		super();
		if(prereleaseIdentifier == null) {
			throw new IllegalArgumentException("PrereleaseIdentifier cannot be null");
		}
		this.prereleaseIdentifier = prereleaseIdentifier;
		this.dotSeparatedPrereleaseIdentifiers = dotSeparatedPrereleaseIdentifiers;
	}


	@Override
	public void toString(StringBuilder builder) {
		prereleaseIdentifier.toString(builder);
		if(dotSeparatedPrereleaseIdentifiers != null) {
			builder.append(".");
			dotSeparatedPrereleaseIdentifiers.toString(builder);
		}
		
	}
}

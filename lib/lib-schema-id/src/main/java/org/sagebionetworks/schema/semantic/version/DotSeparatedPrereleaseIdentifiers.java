package org.sagebionetworks.schema.semantic.version;

import java.util.Objects;

import org.sagebionetworks.schema.Element;

public final class DotSeparatedPrereleaseIdentifiers extends Element {

	private final PrereleaseIdentifier prereleaseIdentifier;
	private final DotSeparatedPrereleaseIdentifiers dotSeparatedPrereleaseIdentifiers;

	public DotSeparatedPrereleaseIdentifiers(PrereleaseIdentifier prereleaseIdentifier,
			DotSeparatedPrereleaseIdentifiers dotSeparatedPrereleaseIdentifiers) {
		super();
		if (prereleaseIdentifier == null) {
			throw new IllegalArgumentException("PrereleaseIdentifier cannot be null");
		}
		this.prereleaseIdentifier = prereleaseIdentifier;
		this.dotSeparatedPrereleaseIdentifiers = dotSeparatedPrereleaseIdentifiers;
	}

	@Override
	public void toString(StringBuilder builder) {
		prereleaseIdentifier.toString(builder);
		if (dotSeparatedPrereleaseIdentifiers != null) {
			builder.append(".");
			dotSeparatedPrereleaseIdentifiers.toString(builder);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(dotSeparatedPrereleaseIdentifiers, prereleaseIdentifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DotSeparatedPrereleaseIdentifiers)) {
			return false;
		}
		DotSeparatedPrereleaseIdentifiers other = (DotSeparatedPrereleaseIdentifiers) obj;
		return Objects.equals(dotSeparatedPrereleaseIdentifiers, other.dotSeparatedPrereleaseIdentifiers)
				&& Objects.equals(prereleaseIdentifier, other.prereleaseIdentifier);
	}

}

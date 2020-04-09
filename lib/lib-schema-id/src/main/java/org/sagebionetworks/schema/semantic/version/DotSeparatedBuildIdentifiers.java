package org.sagebionetworks.schema.semantic.version;

import java.util.Objects;

import org.sagebionetworks.schema.Element;

public final class DotSeparatedBuildIdentifiers extends Element {

	final BuildIdentifier buildIdentifier;
	final DotSeparatedBuildIdentifiers dotSeparatedBuildIdentifiers;

	public DotSeparatedBuildIdentifiers(BuildIdentifier buildIdentifier,
			DotSeparatedBuildIdentifiers dotSeparatedBuildIdentifiers) {
		if (buildIdentifier == null) {
			throw new IllegalArgumentException("BuildIdentifier cannot be null");
		}
		this.buildIdentifier = buildIdentifier;
		this.dotSeparatedBuildIdentifiers = dotSeparatedBuildIdentifiers;
	}

	@Override
	public void toString(StringBuilder builder) {
		buildIdentifier.toString(builder);
		if (dotSeparatedBuildIdentifiers != null) {
			builder.append(".");
			dotSeparatedBuildIdentifiers.toString(builder);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(buildIdentifier, dotSeparatedBuildIdentifiers);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DotSeparatedBuildIdentifiers)) {
			return false;
		}
		DotSeparatedBuildIdentifiers other = (DotSeparatedBuildIdentifiers) obj;
		return Objects.equals(buildIdentifier, other.buildIdentifier)
				&& Objects.equals(dotSeparatedBuildIdentifiers, other.dotSeparatedBuildIdentifiers);
	}

}

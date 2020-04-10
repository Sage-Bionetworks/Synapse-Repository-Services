package org.sagebionetworks.schema.semantic.version;

import java.util.Objects;

import org.sagebionetworks.schema.element.Element;

public final class VersionCore extends Element {

	private final NumericIdentifier major;
	private final NumericIdentifier minor;
	private final NumericIdentifier patch;
	
	public VersionCore(NumericIdentifier major, NumericIdentifier minor, NumericIdentifier patch) {
		super();
		if(major == null) {
			throw new IllegalArgumentException("Major cannot be null");
		}
		if(minor == null) {
			throw new IllegalArgumentException("Minor cannot be null");
		}
		if(patch == null) {
			throw new IllegalArgumentException("Patch cannot be null");
		}
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	public NumericIdentifier getMajor() {
		return major;
	}

	public NumericIdentifier getMinor() {
		return minor;
	}

	public NumericIdentifier getPatch() {
		return patch;
	}
	
	@Override
	public void toString(StringBuilder builder) {
		major.toString(builder);
		builder.append(".");
		minor.toString(builder);
		builder.append(".");
		patch.toString(builder);
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof VersionCore)) {
			return false;
		}
		VersionCore other = (VersionCore) obj;
		return Objects.equals(major, other.major) && Objects.equals(minor, other.minor)
				&& Objects.equals(patch, other.patch);
	}	
}

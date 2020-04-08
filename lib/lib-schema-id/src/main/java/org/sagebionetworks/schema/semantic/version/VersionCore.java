package org.sagebionetworks.schema.semantic.version;

import org.sagebionetworks.schema.Element;

public class VersionCore extends Element {

	private NumericIdentifier major;
	private NumericIdentifier minor;
	private NumericIdentifier patch;
	
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((major == null) ? 0 : major.hashCode());
		result = prime * result + ((minor == null) ? 0 : minor.hashCode());
		result = prime * result + ((patch == null) ? 0 : patch.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VersionCore other = (VersionCore) obj;
		if (major == null) {
			if (other.major != null)
				return false;
		} else if (!major.equals(other.major))
			return false;
		if (minor == null) {
			if (other.minor != null)
				return false;
		} else if (!minor.equals(other.minor))
			return false;
		if (patch == null) {
			if (other.patch != null)
				return false;
		} else if (!patch.equals(other.patch))
			return false;
		return true;
	}	
}

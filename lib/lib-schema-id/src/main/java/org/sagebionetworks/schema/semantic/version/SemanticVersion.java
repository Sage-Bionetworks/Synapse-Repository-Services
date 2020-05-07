package org.sagebionetworks.schema.semantic.version;

import java.util.Objects;

import org.sagebionetworks.schema.element.Element;

public final class SemanticVersion extends Element {

	private final VersionCore core;
	private final Prerelease prerelease;
	private final Build build;

	public SemanticVersion(VersionCore core, Prerelease prerelease, Build build) {
		super();
		if (core == null) {
			throw new IllegalArgumentException("Core cannot be null");
		}
		this.core = core;
		this.prerelease = prerelease;
		this.build = build;
	}

	/**
	 * @return the core
	 */
	public VersionCore getCore() {
		return core;
	}

	/**
	 * @return the prerelease
	 */
	public Prerelease getPrerelease() {
		return prerelease;
	}

	/**
	 * @return the build
	 */
	public Build getBuild() {
		return build;
	}

	@Override
	public void toString(StringBuilder builder) {
		core.toString(builder);
		if (prerelease != null) {
			builder.append("-");
			prerelease.toString(builder);
		}
		if (build != null) {
			builder.append("+");
			build.toString(builder);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(build, core, prerelease);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SemanticVersion)) {
			return false;
		}
		SemanticVersion other = (SemanticVersion) obj;
		return Objects.equals(build, other.build) && Objects.equals(core, other.core)
				&& Objects.equals(prerelease, other.prerelease);
	}

}

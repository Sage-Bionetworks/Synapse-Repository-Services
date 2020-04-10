package org.sagebionetworks.schema.element;

import java.util.Objects;

/**
 * An abstract element the represents a simple branch between one or more
 * elements
 *
 */
public class SimpleBranch extends Element {

	private final Element child;

	public SimpleBranch(Element child) {
		super();
		if (child == null) {
			throw new IllegalArgumentException("Child cannot be null");
		}
		this.child = child;
	}

	@Override
	public final void toString(StringBuilder builder) {
		child.toString(builder);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(child);
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SimpleBranch)) {
			return false;
		}
		SimpleBranch other = (SimpleBranch) obj;
		return Objects.equals(child, other.child);
	}

}

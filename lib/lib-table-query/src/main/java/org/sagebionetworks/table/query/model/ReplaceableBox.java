package org.sagebionetworks.table.query.model;

import java.util.Objects;

/**
 * This box serves as a container for an Element that can be replaced.
 * </p>
 * Usage:
 * </p>
 * For cases where a parent has multiple children that could be replaced,
 * implementing {@link HasReplaceableChildren} would be ambiguous. For such
 * cases, each child can be placed within its own {@link ReplaceableBox}. Note:
 * The parent implementation of {@link Element#getChildren()} should include the
 * box (do not un-box the child). This will ensure that the
 * {@link Element#recursiveSetParent()} will correctly set the child's parent to
 * the box.
 * 
 * 
 * @param <T>
 */
public class ReplaceableBox<T extends Element> extends SQLElement implements HasReplaceableChildren<T> {

	private T child;

	public ReplaceableBox(T child) {
		super();
		this.child = child;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		child.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(child);
	}

	public T getChild() {
		return child;
	}

	@Override
	public void replaceChildren(T replacement) {
		this.child = replacement;
	}

	@Override
	public int hashCode() {
		return Objects.hash(child);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ReplaceableBox)) {
			return false;
		}
		ReplaceableBox other = (ReplaceableBox) obj;
		return Objects.equals(child, other.child);
	}

	@Override
	public String toString() {
		return "ReplaceableBox [child=" + child + "]";
	}

}

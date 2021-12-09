package org.sagebionetworks.table.query.model;

import java.util.Objects;

/**
 * Box of an Element that be be replaced. *
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

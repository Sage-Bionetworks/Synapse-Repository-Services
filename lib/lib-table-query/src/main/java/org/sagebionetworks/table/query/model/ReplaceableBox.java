package org.sagebionetworks.table.query.model;

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

}

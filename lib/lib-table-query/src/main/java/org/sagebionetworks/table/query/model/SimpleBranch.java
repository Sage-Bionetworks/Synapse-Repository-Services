package org.sagebionetworks.table.query.model;

/**
 * An abstract SQLElement that acts as a branch for a single child element.
 * For example, if element c =: a | b then element c can be represented as a 
 * simple branch for either a or b.
 *
 */
public abstract class SimpleBranch extends SQLElement implements HasReplaceableChildren<Element> {

	/**
	 * The single child of this branch.
	 */
	Element child;
	
	
	/**
	 * Create a new branch with a single child.
	 * @param child
	 */
	public SimpleBranch(SQLElement child) {
		super();
		this.child = child;
	}

	public Element getChild() {
		return child;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		child.toSql(builder, parameters);
	}
	
	/**
	 * Replace the single child of this element with 
	 * @param replacement
	 */
	final public void replaceChildren(Element replacement){
		this.child = replacement;
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(child);
	}

}

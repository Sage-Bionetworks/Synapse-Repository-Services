package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * An abstract SQLElement that acts as a branch for a single child element.
 * For example, if element c =: a | b then element c can be represented as a 
 * simple branch for either a or b.
 *
 */
public abstract class SimpleBranch extends SQLElement implements HasReplaceableChildren {

	/**
	 * The single child of this branch.
	 */
	SQLElement child;
	
	
	/**
	 * Create a new branch with a single child.
	 * @param child
	 */
	public SimpleBranch(SQLElement child) {
		super();
		this.child = child;
	}

	public SQLElement getChild() {
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
	final public void replaceChildren(SQLElement replacement){
		this.child = replacement;
	}

	@Override
	final <T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, child);
	}

}

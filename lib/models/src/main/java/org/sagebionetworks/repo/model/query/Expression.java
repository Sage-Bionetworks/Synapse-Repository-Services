package org.sagebionetworks.repo.model.query;


/**
 * Represents a single query expression.
 * 
 * @author jmhill
 *
 */
public class Expression {
	CompoundId id;
	Comparator compare;
	Object value;
	public Expression(CompoundId id, Comparator compare, Object value) {
		super();
		this.id = id;
		this.compare = compare;
		this.value = value;
	}
	public CompoundId getId() {
		return id;
	}
	public Comparator getCompare() {
		return compare;
	}
	public Object getValue() {
		return value;
	}
	public void setId(CompoundId id) {
		this.id = id;
	}
	public void setCompare(Comparator compare) {
		this.compare = compare;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	
}
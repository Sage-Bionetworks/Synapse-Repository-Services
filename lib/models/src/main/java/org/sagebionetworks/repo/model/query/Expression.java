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
	@Override
	public String toString() {
		return "Expression [id=" + id + ", compare=" + compare + ", value="
				+ value + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((compare == null) ? 0 : compare.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Expression other = (Expression) obj;
		if (compare != other.compare)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
}
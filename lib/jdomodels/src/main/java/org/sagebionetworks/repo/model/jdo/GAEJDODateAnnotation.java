package org.sagebionetworks.repo.model.jdo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



/**
 * This is the persistable class for a Annotations whose values are Dates
 * 
 * Note: equals and hashcode are based on the attribute and value, allowing
 * distinct annotations with the same attribute.
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "true")
public class GAEJDODateAnnotation implements GAEJDOAnnotation<Date> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Persistent
	private GAEJDOAnnotations owner; // this is the backwards pointer for the
										// 1-1 owned relationship

	@Persistent
	private String attribute;

	private Date value;

	public GAEJDODateAnnotation() {
	}

	public GAEJDODateAnnotation(String attr, Date value) {
		setAttribute(attr);
		setValue(value);
	}

	private static final DateFormat df = new SimpleDateFormat(
			"ddMMMyyyy HH:mm:ss.");

	public String toString() {
		return getAttribute() + ": " + df.format(getValue());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GAEJDOAnnotations getOwner() {
		return owner;
	}

	public void setOwner(GAEJDOAnnotations owner) {
		this.owner = owner;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public Date getValue() {
		return value;
	}

	public void setValue(Date value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GAEJDODateAnnotation))
			return false;
		GAEJDODateAnnotation other = (GAEJDODateAnnotation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

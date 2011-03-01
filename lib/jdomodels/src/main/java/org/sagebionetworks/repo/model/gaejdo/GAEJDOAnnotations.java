package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Note: equals and hashcode are based only on the id field.
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "false")
public class GAEJDOAnnotations {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	/**
	 * A factory for new objects. (We avoid putting the creation of the
	 * collections into the no-arg constructor, since the persistance machinery
	 * may be calling it for other purposes.)
	 * 
	 * @return
	 */
	public static GAEJDOAnnotations newGAEJDOAnnotations() {
		GAEJDOAnnotations obj = new GAEJDOAnnotations();
		obj.setStringAnnotations(new HashSet<GAEJDOStringAnnotation>());
		obj.setDoubleAnnotations(new HashSet<GAEJDODoubleAnnotation>());
		obj.setLongAnnotations(new HashSet<GAEJDOLongAnnotation>());
		obj.setDateAnnotations(new HashSet<GAEJDODateAnnotation>());
		return obj;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("String Annots: " + getStringAnnotations() + "\n");
		sb.append("Double Annots: " + getDoubleAnnotations() + "\n");
		sb.append("Long Annots: " + getLongAnnotations() + "\n");
		sb.append("Date Annots: " + getDateAnnotations() + "\n");
		return sb.toString();
	}

	/**
	 * Create a new instance having copies of the contained collections. Note:
	 * This method does not persist the new instance.
	 * 
	 * @param a
	 *            the object to be cloned
	 * @return
	 */
	public GAEJDOAnnotations cloneJdo() {
		GAEJDOAnnotations ans = newGAEJDOAnnotations();

		for (GAEJDOAnnotation<String> annot : getStringIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Double> annot : getDoubleIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Long> annot : getLongIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Date> annot : getDateIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}
		return ans;
	}

	@Element(dependent = "true")
	private Set<GAEJDOStringAnnotation> stringAnnotations;

	@Element(dependent = "true")
	private Set<GAEJDODoubleAnnotation> doubleAnnotations;

	@Element(dependent = "true")
	private Set<GAEJDOLongAnnotation> longAnnotations;

	@Element(dependent = "true")
	private Set<GAEJDODateAnnotation> dateAnnotations;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void add(String a, String v) {
		stringAnnotations.add(new GAEJDOStringAnnotation(a, v));
	}

	public void remove(String a, String v) {
		stringAnnotations.remove(new GAEJDOStringAnnotation(a, v));
	}

	public Iterable<GAEJDOAnnotation<String>> getStringIterable() {
		return new Iterable<GAEJDOAnnotation<String>>() {
			public Iterator<GAEJDOAnnotation<String>> iterator() {
				return new Iterator<GAEJDOAnnotation<String>>() {
					private Iterator<GAEJDOStringAnnotation> it = stringAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public GAEJDOAnnotation<String> next() {
						return it.next();
					}

					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	public void add(String a, Double v) {
		doubleAnnotations.add(new GAEJDODoubleAnnotation(a, v));
	}

	public void remove(String a, Double v) {
		// this doesn't seem to work as is...
		doubleAnnotations.remove(new GAEJDODoubleAnnotation(a, v));
		// ... and this doesn't work either...
		//JDOHelper.makeDirty(this, "doubleAnnotations");
		// this doesn't work either...
		// setDoubleAnnotations(new HashSet<GAEJDODoubleAnnotation>(getDoubleAnnotations()));
	}

	public Iterable<GAEJDOAnnotation<Double>> getDoubleIterable() {
		return new Iterable<GAEJDOAnnotation<Double>>() {
			public Iterator<GAEJDOAnnotation<Double>> iterator() {
				return new Iterator<GAEJDOAnnotation<Double>>() {
					private Iterator<GAEJDODoubleAnnotation> it = doubleAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public GAEJDOAnnotation<Double> next() {
						return it.next();
					}

					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	public void add(String a, Long v) {
		longAnnotations.add(new GAEJDOLongAnnotation(a, v));
	}

	public void remove(String a, Long v) {
		// this doesn't seem to work as is...
		longAnnotations.remove(new GAEJDOLongAnnotation(a, v));
		// ... and this doesn't work either...
		//JDOHelper.makeDirty(this, "longAnnotations");
		// this doesn't work either...
		// setLongAnnotations(new HashSet<GAEJDODoubleAnnotation>(getLongAnnotations()));
	}

	public Iterable<GAEJDOAnnotation<Long>> getLongIterable() {
		return new Iterable<GAEJDOAnnotation<Long>>() {
			public Iterator<GAEJDOAnnotation<Long>> iterator() {
				return new Iterator<GAEJDOAnnotation<Long>>() {
					private Iterator<GAEJDOLongAnnotation> it = longAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public GAEJDOAnnotation<Long> next() {
						return it.next();
					}

					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	public void add(String a, Date v) {
		dateAnnotations.add(new GAEJDODateAnnotation(a, v));
	}

	public void remove(String a, Date v) {
		dateAnnotations.remove(new GAEJDODateAnnotation(a, v));
	}

	public Iterable<GAEJDOAnnotation<Date>> getDateIterable() {
		return new Iterable<GAEJDOAnnotation<Date>>() {
			public Iterator<GAEJDOAnnotation<Date>> iterator() {
				return new Iterator<GAEJDOAnnotation<Date>>() {
					private Iterator<GAEJDODateAnnotation> it = dateAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public GAEJDOAnnotation<Date> next() {
						return it.next();
					}

					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	public Set<GAEJDOStringAnnotation> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(
			Set<GAEJDOStringAnnotation> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Set<GAEJDODoubleAnnotation> getDoubleAnnotations() {
		return doubleAnnotations;
	}

	public void setDoubleAnnotations(Set<GAEJDODoubleAnnotation> doubleAnnotations) {
		this.doubleAnnotations = doubleAnnotations;
	}

	public Set<GAEJDOLongAnnotation> getLongAnnotations() {
		return longAnnotations;
	}

	public void setLongAnnotations(Set<GAEJDOLongAnnotation> longAnnotations) {
		this.longAnnotations = longAnnotations;
	}

	public Set<GAEJDODateAnnotation> getDateAnnotations() {
		return dateAnnotations;
	}

	public void setDateAnnotations(Set<GAEJDODateAnnotation> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
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
		if (getClass() != obj.getClass())
			return false;
		GAEJDOAnnotations other = (GAEJDOAnnotations) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

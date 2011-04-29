package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.jdo.JDOAnnotation;

/**
 * Note: equals and hashcode are based only on the id field.
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "false")
public class JDOAnnotations {
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
	public static JDOAnnotations newJDOAnnotations() {
		JDOAnnotations obj = new JDOAnnotations();
		obj.setStringAnnotations(new HashSet<JDOStringAnnotation>());
		obj.setDoubleAnnotations(new HashSet<JDODoubleAnnotation>());
		obj.setLongAnnotations(new HashSet<JDOLongAnnotation>());
		obj.setDateAnnotations(new HashSet<JDODateAnnotation>());
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
	public JDOAnnotations cloneJdo() {
		JDOAnnotations ans = newJDOAnnotations();

		for (JDOAnnotation<String> annot : getStringIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (JDOAnnotation<Double> annot : getDoubleIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (JDOAnnotation<Long> annot : getLongIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (JDOAnnotation<Date> annot : getDateIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}
		return ans;
	}

	@Element(dependent = "true")
	private Set<JDOStringAnnotation> stringAnnotations;

	@Element(dependent = "true")
	private Set<JDODoubleAnnotation> doubleAnnotations;

	@Element(dependent = "true")
	private Set<JDOLongAnnotation> longAnnotations;

	@Element(dependent = "true")
	private Set<JDODateAnnotation> dateAnnotations;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void add(String a, String v) {
		if(stringAnnotations == null){
			stringAnnotations = new HashSet<JDOStringAnnotation>();
		}
		stringAnnotations.add(new JDOStringAnnotation(a, v));
	}

	public void remove(String a, String v) {
		stringAnnotations.remove(new JDOStringAnnotation(a, v));
	}

	public Iterable<JDOAnnotation<String>> getStringIterable() {
		return new Iterable<JDOAnnotation<String>>() {
			public Iterator<JDOAnnotation<String>> iterator() {
				return new Iterator<JDOAnnotation<String>>() {
					private Iterator<JDOStringAnnotation> it = stringAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public JDOAnnotation<String> next() {
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
		if(doubleAnnotations == null){
			doubleAnnotations = new HashSet<JDODoubleAnnotation>();
		}
		doubleAnnotations.add(new JDODoubleAnnotation(a, v));
	}

	public void remove(String a, Double v) {
		// this doesn't seem to work as is...
		doubleAnnotations.remove(new JDODoubleAnnotation(a, v));
		// ... and this doesn't work either...
		//JDOHelper.makeDirty(this, "doubleAnnotations");
		// this doesn't work either...
		// setDoubleAnnotations(new HashSet<JDODoubleAnnotation>(getDoubleAnnotations()));
	}

	public Iterable<JDOAnnotation<Double>> getDoubleIterable() {
		return new Iterable<JDOAnnotation<Double>>() {
			public Iterator<JDOAnnotation<Double>> iterator() {
				return new Iterator<JDOAnnotation<Double>>() {
					private Iterator<JDODoubleAnnotation> it = doubleAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public JDOAnnotation<Double> next() {
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
		if(longAnnotations == null){
			longAnnotations = new HashSet<JDOLongAnnotation>();
		}
		longAnnotations.add(new JDOLongAnnotation(a, v));
	}

	public void remove(String a, Long v) {
		// this doesn't seem to work as is...
		longAnnotations.remove(new JDOLongAnnotation(a, v));
		// ... and this doesn't work either...
		//JDOHelper.makeDirty(this, "longAnnotations");
		// this doesn't work either...
		// setLongAnnotations(new HashSet<JDODoubleAnnotation>(getLongAnnotations()));
	}

	public Iterable<JDOAnnotation<Long>> getLongIterable() {
		return new Iterable<JDOAnnotation<Long>>() {
			public Iterator<JDOAnnotation<Long>> iterator() {
				return new Iterator<JDOAnnotation<Long>>() {
					private Iterator<JDOLongAnnotation> it = longAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public JDOAnnotation<Long> next() {
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
		if(dateAnnotations == null){
			dateAnnotations = new HashSet<JDODateAnnotation>();
		}
		dateAnnotations.add(new JDODateAnnotation(a, v));
	}

	public void remove(String a, Date v) {
		dateAnnotations.remove(new JDODateAnnotation(a, v));
	}

	public Iterable<JDOAnnotation<Date>> getDateIterable() {
		return new Iterable<JDOAnnotation<Date>>() {
			public Iterator<JDOAnnotation<Date>> iterator() {
				return new Iterator<JDOAnnotation<Date>>() {
					private Iterator<JDODateAnnotation> it = dateAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public JDOAnnotation<Date> next() {
						return it.next();
					}

					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	public Set<JDOStringAnnotation> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(
			Set<JDOStringAnnotation> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Set<JDODoubleAnnotation> getDoubleAnnotations() {
		return doubleAnnotations;
	}

	public void setDoubleAnnotations(Set<JDODoubleAnnotation> doubleAnnotations) {
		this.doubleAnnotations = doubleAnnotations;
	}

	public Set<JDOLongAnnotation> getLongAnnotations() {
		return longAnnotations;
	}

	public void setLongAnnotations(Set<JDOLongAnnotation> longAnnotations) {
		this.longAnnotations = longAnnotations;
	}

	public Set<JDODateAnnotation> getDateAnnotations() {
		return dateAnnotations;
	}

	public void setDateAnnotations(Set<JDODateAnnotation> dateAnnotations) {
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
		JDOAnnotations other = (JDOAnnotations) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

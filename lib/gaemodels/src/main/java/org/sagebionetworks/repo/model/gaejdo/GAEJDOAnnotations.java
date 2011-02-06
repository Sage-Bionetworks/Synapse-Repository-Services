package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchPlan;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

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
	private Key id;

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
		obj.setFloatAnnotations(new HashSet<GAEJDOFloatAnnotation>());
		obj.setTextAnnotations(new HashSet<GAEJDOTextAnnotation>());
		obj.setDateAnnotations(new HashSet<GAEJDODateAnnotation>());
		return obj;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("String Annots: " + getStringAnnotations() + "\n");
		sb.append("Float Annots: " + getFloatAnnotations() + "\n");
		sb.append("Text Annots: " + getTextAnnotations() + "\n");
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

		for (GAEJDOAnnotation<Float> annot : getFloatIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Text> annot : getTextIterable()) {
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
	private Set<GAEJDOFloatAnnotation> floatAnnotations;

	@Element(dependent = "true")
	private Set<GAEJDOTextAnnotation> textAnnotations;

	@Element(dependent = "true")
	private Set<GAEJDODateAnnotation> dateAnnotations;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
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

	public void add(String a, Float v) {
		floatAnnotations.add(new GAEJDOFloatAnnotation(a, v));
	}

	public void remove(String a, Float v) {
		// this doesn't seem to work as is...
		floatAnnotations.remove(new GAEJDOFloatAnnotation(a, v));
		// ... and this doesn't work either...
		//JDOHelper.makeDirty(this, "floatAnnotations");
		// this doesn't work either...
		// setFloatAnnotations(new HashSet<GAEJDOFloatAnnotation>(getFloatAnnotations()));
	}

	public Iterable<GAEJDOAnnotation<Float>> getFloatIterable() {
		return new Iterable<GAEJDOAnnotation<Float>>() {
			public Iterator<GAEJDOAnnotation<Float>> iterator() {
				return new Iterator<GAEJDOAnnotation<Float>>() {
					private Iterator<GAEJDOFloatAnnotation> it = floatAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public GAEJDOAnnotation<Float> next() {
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

	public void add(String a, Text v) {
		textAnnotations.add(new GAEJDOTextAnnotation(a, v));
	}

	public void remove(String a, Text v) {
		textAnnotations.remove(new GAEJDOTextAnnotation(a, v));
	}

	public Iterable<GAEJDOAnnotation<Text>> getTextIterable() {
		return new Iterable<GAEJDOAnnotation<Text>>() {
			public Iterator<GAEJDOAnnotation<Text>> iterator() {
				return new Iterator<GAEJDOAnnotation<Text>>() {
					private Iterator<GAEJDOTextAnnotation> it = textAnnotations
							.iterator();

					public boolean hasNext() {
						return it.hasNext();
					}

					public GAEJDOAnnotation<Text> next() {
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

	public Set<GAEJDOFloatAnnotation> getFloatAnnotations() {
		return floatAnnotations;
	}

	public void setFloatAnnotations(Set<GAEJDOFloatAnnotation> floatAnnotations) {
		this.floatAnnotations = floatAnnotations;
	}

	public Set<GAEJDOTextAnnotation> getTextAnnotations() {
		return textAnnotations;
	}

	public void setTextAnnotations(Set<GAEJDOTextAnnotation> textAnnotations) {
		this.textAnnotations = textAnnotations;
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

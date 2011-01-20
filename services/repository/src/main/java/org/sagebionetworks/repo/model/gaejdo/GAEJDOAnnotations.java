package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable(detachable = "false")
public class GAEJDOAnnotations {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
	// I'm not sure if this is quite right, as the data store may wish to
	// populate the fields itself.   However in the worst case the field 
	// are just overwritten and so a little time is wasted creating these empty maps.
	public GAEJDOAnnotations() {
		setStringAnnotations(new HashSet<GAEJDOStringAnnotation>());
		setNumberAnnotations(new HashSet<GAEJDONumberAnnotation>());
		setTextAnnotations(new HashSet<GAEJDOTextAnnotation>());
		setDateAnnotations(new HashSet<GAEJDODateAnnotation>());
	}
	
	public static GAEJDOAnnotations clone(GAEJDOAnnotations a) {
		GAEJDOAnnotations ans = new GAEJDOAnnotations();

		for (GAEJDOAnnotation<String> annot : a.getStringIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Number> annot : a.getNumberIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Text> annot : a.getTextIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}

		for (GAEJDOAnnotation<Date> annot : a.getDateIterable()) {
			ans.add(annot.getAttribute(), annot.getValue());
		}
		return ans;
	}
		
	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOStringAnnotation> stringAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDONumberAnnotation> numberAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOTextAnnotation> textAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDODateAnnotation> dateAnnotations;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}
	
	public void add(String a, String v) {stringAnnotations.add(new GAEJDOStringAnnotation(a,v));}
	public void remove(String a, String v) {stringAnnotations.remove(new GAEJDOStringAnnotation(a,v));}
	public Iterable<GAEJDOAnnotation<String>> getStringIterable() {
		return new Iterable<GAEJDOAnnotation<String>>() {
			public Iterator<GAEJDOAnnotation<String>> iterator() {
				return new Iterator<GAEJDOAnnotation<String>>() {
					 private Iterator<GAEJDOStringAnnotation> it = stringAnnotations.iterator();
					 public boolean hasNext() {return it.hasNext();}
					 public GAEJDOAnnotation<String> next() {return it.next();}
			         public void remove() {it.remove();}
				};
			}
		};
	}
	
	public void add(String a, Number v) {numberAnnotations.add(new GAEJDONumberAnnotation(a,v));}
	public void remove(String a, Number v) {numberAnnotations.remove(new GAEJDONumberAnnotation(a,v));}
	public Iterable<GAEJDOAnnotation<Number>> getNumberIterable() {
		return new Iterable<GAEJDOAnnotation<Number>>() {
			public Iterator<GAEJDOAnnotation<Number>> iterator() {
				return new Iterator<GAEJDOAnnotation<Number>>() {
					private Iterator<GAEJDONumberAnnotation> it = numberAnnotations.iterator();
					 public boolean hasNext() {return it.hasNext();}
					 public GAEJDOAnnotation<Number> next() {return it.next();}
			         public void remove() {it.remove();}
				};
			}
		};
	}
	
	public void add(String a, Date v) {dateAnnotations.add(new GAEJDODateAnnotation(a,v));}
	public void remove(String a, Date v) {dateAnnotations.remove(new GAEJDODateAnnotation(a,v));}
	public Iterable<GAEJDOAnnotation<Date>> getDateIterable() {
		return new Iterable<GAEJDOAnnotation<Date>>() {
			public Iterator<GAEJDOAnnotation<Date>> iterator() {
				return new Iterator<GAEJDOAnnotation<Date>>() {
					 private Iterator<GAEJDODateAnnotation> it = dateAnnotations.iterator();
					 public boolean hasNext() {return it.hasNext();}
					 public GAEJDOAnnotation<Date> next() {return it.next();}
			         public void remove() {it.remove();}
				};
			}
		};
	}
	
	public void add(String a, Text v) {textAnnotations.add(new GAEJDOTextAnnotation(a,v));}
	public void remove(String a, Text v) {textAnnotations.remove(new GAEJDOTextAnnotation(a,v));}
	public Iterable<GAEJDOAnnotation<Text>> getTextIterable() {
		return new Iterable<GAEJDOAnnotation<Text>>() {
			public Iterator<GAEJDOAnnotation<Text>> iterator() {
				return new Iterator<GAEJDOAnnotation<Text>>() {
					 private Iterator<GAEJDOTextAnnotation> it = textAnnotations.iterator();
					 public boolean hasNext() {return it.hasNext();}
					 public GAEJDOAnnotation<Text> next() {return it.next();}
			         public void remove() {it.remove();}
				};
			}
		};
	}

	public Set<GAEJDOStringAnnotation> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(Set<GAEJDOStringAnnotation> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Set<GAEJDONumberAnnotation> getNumberAnnotations() {
		return numberAnnotations;
	}

	public void setNumberAnnotations(Set<GAEJDONumberAnnotation> numberAnnotations) {
		this.numberAnnotations = numberAnnotations;
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
	



}

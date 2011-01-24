package org.sagebionetworks.repo.model.gaejdo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A class to implement versions, like "1.20.3" or "A.10", which implements
 * Comparable
 * 
 * @author bhoff
 * 
 */
public class Version implements Comparable<Version>, Serializable {
	public static final long serialVersionUID = 1348752384765098375L;
	private List<Comparable> l;
	private String separator;

	/**
	 * 
	 * @param s
	 *            a string of values separated by '.', e.g. "1.20.3" or "A.10"
	 */
	public Version(String s) {
		setSeparator(".");
		StringTokenizer st = new StringTokenizer(s, getSeparator());
		List<Comparable> l = new ArrayList<Comparable>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			try {
				Integer I = Integer.parseInt(token);
				l.add(I);
			} catch (NumberFormatException nfe) {
				l.add(token);
			}
		}
		setL(l);
	}

	public Version(String separator, List<Comparable> l) {
		setSeparator(separator);
		setL(l);
	}

	public List<Comparable> getL() {
		return l;
	}

	public void setL(List<Comparable> l) {
		this.l = l;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public boolean equals(Object o) {
		if (!(o instanceof Version))
			return false;
		return 0 == compareTo((Version) o);
	}

	public int compareTo(Version that) {
		List<Comparable> thisL = getL();
		List<Comparable> thatL = that.getL();
		for (int i = 0; i < thisL.size() && i < thatL.size(); i++) {
			int c = thisL.get(i).compareTo(thatL.get(i));
			if (c != 0)
				return c;
		}
		if (thisL.size() < thatL.size())
			return -1;
		if (thisL.size() > thatL.size())
			return 1;
		return 0;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (Comparable c : getL()) {
			if (first)
				first = false;
			else
				sb.append(getSeparator());
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Increments the version. Note: This method may only be used in the case
	 * that the values in the Version's fields are Integer.
	 * 
	 * @param field
	 *            the field to increment
	 * @return a new Version with the specified field incremented.
	 */
	public Version increment(int field) {
		List<Comparable> incL = new ArrayList<Comparable>(getL());

		Object o = incL.get(field);
		if (!(o instanceof Integer))
			throw new ClassCastException("Can't increment object of type "
					+ o.getClass());
		incL.set(field, 1 + ((Integer) o));
		return new Version(getSeparator(), incL);
	}

	/**
	 * Increments the version. Note: This method may only be used in the case
	 * that the values in the Version's fields are Integer.
	 * 
	 * @return a new Version with the least-significant field incremented.
	 */
	public Version increment() {
		return increment(getL().size() - 1);
	}
}

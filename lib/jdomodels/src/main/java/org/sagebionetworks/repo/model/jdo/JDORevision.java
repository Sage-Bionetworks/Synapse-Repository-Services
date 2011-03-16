package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



/**
 * The class for persistent objects containing the revision information for
 * Revisable persistent objects. The Revisable objects 'point' to instances of
 * this class in an 'owned' relationship.
 * 
 * @author bhoff
 * 
 * @param <T>
 */
@PersistenceCapable(detachable = "false")
public class JDORevision<T extends JDORevisable<T>> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE, sequence="GLOBAL_SEQ")
	private Long id;

	@Persistent
	private Long original; // id of original (this.id if *this* is the original)

	@Persistent(serialized = "true")
	private Version version;

	@Persistent
	private Date revisionDate;

	@Persistent
	private Boolean latest; // true iff the latest revision

	public JDORevision<T> cloneJdo() {
		JDORevision<T> clone = new JDORevision<T>();
		clone.setOriginal(getOriginal());
		clone.setVersion(getVersion());
		clone.setRevisionDate(getRevisionDate());
		clone.setLatest(getLatest());
		return clone;
	}

	public Long getOriginal() {
		return original;
	}

	public void setOriginal(Long original) {
		this.original = original;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getRevisionDate() {
		return revisionDate;
	}

	public void setRevisionDate(Date revisionDate) {
		this.revisionDate = revisionDate;
	}

	public Boolean getLatest() {
		return latest;
	}

	public void setLatest(Boolean latest) {
		this.latest = latest;
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
		if (!(obj instanceof JDORevision))
			return false;
		@SuppressWarnings("unchecked")
		JDORevision<T> other = (JDORevision<T>) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

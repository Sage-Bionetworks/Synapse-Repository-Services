package org.sagebionetworks.repo.model.jdo.persistence;

import java.net.URI;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.jdo.JDORevisable;




/**
 * note 'source' may be a reference to a version control repository, like
 * Subversion
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "true")
public class JDOScript implements JDORevisable<JDOScript> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Persistent
	private String name;

	// http://code.google.com/appengine/docs/java/datastore/relationships.html#Owned_One_to_One_Relationships
	@Persistent(dependent = "true")
	private JDORevision<JDOScript> revision;
	
	// this is a link to the next revision of the layer (if any)
	@Persistent(dependent = "true")
	private JDOScript nextVersion;

	@Persistent
	private Date creationDate;

	@Persistent
	private Date publicationDate;

	@Persistent
	@Column(jdbcType="LONGVARCHAR")
	private String overview;

	@Persistent(serialized = "true")
	private URI source;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JDORevision<JDOScript> getRevision() {
		return revision;
	}

	public void setRevision(JDORevision<JDOScript> revision) {
		this.revision = revision;
	}

	public JDOScript getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(JDOScript nextVersion) {
		this.nextVersion = nextVersion;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public URI getSource() {
		return source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public boolean isPublished() {
		return null != publicationDate;
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
		if (!(obj instanceof JDOScript))
			return false;
		JDOScript other = (JDOScript) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}

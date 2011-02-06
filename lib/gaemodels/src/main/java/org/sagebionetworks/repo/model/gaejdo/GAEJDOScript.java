package org.sagebionetworks.repo.model.gaejdo;

import java.net.URI;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * note 'source' may be a reference to a version control repository, like
 * Subversion
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "true")
public class GAEJDOScript implements GAEJDORevisable<GAEJDOScript> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;

	@Persistent
	private String name;

	// http://code.google.com/appengine/docs/java/datastore/relationships.html#Owned_One_to_One_Relationships
	@Persistent(dependent = "true")
	private GAEJDORevision<GAEJDOScript> revision;

	@Persistent
	private Date creationDate;

	@Persistent
	private Date publicationDate;

	@Persistent
	private Text overview;

	@Persistent(serialized = "true")
	private URI source;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GAEJDORevision<GAEJDOScript> getRevision() {
		return revision;
	}

	public void setRevision(GAEJDORevision<GAEJDOScript> revision) {
		this.revision = revision;
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

	public Text getOverview() {
		return overview;
	}

	public void setOverview(Text overview) {
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
		if (!(obj instanceof GAEJDOScript))
			return false;
		GAEJDOScript other = (GAEJDOScript) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}

package org.sagebionetworks.repo.model.gaejdo;

import java.net.URI;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import javax.jdo.annotations.InheritanceStrategy;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * We follow this approach to persist when other classes inherit from this one
 * http
 * ://www.wetfeetblog.com/google-app-engine-java-jdo-inheritance-one-to-many-
 * relationships/242
 * 
 * 
 * @author bhoff
 * 
 */

@PersistenceCapable(detachable = "true")
@Inheritance(strategy = InheritanceStrategy.SUBCLASS_TABLE)
abstract public class GAEJDODatasetLayer implements
		GAEJDORevisable<GAEJDODatasetLayer> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;

	// @Persistent
	// private Dataset dataset; // backwards pointer for the 1-many owned
	// relationship
	//
	// public Dataset getDataset() {
	// return dataset;
	// }
	//
	// public void setDataset(Dataset dataset) {
	// this.dataset = dataset;
	// }

	@Persistent
	private String name;

	@Persistent(serialized = "true")
	private URI uri;

	@Persistent
	private Date publicationDate;

	@Persistent
	private Text releaseNotes;

	@Persistent(dependent = "true")
	private GAEJDORevision<GAEJDODatasetLayer> revision;

	public GAEJDORevision<GAEJDODatasetLayer> getRevision() {
		return revision;
	}

	public void setRevision(GAEJDORevision<GAEJDODatasetLayer> revision) {
		this.revision = revision;
	}

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

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public Text getReleaseNotes() {
		return releaseNotes;
	}

	public void setReleaseNotes(Text releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

}

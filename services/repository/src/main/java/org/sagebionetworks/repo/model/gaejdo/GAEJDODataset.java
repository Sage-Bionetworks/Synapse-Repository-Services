package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * @author bhoff
 *
 */
@PersistenceCapable(detachable = "true")
public class GAEJDODataset implements GAEJDOBase, GAEJDORevisable<GAEJDODataset>, GAEJDOAnnotatable {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
	@Persistent(dependent = "true") 	
	private GAEJDORevision<GAEJDODataset> revision;
	
	@Persistent(dependent = "true")
	//@NotPersistent
	private GAEJDOAnnotations annotations;
	
	@Persistent
	private String name;
	
	@Persistent
	private String description;
	
	@Persistent
	private String creator;
	
	@Persistent
	private Date creationDate;
	
	@Persistent
	private String status;
	
	@Persistent
	private Date releaseDate;
		
	@Persistent
	private Collection<Key> layers;
		
	public GAEJDODataset() {
		GAEJDOAnnotations a = new GAEJDOAnnotations();
		setAnnotations(a);
	}

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public GAEJDORevision<GAEJDODataset> getRevision() {
		return revision;
	}

	public void setRevision(GAEJDORevision<GAEJDODataset> revision) {
		this.revision = revision;
	}

	public GAEJDOAnnotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(GAEJDOAnnotations annotations) {
		this.annotations = annotations;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public Collection<Key> getLayers() {
		return layers;
	}

	public void setLayers(Collection<Key> layers) {
		this.layers = layers;
	}
	
}

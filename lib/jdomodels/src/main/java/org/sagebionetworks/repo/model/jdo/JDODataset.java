package org.sagebionetworks.repo.model.jdo;

import java.util.Date;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



/**
 * This is the persistable class for a dataset
 * 
 * Note: equals and hashcode are based only on the id field.
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "true")
public class JDODataset implements JDOBase,
		JDORevisable<JDODataset>, JDOAnnotatable {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	// this is a reference to the version info object
	@Persistent(dependent = "true")
	private JDORevision<JDODataset> revision;
	
	// this is a link to the next revision of the dataset (if any)
	@Persistent(dependent = "true")
	private JDODataset nextVersion;

	@Persistent(dependent = "true")
	private JDOAnnotations annotations;

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

	@Element(dependent = "true")
	private Set<JDOInputDataLayer> inputLayers;

	public JDODataset() {
		// JDOAnnotations a = JDOAnnotations.newJDOAnnotations();
		// setAnnotations(a);
	}

	/**
	 * @return id of the persistent object
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            id of the persistent object
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 
	 */
	public JDORevision<JDODataset> getRevision() {
		return revision;
	}

	/**
	 * 
	 */
	public void setRevision(JDORevision<JDODataset> revision) {
		this.revision = revision;
	}

	public JDODataset getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(JDODataset nextVersion) {
		this.nextVersion = nextVersion;
	}

	/**
	 * 
	 */
	public JDOAnnotations getAnnotations() {
		return annotations;
	}

	/**
	 * 
	 * @param annotations
	 */
	public void setAnnotations(JDOAnnotations annotations) {
		this.annotations = annotations;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 
	 * @return
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * 
	 * @param creator
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * @return
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * 
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * 
	 * @return
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * 
	 * @param status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * 
	 * @return
	 */
	public Date getReleaseDate() {
		return releaseDate;
	}

	/**
	 * 
	 * @param releaseDate
	 */
	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	/**
	 * 
	 * @return
	 */
	public Set<JDOInputDataLayer> getInputLayers() {
		return inputLayers;
	}

	/**
	 * 
	 * @param layers
	 */
	public void setInputLayers(Set<JDOInputDataLayer> inputLayers) {
		this.inputLayers = inputLayers;
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
		JDODataset other = (JDODataset) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String toString() {
		return getName();
	}
}

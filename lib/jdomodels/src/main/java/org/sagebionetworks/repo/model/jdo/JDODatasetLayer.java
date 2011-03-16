package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;


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

@PersistenceCapable(detachable = "false")
@Inheritance(strategy = InheritanceStrategy.SUBCLASS_TABLE)
abstract public class JDODatasetLayer<T extends JDODatasetLayer<T>>
		implements JDORevisable<T>, JDOBase, JDOAnnotatable {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE, sequence="GLOBAL_SEQ")
	private Long id;

	@Persistent
	private String name;

	@Persistent
	private Date creationDate;

	@Persistent
	private Date publicationDate;

	@Persistent
	@Column(jdbcType="LONGVARCHAR")
	private String description;

	@Persistent
	@Column(jdbcType="LONGVARCHAR")
	private String releaseNotes;

	// this is a reference to the version info object
	@Persistent(dependent = "true")
	private JDORevision<T> revision;

	@Persistent(dependent = "true")
	private JDOAnnotations annotations;

	@Persistent(dependent = "true")
	private JDOLayerLocations locations;

	@Persistent
	@Column(jdbcType="LONGVARCHAR")
	private String preview;

	public JDORevision<T> getRevision() {
		return revision;
	}

	public void setRevision(JDORevision<T> revision) {
		this.revision = revision;
	}

	public JDOAnnotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(JDOAnnotations annotations) {
		this.annotations = annotations;
	}

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	public void setReleaseNotes(String releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	/**
	 * @param preview
	 *            the preview to set
	 */
	public void setPreview(String preview) {
		this.preview = preview;
	}

	/**
	 * @return the preview
	 */
	public String getPreview() {
		return preview;
	}

	/**
	 * @param locations
	 *            the locations to set
	 */
	public void setLocations(JDOLayerLocations locations) {
		this.locations = locations;
	}

	/**
	 * @return the locations
	 */
	public JDOLayerLocations getLocations() {
		return locations;
	}

	public static Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "name", "creationDate",
				"publicationDate", "releaseNotes" });
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
		if (!(obj instanceof JDODatasetLayer))
			return false;
		@SuppressWarnings("unchecked")
		JDODatasetLayer<T> other = (JDODatasetLayer<T>) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

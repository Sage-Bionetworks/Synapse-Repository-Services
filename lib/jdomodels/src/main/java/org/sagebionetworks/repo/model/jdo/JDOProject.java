package org.sagebionetworks.repo.model.jdo;

import java.net.URL;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;




/**
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "true")
public class JDOProject {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE, sequence="GLOBAL_SEQ")
	private Long id;

	@Persistent
	private String name;

	public enum Status {
		PROPOSED, IN_PROGRESS, COMPLETED
	}

	@Persistent
	private Status status;

	@Persistent
	@Column(jdbcType="LONGVARCHAR")
	private String overview;

	@Persistent
	private Date started;

	@Persistent(serialized = "true")
	private URL sharedDocs;

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

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public Date getStarted() {
		return started;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public URL getSharedDocs() {
		return sharedDocs;
	}

	public void setSharedDocs(URL sharedDocs) {
		this.sharedDocs = sharedDocs;
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
		if (!(obj instanceof JDOProject))
			return false;
		JDOProject other = (JDOProject) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

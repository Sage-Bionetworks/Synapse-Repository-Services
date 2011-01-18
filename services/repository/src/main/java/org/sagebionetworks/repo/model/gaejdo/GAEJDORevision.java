package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;


import com.google.appengine.api.datastore.Key;

@PersistenceCapable(detachable = "false")
public class GAEJDORevision<T extends GAEJDORevisable<T>> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
	@Persistent
	private Key original; // id of original or this.id if *this* is the original
	
	@Persistent(serialized = "true")
	private Version version;
	
	@Persistent
	private Date revisionDate;
	
	@Persistent
	private Boolean latest; // true iff the latest revision
	
	public Key getOriginal() {
		return original;
	}
	public void setOriginal(Key original) {
		this.original = original;
	}
	public Version getVersion() {
		return version;
	}
	public void setVersion(Version version) {
		this.version = version;
	}
	public Key getId() {
		return id;
	}
	public void setId(Key id) {
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

}

//@PersistenceCapable(detachable = "true")
//// http://stackoverflow.com/questions/3808874/problem-persisting-collection-of-interfaces-in-jdo-datanucleus-unable-to-assign
//// http://code.google.com/p/datanucleus-appengine/issues/detail?id=207
//@Extension(vendorName = "datanucleus", key = "implementation-classes", 
//		value = "org.sagebionetworks.repo.model.Dataset,org.sagebionetworks.repo.model.InputDataLayer,org.sagebionetworks.repo.model.AnalysisResult,org.sagebionetworks.repo.model.Script")
//// http://code.google.com/appengine/docs/java/datastore/relationships.html#Owned_One_to_One_Relationships
//@Persistent(mappedBy="revision")
//private T owner; // this is the backwards pointer for the 1-1 owned relationship


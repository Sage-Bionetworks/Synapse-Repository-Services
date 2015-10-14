package org.sagebionetworks.repo.model.dbo.persistence;



/**
 * Contains information specific to a message sent to a user
 */
public class DBOMessageToUserBackup extends DBOMessageToUser {
		
	private String subject;
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOMessageToUserBackup other = (DBOMessageToUserBackup) obj;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}

	
}


package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;
import java.util.List;


/**
 * MembershipInvtnSubmissionTransfer
 * 
 * This is the temporary transfer object used to deserialize legacy MembershipInvtnSubmission objects
 * 
 */
public class MembershipInvtnSubmissionTransfer  {

    private Date createdOn;
    private String message;
    private String id;
    private String createdBy;
    private Date expiresOn;
    private String teamId;
    private String inviteeId;
    private List<String> invitees;

    public MembershipInvtnSubmissionTransfer() {
    }


    /**
     * Created On
     * 
     * The date this MembershipInvtnSubmission was created.
     * 
     * 
     * 
     * @return
     *     createdOn
     */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
     * Created On
     * 
     * The date this MembershipInvtnSubmission was created.
     * 
     * 
     * 
     * @param createdOn
     */
    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * The invitation message (optional).
     * 
     * 
     * 
     * @return
     *     message
     */
    public String getMessage() {
        return message;
    }

    /**
     * The invitation message (optional).
     * 
     * 
     * 
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * The id of the MembershipInvtnSubmission.
     * 
     * 
     * 
     * @return
     *     id
     */
    public String getId() {
        return id;
    }

    /**
     * The id of the MembershipInvtnSubmission.
     * 
     * 
     * 
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Created By
     * 
     * The ID of the user that created this MembershipInvtnSubmission.
     * 
     * 
     * 
     * @return
     *     createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Created By
     * 
     * The ID of the user that created this MembershipInvtnSubmission.
     * 
     * 
     * 
     * @param createdBy
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Expires On
     * 
     * The date this invitation expires (optional).
     * 
     * 
     * 
     * @return
     *     expiresOn
     */
    public Date getExpiresOn() {
        return expiresOn;
    }

    /**
     * Expires On
     * 
     * The date this invitation expires (optional).
     * 
     * 
     * 
     * @param expiresOn
     */
    public void setExpiresOn(Date expiresOn) {
        this.expiresOn = expiresOn;
    }

    /**
     * The id of the Team which the user is invited to join.
     * 
     * 
     * 
     * @return
     *     teamId
     */
    public String getTeamId() {
        return teamId;
    }

    /**
     * The id of the Team which the user is invited to join.
     * 
     * 
     * 
     * @param teamId
     */
    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    /**
     * The principal IDs of the ones being invited to join the Team.
     * 
     * 
     * 
     * @return
     *     invitees
     */
    public List<String> getInvitees() {
        return invitees;
    }

    /**
     * The principal IDs of the ones being invited to join the Team.
     * 
     * 
     * 
     * @param invitees
     */
    public void setInvitees(List<String> invitees) {
        this.invitees = invitees;
    }


	public String getInviteeId() {
		return inviteeId;
	}


	public void setInviteeId(String inviteeId) {
		this.inviteeId = inviteeId;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((expiresOn == null) ? 0 : expiresOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((inviteeId == null) ? 0 : inviteeId.hashCode());
		result = prime * result
				+ ((invitees == null) ? 0 : invitees.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((teamId == null) ? 0 : teamId.hashCode());
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
		MembershipInvtnSubmissionTransfer other = (MembershipInvtnSubmissionTransfer) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (expiresOn == null) {
			if (other.expiresOn != null)
				return false;
		} else if (!expiresOn.equals(other.expiresOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (inviteeId == null) {
			if (other.inviteeId != null)
				return false;
		} else if (!inviteeId.equals(other.inviteeId))
			return false;
		if (invitees == null) {
			if (other.invitees != null)
				return false;
		} else if (!invitees.equals(other.invitees))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (teamId == null) {
			if (other.teamId != null)
				return false;
		} else if (!teamId.equals(other.teamId))
			return false;
		return true;
	}




 

 

}

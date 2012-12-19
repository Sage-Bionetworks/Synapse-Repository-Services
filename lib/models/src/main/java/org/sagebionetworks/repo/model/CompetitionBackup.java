package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;

public class CompetitionBackup {
	private Competition competition;
	private List<Participant> participants;
	
	public Competition getCompetition() {
		return competition;
	}
	public void setCompetition(Competition competition) {
		this.competition = competition;
	}
	public List<Participant> getParticipants() {
		return participants;
	}
	public void setParticipants(List<Participant> participants) {
		this.participants = participants;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((competition == null) ? 0 : competition.hashCode());
		result = prime * result
				+ ((participants == null) ? 0 : participants.hashCode());
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
		CompetitionBackup other = (CompetitionBackup) obj;
		if (competition == null) {
			if (other.competition != null)
				return false;
		} else if (!competition.equals(other.competition))
			return false;
		if (participants == null) {
			if (other.participants != null)
				return false;
		} else if (!participants.equals(other.participants))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "CompetitionBackup [competition=" + competition
				+ ", participants=" + participants + "]";
	}


}

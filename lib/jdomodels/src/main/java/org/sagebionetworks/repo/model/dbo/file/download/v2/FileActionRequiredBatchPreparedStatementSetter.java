package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.download.Action;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

public class FileActionRequiredBatchPreparedStatementSetter implements BatchPreparedStatementSetter {
	private FileActionRequired[] actions;

	public FileActionRequiredBatchPreparedStatementSetter(FileActionRequired[] actionsParam) {
		List<FileActionRequired> actions = new ArrayList<FileActionRequired>();
		for (FileActionRequired far : actionsParam) {
			if (isValid(far)) {
				actions.add(far);
			}
		}
		this.actions=actions.toArray(new FileActionRequired[] {});
	}
	
	
	public static boolean isValid(FileActionRequired far) {
		if (far.getAction()==null) {
			return false;
		}
		Action action = far.getAction();
		if (action instanceof MeetAccessRequirement) {
			MeetAccessRequirement mar = (MeetAccessRequirement)action;
			if (mar.getAccessRequirementId()==null) {
				return false;
			}
		} else if (action instanceof RequestDownload) {
			RequestDownload rd = (RequestDownload)action;
			if (rd.getBenefactorId()==null) {
				return false;
			}
		} else {
			return false;
		}
		
		return true;
	}
	
	@Override
	public void setValues(PreparedStatement ps, int i) throws SQLException {
		FileActionRequired required = actions[i];
		int index = 0;
		ps.setLong(++index, required.getFileId());
		Action action = required.getAction();
		if(action instanceof MeetAccessRequirement) {
			ps.setString(++index, ActionType.ACCESS_REQUIREMENT.name());
			ps.setLong(++index, ((MeetAccessRequirement)action).getAccessRequirementId());
		}else if (action instanceof RequestDownload) {
			ps.setString(++index, ActionType.DOWNLOAD_PERMISSION.name());
			ps.setLong(++index, ((RequestDownload)action).getBenefactorId()); 
		}else {
			throw new IllegalStateException("Unknown action type: "+action.getClass().getName());
		}
	}
	@Override
	public int getBatchSize() {
		return actions.length;
	}

}

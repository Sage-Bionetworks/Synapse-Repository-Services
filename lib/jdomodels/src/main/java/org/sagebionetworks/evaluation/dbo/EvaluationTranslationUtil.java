package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.jdo.KeyFactory.ROOT_ID;
import static org.sagebionetworks.repo.model.jdo.KeyFactory.SYNAPSE_ID_PREFIX;

import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Evaluation Translation Utility
 * @author jmhill
 *
 */
public class EvaluationTranslationUtil {


	public static EvaluationDBO createDatabaseObjectFromBackup(EvaluationBackup backup) {
		EvaluationDBO dbo = new EvaluationDBO();
		dbo.setContentSource(getContentSource(backup.getContentSource()));
		dbo.setCreatedOn(backup.getCreatedOn());
		dbo.setDescription(backup.getDescription());
		dbo.seteTag(backup.geteTag());
		dbo.setId(backup.getId());
		dbo.setName(backup.getName());
		dbo.setOwnerId(backup.getOwnerId());
		dbo.setSubmissionInstructionsMessage(backup.getSubmissionInstructions());
		dbo.setSubmissionReceiptMessage(backup.getSubmissionReceiptMessage());
		dbo.setQuota(backup.getQuota());
		dbo.setStartTimestamp(backup.getStartTimestamp());
		dbo.setEndTimestamp(backup.getEndTimestamp());
		return dbo;
	}

	public static EvaluationBackup createBackupFromDatabaseObject(EvaluationDBO dbo) {
		EvaluationBackup backup = new EvaluationBackup();
		backup.setContentSource(KeyFactory.keyToString(dbo.getContentSource()));
		backup.setCreatedOn(dbo.getCreatedOn());
		backup.setDescription(dbo.getDescription());
		backup.seteTag(dbo.getEtag());
		backup.setId(dbo.getId());
		backup.setName(dbo.getName());
		backup.setOwnerId(dbo.getOwnerId());
		backup.setSubmissionInstructions(dbo.getSubmissionInstructionsMessage());
		backup.setSubmissionReceiptMessage(dbo.getSubmissionReceiptMessage());
		backup.setQuota(dbo.getQuota());
		backup.setStartTimestamp(dbo.getStartTimestamp());
		backup.setEndTimestamp(dbo.getEndTimestamp());
		return backup;
	}
	
	/**
	 * We did not start off with requiring a content source to be equals to an entity ID
	 * so we need extra logic to to ensure we parse it correctly.
	 * 
	 * @param contentSource
	 * @return
	 */
	public static Long getContentSource(String contentSource){
		if(contentSource == null || "".equals(contentSource.trim())){
			return ROOT_ID;
		}
		try{
			if(contentSource.toLowerCase().startsWith(SYNAPSE_ID_PREFIX)){
				return Long.parseLong(contentSource.substring(3));
			}else{
				return Long.parseLong(contentSource);
			}
		}catch(NumberFormatException nfe){
			return ROOT_ID;
		}
	}


}

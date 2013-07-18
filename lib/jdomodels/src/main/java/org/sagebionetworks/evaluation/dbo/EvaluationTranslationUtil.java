package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.jdo.KeyFactory.ROOT_ID;
import static org.sagebionetworks.repo.model.jdo.KeyFactory.SYNAPSE_ID_PREFIX;

import java.util.Date;

import org.sagebionetworks.evaluation.dao.EvaluationDAOImpl;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
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
		dbo.setStatusEnum(EvaluationStatus.values()[backup.getStatus()]);
		
		byte[] serializedObject = backup.getSerializedObject();
		if (serializedObject != null) {
			dbo.setSerializedObject(serializedObject);
		} else {
			// no serialized object from source; create it now (PLFM-2043)
			Evaluation dto = convertDboToDtoIgnoreSerialized(dbo);
			EvaluationDAOImpl.copyToSerializedField(dto, dbo);
		}
		return dbo;
	}

	// create DTO from first-class columns of the DBO
	private static Evaluation convertDboToDtoIgnoreSerialized(EvaluationDBO dbo) {
		Evaluation dto = new Evaluation();
		dto.setContentSource(dbo.getContentSource().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setDescription(new String(dbo.getDescription()));
		dto.setEtag(dbo.geteTag());
		dto.setId(dbo.getIdString());
		dto.setName(dbo.getName());
		dto.setOwnerId(dbo.getOwnerId().toString());
		dto.setStatus(dbo.getStatusEnum());
		return dto;
	}

	public static EvaluationBackup createBackupFromDatabaseObject(EvaluationDBO dbo) {
		EvaluationBackup backup = new EvaluationBackup();
		backup.setContentSource(KeyFactory.keyToString(dbo.getContentSource()));
		backup.setCreatedOn(dbo.getCreatedOn());
		backup.setDescription(dbo.getDescription());
		backup.seteTag(dbo.geteTag());
		backup.setId(dbo.getId());
		backup.setName(dbo.getName());
		backup.setOwnerId(dbo.getOwnerId());
		backup.setStatus(dbo.getStatus());
		backup.setSerializedObject(dbo.getSerializedObject());
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

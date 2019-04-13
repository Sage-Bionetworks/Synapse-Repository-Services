package org.sagebionetworks.annotations.worker;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.List;

import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDaoImpl;
import org.sagebionetworks.repo.model.dbo.dao.NodeDAOImpl;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.util.StringUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * If this class shouldn't exists after May 2019.
 */
public class TEMPORARYAnnotationFixWorker implements MessageDrivenRunner {
	@Autowired
	NodeDAO nodeDAO;

	@Autowired
	WorkerLogger logger;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		//well this is hacky but at least this worker is temporary
		String[] lines = message.getBody().split("\n");
		for(String line : lines){
			if(StringUtils.isEmpty(line)){
				continue;
			}

			String[] nodeIdAndRevision = line.split(";");
			long nodeId = Long.parseLong(nodeIdAndRevision[0]);
			long revision = Long.parseLong(nodeIdAndRevision[1]);

			try {
				NamedAnnotations namedAnnotations = nodeDAO.getAnnotationsForVersion(KeyFactory.keyToString(nodeId), revision);
				deleteConcreteTypeAnnotation(namedAnnotations.getAdditionalAnnotations());
				deleteConcreteTypeAnnotation(namedAnnotations.getPrimaryAnnotations());

				((NodeDAOImpl) nodeDAO).TEMPORARYMETHODupdateAnnotationsForVersion(nodeId, revision, namedAnnotations);
			} catch (NotFoundException e){
				throw new RecoverableMessageException(e);
			} catch (Exception e){
				logger.logWorkerFailure(TEMPORARYAnnotationFixWorker.class.getName(), e, false);
			}
		}

	}


	void deleteConcreteTypeAnnotation(Annotations annotation){
		List<String> annoValue = annotation.getStringAnnotations().get(ObjectSchema.CONCRETE_TYPE);
		if(annoValue != null && annoValue.size() == 1 && annoValue.get(0).startsWith("org.sagebionetworks")){
			annotation.deleteAnnotation(ObjectSchema.CONCRETE_TYPE);
		}
	}
}

package org.sagebionetworks.annotations.worker;

import com.amazonaws.services.sqs.model.Message;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDaoImpl;
import org.sagebionetworks.repo.model.dbo.dao.NodeDAOImpl;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		//well this is hacky but at least this worker is temporary
		String[] nodeIdAndRevision = message.getBody().split(";");
		long nodeId = Long.parseLong(nodeIdAndRevision[0]);
		long revision = Long.parseLong(nodeIdAndRevision[1]);

		NamedAnnotations namedAnnotations = nodeDAO.getAnnotationsForVersion(KeyFactory.keyToString(nodeId), revision);
		namedAnnotations.getAdditionalAnnotations().deleteAnnotation(ObjectSchema.CONCRETE_TYPE);
		namedAnnotations.getPrimaryAnnotations().deleteAnnotation(ObjectSchema.CONCRETE_TYPE);

		((NodeDAOImpl) nodeDAO).TEMPORARYMETHODupdateAnnotationsForVersion(nodeId, revision, namedAnnotations);
	}
}

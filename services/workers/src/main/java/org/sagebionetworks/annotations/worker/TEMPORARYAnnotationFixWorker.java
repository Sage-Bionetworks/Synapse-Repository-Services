package org.sagebionetworks.annotations.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
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
		List<Long[]> idAndVersions = new ArrayList<>();
		BufferedReader lineReader = new BufferedReader(new StringReader(message.getBody()));
		for(String line = lineReader.readLine(); !StringUtils.isEmpty(line); line=lineReader.readLine()){
			String[] nodeIdAndRevision = line.split(";");
			idAndVersions.add( new Long[]{Long.parseLong(nodeIdAndRevision[0]), Long.parseLong(nodeIdAndRevision[1])});
		}

		cleanUpAnnotations(idAndVersions);
	}

	@WriteTransaction
	void cleanUpAnnotations(List<Long[]> idAndVersions){
		//todo: check etags
		//TODO: reuse same xstream object?
		long now = System.currentTimeMillis();
		try {
			NodeDAOImpl nodeDaoImpl = (NodeDAOImpl) nodeDAO;
			List<Object[]> listOf_blob_Id_Version = nodeDaoImpl.TEMPORARYGetAnnotations(idAndVersions);

			for(Object[] blob_Id_Version : listOf_blob_Id_Version){
				NamedAnnotations namedAnnotations = JDOSecondaryPropertyUtils.decompressedAnnotations((byte[]) blob_Id_Version[0]);

				deleteConcreteTypeAnnotation(namedAnnotations.getPrimaryAnnotations());
				deleteConcreteTypeAnnotation(namedAnnotations.getAdditionalAnnotations());


				blob_Id_Version[0] = JDOSecondaryPropertyUtils.compressAnnotations(namedAnnotations);
			}

			nodeDaoImpl.TEMPORARYBatchUpdateAnnotations(listOf_blob_Id_Version);
		} catch (IOException e){
			logger.logWorkerFailure(TEMPORARYAnnotationFixWorker.class.getName(), e, false);
		}
		System.out.println("time = " + (System.currentTimeMillis() - now));
	}

	public static void deleteConcreteTypeAnnotation(Annotations annotation){
		Map<String, List<String>> stringAnnotations = annotation.getStringAnnotations();
		List<String> annoValue = stringAnnotations.get(ObjectSchema.CONCRETE_TYPE);
		if(annoValue != null && annoValue.size() == 1 && annoValue.get(0).startsWith("org.sage")){
			stringAnnotations.remove(ObjectSchema.CONCRETE_TYPE);
		}
	}
}
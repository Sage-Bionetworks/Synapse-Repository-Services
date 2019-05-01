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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.NodeManagerImpl;
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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * If this class shouldn't exists after May 2019.
 */
public class TEMPORARYAnnotationFixWorker implements MessageDrivenRunner {
	@Autowired
	NodeManager nodeManager;

	@Autowired
	WorkerLogger logger;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		//well this is hacky but at least this worker is temporary
		List<Long> nodeIds = new ArrayList<>();
		BufferedReader lineReader = new BufferedReader(new StringReader(message.getBody()));
		for(String line = lineReader.readLine(); !StringUtils.isEmpty(line); line=lineReader.readLine()){
			nodeIds.add(Long.parseLong(line));
		}

		for(Long id : nodeIds) {
			((NodeManagerImpl)nodeManager).TEMPORARYcleanUpAnnotations(id);
		}
	}

}
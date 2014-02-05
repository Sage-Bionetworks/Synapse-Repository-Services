package org.sagebionetworks.table.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker updates the index used to support the tables features. It will
 * listen to table changes and apply them to the RDS table that acts as the
 * index of the table feature.
 * 
 * @author John
 * 
 */
public class TableWorker implements Callable<List<Message>> {

	List<Message> messages;
	ConnectionFactory tableConnectionFactory;
	TableRowTruthDAO tableTruthDAO;
	TableIndexDAO tableIndexDAO;
	StackConfiguration configuration;

	
	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		// If the feature is disabled then we simply swallow all messages
		if(!configuration.getTableEnabled()){
			return messages;
		}
		// process each message
		for(Message message: messages){
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			// We only care about entity messages here
			if(ObjectType.TABLE == change.getObjectType()){
				String tableId = change.getObjectId();
				// First attempt to get a connection for this table
				SimpleJdbcTemplate connection = tableConnectionFactory.getConnection(tableId);
				// If we cannot get a connection then this message must go back on the queue.
				if(connection != null){
					// Determine if the index is already up to date for this table.
					Long indexMaxVersion = tableIndexDAO.getMaxVersionForTable(connection, tableId);
					if(indexMaxVersion == null){
						// This means the table index does not exist so we must create it.
						
					}else{
						// We have a version so determine what we need to apply.
						
					}
				}
			}else{
				// Non-table messages must be returned so they can be removed from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}

}

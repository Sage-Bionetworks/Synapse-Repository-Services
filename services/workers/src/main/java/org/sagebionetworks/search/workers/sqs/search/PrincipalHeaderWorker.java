package org.sagebionetworks.search.workers.sqs.search;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.PrincipalType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;

/**
 * The worker that processes messages sending messages to users
 * 
 */
public class PrincipalHeaderWorker implements Callable<List<Message>> {

	static private Logger log = LogManager.getLogger(PrincipalHeaderWorker.class);

	private List<Message> messages;
	private PrincipalHeaderDAO prinHeadDAO;
	private UserGroupDAO userGroupDAO;
	private UserProfileDAO userProfileDAO;
	private TeamDAO teamDAO;

	public PrincipalHeaderWorker(List<Message> messages,
			PrincipalHeaderDAO prinHeadDAO, UserGroupDAO userGroupDAO,
			UserProfileDAO userProfileDAO, TeamDAO teamDAO) {
		if (messages == null) {
			throw new IllegalArgumentException("Messages cannot be null");
		}
		if (prinHeadDAO == null) {
			throw new IllegalArgumentException("PrincipalHeaderDAO cannot be null");
		}
		if (userGroupDAO == null) {
			throw new IllegalArgumentException("UserGroupDAO cannot be null");
		}
		if (userProfileDAO == null) {
			throw new IllegalArgumentException("UserProfileDAO cannot be null");
		}
		if (teamDAO == null) {
			throw new IllegalArgumentException("TeamDAO cannot be null");
		}
		this.messages = messages;
		this.prinHeadDAO = prinHeadDAO;
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
		this.teamDAO = teamDAO;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		for (Message message : messages) {
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			
			// We only care about PRINCIPAL or TEAM messages here
			if (ObjectType.PRINCIPAL == change.getObjectType() || ObjectType.TEAM == change.getObjectType()) {
				try {
					switch (change.getChangeType()) {
					case CREATE:
					case UPDATE:
						processPrincipal(Long.parseLong(change.getObjectId()));
						break;
					case DELETE:
						// The table cascade deletes, so there's nothing to do here
						break;
					default:
						throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
					}
					
					// This message was processed
					processedMessages.add(message);
				} catch (NotFoundException e) {
					log.info("NotFound: " + e.getMessage() + ". The message will be returned as processed and removed from the queue");
					processedMessages.add(message);
				} catch (Throwable e) {
					// Something went wrong and we did not process the message
					log.error("Failed to process message", e);
				}
			} else {
				// Non-MESSAGE messages must be returned so they can be removed from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}
	
	/**
	 * Refreshes the PrincipalHeader rows for the given principal
	 * 
	 * @throws NotFoundException
	 *             These are expected and generally mean that there is not
	 *             enough information to construct any identifying fragments for
	 *             the principal
	 */
	private void processPrincipal(long principalId) throws NotFoundException {
		// Clear all existing entries for the principal 
		prinHeadDAO.delete(principalId);
		
		Set<String> fragments = new HashSet<String>();
		PrincipalType pType;
		
		//TODO There's currently no way of differentiating between Synapse and Bridge users
		DomainType domain = DomainType.SYNAPSE;
		
		// Is this a user or team?  (or neither, in which case a NotFound will be thrown)
		boolean isIndividual = userGroupDAO.get("" + principalId).getIsIndividual();
		
		// Gather up the identifier fragments
		//TODO There are probably more fragments that can be made
		if (isIndividual) {
			pType = PrincipalType.USER;
			
			UserProfile profile = userProfileDAO.get("" + principalId);
			String displayName = profile.getDisplayName();
			if (displayName != null) {
				fragments.addAll(Lists.newArrayList(StringUtils.split(displayName)));
			}
		} else {
			pType = PrincipalType.TEAM;
			
			Team team = teamDAO.get("" + principalId);
			String teamName = team.getName();
			if (teamName != null) {
				fragments.addAll(Lists.newArrayList(StringUtils.split(teamName)));
			}
		}
		
		// Fill the entries back in
		prinHeadDAO.insertNew(principalId, fragments, pType, domain);
	}

}

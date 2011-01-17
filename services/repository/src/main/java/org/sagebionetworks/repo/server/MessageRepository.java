
package org.sagebionetworks.repo.server;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.Extent;
import javax.jdo.JDODataStoreException;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.sagebionetworks.repo.model.Message;
import org.sagebionetworks.repo.model.gaejdo.PMF;

/**
 * Example persistence manager class
 * <p>
 *
 * Code lifted from the <a
 * href="http://code.google.com/p/maven-gae-plugin/source/browse/trunk/gae-archetype-jsp/src/main/resources/archetype-resources/src/main/java/server/MessageRepository.java?r=738http://code.google.com/p/maven-gae-plugin/source/browse/trunk/gae-archetype-jsp/src/main/resources/archetype-resources/src/main/java/model/Message.java?r=738">maven-gae-plugin</a>
 * template and modified a tiny bit to serve as an example
 */
public class MessageRepository {

    //static final PersistenceManagerFactory pmfInstance = JDOHelper.getPersistenceManagerFactory("transactions-optional");

    /**
     * Return all messages stored in the repository
     * 
     * @return collection of all messages stored in the repository
     */
    public List<Message> getAll() {
        PersistenceManager pm = PMF.get(); //pmfInstance.getPersistenceManager();
        try {
            List<Message> messages = new ArrayList<Message>();
            Extent<Message> extent = pm.getExtent(Message.class, false);
            for (Message message : extent) {
                messages.add(message);
            }

            extent.closeAll();

            return messages;
        }
        finally {
            pm.close();
        }
    }

    /**
     * @param message
     */
    public void create(Message message) {
        PersistenceManager pm = PMF.get(); //pmfInstance.getPersistenceManager();
        try {
            pm.makePersistent(message);
        }
        finally {
            pm.close();
        }
    }

    /**
     * @param id
     * @return the message corresponding to the id, null otherwise
     */
    public Message getById(Long id) {
        PersistenceManager pm = PMF.get(); //pmfInstance.getPersistenceManager();
        try {
            Message message = pm.getObjectById(Message.class, id);
            return message;
        }
        catch(JDODataStoreException ex) {
            return null;
        }
        finally {
            pm.close();
        }
    }

    /**
     * @param id
     * @return true if found and deleted, false otherwise
     */
    public boolean deleteById(Long id) {
        PersistenceManager pm = PMF.get(); //pmfInstance.getPersistenceManager();
        try {
            pm.deletePersistent(pm.getObjectById(Message.class, id));
            return true;
        }
        catch(JDODataStoreException ex) {
            return false;
        }
        finally {
            pm.close();
        }
    }

    /**
     * Return all messages stored in the repository within a particular range<p>
     * 
     * TODO this is a dummy class, therefore I have not bothered to implement this yet
     * 
     * @param offset
     * @param limit
     * @return collection of all messages stored in the repository
     */
    public List<Message> getRange(Integer offset, Integer limit) {
        // TODO implement me!
        return getAll();
    }

    /**
     * Return the count of all messages stored in the repository<p>
     * 
     * TODO this is a dummy class, therefore I have not bothered to implement this yet
     * 
     * @return the count
     */
    public Integer getCount() {
        // TODO implement me!
        return 42;
    }

}

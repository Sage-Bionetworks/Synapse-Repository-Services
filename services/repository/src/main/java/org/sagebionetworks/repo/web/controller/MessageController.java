/*
 * GreetController.java
 *
 * Sage Bionetworks http://www.sagebase.org
 *
 * Original author: Nicole Deflaux (nicole.deflaux@sagebase.org)
 *
 * @file   $Id: $
 * @author $Author: $
 * @date   $DateTime: $
 *
 */

package org.sagebionetworks.repo.web.controller;

import java.util.Collection;
import java.util.logging.Logger;

import org.sagebionetworks.repo.model.Message;
import org.sagebionetworks.repo.server.MessageRepository;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for CRUD operations on Message objects
 * <p><ul>  
 * <li>TODO more error handling with correct HTTP error response codes
 * <li>TODO PUT
 * <li>TODO etags
 * </ul>
 * @author deflaux
 */
@Controller
@RequestMapping("/message")
public class MessageController extends BaseController {

    private static final Logger log = Logger.getLogger(MessageController.class.getName());
    
    @Autowired
    private MessageRepository messageRepository;

    /**
     * Get messages
     * <p><ul>
     * <li>TODO pagination
     * <li>TODO filter by date
     * </ul>
     * @return collection of all messages stored in the respository 
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "", method = RequestMethod.GET)
        public @ResponseBody Collection<Message> getMessages() {
        Collection<Message> messages = messageRepository.getAll();
        return messages;
    }

    /**
     * Get a specific message
     * <p><ul>
     * <li>TODO generate java doc and see if Spring adds anything about
     *   the url template
     * </ul>
     *   
     * @param id the unique identifier for the message to be returned 
     * @return the message or exception if not found
     * @throws NotFoundException 
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
        public @ResponseBody Message getMessage(@PathVariable Long id) throws NotFoundException {
        Message message = messageRepository.getById(id);
        if(null == message) {
            throw new NotFoundException("no message with id " + id + " exists");
        }
        return message;
    }

    /**
     * Create a new message
     * <p><ul>
     * <li>TODO validate minimum requirements for new message object
     * </ul>
     * @param newMessage 
     * @return the newly created message 
     */
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public @ResponseBody Message createMessage(@RequestBody Message newMessage) {
        // TODO check newMessage.isValid()
        // newMessage.getValidationErrorMessage()
        messageRepository.create(newMessage);
        return newMessage;
    }

    /**
     * Update an existing message
     * <p><ul>
     * <li>TODO implement me!
     * <li>validate updated message
     * </ul>
     * @param id the unique identifier for the message to be updated
     * @param updatedMessage 
     */
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public void updateMessage(@PathVariable Long id, @RequestBody Message updatedMessage) {
        return;
    }
    
    /**
     * Delete a specific message
     * <p>
     * @param id the unique identifier for the message to be deleted 
     * @throws NotFoundException 
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteMessage(@PathVariable Long id) throws NotFoundException {
        if(!messageRepository.deleteById(id)) {
            throw new NotFoundException("no message with id " + id + " exists");   
        }
        return;
    }

    /**
     * Simple sanity check test request, using the default view 
     * 
     * @param modelMap the parameter into which output data is to be stored
     * @return a dummy hard-coded response
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "test", method = RequestMethod.GET)
        public String sanityCheck(ModelMap modelMap) {
        modelMap.put("hello","REST rocks");
        return ""; // use the default view
    }
        
}

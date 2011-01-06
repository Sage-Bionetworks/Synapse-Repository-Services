
package org.sagebionetworks.repo.model;

import java.io.Serializable;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Example model class annotated to produce XML in addition to JSON
 * <p>
 * TODO Notice the size limit placed on the content string; Google datastore limits String properties
 * to no more than 500 characters. (Use com.google.appengine.api.datastore.Text for unlimited-size text properties.)

 * <p>
 *
 * Code lifted from the <a href="http://code.google.com/p/maven-gae-plugin/source/browse/trunk/gae-archetype-jsp/src/main/resources/archetype-resources/src/main/java/model/Message.java?r=738">maven-gae-plugin</a> template
 *
 */
@XmlRootElement
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Persistent
    private String text;

    /**
     * Default constructor
     */
    public Message() {
    }

    /**
     * @return the unique identifier for the message
     */
    @XmlElement
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the text of the message
     */
    @XmlElement
    public String getText() {
        return text;
    }

    /**
     * @param text
     */
    public void setText(String text) {
        this.text = text;
    }

}
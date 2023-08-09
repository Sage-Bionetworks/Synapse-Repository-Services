package org.sagebionetworks.repo.model;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Extract field of the object.
 *
 * @author John
 */
public class AccessRecordExtractorUtil {
    /**
     * Exact the field from an object
     *
     * @param object
     * @return
     */
    public static Optional<String> getObjectFieldValue(Object object, String fieldName) {
        if (object == null) return Optional.empty();
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object ob = field.get(object);
            if (ob instanceof String) {
                return Optional.of(ob.toString());
            } else if (ob instanceof Long) {
                return Optional.of(((Long) ob).toString());
            } else return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

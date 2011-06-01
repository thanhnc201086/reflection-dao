package org.pleasantnightmare.dbase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3/31/11 2:49 PM
 */
public class DefaultIdentified implements Identified {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIdentified.class);
    private Integer id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        LOGGER.debug("Object '{}' identified and persisted: {}", this, this.id);
    }

    public boolean isPersisted() {
        return id != null;
    }
}

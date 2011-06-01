package org.pleasantnightmare.dbase;

/**
 * Persistence uses this interface to correctly connect
 * objects to their persisted state.
 * <p/>
 * Please <strong>DO NOT</strong> set id's by hand, persistence layer will do that
 * automatically for you. If you need to persist new object,
 * make sure that {@code isAlreadyIdentified} returns false, because
 * by using check on that method, persistence will figure out
 * if object is already stored and needs update, or it is a new
 * object that needs autogenerated ID.
 *
 * @author ivicaz
 */
public interface Identified {
    int getId();

    void setId(int id);

    boolean isPersisted();
}

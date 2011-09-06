package org.pleasantnightmare.dbase.reflection;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.pleasantnightmare.dbase.Identified;
import sun.beans.editors.*;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

/**
 * Contains methods to ensure that reflection DAO is properly initialized
 * and provides connection for all reflection DAO implementations.
 *
 * @author ivicaz
 */
public final class ReflectionDAO {
    private static final String EL_ROOT = "reflection-dao";
    private static final String EL_FIELD = "field";
    private static final String EL_CLASS = "class";
    private static final String EL_LIST = "list";
    private static final String ATTR_LIST_GENERICTYPE = "type";
    private static final String EL_SET = "set";
    private static final String ATTR_SET_GENERICTYPE = "type";
    private static final String UNDETERMINED_TYPE = "NULL";
    private static final String EL_COLLECTIONDATA = "data";
    private static final String EL_MAP = "map";
    private static final String EL_MAPENTRY = "entry";
    private static final String ATTR_MAP_ENTRYKEY = "key";
    private static final String ATTR_MAP_VALUETYPE = "value-type";
    private static final String ATTR_MAP_KEYTYPE = "key-type";

    private static ReflectionDAO INSTANCE;

    public static ReflectionDAO getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ReflectionDAO();
        return INSTANCE;
    }

    public static <T extends Identified> Table<T> instantiateViewOn(Class<T> dataClass) {
        return new Table<T>(getInstance().dbaseRoot, dataClass);
    }


    private File dbaseRoot;

    private ReflectionDAO() {
        setupRoot();
        setupPropertyEditors();
    }

    private void setupPropertyEditors() {
//        PropertyEditorManager.registerEditor(Boolean.class, BoolEditor.class);
        PropertyEditorManager.registerEditor(Byte.class, ByteEditor.class);
        PropertyEditorManager.registerEditor(Color.class, ColorPropertyEditor.class);
        PropertyEditorManager.registerEditor(Double.class, DoubleEditor.class);
        PropertyEditorManager.registerEditor(Float.class, FloatEditor.class);
//        PropertyEditorManager.registerEditor(Integer.class, IntEditor.class);
        PropertyEditorManager.registerEditor(Long.class, LongEditor.class);
        PropertyEditorManager.registerEditor(Short.class, ShortEditor.class);
        PropertyEditorManager.registerEditor(String.class, StringEditor.class);

        PropertyEditorManager.registerEditor(Integer[].class, ArrayPropertyEditor.class);
        PropertyEditorManager.registerEditor(String[].class, ArrayPropertyEditor.class);
        // Add more property editors as needed
    }

    private void setupRoot() {
        // Setup root of serialized classes here
        String userHome = System.getProperty("user.home");
        dbaseRoot = new File(userHome, ".reflectiondao/persistence");

        if (!dbaseRoot.exists())
            dbaseRoot.mkdirs();
    }

    public void serialize(Object o, OutputStream outputStream) {
        Element root = new Element(EL_ROOT);
        checkDefaultConstructor(o);
        setupClass(o, root);
        setupFields(o, root);
        output(outputStream, root);
    }

    private void checkDefaultConstructor(Object o) {
        try {
            o.getClass().getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing default constructor!", e);
        }
    }

    private void output(OutputStream outputStream, Element root) {
        XMLOutputter output = new XMLOutputter();
        try {
            output.output(root, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setupFields(Object persistingObject, Element root) {
        Class<? extends Object> persistingClass = persistingObject.getClass();

        while (persistingClass != Object.class) {
            Field[] declaredFields = persistingClass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (isFieldValidForPersisting(field)) {
                    try {
                        boolean wasAccesible = field.isAccessible();
                        field.setAccessible(true);

                        Element fieldElement = new Element(EL_FIELD);
                        persistField(fieldElement, field.getType(), field.get(persistingObject));
                        root.addContent(fieldElement);

                        field.setAccessible(wasAccesible);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }

            persistingClass = persistingClass.getSuperclass();
        }
    }

    private void persistField(Element element, Class<?> type, Object fieldValue) throws IllegalAccessException {
        if (List.class.equals(type))
            persistList(element, (List<?>) fieldValue);
        else if (Set.class.equals(type))
            persistSet(element, (Set<?>) fieldValue);
        else if (Map.class.equals(type))
            persistMap(element, (Map<?, ?>) fieldValue);
        else
            element.setText(convert(fieldValue));
    }

    private void persistList(Element fieldElement, List<?> list) throws IllegalAccessException {
        Element listElement = new Element(EL_LIST);
        if (!list.isEmpty()) {
            listElement.setAttribute(ATTR_LIST_GENERICTYPE, list.get(0).getClass().getName());
            for (Object o : list) {
                Element listDataElement = new Element(EL_COLLECTIONDATA);
                persistField(listDataElement, o.getClass(), o);
                listElement.addContent(listDataElement);
            }
        } else {
            listElement.setAttribute(ATTR_LIST_GENERICTYPE, UNDETERMINED_TYPE);
        }
        fieldElement.addContent(listElement);
    }

    private void persistSet(Element fieldElement, Set<?> set) throws IllegalAccessException {
        Element setElement = new Element(EL_SET);
        if (!set.isEmpty()) {
            setElement.setAttribute(ATTR_SET_GENERICTYPE, set.iterator().next().getClass().getName());
            for (Object o : set) {
                Element listDataElement = new Element(EL_COLLECTIONDATA);
                persistField(listDataElement, o.getClass(), o);
                setElement.addContent(listDataElement);
            }
        } else {
            setElement.setAttribute(ATTR_SET_GENERICTYPE, UNDETERMINED_TYPE);
        }
        fieldElement.addContent(setElement);
    }

    private void persistMap(Element fieldElement, Map<?, ?> map) throws IllegalAccessException {
        Element mapElement = new Element(EL_MAP);
        if (!map.isEmpty()) {
            Map.Entry<?, ?> classDeterminingEntry = map.entrySet().iterator().next();
            mapElement.setAttribute(ATTR_MAP_KEYTYPE, classDeterminingEntry.getKey().getClass().getName());
            mapElement.setAttribute(ATTR_MAP_VALUETYPE, classDeterminingEntry.getValue().getClass().getName());

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Element mapEntryElement = new Element(EL_MAPENTRY);

                mapEntryElement.setAttribute(ATTR_MAP_ENTRYKEY, convert(entry.getKey()));
                persistField(mapEntryElement, entry.getValue().getClass(), entry.getValue());
                mapElement.addContent(mapEntryElement);
            }
        } else {
            mapElement.setAttribute(ATTR_MAP_KEYTYPE, UNDETERMINED_TYPE);
            mapElement.setAttribute(ATTR_MAP_VALUETYPE, UNDETERMINED_TYPE);
        }
        fieldElement.addContent(mapElement);
    }

    private void setupClass(Object o, Element root) {
        Element classElement = new Element(EL_CLASS);
        classElement.setText(o.getClass().getName());
        root.addContent(classElement);
    }

    public <T> T deserialize(Class<T> dataClass, InputStream inputStream) {
        Element root = getRootElement(inputStream);
        checkClazz(dataClass, root);
        T object = createNewInstance(dataClass);
        fillInFields(dataClass, root, object);
        return object;
    }

    private <T> void fillInFields(Class<T> dataClass, Element root, T loadedObject) {
        List<Element> fieldElements = (List<Element>) root.getChildren(EL_FIELD);
        Iterator<Element> fieldIterator = fieldElements.iterator();
        Class persistingClass = dataClass;

        while (persistingClass != Object.class) {
            Field[] fields = persistingClass.getDeclaredFields();
            for (Field field : fields) {
                if (isFieldValidForPersisting(field)) {
                    boolean wasAccesible = field.isAccessible();
                    field.setAccessible(true);

                    Element fieldEl = fieldIterator.next();
                    try {
                        Object fieldObject = loadObject(fieldEl, field.getType());
                        field.set(loadedObject, fieldObject);
                    } catch (IllegalAccessException e) {
                        // Shouldn't ever happen
                        throw new IllegalStateException(e);
                    } catch (ClassNotFoundException e) {
                        // Shouldn't ever happen
                        throw new IllegalStateException(e);
                    }

                    field.setAccessible(wasAccesible);
                }
            }

            persistingClass = persistingClass.getSuperclass();
        }
    }

    private boolean isFieldValidForPersisting(Field field) {
        return !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers());
    }

    private Object loadObject(Element fieldEl, Class<?> type) throws ClassNotFoundException {
        if (List.class.equals(type))
            return loadList(fieldEl);
        else if (Set.class.equals(type))
            return loadSet(fieldEl);
        else if (Map.class.equals(type))
            return loadMap(fieldEl);
        else
            return convert(type, fieldEl.getText());
    }

    private List<?> loadList(Element fieldEl) throws ClassNotFoundException {
        Element listElement = fieldEl.getChild(EL_LIST);
        String genericTypeString = listElement.getAttributeValue(ATTR_LIST_GENERICTYPE);
        if ("NULL".equals(genericTypeString))
            return new LinkedList<Object>();

        Class<?> listGenericType = Class.forName(genericTypeString);
        // TODO BEWARE OF EMPTY COLLECTION!!!
        List<Element> children = (List<Element>) listElement.getChildren(EL_COLLECTIONDATA);
        List<Object> returnable = new ArrayList<Object>(children.size());
        for (Element child : children)
            returnable.add(loadObject(child, listGenericType));
        return returnable;
    }

    private Set<?> loadSet(Element fieldEl) throws ClassNotFoundException {
        Element setElement = fieldEl.getChild(EL_SET);
        String genericTypeString = setElement.getAttributeValue(ATTR_SET_GENERICTYPE);
        if ("NULL".equals(genericTypeString))
            return new HashSet<Object>();

        Class<?> setGenericType = Class.forName(genericTypeString);
        // TODO BEWARE OF EMPTY COLLECTION!!!
        List<Element> children = (List<Element>) setElement.getChildren(EL_COLLECTIONDATA);
        Set<Object> returnable = new HashSet<Object>(children.size());
        for (Element child : children)
            returnable.add(loadObject(child, setGenericType));
        return returnable;
    }

    private Map<?, ?> loadMap(Element fieldEl) throws ClassNotFoundException {
        Element setElement = fieldEl.getChild(EL_MAP);
        String keyGenericTypeString = setElement.getAttributeValue(ATTR_MAP_KEYTYPE);
        if (keyGenericTypeString.equals("NULL"))
            return new HashMap<Object, Object>();

        Class<?> keyGenericType = Class.forName(keyGenericTypeString);
        Class<?> valueGenericType = Class.forName(setElement.getAttributeValue(ATTR_MAP_VALUETYPE));

        // TODO BEWARE OF EMPTY COLLECTION!!!
        List<Element> children = (List<Element>) setElement.getChildren(EL_MAPENTRY);
        Map<Object, Object> returnable = new HashMap<Object, Object>(children.size());
        for (Element child : children) {
            Object key = convert(keyGenericType, child.getAttributeValue(ATTR_MAP_ENTRYKEY));
            Object value = loadObject(child, valueGenericType);
            returnable.put(key, value);
        }
        return returnable;

    }

    private Object convert(Class<?> type, String text) {
        if ("null".equals(text))
            return null;
        PropertyEditor pe = PropertyEditorManager.findEditor(type);
        if (pe == null)
            throw new IllegalStateException("Don't know how to convert class: " + type);

        pe.setAsText(text);
        return pe.getValue();
    }

    private String convert(Object o) {
        if (o == null)
            return "null";

        PropertyEditor pe = PropertyEditorManager.findEditor(o.getClass());
        if (pe == null)
            throw new IllegalStateException("Don't know how to convert class: " + o.getClass());

        pe.setValue(o);
        return pe.getAsText();
    }

    private <T> T createNewInstance(Class<T> dataClass) {
        try {
            Constructor<T> defaultConstructor = dataClass.getDeclaredConstructor();
            boolean wasAccessible = defaultConstructor.isAccessible();
            defaultConstructor.setAccessible(true);
            T instance = defaultConstructor.newInstance();
            defaultConstructor.setAccessible(wasAccessible);
            return instance;
        } catch (InstantiationException e) {
            throw new IllegalStateException("Missing default constructor!", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Default constructor is not public!", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing default constructor (how did you manage to serialize this class?)", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to instantiate object, dunno why.", e);
        }
    }

    private void checkClazz(Class<?> dataClass, Element root) {
        String clazz = root.getChild(EL_CLASS).getText();
        if (!dataClass.getName().equals(clazz))
            throw new IllegalStateException("Deserialization error: trying to deserialize class '" + dataClass.getName() + "' from stored object class ' " + clazz + "'");
    }

    private Element getRootElement(InputStream inputStream) {
        try {
            SAXBuilder b = new SAXBuilder();
            Document document = b.build(inputStream);
            return document.getRootElement();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (JDOMException e) {
            throw new IllegalStateException(e);
        }
    }
}

package org.pleasantnightmare.dbase.reflection;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArrayPropertyEditor extends PropertyEditorSupport {
  private static final Pattern REGEX = Pattern.compile("\\[L(.+?);:([0-9]+?):\\((.*?)\\)");

  @Override public void setAsText(String text) throws IllegalArgumentException {
    Matcher m = REGEX.matcher(text);
    if (!m.matches())
      throw new IllegalArgumentException("Cannot translate to array: " + text);

    Class<?> clazz;
    try {
      clazz = Class.forName(m.group(1));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }

    int length = Integer.parseInt(m.group(2));
    Object[] array = (Object[]) Array.newInstance(clazz, length);
    String[] data = m.group(3).split(",");
    PropertyEditor pe = PropertyEditorManager.findEditor(clazz);

    for (int i = 0; i < array.length; i++) {
      if ("null".equals(data[i]))
        array[i] = null;
      else {
        pe.setAsText(data[i]);
        array[i] = pe.getValue();
      }
    }

    setValue(array);
  }

  @Override public String getAsText() {
    Object[] array = (Object[]) getValue();

    StringBuilder builder = new StringBuilder();
    builder.append(array.getClass().getName());
    builder.append(':').append(array.length).append(':');
    builder.append("(");
    if (array.length > 0) {
      builder.append(array[0] != null ? array[0].toString() : null);
      for (int i = 1; i < array.length; i++) {
        Object o = array[i];
        builder.append(",");
        builder.append(o != null ? o.toString() : null);
      }
    }

    builder.append(")");
    return builder.toString();
  }
}

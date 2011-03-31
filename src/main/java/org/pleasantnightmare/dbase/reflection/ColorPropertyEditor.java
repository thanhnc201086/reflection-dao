package org.pleasantnightmare.dbase.reflection;

import java.awt.Color;
import java.beans.PropertyEditorSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * There already is a ColorEditor from sun, but it seems
 * that one requires some strange Swing/AWT functionality
 * that prevents Teamcity from running tests. Because this
 * might
 *
 * @author ivicaz
 */
public class ColorPropertyEditor extends PropertyEditorSupport {
  private static final Pattern REGEX = Pattern.compile("([0-9]+?),([0-9]+?),([0-9]+?)");

  @Override public void setAsText(String text) throws IllegalArgumentException {
    Matcher m = REGEX.matcher(text);
    if (!m.matches())
      throw new IllegalArgumentException("Cannot translate to color: " + text);

    int r = Integer.parseInt(m.group(1));
    int g = Integer.parseInt(m.group(2));
    int b = Integer.parseInt(m.group(3));
    setValue(new Color(r, g, b));
  }

  @Override public String getAsText() {
    Color c = (Color) getValue();
    return String.format("%s,%s,%s", c.getRed(), c.getGreen(), c.getBlue());
  }
}

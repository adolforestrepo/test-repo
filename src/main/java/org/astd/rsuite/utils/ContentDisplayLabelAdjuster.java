package org.astd.rsuite.utils;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;

public class ContentDisplayLabelAdjuster {

  private ManagedObject mo;

  private StringBuffer label;

  private StringBuffer ancilliaryLabel;

  private StringBuffer title;

  private StringBuffer tempTitle = new StringBuffer();

  private boolean labelAdjusted;

  private boolean ancilliaryLabelAdjusted;

  public enum DisplayCondition {
    EQUALS,
    NOT_BLANK,
    NOT_EQUALS,
    ALWAYS
  }

  public ContentDisplayLabelAdjuster(
      ManagedObject mo, StringBuffer label, StringBuffer ancilliaryLabel, StringBuffer title) {
    this.mo = mo;
    this.label = label;
    this.ancilliaryLabel = ancilliaryLabel;
    this.title = title;
  }

  public boolean isAncilliaryLabelAdjusted() {
    return ancilliaryLabelAdjusted;
  }

  public boolean isLabelAdjusted() {
    return labelAdjusted;
  }

  public String getNewAncilliaryLabel() {
    return ancilliaryLabel.toString();
  }

  public String getNewLabel() {
    String htmlBrowseTreeNode = "<div title=\"TITLE\">LABEL</div>";
    title.append("&#10;" + tempTitle.toString());
    htmlBrowseTreeNode = htmlBrowseTreeNode.replace("TITLE", title.toString()).replace("LABEL",
        label.toString());
    return htmlBrowseTreeNode;
  }

  public void adjustAncillaryLabel(String lmdName, String expectedLmdValue,
      DisplayCondition condition, String ancillaryLabel, String color) throws RSuiteException {
    boolean adjustLabel = isLabelAdjustable(lmdName, expectedLmdValue, condition);

    ancilliaryLabelAdjusted = (ancilliaryLabelAdjusted || adjustLabel);

    if (adjustLabel) {
      ancilliaryLabel.append(createColorSpan(ancillaryLabel, color, true));
    }
  }

  /**
   * Conditionally append text to the end of item's current label.
   * 
   * @param lmdName
   * @param expectedLmdValue
   * @param condition
   * @param labelText
   * @throws RSuiteException
   */
  public void adjustLabel(String lmdName, String expectedLmdValue, DisplayCondition condition,
      String labelText) throws RSuiteException {

    adjustLabel(lmdName, expectedLmdValue, condition, labelText, true, null);

  }

  /**
   * Conditionally append text to the beginning or end of an item's label, with an optional join
   * text.
   * 
   * @param lmdName
   * @param expectedLmdValue
   * @param condition
   * @param labelText
   * @param append
   * @param joinText
   * @throws RSuiteException
   */
  public void adjustLabel(String lmdName, String expectedLmdValue, DisplayCondition condition,
      String labelText, boolean append, String joinText) throws RSuiteException {

    boolean adjustLabel = isLabelAdjustable(lmdName, expectedLmdValue, condition);

    labelAdjusted = (labelAdjusted || adjustLabel);

    if (adjustLabel) {
      if (append) {
        if (StringUtils.isNotEmpty(joinText)) {
          label.append(joinText);
        }
        label.append(labelText);
      } else {
        if (StringUtils.isNotEmpty(joinText)) {
          label.insert(0, joinText);
        }
        label.insert(0, labelText);
      }
    }

  }

  public void addLabelTitle(String lmdName, String lmdFieldName, String expectedLmdValue,
      DisplayCondition condition, String title) throws RSuiteException {

    boolean adjustLabel = isLabelAdjustable(lmdName, expectedLmdValue, condition);

    labelAdjusted = (labelAdjusted || adjustLabel);

    if (adjustLabel) {
      if (tempTitle.toString().length() > 0) {
        tempTitle.append("&#10;");
      }

      tempTitle.append(lmdFieldName + ": " + title);
    }

  }

  private boolean isLabelAdjustable(String lmdName, String expectedLmdValue,
      DisplayCondition condition) throws RSuiteException {
    String lmdValue = mo.getLayeredMetadataValue(lmdName);

    boolean adjustLabel = false;

    switch (condition) {
      case ALWAYS:
        adjustLabel = true;
        break;

      case EQUALS:
        if (expectedLmdValue == lmdValue || (expectedLmdValue != null && expectedLmdValue.equals(
            lmdValue))) {
          adjustLabel = true;
        }
        break;

      case NOT_EQUALS:

        if (!StringUtils.isBlank(lmdValue) && !lmdValue.equals(expectedLmdValue)) {
          adjustLabel = true;
        }

        break;

      case NOT_BLANK:
        if (!StringUtils.isBlank(lmdValue)) {
          adjustLabel = true;
        }

    }

    return adjustLabel;
  }

  /**
   * Create a color-coded message.
   * 
   * @param contents Optional label for the value
   * @param color Color for this message
   * @param includeBrackets Submit true to wrap content with brackets.
   * @return A color-coded message.
   */
  public static StringBuilder createColorSpan(String contents, String color,
      boolean includeBrackets) {
    StringBuilder buf = new StringBuilder();
    buf.append(" <span style=\"color: ").append(color).append(";\">");
    if (includeBrackets)
      buf.append("[");
    buf.append(contents);
    if (includeBrackets)
      buf.append("]");
    buf.append("</span> ");

    return buf;
  }

}

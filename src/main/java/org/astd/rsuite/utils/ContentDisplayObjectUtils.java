package org.astd.rsuite.utils;

import org.apache.commons.lang3.StringUtils;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.DataTypeConstants;
import org.astd.rsuite.constants.LayeredMetadataConstants;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.content.ContentAdvisorContext;
import com.reallysi.rsuite.api.content.ContentDisplayObject;

public class ContentDisplayObjectUtils
    implements DataTypeConstants, LayeredMetadataConstants {

  public static ManagedObject getRealMo(ContentAdvisorContext context, ManagedObject mo)
      throws RSuiteException {
    String targetId = mo.getTargetId();
    ManagedObject realMo = mo;
    if (targetId != null) {
      ManagedObject referencedMo = context.getManagedObjectService().getManagedObject(context
          .getUser(), targetId);
      realMo = (referencedMo == null ? mo : referencedMo);
    }

    return realMo;
  }

  public static StringBuffer createLabel(ManagedObject mo) throws RSuiteException {

    String displayName;
    String id;
    String CA_ID = "CA_ID";
    String htmlFormatId = " <span class=\"tag-bracket\">[</span>" + "<span class=\"tag-name\">"
        + CA_ID + "</span>" + "<span class=\"tag-bracket\">]</span>";
    if (mo == null) {
      displayName = "<span style=\"color: red\">" + "Missing mo " + "</span>";
      id = "<span style=\"color: red\">" + "Missing refenced mo " + "</span>";
    } else {
      displayName = mo.getDisplayName();
      id = mo.getId();
    }

    if (displayName == null) {
      displayName = "";
    }

    StringBuffer label = new StringBuffer(displayName);
    label.append(htmlFormatId.replace(CA_ID, id));

    return label;
  }

  public static StringBuffer createTitle(ManagedObject mo) throws RSuiteException {
    String displayName = mo.getDisplayName();

    if (displayName == null) {
      displayName = "";
    }

    StringBuffer title = new StringBuffer(displayName);
    String formatedMoId = " [" + mo.getId() + "]";
    title.append(formatedMoId);

    return title;
  }

  public static StringBuffer createAncilliaryLabel(ContentDisplayObject item) {
    String ancilliaryLabel = StringUtils.isEmpty(item.getAncillaryLabel()) ? "" : item
        .getAncillaryLabel();
    return new StringBuffer(ancilliaryLabel);
  }

  public static String getAncillaryLabelFromDataType(ContentAdvisorContext context,
      ManagedObject mo, String dataTypeName, String lmdField) throws RSuiteException {
    return DataTypeUtils.getDataTypeOptionValueLabel(context, context.getUser(), dataTypeName, mo
        .getLayeredMetadataValue(lmdField));
  }

  public static String getAncillaryLabelFromDataTypeWithCustomMessage(ContentAdvisorContext context,
      ManagedObject mo, String dataTypeName, String lmdField, String projectMessageProperty)
      throws RSuiteException {
    StringBuffer ancillaryLabel = new StringBuffer();

    String dataTypeValue = getAncillaryLabelFromDataType(context, mo, dataTypeName, lmdField);

    if (StringUtils.isNotBlank(projectMessageProperty)) {
      ancillaryLabel.append(ProjectMessageResource.getMessageText(projectMessageProperty,
          dataTypeValue));
    } else {
      ancillaryLabel.append(dataTypeValue);
    }

    return ancillaryLabel.toString();
  }

  /**
   * Get a label reflecting either the current check-out owner or, when the MO isn't checked out,
   * the who created the MO.
   * 
   * @param mo
   * @return Label reflecting current check out owner or creator.
   * @throws RSuiteException
   */
  public static String getOwnerOrOriginatorAncillaryLabel(ManagedObject mo) throws RSuiteException {
    if (mo.isCheckedout()) {
      return ProjectMessageResource.getMessageText("content.display.advisor.presently.with", mo
          .getCheckOutOwner());
    }
    return ProjectMessageResource.getMessageText("content.display.advisor.originated.by", mo
        .getVersionEntry("1.0").getUserId());
  }

  /**
   * Interpret a LMD value as a percent, round, and tack on a label. For example, an LMD value of
   * "34.2942" could be returned as "34% indexed".
   * 
   * @param context
   * @param mo
   * @param lmdNameNumerator Name of LMD identifying the numerator value.
   * @param lmdNameDenominator Name of LMD identifying the demonimator value.
   * @param lmdNameMissingInput Name of LMD whose value denotes if some input was missing, and
   *        therefore the derived percent is incomplete.
   * @param label
   * @return A labeled and rounded LMD value, as a percent.
   * @throws RSuiteException
   */
  public static String getAndLabelPercentFromLmd(ContentAdvisorContext context, ManagedObject mo,
      String lmdNameNumerator, String lmdNameDenominator, String lmdNameMissingInput, String label)
      throws RSuiteException {
    String num = mo.getLayeredMetadataValue(lmdNameNumerator);
    String den = mo.getLayeredMetadataValue(lmdNameDenominator);
    if (StringUtils.isNotBlank(num) && StringUtils.isNotBlank(den) && !"0".equals(den)) {

      // Visual cue that calculation is incomplete.
      String disclaimer = Boolean.parseBoolean(mo.getLayeredMetadataValue(lmdNameMissingInput))
          ? "***" : StringUtils.EMPTY;

      return new StringBuilder(MathUtils.round(MathUtils.getPercent(num, den), 0).toPlainString())
          .append("%").append(disclaimer).append(" ").append(label).toString();
    }
    return StringUtils.EMPTY;
  }

}

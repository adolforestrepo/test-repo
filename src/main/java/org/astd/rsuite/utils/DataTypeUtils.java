package org.astd.rsuite.utils;

import com.reallysi.rsuite.api.DataType;
import com.reallysi.rsuite.api.DataTypeOptionValue;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.forms.DataTypeManager;

/**
 * A collection of static data type utility methods.
 */
public class DataTypeUtils {

  /**
   * Get a data type option value label by data type name and option value ID.
   * 
   * @param context
   * @param user
   * @param dataTypeName Name of an existing data type.
   * @param dataTypeOptionValueId ID of the data type option value the label is desired of.
   * @return Label of the data type option value identified by dataTypeOptionValueId.
   * @throws RSuiteException Thrown if unable to retrieve the specified information from RSuite.
   */
  public static String getDataTypeOptionValueLabel(ExecutionContext context, User user,
      String dataTypeName, String dataTypeOptionValueId) throws RSuiteException {
    DataTypeOptionValue ov = getDataTypeOptionValue(context, user, dataTypeName,
        dataTypeOptionValueId);
    return (ov == null ? null : ov.getLabel());
  }

  /**
   * Get a data type option value ID by data type name and option value label.
   * 
   * @param context
   * @param user
   * @param dataTypeName Name of an existing data type.
   * @param dataTypeOptionValueLabel Label of the data type option value the ID is desired of.
   * @return ID of the data type option value identified by dataTypeOptionValueLabel.
   * @throws RSuiteException Thrown if unable to retrieve the specified information from RSuite.
   */
  public static String getDataTypeOptionValueId(ExecutionContext context, User user,
      String dataTypeName, String dataTypeOptionValueLabel) throws RSuiteException {
    DataTypeOptionValue ov = getDataTypeOptionValue(context, user, dataTypeName,
        dataTypeOptionValueLabel);
    return (ov == null ? null : ov.getValue());
  }

  /**
   * Get a data type option value by data type name and the options's label <i>or</i> value.
   * 
   * @param context
   * @param user
   * @param dataTypeName Name of an existing data type.
   * @param dataTypeOptionLabelOrId Either the data type option's label or ID. The comparisons are
   *        case-insensitive.
   * @return The data type option value from the specified data type matching the given criteria.
   * @throws RSuiteException Thrown if unable to retrieve the specified information from RSuite.
   */
  public static DataTypeOptionValue getDataTypeOptionValue(ExecutionContext context, User user,
      String dataTypeName, String dataTypeOptionLabelOrId) throws RSuiteException {
    // Make sure the criteria is not null. An empty or blank string could be fine.
    if (dataTypeOptionLabelOrId != null) {
      DataTypeManager dtm = context.getDomainManager().getCurrentDomainContext()
          .getDataTypeManager();
      DataType dt = dtm.getDataType(user, dataTypeName);
      DataTypeOptionValue[] optionValueList = dt.getDiscreteValues();
      for (DataTypeOptionValue ov : optionValueList) {
        if (dataTypeOptionLabelOrId.equalsIgnoreCase(ov.getLabel()) || dataTypeOptionLabelOrId
            .equalsIgnoreCase(ov.getValue())) {
          return ov;
        }
      }
    }
    return null;
  }
}

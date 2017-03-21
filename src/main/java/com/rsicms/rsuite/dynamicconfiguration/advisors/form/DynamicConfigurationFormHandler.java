package com.rsicms.rsuite.dynamicconfiguration.advisors.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.utils.UserUtils;

import com.reallysi.rsuite.api.DataTypeOptionValue;
import com.reallysi.rsuite.api.FormControlType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.forms.DefaultFormHandler;
import com.reallysi.rsuite.api.forms.FormColumnInstance;
import com.reallysi.rsuite.api.forms.FormInstance;
import com.reallysi.rsuite.api.forms.FormInstanceCreationContext;
import com.reallysi.rsuite.api.forms.FormParameterInstance;
import com.rsicms.rsuite.dynamicconfiguration.utils.DynamicConfigurationUtils;

/**
 * Adjusts a form to show current values for dynamic configuration settings when there is a match in
 * parameter and setting name between the form definition and the dynamic configuration settings.
 * And adds parameters when there are existing settings in the dynamic configuration but no
 * corresponding parameter in the form definition (thus preventing settings from being lost in the
 * abyss).
 * <p>
 * Current assumptions: Assuming uniquely named form parameters Assuming form control types are
 * radio buttons only offering true or false Assuming that the stored dynamic configuration settings
 * will also have value true or false
 */
public class DynamicConfigurationFormHandler
    extends DefaultFormHandler {

  /**
   * Log this class is to use.
   */
  private static Log log = LogFactory.getLog(DynamicConfigurationFormHandler.class);

  @Override
  public void adjustFormInstance(FormInstanceCreationContext context, FormInstance form)
      throws RSuiteException {

    String dynamicConfigurationUsername = context.getContextParameters().get(
        DynamicConfigurationUtils.FORM_PARAM_NAME_DYNAMIC_CONFIGURATION_USERNAME);

    // FIXME: UserUtils is in the customer package.
    User dynamicConfigUser = UserUtils.getUser(context.getAuthorizationService(),
        dynamicConfigurationUsername);

    // Check to see if the dynamic configuration user exists
    if (dynamicConfigUser == null) {
      form.setInstructions("WARNING: Dynamic configuration user [".concat(
          dynamicConfigurationUsername).concat("] does not exist."));
      form.setColumns(null);
      // This will still submit to the web service configured in the plugin descriptor (if there is
      // one)
    } else {
      // get prefixes of dynamic settings that the form cares about
      String argPrefixes = context.getContextParameters().get(
          DynamicConfigurationUtils.FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES);
      List<String> prefixes = DynamicConfigurationUtils.getPrefixes(argPrefixes);

      // map to hold all parameters defined for the form
      Map<String, FormParameterInstance> allParams = new HashMap<String, FormParameterInstance>();

      // get all the parameters defined for the form
      for (FormColumnInstance fci : form.getColumns()) {
        List<FormParameterInstance> colParams = fci.getParams();
        for (FormParameterInstance fpi : colParams) {
          allParams.put(fpi.getName(), fpi);
        }
      }


      // iterate through the provided prefixes
      for (String curPrefix : prefixes) {
        // get the current settings for the prefix so that we can display them to the user
        Map<String, String> curSettingsForPrefix = DynamicConfigurationUtils.getSettings(context,
            dynamicConfigUser, curPrefix);

        if (curSettingsForPrefix != null) {

          for (Map.Entry<String, String> entry : curSettingsForPrefix.entrySet()) {
            // for each setting for the given prefix, if there is a defined form parameter for that
            // setting, then populate the parameter's value; if there is not a defined form
            // parameter then add one (that way we don't have settings just floating in the abyss)

            String settingName = entry.getKey();
            String settingValue = entry.getValue();
            if (allParams.containsKey(settingName)) {
              FormParameterInstance fpi = allParams.get(settingName);

              // With radio buttons, you signify the value that is selected with addValue()

              fpi.addValue(settingValue);

              allParams.put(settingName, fpi);
            } else {
              // setting is not in form but we need to let the user know that there is an existing
              // setting value
              FormParameterInstance fpi = new FormParameterInstance();
              fpi.setName(settingName);

              // Build the list of options
              List<DataTypeOptionValue> opts = new ArrayList<DataTypeOptionValue>();

              if ("true".equals(settingValue)) {
                // Stored setting is true so show it first
                opts.add(new DataTypeOptionValue(settingValue, settingValue));
                opts.add(new DataTypeOptionValue("false", "false"));
              } else {
                opts.add(new DataTypeOptionValue("true", "true"));
                // Stored setting is false so show it last
                opts.add(new DataTypeOptionValue(settingValue, settingValue));
              }

              fpi.setBeforeOptions(opts);

              // With radio buttons, you signify the value that is selected with addValue()
              fpi.addValue(settingValue);

              fpi.setLabel("From Config: ".concat(settingName));
              fpi.setFormControlType(FormControlType.RADIOBUTTON);
              // Params without a set column seem to all be in their own column, which draws greater
              // attention to these params that were not declared in the form definition (not
              // actually sure if the column placement is true but it is why a column is not being
              // explicitly set)
              allParams.put(settingName, fpi);
              log.info(new StringBuilder("Found setting with name [").append(settingName).append(
                  "] and value [").append(settingValue).append(
                      "] in dynamic configuration that was not present in form parameters.")
                  .toString());
            }

          }
        }
      }

      // Add a hidden form parameter that specifies the prefixes so that they do not need to be
      // listed again in the plugin descriptor as a service param
      FormParameterInstance fpi = new FormParameterInstance();
      fpi.setName(DynamicConfigurationUtils.FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES);
      fpi.addValue(argPrefixes);
      fpi.setLabel("Prefixes [DO NOT CHANGE]");
      fpi.setFormControlType(FormControlType.INPUT);
      allParams.put(DynamicConfigurationUtils.FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES, fpi);

      List<FormParameterInstance> updatedParams = new ArrayList<FormParameterInstance>(allParams
          .values());
      form.setFormParams(updatedParams);

      // IMPROVE: report on unused prefixes and prefixes that exist in settings that weren't asked
      // for?
      // IMPROVE: Column placement for unmentioned settings
    }

  }

}

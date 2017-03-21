package org.astd.rsuite.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;

/**
 * A collection of static workflow utility methods. Any business logic should be controlled by
 * method parameters.
 */
public class WorkflowUtils {

  /**
   * Class log
   */
  @SuppressWarnings("unused")
  private final static Log log = LogFactory.getLog(WorkflowUtils.class);

  /**
   * Construct a workflow variable map from an instance of <code>ProcessInstanceInfo</code>. As of
   * RSuite v5.0.5, simply wraps {@link WorkflowInstance#getInstanceVariables()}
   * 
   * @param wfInstance
   * @return The workflow variables associated to a process instance, where the map key is the
   *         variable name, and the map value is the workflow variable value.
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, Object> getVariables(WorkflowInstance wfInstance) {
    Map<String, Object> varMap = new HashMap<String, Object>();
    if (wfInstance != null) {
      varMap = wfInstance.getInstanceVariables();
    }
    return varMap;
  }

  /**
   * Determine if a string variable is defined in the provided map.
   * 
   * @param vars
   * @param varName
   * @return True if the variable name is not blank, the map contains a variable by that name, and
   *         the variable value is a String.
   */
  public static boolean hasStringVariable(Map<String, Object> vars, String varName) {
    return StringUtils.isNotBlank(varName) && vars.containsKey(varName) && vars.get(
        varName) instanceof String;
  }

  /**
   * Determine if a string variable is defined in the provided map.
   * 
   * @param vars
   * @param varName
   * @return True if the variable name is not blank, the map contains a variable by that name, and
   *         the variable value is a String.
   */
  public static boolean hasVariable(Map<String, Object> vars, String varName) {
    return StringUtils.isNotBlank(varName) && vars != null && vars.containsKey(varName);
  }

}

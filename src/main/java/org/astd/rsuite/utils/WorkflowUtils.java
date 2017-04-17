package org.astd.rsuite.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;
import com.reallysi.rsuite.service.RepositoryService;

/**
 * A collection of static workflow utility methods. Any business logic should be controlled by
 * method parameters.
 */
public class WorkflowUtils {
	
	static final String CLASSIFICATION_GI = "classification";
	static final String CLASSIFICATION_OTAG = "<"+CLASSIFICATION_GI+">";
	static final String CLASSIFICATION_ETAG = "</"+CLASSIFICATION_GI+">";

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
  
  /**
	 * Retrieve the taxonomy classification content for a given MO.
	 * <p>At this time, this function assumes the classifications is
	 * stored in the &lt;classification&gt; child element of
	 * the &lt;prolog&gt; descendent of the root element of the MO.
	 * </p>
	 * <p>TODO: Modify code to key off of DITA type values instead
	 * of element names.
	 * </p>
	 * <p>The classification XML content will be wrapped in a
	 * &lt;RESULT&gt; element.
	 * </p>
	 * @param context  Execution context.
	 * @param log  Log to use to print any log messages.
	 * @param moid  ID of MO to get classification data of.
	 * @return  XML string containing classification data.
	 * @throws RSuiteException  If an error occurs retrieving data.
	 */
	public static String getClassificationXmlOfMo(
			ExecutionContext context,
			Log log,
			String moid
	) throws RSuiteException
	{
		RepositoryService rs = context.getRepositoryService();
		String moPath = rs.getXPathToObject(moid);
		StringBuilder buf = new StringBuilder(256);
		buf.append("<RESULT>")
		   .append("{").append(moPath).append("/prolog/")
		   .append(CLASSIFICATION_GI)
		   .append("}")
		   .append("</RESULT>");
		log.info("Executing query: "+buf);
		String results = context.getRepositoryService().queryAsString(buf.toString());
		log.info("Query results: "+results);
		return results;
	}

}

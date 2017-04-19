package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectSetDynamicValuesToVariablesActionHandler extends BaseWorkflowAction
implements TempWorkflowConstants {

	private static final String NOW_STRING_FORMAT_STRING = "yyyyMMdd-HHmmss";
	private static final DateFormat NOW_STRING_DATE_FORMAT = 
			new SimpleDateFormat(NOW_STRING_FORMAT_STRING);

	private static final String DATE_STRING_FORMAT_STRING = "yyyyMMdd";
	private static final DateFormat DATE_STRING_DATE_FORMAT = 
			new SimpleDateFormat(DATE_STRING_FORMAT_STRING);

	/**
	 * Semicolon-delimited list of item names, where items name dynamic
	 * properties whose values are to be captured into variables.
	 */
	public static final String ITEMS_PARAM = "items";
	
	/**
	 * Semicolon-delimited list of variable names.
	 */
	public static final String VARIABLE_NAMES_PARAM = "variableNames";
	
		
	protected Expression variableNames;
	protected Expression items;

	public enum ItemType {
		pId("pId", "process ID"),
		// WEK: Don't know how to get token ID from workflow context.
		// tokenId ("tokenId", "token ID"),
		nowString("nowString", "Current time and date"), 
		dateString(
				"dateString", "Current date in form yyyymmdd"), ;

		private String itemName;
		private String itemDesc;

		ItemType(String itemName, String itemDesc) {
			this.itemName = itemName;
			this.itemDesc = itemDesc;
		}

		public String getItemName() {
			return itemName;
		}

		public String getItemDesc() {
			return itemDesc;
		}
	}

	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		wfLog.info("Starting Set Dynamic Values to Variables...");
		String variableNamesParam = resolveExpression(variableNames);
		String itemsParam = resolveExpression(items);
				
		if(StringUtils.isBlank(variableNamesParam) || StringUtils.isBlank(itemsParam)) {
			throw new RSuiteException("At least one of the parameters [" + variableNamesParam+" "+itemsParam+ "] must be specified");
		}
		
		String[] variableNames = variableNamesParam.split(";");
		String[] items = itemsParam.split(";");

		if (variableNames.length != items.length) {
			String msg = "The parameters \"" + VARIABLE_NAMES_PARAM
					+ "\" and \"" + ITEMS_PARAM + "\" must have the same number of "
					+ "semicolon-separated values. Values as specified are \""
					+ variableNamesParam + "\" and \"" + itemsParam + "\"";
			reportAndThrowRSuiteException(msg);
		}

		wfLog.info("Setting " + variableNames.length + " variables...");
		for (int i = 0; i < items.length; i++) {
			String itemName = resolveVariables(items[i]);
			String varName = resolveVariables(variableNames[i]);
			ItemType itemType = ItemType.valueOf(itemName);
			String value = null;
			if (itemType != null) {
				switch (itemType) {
				case pId:
					value = context.getWorkflowInstanceId();
					break;
				case nowString:
					value = getNowString();
					break;
				case dateString:
					value = getDateString();
					break;
				default:
					reportAndThrowRSuiteException(
							"Unrecognized item name \"" + itemName + "\"");
				}
				wfLog.info("[" + i + "] Setting " + varName + " to \"" + value + "\"");
				context.setVariable(varName, value);
			}
		}

		wfLog.info("Set Dynamic Values To Variables done.");
	}

	private String getDateString() {
		return DATE_STRING_DATE_FORMAT.format(new Date());
	}

	public static String getNowString() {
		return NOW_STRING_DATE_FORMAT.format(new Date());
	}

	public void setVariableNames(String variableNames) {
		setParameter(VARIABLE_NAMES_PARAM, variableNames);
	}

	public void setItems(String items) {
		setParameter(ITEMS_PARAM, items);
	}

}

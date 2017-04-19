package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.RSuiteException;

/**
 * A filename following ASTD rules for an article.
 * <p>
 * An ASTD article filename is comprised of the following compoents:
 * </p>
 * <ul>
 * <li><tt>TD</tt>
 * <li>2 character alpha code denoted article type.
 * <li>2 digit code representing volume number.
 * <li>2 digit code representing issue number.
 * <li>2 digit code representing object sequence of issue.
 * <li>0 or more characters indicating author's name.
 * <li>A filename extension.
 * </ul>
 * </p>
 */
public class ProjectAstdArticleFilename {

	/**
	 * Create new ASTD article filename.
	 * <p>
	 * If filename given does not conform to ASTD naming convention, an
	 * {@link IllegalArgumentException} will be thrown.
	 * </p>
	 * 
	 * @param name
	 *            Article filename.
	 */
	public ProjectAstdArticleFilename(String name) {
		Matcher matcher = pattern.matcher(name);

		if (!matcher.matches()) {
			throw new IllegalArgumentException(name
					+ " does not match the defined naming convention");
		} else {
			matcher.reset();
			matcher.find();

			fullFileName = matcher.group(0);
			sourceFileName = matcher.group(1);
			pubCode = matcher.group(2);
			type = matcher.group(3);
			volume = matcher.group(4);
			issue = matcher.group(5);
			sequence = matcher.group(6);
			author = matcher.group(7);
			extension = matcher.group(8);

		/*	if (!checkpubtype(pubCode, type)) {
				throw new IllegalArgumentException("Invalid article type \""
						+ type + "\" for \"" + name + "\"");
			}*/
		}
	}

	/**
	 * Set workflow variables based on filename components.
	 * 
	 * @param context
	 *            Execution context to set variable for.
	 */
/*	public void setWorkflowVariables(WorkflowExecutionContext context)
			throws RSuiteException {
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_PUB_CODE, pubCode);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_FULL_FILENAME,
				fullFileName);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_SOURCE_FILENAME,
				sourceFileName);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_ARTICLE_TYPE, type);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_VOLUME_NUMBER,
				volume);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_ISSUE, issue);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_MONTH, issue);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_SEQUENCE, sequence);
		context.setVariable(AstdWorkflowConstants.ASTD_VAR_FILENAME_AUTHOR,
				author);
	}*/

	/**
	 * Get basename.
	 */
	public String toString() {
		return getArticleBasename();
	}

	/**
	 * Get article basename.
	 * 
	 * @return Article basename.
	 */
	public String getArticleBasename() {
		return pubCode + type + volume + issue + sequence + author;
	}

	/* Check TD or TPM pubcode */

/*	private boolean checkpubtype(String pubtype, String type) {

		boolean result = false;
		switch (ArticlePubCode.valueOf(pubtype)) {
		case TD:
			result = TandDArticleType.isTypeCode(type);
			break;

		case TPM:
			result = TPMArticleType.isTypeCode(type);
			break;
			
		case CT:
			result = CTDOArticleType.isTypeCode(type);
			break;

		default:
			break;
		}

		return result;

	} */

	// ///////////////////////////////////////////////////////////////////////

	String pubCode = null;
	String fullFileName = null;
	String sourceFileName = null;
	String type = null;
	String volume = null;
	String issue = null;
	String sequence = null;
	String author = null;
	String extension = null;

	// ///////////////////////////////////////////////////////////////////////

	private static final String regex = "^((TD|TPM|CT)(\\w{2})(\\d{2})(\\d{2})(\\d{2})([^.]*))(\\.[^\\s.]+)?$";
	private static final Pattern pattern = Pattern.compile(regex,
			Pattern.CASE_INSENSITIVE);

}

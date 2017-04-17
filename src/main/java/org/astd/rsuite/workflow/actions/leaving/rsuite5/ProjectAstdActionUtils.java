package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.apache.commons.logging.Log;
import org.astd.reallysi.rsuite.service.FolderService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.RepositoryService;
import org.astd.rsuite.workflow.actions.leaving.rsuite5.ProjectAstdWorkflowConstants;
/**
 * Utilities for creating folders and content assemblies in the repository
 * within the context of a workflow.
 */
public class ProjectAstdActionUtils
{
	/**
	 * Create a new CA.
	 * <p>This method will create any folder path components if
	 * <var>parentUri</var> defines a path does not exist.
	 * </p>
	 * @param   repositoryService Repository service.
	 * @param   user User making the request.
	 * @param   ca Name of new CA.
	 * @param   parentUri Location to create CA.
	 * @param   executionContext Workflow execution context.
	 * @return	Moid of CA created, or the existing CA if it already exists.
	 * @throws	RSuiteException If an error occurs.
	 */
	public static String createCA(
			RepositoryService repositoryService,
			User user,
			String ca,
			String parentUri,
			WorkflowExecutionContext executionContext)
	throws RSuiteException
	{
		Log workflowLog = executionContext.getWorkflowLog();

		// NOTE: We have to massage parentUri here because the massaged value is
		// needed further down, after
		// we've tried to create folder (which also does this same massaging).
		if (parentUri == null)
			parentUri = "/";

		if (!parentUri.startsWith("/"))
			parentUri = "/" + parentUri;

		if (!parentUri.endsWith("/"))
			parentUri = parentUri + "/";

		createFolder(user, parentUri, executionContext);
		workflowLog
		.info("AstdActionUtils.createCA(): after createFolder, parentUri=\""
				+ parentUri + "\"");
		ContentAssemblyService caService =
			executionContext.getContentAssemblyService();

		ContentAssembly contentAssembly = caService.createContentAssembly(user,
				parentUri, ca, true);
		String moid = contentAssembly.getId();
		workflowLog.info("AstdActionUtils: CA \"" + ca
				+ "\" created. ID is " + moid);

		return moid;
	}

	/**
	 * Create folder (path).
	 * @param user User making request.
	 * @param folder Folder (path) to create.
	 * @param executionContext Execution context.
	 * @return Folder path created.
	 * @throws RSuiteException If an error occurs.
	 */
	public static String createFolder(
			User user,
			String folder,
			WorkflowExecutionContext executionContext)
	throws RSuiteException
	{
		Log workflowLog = executionContext.getWorkflowLog();

		if (folder == null || "".equals(folder) || "/".equals(folder))
			return null;
		if (!folder.startsWith("/"))
			folder = "/" + folder;
		if (folder.endsWith("/"))
			folder = folder.substring(0, folder.length() - 1);

		workflowLog.info(
				"AstdActionUtils: In createFolder(), effective folder string is \""
				+ folder + "\"");

		FolderService service = executionContext.getFolder();

		String folderpath = null;
		String p = "/";
		String f = folder;

		if (!service.hasFolder(folder + "/"))
		{
			int index = folder.lastIndexOf("/");

			if (index > 0)
			{
				p = folder.substring(0, index);
				f = folder.substring(index + 1);
			}
			else if (index == 0)
			{
				f = folder.substring(1);
			}
			workflowLog
			.info("AstdActionUtils: Before createFolder, p=\""
					+ p + "\", f=\"" + f + "\"");
			folderpath = service.createFolder(user, p, f, true, null, true)
			.getPath();
			workflowLog.info("AstdActionUtils: Folder \""
					+ folderpath + "\" created.");

		} else {
			folderpath = service.getFolder(user, folder + "/").getPath();
			workflowLog.info("AstdActionUtils: Folder \"" + folder
					+ "\" already exists, not created.");
		}

		return folderpath;
	}
	
	/**
	 * Check if article has any of its children locked.
	 * @param ca Article CA to check.
	 * @return <tt>true</tt> if any children are locked, else <tt>false</tt>
	 * @throws RSuiteException If an error occurs.
	 */
	public static boolean isArticleContentsLocked(
			ContentAssemblyItem ca
	) throws RSuiteException {
		List<? extends ContentAssemblyItem> items =
			ca.getChildrenObjects();
		if (items == null || items.size() < 1) return false;
		for (ContentAssemblyItem item : items) {
			if (item instanceof ManagedObjectReference) {
				ManagedObjectReference mor = (ManagedObjectReference)item;
				if (mor.isCheckedout()) {
					return true;
				}
				continue;
			}
			if (isArticleContentsLocked(item)) return true;
		}
		return false;
	}

	/**
	 * Check of a MO reference is to a non-XML object or not.
	 * @param mor MO reference.
	 * @return <tt>true</tt> if reference to a non-XML MO, otherwise <tt>false</tt>.
	 */
	public static boolean isNonXml(
			ManagedObjectReference mor
	) {
		// XXX: Current implementation will throw not-implemented exception
		// when trying to retrieve the namespace URI.
		/*
		String ns = mor.getNamespaceURI();
		if (ns == null || !ns.equals(AstdWorkflowConstants.RSUITE_NS_URI)) {
			return false;
		}
		*/
		String lname = mor.getLocalName();
		if (lname != null && lname.equals(ProjectAstdWorkflowConstants.RSUITE_NON_XML_LOCAL_NAME)) {
			return true;
		}
		return false;
	}

	/**
	 * Rename a non-XML MO that is contained in an article CA.
	 * <p>This method is used for supporting reassignment of articles.
	 * </p>
	 * @param user User making the request.
	 * @param mosvc MO service.
	 * @param moid ID of non-XML MO.
	 * @param basename New basename of MO.
	 * @throws RSuiteException If an error occurs.
	 */
	public static String renameArticleNonXmlMo(
			User user,
			ManagedObjectService mosvc,
			Log log,
			String moid,
			String basename
	) throws RSuiteException {
		ManagedObject mo = mosvc.getManagedObject(user, moid);
		String curName = mo.getDisplayName();
		log.info("Attempting to rename non-XML object: "+curName);
		ProjectAstdArticleFilename aname = null;;
		try {
			aname = new ProjectAstdArticleFilename(curName);
		} catch (Exception e) {
			log.info("MO \""+curName+"\" is not article naming type:"+
					e.getLocalizedMessage());
			// MO does not use article naming convention, nothing to do.
			return null;
		}
		log.info("Current MO article basename: "+aname);
		log.info("Current MO article fullname: "+aname.fullFileName);
		String ext = aname.extension;
		log.info("Filename extension: "+ext);
		String cType = (ext.charAt(0)=='.')
		               ? ext.substring(1).toUpperCase()
		               : ext.toUpperCase();
		String newName = basename + ext;
		log.info("New name to set: "+newName);
		log.info("File content type: "+cType);

		ObjectUpdateOptions options = ObjectUpdateOptions.constructOptionsForNonXml(
				newName, cType);
		options.setAliases(new String[] {newName});

		int size = (int)mo.getContentSize(); // Better not be larger than 2G!
		byte[] buf = new byte[size];
		try {
			buf = fillBuffer(mo.getInputStream(), buf, 0, size);
		} catch (java.io.IOException ioe) {
			throw new RSuiteException(ioe.getLocalizedMessage());
		}
		log.info("Checking out "+moid);
		mosvc.checkOut(user, moid);
		try {
			log.info("Updating moid: "+moid);
			mosvc.update(user, moid, new NonXmlObjectSource(buf), options);
		} finally {
			log.info("Checking in moid: "+moid);
			mosvc.checkIn(user, moid, VersionType.MINOR, "Article reassign", false);
		}
		return newName;
	}
	
	/**
	 * Fill byte array with data from an input stream.
	 * <p>If <var>off</var> and <var>len</var> parameters would cause
	 * filling to exceed <var>dest</var>, a new byte array is allocated
	 * containing a copy of <var>dest</var> contents and enough room to
	 * satisfy the fill length.
	 * </p>
	 * <p>This method will continue reading until <var>len</var> bytes
	 * have been read.
	 * </p>
	 * @param in          {@link InputStream} to get data from.
	 * @param dest        Byte array to fill.
	 * @param off         Starting fill offset into <var>dest</var>.
	 * @param len         Number of bytes to fill.
	 * @return            Reference to filled byte array, which is
	 *                    <var>dest</var> unless a new array was allocated
	 *                    because fill length exceeded <var>dest</var>
	 *                    bounds.
	 * @throws            IOException if an I/O error occurs while reading
	 *                    from <var>in</var>.
	 */
	public static byte[] fillBuffer(
			InputStream in,
			byte[] dest,
			int off,
			int len
	) throws java.io.IOException {
		int n = len+off;
		if (n > dest.length) {
			byte[] newDest = new byte[n];
			System.arraycopy(dest, 0, newDest, 0, dest.length);
			dest = newDest;
		}
		for (n=0;
		(len > 0) && (n = in.read(dest, off, len)) >= 0;
		off+=n, len-=n);
		return dest;
	}
	
	/**
	 * Write input stream to specified file.
	 * 
	 * @param file File to write to
	 * @param inputstream Input stream to write to file
	 * @return File
	 */
	public static File writeToFile(
			File file,
			InputStream inputStream
	) throws IOException
	{
		OutputStream out = new FileOutputStream(file);
		byte buf[] = new byte[1024];
		int len;
		while ((len = inputStream.read(buf)) > 0)
			out.write(buf, 0, len);
		out.close();
		inputStream.close();
		return file;
	}
	
	/**
	 * Clear article LMD field that stores current process ID.
	 * @param	context	  Execution context.
	 * @param	mo		  MO of article CA.
	 * @param	user	  User to execute action under.
	 * @return	<tt>true</tt> if field was cleared, otherwise <tt>false</tt> if
	 *          field did not exist.
	 * @throws RSuiteException
	 */
	public static boolean clearArticlePidLmdField(
			ExecutionContext context,
			ManagedObject mo,
			User user
	) throws RSuiteException
	{
		// Metadata value must be out of sync (e.g., process was manually killed)
		List<MetaDataItem> items = mo.getMetaDataItems();
		MetaDataItem pIdItem = null;
		for (MetaDataItem candItem : items) {
			// There should only be one instance of this LMD field:
			if (candItem.getName().equals(
					ProjectAstdWorkflowConstants.ASTD_ARTICLE_PID_LMD_FIELD)) {
				pIdItem = candItem;
				break;
			}
		}
		if (pIdItem != null) { // Should never be null if we got here.
			context.getManagedObjectService().removeMetaDataEntry(
					user, mo.getId(), pIdItem);
			return true;
		}
		return false;
	}
	
	/**
	 * Slurp of MO (textual) content into string.
	 * @param mo  MO to get content from.
	 * @param encoding  Text encoding to use; if null, UTF-8 is used.
	 * @return  String contain contents.
	 * @throws RSuiteException  If an I/O error occurs.
	 */
	public static String getMoText(
			ManagedObject mo,
			String encoding
	) throws RSuiteException
	{
		int len = (int)mo.getContentSize();
		if (len <= 0) {
			len = 4096;
		}
		if (encoding == null) {
			encoding = "UTF-8";
		}
		StringBuilder buf = new StringBuilder(len);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new InputStreamReader(mo.getInputStream(), encoding));
			String line;
			while ((line = reader.readLine()) != null) {
				buf.append(line);
			}
		} catch (IOException ioe) {
			throw new RSuiteException("Error reading MO: "+ioe);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (Exception e) { }
			}
		}
		return buf.toString();
	}
	
	static final String CLASSIFICATION_GI = "classification";
	static final String CLASSIFICATION_OTAG = "<"+CLASSIFICATION_GI+">";
	static final String CLASSIFICATION_ETAG = "</"+CLASSIFICATION_GI+">";
	
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

	private static String[] postPrologCandidates = {
		"<subsection",
		"<sidebar",
		"<interview-question",
		"<glossentry",
		"<glossgroup"
	};
	
	/**
	 * Set &lt;classification&gt; for given MO.
	 * @param  context  Execution context.
	 * @param  user		User to perform operations under.
	 * @param  log      Log to use for logging messages.
	 * @param  moid     ID of MO to set.
	 * @param  xml      &lt;classification&gt element and its content.
	 * @throws RSuiteException if an error occurs.
	 */
	public static void setClassificationXmlForMo(
			ExecutionContext context,
			User user,
			Log log,
			String moid,
			String xml
	) throws RSuiteException
	{
		ManagedObjectService mos = context.getManagedObjectService();
		ManagedObject mo = mos.getManagedObject(user, moid);
		setClassificationXmlForMo(context, user, log, mo, xml);
	}

	/**
	 * Set &lt;classification&gt; for given MO.
	 * @param  context  Execution context.
	 * @param  user		User to perform operations under.
	 * @param  log      Log to use for logging messages.
	 * @param  mo       MO to set.
	 * @param  xml      &lt;classification&gt element and its content.
	 * @throws RSuiteException if an error occurs.
	 */
	public static void setClassificationXmlForMo(
			ExecutionContext context,
			User user,
			Log log,
			ManagedObject mo,
			String xml
	) throws RSuiteException
	{
		ManagedObjectService mos = context.getManagedObjectService();
		String moid = mo.getId();
		if (mo.isNonXml()) {
			throw new RSuiteException(
					"Cannot set classification for non-xml object");
		}
		boolean doCheckIn = false;
		if (!mo.isCheckedout()) {
			log.info("Checking out MO ("+moid+") under user "+
					user.getUserId());
			mos.checkOut(user, moid);
			doCheckIn = true;
		}
		log.info("Classification xml: "+xml);
		
		String moXml = getMoText(mo, "UTF-8");
		StringBuilder newXml = new StringBuilder(
				moXml.length()+xml.length()+20);
		
		// XXX: Using basic string manipulation to update element.  There is
		// some risk to this since comment declarations could throw things
		// off, but seems a waste of resource to build an entire DOM to do
		// what we want.  Ideally RepositoryService would provide most
		// efficient method, but it is considered a "backdoor" and bypasses
		// RSuite application layer, which could cause problem, if not now,
		// in the future.
		int start = moXml.indexOf("<prolog");
		if (start < 0) {
			// No <prolog>, so we need to add it.
			log.info("MO has no <prolog>, trying to add...");
			start = moXml.indexOf("<body");
			if (start < 0) {
				start = moXml.indexOf("<related-links");
				if (start < 0) {
					int i, n;
					for (i=0; i < postPrologCandidates.length; ++i) {
						start = moXml.indexOf(postPrologCandidates[i]);
						if (start >= 0) {
							log.info(postPrologCandidates[i]+
									" is current insertion candidate");
							break;
						}
					}
					if (i >= postPrologCandidates.length) {
						throw new RSuiteException(
								"Cannot find insertion point for <prolog>"+
								" for MO ("+moid+")");
					}
					for (; i < postPrologCandidates.length; ++i) {
						n = moXml.indexOf(postPrologCandidates[i]);
						if (n >= 0 && n < start) {
							log.info(postPrologCandidates[i]+
									" is current insertion candidate");
							start = n;
						}
					}
				}
			}
			newXml.append(moXml.substring(0, start));
			newXml.append("<prolog>");
			newXml.append(xml);
			newXml.append("</prolog>");
			newXml.append(moXml.substring(start));

		} else {
			int end = moXml.indexOf("</prolog", start);
			if (end < 0) {
				// Have an empty prolog tag
				end = moXml.indexOf("/>", start);
				if (end < 0) {
					// Should not get here(?)
					throw new RSuiteException(
							"MO ("+moid+") has malformed <prolog>");				
				}
				newXml.append(moXml.substring(0, end));
				newXml.append('>');
				newXml.append(xml);
				newXml.append("</prolog>");
				newXml.append(moXml.substring(end+2));

			} else {
				int n = moXml.indexOf("<"+CLASSIFICATION_GI);
				if (n < 0 || n > end) {
					// No classification element, or at least not in prolog.
					// Content model allows classification element at end
					// of prolog child list, so stick before prolog etag.
					newXml.append(moXml.substring(0, end));
					newXml.append(xml);
					newXml.append(moXml.substring(end));
				} else {
					int m = moXml.indexOf(CLASSIFICATION_ETAG, n);
					int skip = CLASSIFICATION_ETAG.length();
					if (m < 0) {
						m = moXml.indexOf("/>", n);
						skip = 2;
					}
					newXml.append(moXml.substring(0,n));
					newXml.append(xml);
					newXml.append(moXml.substring(m+skip));
				}
			}
		}
		try {
			log.info("Updating MO ("+moid+") with new classification...");
			//log.info("New XML: "+newXml);
			mos.update(user, moid, new XmlObjectSource(
					newXml.toString()), null);
		} catch (Exception rse) {
			if (doCheckIn) {
				log.error("Error updating MO ("+moid+"): "+rse+
						" [attempting to rollback...]");
				try {
					mos.rollback(user, moid);
				} catch (Exception rse2) {
					log.error(
						"Unable to rollback MO do to earlier error: "+rse2,
						rse2);
				}
			}
			if (rse instanceof RSuiteException) {
				throw (RSuiteException)rse;
			}
			throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR,
					rse.getLocalizedMessage(), rse);
		}
		
		if (doCheckIn) {
			log.info("Checking in MO ("+moid+") under user "+
					user.getUserId());
			mos.checkIn(user, moid, VersionType.MINOR,
					"Classification change", true);
		}
	}
}

package com.reallysi.rsuite.webservice;

import static org.apache.commons.lang3.StringUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.content.ContentDisplayObject;
import com.reallysi.rsuite.api.content.ContentObjectPath;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.DefaultRemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.utils.DitaMapUtils;
import com.reallysi.tools.ditaot.DitaOTUtils;
import com.reallysi.tools.ditaot.DitaOtAntTaskCommand;
import com.reallysi.tools.ditaot.DitaOtOptions;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Takes a publication content assembly and execute a ditaot plugin process.
 */
public class RunDitaOTWebService extends DefaultRemoteApiHandler {

	private static Log log = LogFactory.getLog(RunDitaOTWebService.class);

	@Override
	public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args) throws RSuiteException {
		log.info("execute(): Starting...");

		String transtype = args.getFirstValue( "transtype");
		String zipOutput = args.getFirstValue(DitaOtOptions.ZIP_OUTPUT_PARAM);
		String outputType = (transtype == null ? " transformation " : transtype.toUpperCase());
		String outputPath = args.getFirstValue(DitaOtOptions.OUTPUT_PATH_PARAM);
		String outputExtension = defaultString(args.getFirstValue(DitaOtOptions.OUTPUT_EXTENSION));
		String attachingFolderName = defaultString(args.getFirstValue(DitaOtOptions.ATTACHING_FOLDER_NAME));
		String alias = args.getFirstValue(DitaOtOptions.ALIAS_PARAM);
		String commitMsg = args.getFirstValue(DitaOtOptions.COMMIT_MSG_PARAM);
		
		if (outputPath == null || outputPath.isEmpty()) {
			File tempDir = null;
			try {
				tempDir = context.getRSuiteServerConfiguration().getTmpDir();
			} catch (Exception e) {
				log.error("Failed to get a temporary directory: " + e.getMessage());
				throw new RSuiteException("Failed to get a temporary directory: " + e.getMessage());
			} 
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
			//Include plain transtype at end so zipped uploads will have a useful name
			outputPath = tempDir.getAbsolutePath() + File.separator + transtype + "-" + sdf.format(new Date()) + File.separator + transtype;
		}
		
		if ( StringUtils.isEmpty(transtype)) {
			return new MessageDialogResult( MessageType.ERROR, "DITA OT", "This output configuration is incomplete. Please see your administrator. " + 
					"<ul><li>The arguments 'transtype' and 'outputPath' are required.</li></ul>");
		}

		User user = context.getAuthorizationService().findUser(context.getPrincipal().getName());
		Session session = context.getSession();
		List<ManagedObject> moList = args.getManagedObjects(user);
		ManagedObject mo = moList.get(0);
		mo = RSuiteUtils.getRealMo(context, user, mo);
		String targetId = mo.getTargetId();
		if (targetId != null) {
			mo = context.getManagedObjectService().getManagedObject(user, targetId);
		}
		// FIXME: Handle multiple input MOs.

		String xsltUrlString = args.getFirstValue(DitaOtOptions.XSLT_URI_PARAM, DitaOtOptions.DEFAULT_CA_TO_MAP_TRANSFORM_URL);
		String caNodeMoId = args.getFirstValue(DitaOtOptions.CA_NODE_ID_PARAM);
        String forceNewMapStr = args.getFirstValue(DitaOtOptions.FORCE_NEW_MAP_PARAM, "false");
        log.info("forceNewMapStr="+forceNewMapStr);

    ManagedObject folderMo = null;
		ManagedObject mapMo = null;
        if (mo.isAssemblyNode()) {
        	try {
	            mapMo = DitaMapUtils.getMapMoForContainer(
	                    context,
	                    log,
	                    moList,
	                    xsltUrlString,
	                    caNodeMoId,
	                    forceNewMapStr,
	                    context.getSession().getKey());
        	} catch (Exception e) {
        		throw new BusinessRuleException(e.getMessage());
        	}
    		if (mapMo == null) {
    			return new MessageDialogResult( MessageType.ERROR, "DITA OT", "There is no DITA map in the selected folder. A map is required.");
    		}
			folderMo = mo;
			mo = mapMo;
        } else {
			List<ContentObjectPath> contentObjectPaths = args.getContentObjectPaths(user);
			ContentObjectPath firstPath = contentObjectPaths.get(0);
			ContentDisplayObject parentObject = null;
			if (firstPath.getSize() > 1) {
				parentObject = firstPath.getPathObjects().get(firstPath.getSize() - 2);
				folderMo = parentObject.getManagedObject();
				targetId = folderMo.getTargetId();
				if (targetId != null) {
					folderMo = context.getManagedObjectService().getManagedObject(user, targetId);
				}
			} else {
				// No parent
			}
        }
		
		log.info("execute(): Effective MO: " + RSuiteUtils.formatMoId(mo));
		if (folderMo != null)
			log.info("execute(): Folder context: " + RSuiteUtils.formatMoId(folderMo));

		/**
		 * Stream to hold report output
		 */
		StringWriter stringWriter = new StringWriter();

		PrintWriter writer = new PrintWriter(stringWriter);
		writer.write("<h1>" + "Transformation Report for " + RSuiteUtils.formatMoId(mo) + "</h1>");
		writer.write("<div");
		writer.flush(); // Make sure everything is written to the underlying stream.

		DitaOtOptions ditaotOptions = new DitaOtOptions(context, user, transtype);
		ditaotOptions.setSession(session);
		try {
			ditaotOptions.setMessageWriter(writer);
			ditaotOptions.setLog(log);

			File outdir = new File(outputPath);
			outdir.mkdirs();
			ditaotOptions.setOutputDir(outdir);

			for (CallArgument arg : args.getAll()) {
				// args.copycss, args.css, etc.
				if (arg.getName().startsWith("ditaot.")) {
					String propName = arg.getName().replaceFirst("ditaot.", "");
					String propValue = arg.getValue();
					if (StringUtils.isNotEmpty(propValue)) {
						ditaotOptions.setProperty(propName, propValue);
					}
				}
			}

			DitaOtAntTaskCommand ditaot = new DitaOtAntTaskCommand();

			List<ManagedObject> moForOtList = new ArrayList<ManagedObject>();
			moForOtList.add(mo);
			log.info("execute(): Generating " + outputType + " from MO...");
			ditaot.execute(moForOtList, ditaotOptions);
			log.info("execute(): " + outputPath  + " generated");
			
			if (folderMo != null) {
				
				String targetMoId = folderMo.getId();
				
				if (StringUtils.isNotEmpty(attachingFolderName)) {
					ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
					options.setType("folder");
					options.setSilentIfExists(true);
					targetMoId = context.getContentAssemblyService()
							.createContentAssembly(user, folderMo.getId(), attachingFolderName, options).getId();
				}
				
    			if (StringUtils.equals(zipOutput, "true")) {
    				File zipFile = DitaOTUtils.zipDirectoryToWO(ditaotOptions.getOutputDir(), mo.getId(), ditaotOptions.getTempDir().getAbsolutePath(), log);
    				if (zipFile != null) {
    					loadOrUpdateObject(zipFile, context, targetMoId, alias, commitMsg);
    				}
    			} else {
    				File[] resultFiles = DitaOTUtils.getTransformedFiles(ditaotOptions.getOutputDir(), outputExtension);
    				for (File file : resultFiles) {
    					loadOrUpdateObject(file, context, targetMoId, alias, commitMsg);
    				} 
    			}
			}
			
			StoredReport report = ditaotOptions.getStoredReport();
			ditaotOptions.addInfoMessage(	"ditaot", mo.getDisplayName(),
																		"<a target='log' href='/rsuite/rest/v1/report/generated/" + report.getId()
																						+ "?skey='" + session.getKey() + ">Process Details</a>");
		} catch (RSuiteException e) {
			ditaotOptions.addFailureMessage("RSuiteException", mo.getDisplayName(),
																			"Unexpected RSuite exception: " + e.getMessage(), e);
		} catch (Throwable t) {
			log.error("execute(): Unexpected " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
			ditaotOptions.addFailureMessage("RSuiteException", mo.getDisplayName(),
																			t.getClass().getSimpleName() + ": " + t.getMessage());
		}
		ProcessMessageContainer messages = ditaotOptions.getMessages();

		writer.write("</div>");
		if (messages.hasFailures()) {
			writer.write("<p");
			writer.write(" style=\"color: red;\"");
			writer.write(">");
			writer.write("Failures during conversion.");
			writer.write("</p>\n");
		}

		writer.write("<h2>" + "Messages" + "</h2>");

		messages.reportMessagesByObjectAsHtml(context.getSession().getKey(), writer);

		writer.flush();
		writer.close();

		MessageType messageType = MessageType.SUCCESS;
		if (messages.hasFailures()) {
			messageType = MessageType.ERROR;
		}

		log.info("execute(): Returning MessageDialogResult");
		MessageDialogResult result = new MessageDialogResult(messageType, "DITA OT", stringWriter.toString());

		if (folderMo != null) {
			Map<String, Object> props = new HashMap<String, Object>();
			props.put("objects", folderMo.getId());
			UserInterfaceAction refreshAction = new UserInterfaceAction("rsuite:refreshManagedObjects");
			refreshAction.setProperties(props);
			result.addAction(refreshAction);
		}

		return result;
	}
	
	/**
	 * Method to load or update on RSuite the ZIP file created.
	 * 
	 * @param outputFile The output file transformed.
	 * @param context The context.
	 * @param parentMo The MO parent.
	 * @param aliasParam The alias if any that comes as a parameter.
	 * @param commitMsgParam The commit message if any that comes as a parameter.
	 * @throws IOException
	 * @throws RSuiteException
	 */
	private void loadOrUpdateObject(File outputFile, RemoteApiExecutionContext context, String parentId,
	                                String aliasParam, String commitMsgParam) throws IOException, RSuiteException {
		ObjectSource src = new NonXmlObjectSource(outputFile);
		ManagedObjectService moService = context.getManagedObjectService();
		
		String alias = (aliasParam == null || StringUtils.isEmpty(aliasParam)) ? outputFile.getName() : aliasParam;
		
		User systemUser = context.getAuthorizationService().getSystemUser();
		List<ManagedObject> managedObjects = getManagedObjectsByAlias(moService, systemUser, alias);
		
		ManagedObject managedObject = null;
		if (managedObjects.size() == 0) {
			managedObject = insertNewManagedObject(alias, outputFile, moService, systemUser, src);
		} else if (managedObjects.size() > 1) {
			throw new RSuiteException(0, "Found " + managedObjects.size() + " with alias \"" + alias + ". "
			                          + "There should be at most one such MO.");
		} else {
			managedObject = updateManagedObject(managedObjects, moService, systemUser, outputFile, src, commitMsgParam);
		}
		
		log.info("Parent MO ID specified, trying to find and attach...");
		attachMoToParent(context, systemUser, parentId, managedObject);
	}
	
	/**
	 * Method that finds if there are any managed objects given an alias.
	 * 
	 * @param moService The Managed Object Service.
	 * @param user The user.
	 * @param alias The alias to search for.
	 * @return The list of managed objects that match the alias.
	 */
	private List<ManagedObject> getManagedObjectsByAlias(ManagedObjectService moService, User user, String alias) {
		try {
			return moService.getObjectsByAlias(user, alias);
		} catch (RSuiteException e) {
			log.info(e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Method to insert a new Managed Object that it hasn't been inserted before into RSuite.
	 * 
	 * @param alias The alias of the object.
	 * @param outputFile The output file.
	 * @param moService The managed object service.
	 * @param systemUser The user.
	 * @param src The object source.
	 * @throws RSuiteException
	 */
	private ManagedObject insertNewManagedObject(String alias, File outputFile, ManagedObjectService moService,
	                                    User systemUser, ObjectSource src) throws RSuiteException {
		log.info("No existing MO with alias \"" + alias + "\", creating new MO...");
		String[] aliases = { alias };
		
		ObjectInsertOptions insertOptions = new ObjectInsertOptions(alias, aliases, null, true);
		insertOptions.setDisplayName(outputFile.getName());
		ManagedObject newMo = moService.load(systemUser, src, insertOptions);
		log.info("Created new MO \"" + newMo.getDisplayName() + "[" + newMo.getId() + "]");
		return newMo;
	}
	
	/**
	 * Method that will update an already existing Managed Object in RSuite.
	 * 
	 * @param managedObjects The list of managed objects found.
	 * @param moService The Managed Service object.
	 * @param user The user.
	 * @param inFile The file to be loaded.
	 * @param src The Object Source.
	 * @param commitMsg The commit message if any.
	 * @throws RSuiteException
	 */
	private ManagedObject updateManagedObject(List<ManagedObject> managedObjects, ManagedObjectService moService, User user,
	                                 File inFile, ObjectSource src, String commitMsg) throws RSuiteException {
		ManagedObject mo = managedObjects.get(0);
		log.info("Found existing MO \"" + mo.getDisplayName() + "[" + mo.getId() + "], updating with XML from incoming file...");
		
		moService.checkOut(user, mo.getId());
		ObjectUpdateOptions options = ObjectUpdateOptions.constructOptionsForNonXml(inFile.getName(),
		                                                                            FilenameUtils.getExtension(inFile.getName()));
		options.setValidate(true);
		ManagedObject managedObject = null;
		try {
			managedObject = moService.update(user, mo.getId(), src, options);
		} catch (Exception e) {
			String msg = "Unexpected failure on update of mo [" + mo.getId() + "]: " + e.getMessage();
			log.info(msg);
			throw new RSuiteException(msg);
		} finally {
			moService.checkIn(user, managedObject.getId(), VersionType.MINOR, commitMsg, true);
		}
		log.info("MO updated, new display name is \"" + managedObject.getDisplayName() + "\"");
		return managedObject;
	}

	/**
	 * Method that will attach a managed object to its parent.
	 * 
	 * @param context The context.
	 * @param user The user.
	 * @param parentMoId The id of the parent object.
	 * @param managedObject The managed object to be attached.
	 * @throws RSuiteException
	 */
	private void attachMoToParent(RemoteApiExecutionContext context, User user, String parentMoId,
	                              ManagedObject managedObject) throws RSuiteException {
		try {
			ContentAssemblyService caService = context.getContentAssemblyService();
			// Attach new MO to parent if not already there:
			ContentAssemblyNodeContainer caNode = caService.getContentAssemblyNodeContainer(user, parentMoId);
			log.info("Found specified parent CA Node " + caNode.getDisplayName() + " [" + caNode.getId() + "]");
			
			if (!caNode.hasChild(managedObject)) {
				log.info("CA Node does not already have new MO, attaching it...");
				ObjectAttachOptions options = new ObjectAttachOptions();
				List<ManagedObject> moList = new ArrayList<ManagedObject>();
				moList.add(managedObject);
				caService.attach(user, caNode.getId(), moList, options);
				log.info("New MO attached to CA node.");
			} else {
				log.info("New MO is already attached to the specified parent CA node.");
			}
		} catch (Exception e) {
			throw new RSuiteException("Exception attempting to attach new/updated MO to browse tree: " + e.getMessage());
		}
	}
}

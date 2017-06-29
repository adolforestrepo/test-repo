package com.reallysi.tools.ditaot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ConfigurationProperties;
import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.rsuite.utils.DitaMapUtils;
import com.reallysi.tools.dita.BosVisitor;
import com.reallysi.tools.dita.BoundedObjectSet;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.ExportingBosVisitor;
import com.reallysi.tools.dita.PluginVersionConstants;
import com.reallysi.tools.dita.RSuiteDitaHelper;
import com.reallysi.tools.dita.TreeGeneratingOutputFilenameSettingBosVisitor;
import com.reallysi.tools.dita.export.CaToDirTreeOutputFilenameSettingBosVisitor;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Manages generic setup and execution of DITA Open Toolkit Ant tasks.
 * <p>
 * Options classes are responsible for setting all the Ant properties
 * needed for a given build target. The Ant log is echoed to the
 * log and report.
 * </p>
 */
public class DitaOtAntTaskCommand {

	private DitaOtOptions ditaOptions = null;
	
	public static final String DATE_FORMAT_STRING = "yyyyMMdd-HHmmss"; // Constructed to work in filenames
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);
	/**
	 * Applies an Open Toolkit transform to the managed objects 
	 * @param options dita open toolkit options.
	 * @throws Exception 
	 */
	public void execute( List<ManagedObject> moList, DitaOtOptions options) throws Exception { 
		ditaOptions = options;
		
		ExecutionContext context = ditaOptions.getExecutionContext();
		User user = ditaOptions.getUser();
		DitaOpenToolkit toolkit = ditaOptions.getToolkit();
		String transtype = ditaOptions.getTranstype();
		List<String> additionalJars = ditaOptions.getAdditionalJars();
		boolean cleanExportDir = ditaOptions.getCleanExportDir();
		
		Log log = ditaOptions.getLog();
		
		File buildFile = null;
		try {
			buildFile = toolkit.getBuildFile();
		} catch (Exception e) {
			String msg = "Failed to get build file for Open Toolkit \"" + toolkit.getName() + "\". Make sure the Toolkit configuration is correct.";
			throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, msg, e);
		}
				
		log.info("Got a build file for the Toolkit");
				
		// The MO list should be a list of DITA map or topic MOs to be processed, normally
		// a single map or topic.
				
		for ( ManagedObject mo : moList) {
			Element rootElem = mo.getElement();
			if (mo.isAssemblyNode()) {
				// Assembly nodes are handled by the exporter.
			} else {
				if (!DitaUtil.isDitaMap(rootElem) && !DitaUtil.isDitaTopic(rootElem)) {
					log.warn("Managed object \"" + mo.getId() + "\" is not a DITA map or topic, found \"" + rootElem.getNodeName() + "\". Skipping.");
					continue;
				}
			}

			ditaOptions.addInfoMessage( "ditaot", mo.getDisplayName(), "Exporting Map '"+mo.getDisplayName()+"'");
			
			File exportDir = getExportDir( context, mo);
			if ( cleanExportDir) {
				try {
					cleanDir( exportDir);
				} catch (IOException e) {
					throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, 
			                   "Exception cleaning export directory: "+
			                   e.getLocalizedMessage(), e);
				}
			}
			File exportedMapFile = 
					exportMap( context, 
							   user, 
							   log, 
							   mo, 
							   exportDir);
			
			File outputDir = ditaOptions.getOutputDir();
			try {
				cleanDir( outputDir);
			} catch (IOException e) {
				throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, 
						                   "Exception cleaning output directory: "+
		                                   e.getLocalizedMessage(), e);
			}
			String reportFileName = "dita2" + transtype + "_mo_" + mo.getId() + "_at_" + getNowString() + ".txt";
			ditaOptions.setParameter( DitaOtOptions.REPORT_FILENAME, reportFileName);
			
			String resolvedOutputPath = outputDir.getAbsolutePath();
			log.info("Toolkit output dir=\"" + resolvedOutputPath + "\"");
				
			String mapFilePath = exportedMapFile.getAbsolutePath();
						
			log.info("Processing map file \"" + mapFilePath + "\"");
			ditaOptions.addInfoMessage( "ditaot", mo.getDisplayName(), "Processing map file \"" + mapFilePath + "\"");
				
			ditaOptions.setProperty("args.input", mapFilePath); // The map or topic document to process
			ditaOptions.setProperty("output.dir", resolvedOutputPath);
			ditaOptions.setProperty("basedir.dir", exportedMapFile.getParent()); // Base directory containing the map to be processed.
			
			executeAnt();

			log.info("");
			log.info("Generated output is in \"" + resolvedOutputPath + "\"");
			ditaOptions.addInfoMessage( "ditaot", mo.getDisplayName(), "Generated output is in \"" + resolvedOutputPath + "\"");
		}
	}

	/**
	 * Constructs the Ant command line and executes it, capturing the log output into the appropriate
	 * log and to a stored report.
	 * @param toolkit 
	 * @param buildFile
	 * @param props
	 * @param additionalJars List of additional jar files to be used for the Ant command. These are 
	 * put before the base jars so that you can override the default jars if necessary.
	 * Each item in the list is either an absolute path to a jar or a path relative to the Toolkit's
	 * lib/ directory.
	 * @param reportId
	 * @throws IOException 
	 */
	public void executeAnt() throws RSuiteException { 
		DitaOpenToolkit toolkit = ditaOptions.getToolkit();
		Log log = ditaOptions.getLog();
		//File buildFile;
		Properties props = ditaOptions.getProperties();
		List<String> additionalJars = ditaOptions.getAdditionalJars(); 
		String reportFileName = ditaOptions.getParameter( DitaOtOptions.REPORT_FILENAME);
		
		log.info( "reportFileName="+reportFileName);
		File toolkitDir = toolkit.getDir();
		File toolkitLibDir = new File(toolkitDir, "lib");
		File toolkitSaxonLibDir = new File(toolkitLibDir, "saxon");
		
		// FIXME: Parameterize the location of the Ant executable.
		
		File toolsDir = new File(toolkitDir, "tools");
		File antDir = new File(toolsDir, "ant");
		File antLibDir = new File(antDir, "lib");
		
		// Build up ant command in an array to avoid any shell interpretation of arguments
		ArrayList<String> execCmd = new ArrayList<String>(32);

		String javaHome = System.getProperty("java.home");
		log.info("java.home=" + javaHome);
		File javaHomeDir = new File(javaHome);
		String javaCmd = new File(new File(javaHomeDir, "bin"), "java").getAbsolutePath();
		String antHome = antDir.getAbsolutePath();
		String classPath = new File(antLibDir, "ant-launcher.jar").getAbsolutePath();
		// String antClass = " org.apache.tools.ant.launch.Launcher";
		log.info("classPath: " + classPath);

		execCmd.add(javaCmd);
		execCmd.add("-Dant.home="+antHome);

		// If the maxJavaMemory parameter has been specified, use it for 
		// the JVM that runs the Ant task as well.
		String maxMemory = props.getProperty( DitaOtOptions.MAX_JAVA_MEMORY_PARAM);
		if (maxMemory !=  null) {
			execCmd.add("-Xmx"+ maxMemory);
		}

		execCmd.add("-jar");
		execCmd.add(classPath);
		// execCmd.add(antClass);  // Using -jar method to invoke, no need to specify class

		// execCmd.add("-logfile");
		// execCmd.add("c:/tmp/dita-ot-ant.log");
		
		log.info("Ant properties:");
		log.info("");
		for (Object key : props.keySet()) {
			String propName = (String)key;
			String value = props.getProperty(propName);
			log.info("\n" + propName + "=" + value);
			execCmd.add("-D"+propName+"="+value);
		}
			
		// Add any additional jars to the command:
		// These come first so they can override the base
		// jars.
		for (String jarItem : additionalJars) {
			File file = new File(jarItem);
			if (!file.isAbsolute()) {
				file = new File(toolkitLibDir, jarItem);
			}
			execCmd.add("-lib");
			execCmd.add(file.getAbsolutePath());
			
		}

		// Construct base Open Toolkit library set. 
		// This set is accurate through version 1.6.3
		execCmd.add("-lib");
		execCmd.add(toolkitDir.getAbsolutePath());
		// NOTE: The lib/ dir appears to be required so that catalog-based
		// resolution of transform components will work.
		execCmd.add("-lib");
		execCmd.add(toolkitLibDir.getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitLibDir, "dost.jar").getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitLibDir, "commons-codec-1.4.jar").getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitLibDir, "icu4j.jar").getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitLibDir, "resolver.jar").getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitLibDir, "xercesImpl.jar").getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitSaxonLibDir, "saxon9.jar").getAbsolutePath());
		execCmd.add("-lib");
		execCmd.add(new File(toolkitSaxonLibDir, "saxon9-dom.jar").getAbsolutePath());
		
		ProcessBuilder pb = new ProcessBuilder(execCmd);
		pb.directory(toolkitDir);
		pb.redirectErrorStream(true);
		Process proc = null;
		try {
			log.info("Ant exec command:" + execCmd);
			log.info("Running Ant process...");
			proc = pb.start();
			StoredReport report = captureProcessStream( proc.getInputStream());
			ditaOptions.setStoredReport(report);

		} catch (Exception e) {
			throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, 
					                   "Exception running Ant: "+
					                   e.getLocalizedMessage(), e);
		} finally {
			if (proc != null) {
				try {
					proc.getInputStream().close();
				} catch (IOException e) {
					// ignore
				}
				try {
					proc.getOutputStream().close();
				} catch (IOException e) {
					// ignore
				}
				try {
					proc.getErrorStream().close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * @param context 
	 * @param reportFileName
	 * @param inputStream
	 * @param inputStream2 
	 * @throws IOException 
	 * @throws RSuiteException 
	 */
	private StoredReport captureProcessStream( InputStream in) throws IOException, RSuiteException {
		DitaOpenToolkit toolkit = ditaOptions.getToolkit();
		Log log = ditaOptions.getLog();
		ExecutionContext context = ditaOptions.getExecutionContext();
		String reportFileName = ditaOptions.getParameter( DitaOtOptions.REPORT_FILENAME);
		
		log.info("Capturing Ant output...");
		StringBuilder reportStr = new StringBuilder("DITA OT Ant report " + reportFileName + "\n\n");
		reportStr.append("Using RSuite-integrated Open Toolkit named ")
		.append(toolkit.getName())
		.append(", installed at \"")
		.append(toolkit.getPath())
		.append("\n\n");

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = reader.readLine();
		while (line != null) {
			log.info(line);
			reportStr.append(line).append("\n");
			line = reader.readLine();
		}
		reportStr.append("\n");
		log.info("");
	
		log.info("Saving ant output to stored report...");
		StoredReport report = context.getReportManager().saveReport(reportFileName, reportStr.toString());
		return report;
	}

	public void copyStreamToLog( InputStream inStream, Log log)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream)); 
		String line = reader.readLine();
		while (line != null) {
			log.info(line);
			line = reader.readLine();
		}
	}

	protected File getExportDir( ExecutionContext context, ManagedObject mo)
			throws RSuiteException {
		ConfigurationProperties configProps = context.getRSuiteServerConfiguration().getConfigurationProperties();
		String tempPath = configProps.getProperty( "rsuite.ditaXmlExportDir", context.getRSuiteServerConfiguration().getTmpDir().getAbsolutePath());
		File exportDir = new File( tempPath); 
		// Create a map-specific directory in the specified output directory:
		String mapName = "map_" + mo.getId();
		// If there's an alias, assume it's the map's file name:
		if (mo.getAliases().length > 0) {
			// FIXME: This bit is to handle 3.3.1/3.3.2 API change
			// for mo.getAliases() from String[] to Alias[];
			Alias alias = mo.getAliases()[0];
			String aliasStr = alias.getText();
			mapName = FilenameUtils.getBaseName(aliasStr);
		}
			
		exportDir = new File( exportDir, mapName);
			
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		if (!exportDir.exists()) {
			throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, "Failed to find or create output directory \"" + exportDir.getAbsolutePath() + "\"");
		}
		if (!exportDir.canWrite()) {
			throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, "Cannot write to output directory \"" + exportDir.getAbsolutePath() + "\"");
		}

		return exportDir;
	}

	/**
	 * @param context 
	 * @throws IOException 
	 * 
	 */
	protected void cleanDir( File dir) throws IOException {
		FileUtils.deleteDirectory( dir);
		dir.mkdirs();
	}

	protected static String getNowString() {
		String timeStr = DATE_FORMAT.format(new Date());
		return timeStr;
	}
	
	// Map Exporter methods
	
	protected File exportMap(
			ExecutionContext context,
			User user,
			Log log, 
			ManagedObject mo, 
			File exportDir) throws Exception {
		log.info("exportMap(): Using rsuite-dita-support version " + PluginVersionConstants.getVersion() + ".");
		log.info("MO will be exported to: \"" + exportDir.getAbsolutePath() + "\"");
		
		// If the mo is an assembly node, treat it as the container of the map to export
		// and use the CA-based file setting visitor, otherwise treat the MO as a map
		// to be exported.
		
		File exportedMapFile = null;
		if ( mo.isAssemblyNode()) {
			log.info( "Managed object is a container.");
			ContentAssembly container = context.getContentAssemblyService().getContentAssembly(user, mo.getId());
			if (container == null) {
				throw new RSuiteException(0, "Workflow object [" + mo.getId() + "] \"" + mo.getDisplayName() + "\" is not a container.");
			}
			exportedMapFile = exportMapFromContainer( context, user, log, container, exportDir);
		} else {
			log.info("Managed objectisn't a folder; assume is a map or topic, attempting export...");

			Properties filenameSettingOptions = new Properties();
			filenameSettingOptions.setProperty(
					TreeGeneratingOutputFilenameSettingBosVisitor.ROOT_DIR_PATH_OPTION, 
					exportDir.getAbsolutePath());
			filenameSettingOptions.setProperty(
					TreeGeneratingOutputFilenameSettingBosVisitor.MAPS_TO_NEW_DIRS_OPTION, 
					"true");
			filenameSettingOptions.setProperty(
					TreeGeneratingOutputFilenameSettingBosVisitor.GRAPHICS_DIR_NAME_OPTION, 
					"images");
			// Would be useful to have topic type-to-directory-name map that
			// specifies what directory name each kind of thing goes into.
			BosVisitor outputFilenameSettingBosVisitor = 
					new TreeGeneratingOutputFilenameSettingBosVisitor(
							filenameSettingOptions, 
							log);

			exportedMapFile = exportMapOrTopic(context, log, exportDir.getAbsolutePath(), mo, outputFilenameSettingBosVisitor);
			
		}
		
		return exportedMapFile;
	}

	protected File exportMapFromContainer(
			ExecutionContext context, 
			User user,
			Log log, 
			ContentAssemblyNodeContainer container, 
			File exportDir) throws Exception {
		String outputPath = exportDir.getAbsolutePath();
		File exportedMapFile = null;

		List<ManagedObject> moList = new ArrayList<ManagedObject>();
		moList.add(context.getManagedObjectService().getManagedObject(user, container.getId()));
		
		ManagedObject mapMo = DitaMapUtils.getMapMoForContainer(context, log, moList, 
				ditaOptions.getProperties().getProperty(DitaOtOptions.XSLT_URI_PARAM), 
				ditaOptions.getProperties().getProperty(DitaOtOptions.CA_NODE_ID_PARAM), 
				ditaOptions.getProperties().getProperty(DitaOtOptions.FORCE_NEW_MAP_PARAM), 
				getSession(context));

		if (mapMo == null) {
			log.warn("No DITA map managed objects found in container [" + container.getId() + "] \"" + container.getDisplayName() + "\". Nothing to do.");
		} else {
			log.info("Found reference to a DITA map in the container. Attempting export...");
			log.info("Exporting managed object [" + mapMo.getId() + "] \"" + mapMo.getDisplayName() + "\"...");
			ManagedObjectReference mapMoRef = null;
			for (ContentAssemblyItem caItem : container.getChildrenObjects()) {
				ManagedObject candMo = null;
				if (caItem.getObjectType().equals(ObjectType.MANAGED_OBJECT_REF)) {
					candMo = context.getManagedObjectService().getManagedObject(user, ((ManagedObjectReference)caItem).getTargetId());
					if (candMo != null) {
						Element root = candMo.getElement();
						if ( DitaUtil.isDitaMap(root) && RSuiteUtils.getRealMo(context, user, candMo).getId().equals(mapMo.getId())) {
							mapMoRef= (ManagedObjectReference)caItem;
						}
					}
				}
			}
			Properties options = new Properties();
			options.setProperty(TreeGeneratingOutputFilenameSettingBosVisitor.ROOT_DIR_PATH_OPTION, outputPath);
			BosVisitor filenameSettingBosVisitor = 
					new CaToDirTreeOutputFilenameSettingBosVisitor(
							context,
							user,
							container,
							mapMoRef, 
							options, 
							log);
			exportedMapFile = exportMapOrTopic( context, 
								                log, 
								                outputPath, 
								                mapMo, 
								                filenameSettingBosVisitor);
		}
		return exportedMapFile;
		
	}

	private String getSession(ExecutionContext context) throws RSuiteException {
		UserAgent userAgent = new UserAgent("rsuite-workflow-process");
		// Create a session so the transform can use the Web service via the
		// external HTTP access. This is a workaround for the fact that as of
		// RSuite 3.6.3 there is no internal URL for accessing plugin-provided
		// Web services.
		Session session = context.getSessionService().
				createSession("Realm", userAgent,
				              "http://" + context.getRSuiteServerConfiguration().getHostName() +
				              ":" + context.getRSuiteServerConfiguration().getPort() +
				              "/rsuite/rest/v1", context.getAuthorizationService().getSystemUser());
		String skey = session.getKey();
		return skey;
	}

	protected static File exportMapOrTopic( ExecutionContext context, Log log, 
			                                String outputPath, 
			                                ManagedObject mo,
			                                BosVisitor outputFilenameSettingBosVisitor) throws RSuiteException {
		
		log.info("Exporting DITA map [" + mo.getId() + "] " + mo.getDisplayName() + "...");
		
		log.info("Calculating the map's bounded object set of dependencies...");
		BoundedObjectSet bos = RSuiteDitaHelper.calculateMapBos(context, log, mo);
		bos.reportBos(log);
		
		// Process the BOS members to set their paths as exported
		
		log.info("Setting export paths for BOS members... ");
		bos.accept(outputFilenameSettingBosVisitor);
		
		log.info("*** After associating file names to BOS:");
		bos.reportBos(log);

		log.info("Exporting map BOS to the file system...");
		
		context.getManagedObjectService();
		
		BosVisitor exportingBosVisitor = new ExportingBosVisitor(context, log);
		bos.accept(exportingBosVisitor);		
		
		File exportedFile = bos.getRoot().getFile();
		if (exportedFile == null) {
			throw new RSuiteException(0, "Null file from root member following export. This should not happen.");
		}

		log.info("Map exported");

		return exportedFile;
	}
	
}
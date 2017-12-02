/**
 * 
 */
package com.reallysi.tools.ditaot;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.ExecutionContextAware;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.messages.impl.ProcessMessageContainerImpl;

/**
 * Class for ditaot options
 */
public class DitaOtOptions extends ProcessMessageContainerImpl implements ExecutionContextAware {
	/**
	 * If set to "true" or "yes", forces recreation of maps for
	 * content assemblies even if there is an existing map. Default
	 * is false (don't force).
	 */
	public static final String FORCE_NEW_MAP_PARAM = "forceNewMap";

	/**
	 * URI of the XSLT transform to apply to the CA. If not specified, the default
	 * transform is used. 
	 */
	public static final String XSLT_URI_PARAM = "xsltUri";
	
	/**
	 * MO ID of the CA node to be transformed into a map.
	 */
	public static final String CA_NODE_ID_PARAM = "caNodeId";
	
	
	public static final String DEFAULT_CA_TO_MAP_TRANSFORM_URL = "rsuite:/res/plugin/astd-plugin/xslt/canode2map/canode2map_shell.xsl"; 
	
	/**
	 * Controls debugging messages in the transform. Set to "true" turn debugging
	 * messages on.
	 */
	public static final String DEBUG_PARAM = "debug";

	public static final String MAX_JAVA_MEMORY_PARAM = "maxJavaMemory";
	public static final String DITAVAL_FILEPATH_PARAM = "ditavalFilepath";
	public static final String CLEAN_TEMP_PARAM = "cleanTemp";
	public static final String XSL_PARAM = "xsl";
	public static final String DRAFT_PARAM = "draft";
	public static final String ONLY_TOPIC_IN_MAP_PARAM = "onlyTopicInMap";
	public static final String REPORT_FILENAME = "reportFilename";
	/**
	 * Pipe ("|")-delimited list of name/value pairs, where each pair is
	 * a property name, a equals sign, and the property value:
	 * <p><code>prop1=value1|prop2=value2</code></p>
	 */
	public static final String BUILD_PROPERTIES_PARAM = "buildProperties";
	/**
	 * The Ant target to run. If unspecified, default target configured
	 * for the project is used.
	 */
	public static final String BUILD_TARGET_PARAM = "buildTarget";
	/**
	 * Absolute path of the Ant build file to process. Required.
	 */
	public static final String BUILD_FILE_PATH_PARAM = "buildFilePath";
	/**
	 * The name of the configured Open Toolkit to use. Default is "default".
	 * Open Toolkits are configured in the DITA Open Toolkit properties file
	 * in the RSuite conf/ directory.
	 * <p>
	 * The maps to be processed are listed in the variable "exportedMapFiles", 
	 * which is a list of WorkflowFileObjects, one for each map.
	 * </p>
	 */
	public static final String OPEN_TOOLKIT_NAME_PARAM = "openToolkitName";

	static final String MAP_EXPORT_PATH_PARAM = "exportPath";
	
	/**
	 * The path to which the generated output is put.
	 */
	public static final String OUTPUT_PATH_PARAM = "outputPath";
	
	/**
	 * The output file extension.
	 */
	public static final String OUTPUT_EXTENSION = "outputExtension";
	
	/**
	 * The RSuite folder name to attach contents.
	 */
	public static final String ATTACHING_FOLDER_NAME = "attachingFolderName";
	
	public static final String EXCEPTION_OCCUR = "EXCEPTION_OCCUR";
	
	static final String DATE_FORMAT_STRING = "yyyyMMdd-HHmmss"; // Constructed to work in filenames
	static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);
	
	/**
	 * Attribute to denote if the output will be zipped or not.
	 */
	public static String ZIP_OUTPUT_PARAM = "zipOutput";
	
	/**
	 * Attribute to denote an EPUB transformation.
	 */
	public static String EPUB_TRANSFORMATION = "epub";
	
	/**
	 * Attribute to denote an PDF transformation.
	 */
	public static String PDF_TRANSFORMATION = "pdf";
	
	/**
	 * Attribute to denote the default transformation.
	 */
	public static final String DEFAULT_TRANSTYPE = "xhtml";
	
	/**
	 * Attribute to denote if there is a preferred alias when executing
	 * the transformation.
	 */
	public static final String ALIAS_PARAM = "alias";
	
	/**
	 * Attribute to denote if there is a preferred commit message for the
	 * transformation.
	 */
	public static final String COMMIT_MSG_PARAM = "commitMessage";

	private ExecutionContext context;
	private User user;
	private DitaOpenToolkit toolkit;
	private PrintWriter messageWriter;
	private Log log;
	private Session session;
	private String transtype;
	private File outputDir;
	private File tempDir;
	private File logDir;
	private List<String> additionalJars = new ArrayList<String>();
	private boolean cleanExportDir = false;
	private boolean loadToRsuite = true;
	private Map<String,String> parameters = new HashMap<String,String>();
	private Properties properties = new Properties();
	private StoredReport storedReport;
	
	public DitaOtOptions( ExecutionContext context, User user, String transtype) {
		this.context = context;
		this.user = user;
		this.transtype = transtype;
		this.log = LogFactory.getLog( DitaOtOptions.class);
		
		//setCommonTaskProperties();
	}

	@Override
	public void setExecutionContext( ExecutionContext context) {
		this.context = context;
	}
	
	public ExecutionContext getExecutionContext() {
		return context;
	}

	public void setToolkit( DitaOpenToolkit toolkit) {
		this.toolkit = toolkit;
	}
	
	public DitaOpenToolkit getToolkit()
			throws RSuiteException {
		if ( toolkit != null) {
			return toolkit;
		}
		String otName = getParameterWithDefault( OPEN_TOOLKIT_NAME_PARAM, "default");
		log.info("Using Open Toolkit named \"" + otName + "\"");
		DitaOpenToolkit toolkit = context.getXmlApiManager().getDitaOpenToolkitManager().getToolkit(otName);
		if (toolkit == null) {
			throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, "No DITA Open Toolkit named \"" + otName + "\" provided by Open Toolkit Manager. Cannot continue.");
		}
		return toolkit;
	}

	public void setStoredReport( StoredReport report) {
		this.storedReport = report;
	}

	public StoredReport getStoredReport() {
		return storedReport;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public ProcessMessageContainer getMessages() {
		return this;
	}

	public void setMessageWriter(PrintWriter messageWriter) {
		this.messageWriter = messageWriter;
	}

	public PrintWriter getMessageWriter() {
		return messageWriter;
	}

	public void setLog(Log log) {
		this.log = log;
	}

	public Log getLog() {
		return log;
	}

	/**
	 * Set the session to use to connect back to RSuite.
	 * @param session
	 */
	public void setSession(Session session) {
		this.session = session;
	}

	/**
	 * Get the session to use for connecting back to RSuite.
	 * @return The session, or null if there is no session.
	 */
	public Session getSession() {
		return session;
	}

	public void setTranstype( String transtype) {
		this.transtype = transtype;
	}

	public String getTranstype() {
		return transtype;
	}

	public Properties getProperties() {
		return properties;
	}
	
	public void setProperty( String name, String value) {
		properties.setProperty( name, value);
	}

	public void addProperties( Properties newProps) {
		properties.putAll( newProps);
	}
	
	public void setAdditionalJars( List<String> additionalJars) {
		this.additionalJars = additionalJars;
	}

	public List<String> getAdditionalJars() {
		return additionalJars ;
	}

	public void setCleanExportDir( boolean cleanExportDir) {
		this.cleanExportDir = cleanExportDir;
	}
	
	public boolean getCleanExportDir() {
		return cleanExportDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	public File getOutputDir() throws RSuiteException {
		if (this.outputDir != null)
			return this.outputDir;

		String outputPath = getParameter(OUTPUT_PATH_PARAM);
		if (outputPath == null || "".equals(outputPath.trim())) {
			outputDir = new File( context.getRSuiteServerConfiguration().getTmpDir(), "ditaot_" + DATE_FORMAT.format(new Date()));
		} 
		else {
			outputDir = new File(outputPath);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			if (!outputDir.exists()) {
				throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, "Failed to find or create output directory \"" + outputPath + "\"");
			}
			if (!outputDir.canWrite()) {
				throw new RSuiteException( RSuiteException.ERROR_INTERNAL_ERROR, "Cannot write to output directory \"" + outputPath + "\"");
			}	
		}
		return outputDir;
	}

	public void setTempDir(File tempDir) {
		this.tempDir = tempDir;
	}

	public File getTempDir() throws IOException {
		if ( tempDir != null) {
			return tempDir;
		}
		String transtype = getTranstype();
		File tempDir = new File(File.createTempFile( "dita-ot-"+transtype, "").getParentFile(), getNowString());
		tempDir.mkdirs();
		
		return tempDir;
	}
	
	public void setLogDir(File logDir) {
		this.logDir = logDir;
	}

	public File getLogDir() throws IOException {
		if ( logDir != null) {
			return logDir;
		}
		String transtype = getTranstype();
		File logDir = new File(File.createTempFile("dita-ot-"+transtype+"_log", "").getParentFile(), getNowString());
		logDir.mkdirs();
		
		return logDir;
	}

	public boolean loadResultToRSuite() {
		return loadToRsuite;
	}
	
	/**
	 * Indicates whether or not the generated files should be loaded to RSuite.
	 * @param loadToRsuite Set to true to turn on loading to RSuite. Default is "true".
	 */
	public void setLoadResultToRSuite(boolean loadToRsuite) {
		this.loadToRsuite = loadToRsuite;
	}

	/**
	 * Set an parameter to be passed to the transform in addition to
	 * any other parameters that might get passed.
	 * @param name The parameter name
	 * @param value The parameter value
	 */
	public void setParameter(String name, String value) {
		parameters.put( name, value);
	}

	/**
	 * Get the set of additional parameters set on the options object.
	 * @return Set, possibly empty, of option names.
	 */
	public Set<String> getParamNames() {
		return parameters.keySet();
	}

	/**
	 * Get the XSLT parameter with the specified name.
	 * @param name The parameter name
	 * @return The parameter value, or null if it doesn't exist.
	 */
	public String getParameter(String name) {
		return parameters.get(name);
	}
	
	protected String getParameterWithDefault( String name, String defaultVal) {
		if ( parameters.containsKey( name)) {
			return parameters.get( name);
		}
		else {
			return defaultVal;
		}
	}
	
	public File getBuildFile()
			throws RSuiteException { 
		File buildFile = null;
		String buildFilePath = getParameter(BUILD_FILE_PATH_PARAM);
				
		if (StringUtils.isNotBlank(buildFilePath)) {
			buildFile = new File(buildFilePath);
			if (!buildFile.exists()) {
				String msg = "Build file \"" + buildFile.getAbsolutePath() + "\" does not exist."; 
				log.error(msg);
			}
			if (!buildFile.canRead()) {
				String msg = "Build file \"" + buildFile.getAbsolutePath() + "\" exists but cannot be read."; 
				log.error(msg);
			}
		}
		return buildFile;
	}
	
	public void setCommonTaskProperties() {
		properties.setProperty("transtype", getTranstype());
		try {
			File tempDir = getTempDir();
			properties.setProperty("dita.temp.dir", tempDir.getAbsolutePath()); 
		} catch (IOException e) {
			log.error( "Unexpected exception getting the temp directory: " + e.getMessage(), 
					   e);	
		}
		try {
			properties.setProperty("dita.dir", getToolkit().getDir().getAbsolutePath());
		} catch (RSuiteException e) {
			log.error( "Unexpected exception getting the tookit directory: " + e.getMessage(), 
					   e);	
		}
		properties.setProperty("outer.control", "quiet");
		//generate.copy.outer = 2: Generate/copy all files, even those that will 
		// end up outside of the output directory.
		// Note that the RSuite exporter should always export the content so that
		// all dependencies are below the root map, so values "1" and "2" have 
		// the same effect (all files are processed and copied to the output).
		properties.setProperty("generate.copy.outer", "2");

		setArgumentIfSpecified( "onlytopic.in.map", ONLY_TOPIC_IN_MAP_PARAM);
		setArgumentIfSpecified( "args.draft", DRAFT_PARAM);
		setArgumentIfSpecified( "args.xsl", XSL_PARAM);
		setArgumentIfSpecified( "clean.temp", CLEAN_TEMP_PARAM);
		setArgumentIfSpecified( "dita.input.valfile", DITAVAL_FILEPATH_PARAM);
		setArgumentIfSpecified( "maxJavaMemory", MAX_JAVA_MEMORY_PARAM);

		try {
			File logDir = getLogDir();
			log.info("Setting logdir to \"" + logDir.getAbsolutePath() + "\"");
			properties.setProperty("args.logdir", logDir.getAbsolutePath());
		} catch ( IOException e) {
			log.error( "Unexpected exception getting the log directory: " + e.getMessage(), 
					   e);	
		}

		// See if buildProperties has been specified:
		String buildProperties = getParameter( BUILD_PROPERTIES_PARAM);
		if (buildProperties != null && !"".equals(buildProperties.trim())) {
			log.info("Got " + BUILD_PROPERTIES_PARAM + " value: \"" + buildProperties + "\"");
			try {
				Properties newProps = parseBuildPropertiesString( buildProperties);
				log.info("--------------");
				properties.putAll(newProps);
			} catch ( RSuiteException e) {
				log.error( "Unexpected exception parsing " + BUILD_PROPERTIES_PARAM + " value: " + e.getMessage(), 
						   e);	
			}
		}
	}
	
	/**
	 * @param buildProperties
	 * @return
	 * @throws RSuiteException 
	 */
	protected Properties parseBuildPropertiesString( String buildProperties)
			throws RSuiteException {
		Properties props = new Properties();
		if (StringUtils.isNotBlank(buildProperties)) {
			log.info("Parsing build properties using parameter value \"" + buildProperties + "\"...");
			StringTokenizer tokenizer = new StringTokenizer(buildProperties, "|");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (!token.contains("=")) {
					throw new RSuiteException(0, "build properties property item + \"" + token + "\" did not contain expected equals (=) character, value was \"" + token + "\"");
				}
				String propName = token.substring(0, token.indexOf("="));
				String propValue = token.substring(token.indexOf("=") + 1);
				log.info("  - " + propName + "=\"" + propValue + "\"");
				props.setProperty(propName, propValue);
			}
		}
		return props;
	}

	public void setArgumentIfSpecified( String toolkitParamName, String propParamName) {
		String value = getParameter( propParamName);
		if (value != null && !"".equals(value.trim())) {
			log.info( "Setting parameter " + toolkitParamName + " to value \"" + value + "\"");
			properties.setProperty(toolkitParamName, value);
		}
	}

	public static String getNowString() {
		String timeStr = DATE_FORMAT.format(new Date());
		return timeStr;
	}

}

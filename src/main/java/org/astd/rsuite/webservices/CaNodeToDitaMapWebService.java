package org.astd.rsuite.webservices;

import java.io.ByteArrayOutputStream;
import java.util.*;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.utils.*;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.system.RSuiteServerConfiguration;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.tools.dita.conversion.beans.TransformSupportBean;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;

import net.sf.saxon.s9api.Serializer;

/**
 * Generic action handler for transforming a content assembly or content
 * assembly node into a DITA map that is then imported into RSuite.
 */
public class CaNodeToDitaMapWebService extends RemoteApiHandlerBase {

	private static Log log = LogFactory.getLog(CaNodeToDitaMapWebService.class);

	/**
	 * URI of the XSLT transform to apply to the CA. If not specified, the
	 * default transform is used.
	 */
	public static final String XSLT_URL_PARAM = "xsltUrl";

	/**
	 * URI of the XSLT transform to apply to the CA. If not specified, the
	 * default transform is used.
	 */
	public static final String XSLT_URI_PARAM = "xsltUri";

	/**
	 * MO ID of the CA node to be transformed into a map.
	 */
	public static final String CA_NODE_ID_PARAM = "caNodeId";

	public static final String DEFAULT_CA_TO_MAP_TRANSFORM_URL = "rsuite:/res/plugin/rsuite-dita-support/canode2map/canode2map_shell.xsl";

	/**
	 * Controls debugging messages in the transform. Set to "true" turn
	 * debugging messages on.
	 */
	public static final String DEBUG_PARAM = "debug";

	@Override
	public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args) throws RSuiteException {

		String rsuiteId = args.getFirstString("rsuiteId");
		User user = context.getSession()
				.getUser();
		ManagedObjectService moService = context.getManagedObjectService();
		ManagedObject caNodeMo = moService
				.getManagedObject(user, rsuiteId);

		String xsltUrlString = args.getFirstValue(XSLT_URI_PARAM, DEFAULT_CA_TO_MAP_TRANSFORM_URL);

		if ("".equals(xsltUrlString) || xsltUrlString == null) {
			throw new RSuiteException("No transform URL, cannot continue.");
		}

		LoggingSaxonMessageListener logger = context.getXmlApiManager()
				.newLoggingSaxonMessageListener(log);
		Map<String, String> params = new HashMap<String, String>();
		params.put("debug", "false");
		String skey = "";
		if (skey == null || "".equals(skey)) {
			UserAgent userAgent = new UserAgent("canode-to-dita-map-handler");
			Session session = context.getSessionService()
					.createSession("Realm", userAgent, "http://" + context.getRSuiteServerConfiguration()
							.getHostName() + ":"
							+ context.getRSuiteServerConfiguration()
									.getPort()
							+ "/rsuite/rest/v1",
							context.getAuthorizationService()
									.getSystemUser());
			skey = session.getKey();
		}
		params.put("rsuiteSessionKey", skey);
		params.put("rsuiteHost", context.getRSuiteServerConfiguration()
				.getHostName());
		params.put("rsuitePort", context.getRSuiteServerConfiguration()
				.getPort());

		TransformSupportBean tsBean = new TransformSupportBean(context, xsltUrlString);
		Source source = new DOMSource(caNodeMo.getElement());
		RSuiteServerConfiguration serverConfig = context.getRSuiteServerConfiguration();
		source.setSystemId("http://" + serverConfig.getHostName() + ":" + serverConfig.getPort()
				+ "/rsuite/rest/v1/content/" + caNodeMo.getId());

		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
		Serializer dest = new Serializer();
		dest.setOutputStream(resultStream);

		log.info("Transforming CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId()
				+ "] to a DITA map using transform \"" + xsltUrlString + "\"...");
		tsBean.applyTransform(source, dest, params, logger, log);

		log.info("Loading new DITA map to RSuite...");

		String ditaMapFileName = "publication_" + caNodeMo.getId() + ".ditamap";
		
		ObjectInsertOptions insertOptions = new ObjectInsertOptions(ditaMapFileName,
				null, null, true);
		ObjectSource loadSource = new XmlObjectSource(resultStream.toByteArray());

		ContentAssemblyNodeContainer caContainer = 
				RSuiteUtils.getContentAssemblyNodeContainer(context, user, caNodeMo.getId());
		
		ManagedObject existingMo = MOUtils.
				getMoByFileNameAliasFromContainer(context, user, ditaMapFileName, caContainer);
		
		if (existingMo != null) {
			boolean existinMoCheckOut = MOUtils.checkout(context, user, existingMo.getId());
			// update
			MOUtils.updateAndCheckIn(user, moService, loadSource, existingMo, "Map regenerated.");

			if (existinMoCheckOut && moService.isCheckedOutAuthor(user, existingMo.getId())) {
				moService.undoCheckout(user, existingMo.getId());
			}
			log.info("Map attached as a new version to CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId() + "]");
		} else {
			//insert
			ManagedObject mapMo = null;
			try {
				mapMo = moService
						.load(context.getAuthorizationService()
								.getSystemUser(), loadSource, insertOptions);
			} catch (Exception e) {
				throw new RSuiteException(0, "Exception loading generated DITA map to RSuite: " + e.getMessage(), e);
			}
			log.info("Map loaded as MO [" + mapMo.getId() + "]");
			context.getContentAssemblyService().attach(context.getAuthorizationService()
					.getSystemUser(), caNodeMo.getId(), mapMo, new ObjectAttachOptions());
			log.info("Map attached to CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId() + "]");
		}

		return new MessageDialogResult("Create DITA map", "Map has been generated.");
	}

}

package org.astd.rsuite.transform;

import java.util.Date;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.utils.MOUtils;
import org.astd.rsuite.utils.TempFileUtils;
import org.astd.rsuite.utils.UserUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.transformation.TransformationContext;
import com.reallysi.rsuite.api.transformation.transformers.BaseXSLTTransformer;
import com.rsicms.rsuite.dynamicconfiguration.utils.DynamicConfigurationUtils;

/**
 * Transformer from DITA to XHTML
 */
public class DitaToXhtmlTransformer
    extends BaseXSLTTransformer
    implements ProjectConstants {

  /**
   * Means to server log.
   */
  private static final Log log = LogFactory.getLog(DitaToXhtmlTransformer.class);

  /**
   * Public identifier of doctype declaration for MO being transformed
   */
  private static String doctypePublic;

  /**
   * System identifier of doctype declaration for MO being transformed
   */
  private static String doctypeSystem;
  
  /**
   * Construct an instance of this transformer.
   * 
   * @param context
   * @param sourceMo
   * @throws RSuiteException 
   */
  public DitaToXhtmlTransformer(
      TransformationContext context, ManagedObject sourceMo) throws RSuiteException {
    super(log, context, sourceMo, "result", "text/html", "", XSL_URI_DITA_TO_XHTML);    
    doctypePublic = sourceMo.getPublicIdProperty();
    doctypeSystem = sourceMo.getSystemIdProperty();
  }

  @Override
  protected void setTransformerParameters(Transformer transformer) {

    super.setTransformerParameters(transformer);

    // Set doctypes
    transformer.setParameter("doctypePublic", doctypePublic);
    transformer.setParameter("doctypeSystem", doctypeSystem);

    // if config is true, then set parameter for write out
    try {
      if ("true".equals(DynamicConfigurationUtils.getSettingValue(context, UserUtils.getUser(context
          .getAuthorizationService(), DYNAMIC_CONFIGURATION_USERNAME),
          DYNAMIC_CONFIGURATION_PARAM_XSLT_AND_MERGE_ALL))) {
        transformer.setParameter("write.out.path.and.name.in", TempFileUtils
            .getTempFilePathForWriteOut(context) + "_dita2xhtml_in.xml");
        transformer.setParameter("write.out.path.and.name.out", TempFileUtils
            .getTempFilePathForWriteOut(context) + "_dita2xhtml_out.html");
      }
    } catch (RSuiteException e) {
      log.error("Error: " + e.getMessage());
    }
  }

  @Override
  public void transform(StreamSource source, StreamResult result) throws RSuiteException {

    // Time the transform
    Date start = new Date();
    log.info(ProjectMessageResource.getMessageText(
        "dita2xhtml.transformer.info.beginning.transform"));
    try {
      super.transform(source, result);
    } finally {
      log.info(ProjectMessageResource.getMessageText("dita2xhtml.transformer.info.elapsed.millis",
          new Date().getTime() - start.getTime()));
    }
  }

  /**
   * Enable caller to ask if this transformer applies.
   * 
   * @param mo
   * @param context
   * @return True if this class believes it applies to the specified MO, in the specified
   *         transformation context.
   */
  public final static boolean doesTransformerApply(TransformationContext context,
      ManagedObject mo) {
    boolean applies = false;
    String transformName = context.getTransformationName();
    try {
      applies = mo != null && TRANSFORM_DITA_TO_XHTML_NAME.equals(transformName) && !mo.isNonXml();
      log.info(ProjectMessageResource.getMessageText("dita2xhtml.transformer.info.applies.result",
          transformName, MOUtils.getDisplayNameQuietly(mo), mo.getId(), applies));
    } catch (Exception e) {
      log.warn(ProjectMessageResource.getMessageText(
          "dita2xhtml.transformer.warn.unable.to.determine.applicability", transformName, MOUtils
              .getDisplayNameQuietly(mo), mo.getId()));
    }
    return applies;
  }

}

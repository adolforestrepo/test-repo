package org.astd.rsuite.reports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.utils.CallArgumentUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.report.DefaultReportGenerator;
import com.reallysi.rsuite.api.report.Report;
import com.reallysi.rsuite.api.report.ReportGenerationContext;

/**
 * A non-production report generator responsible for serving up static content from a plugin.
 */
public class StaticContentReportGenerator
    extends DefaultReportGenerator {

  private static Log log = LogFactory.getLog(StaticContentReportGenerator.class);

  @Override
  public Report generateReport(ReportGenerationContext context, String reportId,
      CallArgumentList args) throws RSuiteException {

    CallArgumentUtils.logArguments(args, log);

    ReportImpl report = new ReportImpl();
    report.setContent(context.getPluginAccessManager().getContent(context.getAuthorizationService()
        .getSystemUser(), args.getFirstString("plugin_resource")));
    report.setLabel("The report's label"); // TODO
    report.setContentType("text/html");
    report.setSuggestedFilename("report.html");
    return report;
  }

}

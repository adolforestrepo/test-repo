/**
 * Copyright (c) 2010, 2012 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;

/**
 * Runs the DITA for Publishers EPUB transform type.
 */
public class DitaOtEPUBTaskRunningActionHandler extends DitaOtXhtmlTaskRunningActionHandlerBase {

	public static final String DEFAULT_TRANSTYPE = "epub";

	public static final String TOPICS_OUTPUT_DIR_PARAM = "topicsOutputDir";
	public static final String MAX_NAV_DEPTH_PARAM = "maxNavDepth";
	public static final String MAX_TOC_DEPTH_PARAM = "maxTocDepth";
	public static final String HTML2_GENERATE_INDEX_PARAM = "html2GenerateIndex";
	public static final String GENERATE_HTML_TOC_PARAM = "generateHtmlToc";
	public static final String GENERATE_GLOSSARY_PARAM = "generateGlossary";
	public static final String IMAGES_OUTPUT_DIR_PARAM = "imagesOutputDir";
	public static final String EXCLUDE_AUTO_RELLINKS_PARAM = "excludeAutoRellinks";
	public static final String COVER_GRAPHIC_URI_PARAM = "coverGraphicUri";
	public static final String HIDE_PARENT_LINK_PARAM = "hideParentLink";
	public static final String TITLE_ONLY_TOPIC_TITLE_CLASS_SPEC_PARAM = "titleOnlyTopicTitleClassSpec";
	public static final String TITLE_ONLY_TOPIC_CLASS_SPEC_PARAM = "titleOnlyTopicClassSpec";

	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		
		String transtype = resolveVariables(getParameterWithDefault("transtype", DEFAULT_TRANSTYPE));
		// Handle case where workflow process has <transtype>${transtype}</transtype>
		//  but ${transtype} isn't set.
		if (transtype == null || transtype.startsWith("${")) {
			transtype = DEFAULT_TRANSTYPE; 
		}

		DitaOpenToolkit toolkit = getToolkit(context, wfLog);

		Properties props = setBaseTaskProperties(context, transtype, toolkit);
		setEpubPropertiesIfSpecified(context, props);
		
		cleanOutputDir(context, getOutputDir(context));

		applyToolkitProcessToMos(context, wfLog, toolkit, transtype, props);
		
		// Make the generated EPUB the context item.
		String filename = context.getVariableAsString("exportedMapFilename");
		String outputDir = context.getVariableAsString("outputDir");
		if (filename != null && outputDir != null) {
			String epubName = FilenameUtils.getBaseName(filename) + ".epub";
			File epubFile = new File(new File(outputDir), epubName);
			FileWorkflowObject fileWO = new FileWorkflowObject(epubFile);
			context.setFileWorkflowObject(fileWO);
			context.setVariable("epubFilename", epubName);
			context.setVariable("epubFilepath", epubFile.getAbsolutePath());
		} else {
			context.getWorkflowLog().error("No exportedMapFilename or outputDir variables set");
		}
			
		
    }

	protected void setEpubPropertiesIfSpecified(WorkflowContext context, Properties props) {
		setArgumentIfSpecified(context, props, "epub.exclude.auto.rellinks", EXCLUDE_AUTO_RELLINKS_PARAM);
		setArgumentIfSpecified(context, props, "cover.graphic.uri", COVER_GRAPHIC_URI_PARAM);
		setArgumentIfSpecified(context, props, "epub.hide.parent.link", HIDE_PARENT_LINK_PARAM);
		setArgumentIfSpecified(context, props, "title.only.topic.class.spec", TITLE_ONLY_TOPIC_CLASS_SPEC_PARAM);
		setArgumentIfSpecified(context, props, "title.only.topic.title.class.spec", TITLE_ONLY_TOPIC_TITLE_CLASS_SPEC_PARAM);
		setArgumentIfSpecified(context, props, "images.output.dir", IMAGES_OUTPUT_DIR_PARAM);
		setArgumentIfSpecified(context, props, "topics.output.dir", TOPICS_OUTPUT_DIR_PARAM);
		setArgumentIfSpecified(context, props, "html2.generate.glossary", GENERATE_GLOSSARY_PARAM);
		setArgumentIfSpecified(context, props, "epub.generate.html.toc", GENERATE_HTML_TOC_PARAM);
		setArgumentIfSpecified(context, props, "html2.generate.index", HTML2_GENERATE_INDEX_PARAM);
		setArgumentIfSpecified(context, props, "d4p.max.toc.depth", MAX_TOC_DEPTH_PARAM);
		setArgumentIfSpecified(context, props, "d4p.max.nav.depth", MAX_NAV_DEPTH_PARAM);
	}

	public void setCoverGraphicUri(String coverGraphicUri) {
		this.setParameter(COVER_GRAPHIC_URI_PARAM, coverGraphicUri);
	}

	public void setExcludeAutoRellinks(String excludeAutoRellinks) {
		this.setParameter(EXCLUDE_AUTO_RELLINKS_PARAM, excludeAutoRellinks);
	}

	public void setHideParentLink(String hideParentLink) {
		this.setParameter(HIDE_PARENT_LINK_PARAM, hideParentLink);
	}

	public void setTitleOnlyTopicClassSpec(String titleOnlyTopicClassSpec) {
		this.setParameter(TITLE_ONLY_TOPIC_CLASS_SPEC_PARAM, titleOnlyTopicClassSpec);
	}

	public void setImagesOutputDir(String imagesOutputDir) {
		this.setParameter(IMAGES_OUTPUT_DIR_PARAM, imagesOutputDir);
	}
	
	public void setTopicsOutputDir(String topicsOutputDir) {
		this.setParameter(TOPICS_OUTPUT_DIR_PARAM, topicsOutputDir);
	}
	
	public void setGenerateGlossary(String generateGlossary) {
		this.setParameter(GENERATE_GLOSSARY_PARAM, generateGlossary);
	}
	
	public void setGenerateHtmlToc(String generateHtmlToc) {
		this.setParameter(GENERATE_HTML_TOC_PARAM, generateHtmlToc);
	}
	
	public void setHtml2GenerateIndexClassSpec(String html2GenerateIndex) {
		this.setParameter(HTML2_GENERATE_INDEX_PARAM, html2GenerateIndex);
	}
	
	public void setMaxTocDepthClassSpec(String maxTocDepth) {
		this.setParameter(MAX_TOC_DEPTH_PARAM, maxTocDepth);
	}
	
	public void setMaxNavDepthClassSpec(String maxNavDepth) {
		this.setParameter(MAX_NAV_DEPTH_PARAM, maxNavDepth);
	}
}

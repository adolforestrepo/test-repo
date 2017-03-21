Notes regarding this organization:

-In RSuite 5, the jBPM engine was changed to Activiti.

-These are copies of OOTB action handlers from Core (LoadNonXml and LoadByAlias along with AddLMD) and OOTB action handlers from dita-support-plugin (DitaOtPdf and DitaOtHtml)

-These copies were necessary - or seemed necessary - to accomplish passing in parameters from the workflow definitions (these are the Expression objects in the handlers, which "map" to activiti:field in workflow defs)

-We are in communication with PD about whether those are necessary or if there is another way to do this.

-Not pushing any further on this because right now RSuite has to be restarted each time you edit an action handler, which is just inefficient to say the least.

-Expectation is that TempWorkflowUtils.java and TempWorkflowConstants.java can be discarded eventually (so by putting them here and not integrating them into the normal project structure we are minimizing future changes)

-EnsureMapActionHandler will likely still be needed

-The duplicated code around resolving expressions (e.g., getWorkflowVariableOrParameter()) but such is life for something that is hopefully short lived and a royal pain to experiment with (e.g., due to having to restart RSuite whenever there is a change)
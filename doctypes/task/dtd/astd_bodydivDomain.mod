<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD BodyDiv Specializations
     
     Contains specializations of <bodydiv> that are used across
     ASTD-specific topic types.
     
     Copyright (c) 2009 American Society for Training and Development.

     ============================================================= -->

<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->



<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 

<!--
  An article callout (as opposed to a figure or chart callout)
  -->
<!ELEMENT callout
 (%bodydiv.cnt;)*
>
<!ATTLIST callout
  %univ-atts;
>

<!--
  An executive-summary for an article. Normally
  an article has at most one executive summary
  but there might be different summaries for different
  purposes. Executive summaries are normally not presented
  inline in the main rendering of an article, but
  pulled out for use elsewhere.
  
  Summaries are nominally metadata for an article but
  there's no way to have arbitrary body content
  in a topic's prolog.
  -->
<!ELEMENT executive-summary
 (%bodydiv.cnt;)*
>
<!ATTLIST executive-summary
  %univ-atts;
>


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->


<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST callout           %global-atts; class CDATA "+ topic/bodydiv astd-bodydiv/callout " >
<!ATTLIST executive-summary %global-atts; class CDATA "+ topic/bodydiv astd-bodydiv/executive-summary " >
 
<!-- ================== End Declaration Set  ======================== -->
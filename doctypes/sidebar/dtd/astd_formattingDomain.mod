<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Formatting Domain
     
     Defines specializations of p and ph for requesting specific
     formatting effects.
     
     Copyright (c) 2009 American Society for Training and Development
     
     ============================================================= -->

<!ENTITY % MATHML.prefixed "INCLUDE">

<!ENTITY % mathml2.dtd 
  SYSTEM "../../mathml2/mathml2.dtd"
>%mathml2.dtd;

<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->

<!ENTITY % inx 
  SYSTEM "inx-decls.ent"
>
%inx;

<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!ELEMENT br
  EMPTY
>
<!ATTLIST br
>

<!ELEMENT tab
  EMPTY
>
<!ATTLIST tab
>

<!ELEMENT frac
  (#PCDATA | 
   ph |
   i |
   b)*
>
<!ATTLIST frac
>

<!ELEMENT eqn_inline
  (inx_snippet |
   m:math |
   art |
   %data;)*
>
<!ATTLIST eqn_inline
  %univ-atts;
>

<!ELEMENT eqn_block
  (inx_snippet |
   m:math |
   art |
   %data;)*
>
<!ATTLIST eqn_block
  %univ-atts;
>

<!ELEMENT art
  (%RSuiteMetadata;,
   art_title?,
   image*,
   classification?)
>
<!ATTLIST art
  %id-atts;
  %localization-atts;
  base       
    CDATA                            
    #IMPLIED
  %base-attribute-extensions;
  outputclass 
    CDATA                            
    #IMPLIED    
>

<!ELEMENT art_title
  (%ph.cnt;)*
>
<!ATTLIST art_title
  %id-atts;
  %localization-atts;
  base       
    CDATA                            
    #IMPLIED
  %base-attribute-extensions;
  outputclass 
    CDATA                            
    #IMPLIED    
>



<!ELEMENT inx_snippet
  (%inx-components;)*
>
<!ATTLIST inx_snippet
  %univ-atts;
>

<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST art              %global-atts;  class CDATA "+ topic/ph    astd-formatting-d/art ">
<!ATTLIST art_title        %global-atts;  class CDATA "+ topic/data  astd-formatting-d/art_title ">

<!ATTLIST br               %global-atts;  class CDATA "+ topic/ph  astd-formatting-d/br ">
<!ATTLIST frac             %global-atts;  class CDATA "+ topic/ph  astd-formatting-d/frac ">
<!ATTLIST tab              %global-atts;  class CDATA "+ topic/ph  astd-formatting-d/tab ">
<!ATTLIST eqn_inline       %global-atts;  class CDATA "+ topic/ph  astd-formatting-d/eqn_inline ">
<!ATTLIST eqn_block        %global-atts;  class CDATA "+ topic/p   astd-formatting-d/eqn_block ">
<!ATTLIST inx_snippet      %global-atts;  class CDATA "+ topic/foreign  astd-formatting-d/inx_snippet ">

<!-- ================== End Formatting Domain ==================== -->
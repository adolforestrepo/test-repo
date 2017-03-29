<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Metadata Domain
     
     Defines specializations of data for classifying components
     of publications (topics, figures, tables, etc.).
     
     Copyright (c) 2009 American Society for Training and Development
     
     ============================================================= -->

<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->



<!ENTITY % disclaimer      "disclaimer" >


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!-- Long Name: Disclaimer. Holds metadata about resources (books, articles)
related to the main article. -->

<!ENTITY % disclaimer.content "
   (%data.cnt;)*
">
<!ENTITY % disclaimer.attributes
'
  name
    NMTOKEN
    "disclaimer"
'
>
<!ELEMENT disclaimer
  %disclaimer.content;
>
<!ATTLIST disclaimer
  %disclaimer.attributes;
>


<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST disclaimer   %global-atts;  class CDATA "+ topic/data  astd-metadata-d/disclaimer ">


<!-- ================== End Metadata Domain ==================== -->
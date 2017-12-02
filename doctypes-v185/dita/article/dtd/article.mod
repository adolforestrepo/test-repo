<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     GenericRSI Article Topic Type Module
     
     Specializes from topic
     
     Copyright (c) 2011RSI.

     ============================================================= -->


<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->

<!-- ============================================================= -->
<!--                   ARCHITECTURE ENTITIES                       -->
<!-- ============================================================= -->

<!-- ============================================================= -->
<!--                   SPECIALIZATION OF DECLARED ELEMENTS         -->
<!-- ============================================================= -->


<!ENTITY % article-info-types 
  "subjection | sidebar"
>


<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->

<!ENTITY % article            "article"                    >
<!ENTITY % article-body       "article-body"               >
<!ENTITY % article-title      "article-title"               >
<!ENTITY % article-maintitle  "article-maintitle"               >
<!ENTITY % article-subtitle   "article-subtitle"               >

<!ENTITY % epigraph           "epigraph"                      >

<!-- ============================================================= -->
<!--                    DOMAINS ATTRIBUTE OVERRIDE                 -->
<!-- ============================================================= -->


<!ENTITY included-domains 
  ""
>


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->
<!--   (%signature;)?,-->
<!ENTITY % article.content
 "((%RSuiteMetadata;)?,
   (%article-title;), 
   (%titlealts;)?,
   (%abstract; | 
    %shortdesc;)?, 
   (%prolog;)?, 
   (%article-body;),
   (%related-links;)?,
   (%article-info-types;)* )"
>
<!ENTITY % article.attributes
  'id         
    ID                               
    #REQUIRED
  conref     
    CDATA                            
    #IMPLIED
  %select-atts;
  %localization-atts;
  %arch-atts;
  outputclass 
    CDATA                            
    #IMPLIED
  domains    
    CDATA                
    "(topic article ur_epigraph-d) &included-domains;"    
 '
>
<!ELEMENT article %article.content; >
<!ATTLIST article %article.attributes;>

<!ENTITY % article-title.content
  "((%article-maintitle;),
    (%article-subtitle;)?)"
>
<!ENTITY % article-title.attributes
             "%id-atts;
              %localization-atts;
              base 
                        CDATA 
                                  #IMPLIED
              %base-attribute-extensions;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT article-title    %article-title.content;>
<!ATTLIST article-title    %article-title.attributes;>

<!ENTITY % article-maintitle.content
  "(%title.cnt;)*"
>
<!ENTITY % article-maintitle.attributes
             "%id-atts;
              %localization-atts;
              base 
                        CDATA 
                                  #IMPLIED
              %base-attribute-extensions;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT article-maintitle    %article-maintitle.content;>
<!ATTLIST article-maintitle    %article-maintitle.attributes;>

<!ENTITY % article-subtitle.content
  "(%title.cnt;)*"
>
<!ENTITY % article-subtitle.attributes
             "%id-atts;
              %localization-atts;
              base 
                        CDATA 
                                  #IMPLIED
              %base-attribute-extensions;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT article-subtitle    %article-subtitle.content;>
<!ATTLIST article-subtitle    %article-subtitle.attributes;>

<!ENTITY % article-body.content
  "((%epigraph;)?,
    (%body.cnt;)*)"
>
<!ENTITY % article-body.attributes
             "%id-atts;
              %localization-atts;
              base 
                        CDATA 
                                  #IMPLIED
              %base-attribute-extensions;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT article-body    %article-body.content;>
<!ATTLIST article-body    %article-body.attributes;>


<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST article                %global-atts;  class CDATA "- topic/topic   article/article ">
<!ATTLIST article-title          %global-atts;  class CDATA "- topic/title   article/article-title ">
<!ATTLIST article-maintitle      %global-atts;  class CDATA "- topic/ph      article/article-maintitle ">
<!ATTLIST article-subtitle       %global-atts;  class CDATA "- topic/ph      article/article-subtitle ">
<!ATTLIST article-body           %global-atts;  class CDATA "- topic/body    article/article-body ">

<!-- ================== End article  ======================== -->
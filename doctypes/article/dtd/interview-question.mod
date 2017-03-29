<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Interview Question Subsection Topic Type Module
     
     Represents a subsection that is a single interview question. 
     
     Specializes from topic
     
     Copyright (c) 2009 American Society for Training and Development.

     ============================================================= -->

<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->

<!-- ============================================================= -->
<!--                   ARCHITECTURE ENTITIES                       -->
<!-- ============================================================= -->

<!-- default namespace prefix for DITAArchVersion attribute can be
     overridden through predefinition in the document type shell   -->
<!ENTITY % DITAArchNSPrefix
  "ditaarch"
>

<!-- must be instanced on each topic type                          -->
<!ENTITY % arch-atts 
  "xmlns:%DITAArchNSPrefix; 
     CDATA
     #FIXED 'http://dita.oasis-open.org/architecture/2005/'
   %DITAArchNSPrefix;:DITAArchVersion
     CDATA
     '1.2'
"
>



<!-- ============================================================= -->
<!--                   SPECIALIZATION OF DECLARED ELEMENTS         -->
<!-- ============================================================= -->


<!ENTITY % interview-question-info-types 
  "%info-types; |
   no-topic-nesting
  "
>


<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 

<!ENTITY % interview-question     "interview-question"               >


<!-- ============================================================= -->
<!--                    DOMAINS ATTRIBUTE OVERRIDE                 -->
<!-- ============================================================= -->


<!ENTITY included-domains 
  ""
>


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!ELEMENT interview-question       
  (%RSuiteMetadata;,
   (%title;), 
   (%titlealts;)?,
   (%abstract; | 
    %shortdesc;)?, 
   (%prolog;)?, 
   (interview-response)?, 
   (%related-links;)?,
   (%interview-question-info-types;)* )                   
>
<!ATTLIST interview-question        
  id         
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
    "&included-domains;"    
>

<!ELEMENT interview-response
  %body.content;
>
<!ATTLIST  interview-response
  %univ-atts;
>

<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->


<!ATTLIST interview-question     %global-atts;  class CDATA "- topic/topic interview-question/interview-question ">
<!ATTLIST interview-response     %global-atts;  class CDATA "- topic/body  interview-question/interview-response ">

<!-- ================== End interview-question  ======================== -->
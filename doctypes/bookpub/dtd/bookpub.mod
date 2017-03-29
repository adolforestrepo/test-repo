<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     Local shell for book publication map

     Copyright (c) 2009 American Society for Training and Development

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




<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 

<!ENTITY % bookpub      "bookpub"                                    >
<!ENTITY % astd-book    "astd-book"                                  >


<!-- ============================================================= -->
<!--                    DOMAINS ATTRIBUTE OVERRIDE                 -->
<!-- ============================================================= -->


<!ENTITY included-domains 
  ""
>


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!ENTITY % bookpub.content 
 "((RSUITE:METADATA)?,
   (%pubtitle;)?, 
   (%pubmeta;)?,
   (%keydefs;)?,
   ((%appendix;) |
    (%article;) |
    (%astd-book;) |
    (%backmatter;) |
    (%bibliolist;) |
    (%chapter;) |
    (%department;) |
    (%forward;) |
    (%frontmatter;) |
    (%glossary-group;) |
    (%glossarylist;) | 
    (%keydefs;) | 
    (%keydef-group;) | 
    (%part;) |
    (%pubbody;) |
    (%subsection;) |
    (%sidebar;) |
    (%wrap-cover;))?)    
 "
>
<!ENTITY % bookpub.attributes
 "title 
            CDATA 
                      #IMPLIED
  id 
            ID 
                      #IMPLIED
  %conref-atts;
  anchorref 
            CDATA 
                      #IMPLIED
  outputclass 
            CDATA 
                      #IMPLIED
  %localization-atts;
              collection-type 
                        (choice | 
                         family | 
                         sequence | 
                         unordered |
                         -dita-use-conref-target) 
                                  'sequence'
              type 
                        CDATA 
                                  #IMPLIED
              processing-role
                        (normal |
                         resource-only |
                         -dita-use-conref-target)
                                  #IMPLIED
              scope 
                        (external | 
                         local | 
                         peer | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              locktitle 
                        (no | 
                         yes | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              format 
                        CDATA 
                                  #IMPLIED
              linking 
                        (none | 
                         normal | 
                         sourceonly | 
                         targetonly |
                         -dita-use-conref-target) 
                                  #IMPLIED
              toc 
                        (no | 
                         yes | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              print 
                        (no | 
                         printonly | 
                         yes | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              search 
                        (no | 
                         yes | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              chunk 
                        CDATA 
                                  #IMPLIED
  %select-atts;
"
>
<!ELEMENT bookpub       
  %bookpub.content;                  
>
<!ATTLIST bookpub
  %bookpub.attributes;
  %arch-atts;
  domains    
    CDATA                
    "(map bookpub-d) &included-domains;"    
>

<!ENTITY % astd-book.contents 
"
    ((%covers;)?,    
     (%colophon;)?, 
     ((%frontmatter;) |
      (%department;) |
      (%page;))*,
     ((%pubbody;)), 
     ((%appendixes;) |
      (%appendix;) |
      (%backmatter;) |
      (%page;) |
      (%department;) |
      (%colophon;))*,
     (%data.elements.incl; |
      %reltable;)*)
">
<!ENTITY % astd-book.attributes "
  id 
            ID 
                      #IMPLIED
  collection-type
            CDATA    
                      'sequence'
  %conref-atts;
  anchorref 
            CDATA 
                      #IMPLIED
  outputclass 
            CDATA 
                      #IMPLIED
  %localization-atts;
  %topicref-atts;
  %select-atts;
 ">
<!ELEMENT astd-book %astd-book.contents; >
<!ATTLIST astd-book %astd-book.attributes; >

<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->


<!ATTLIST bookpub      %global-atts;  class CDATA "- map/map bookpub/bookpub ">
<!ATTLIST astd-book    %global-atts;  class CDATA "- map/topicref bookpub/astd-book ">

<!-- ================== End bookpub Declaration Set  ===================== -->
<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Section Specializations
     
     Contains specializations of <section> that are used across
     ASTD-specific topic types.
     
     Copyright (c) 2009 American Society for Training and Development.

     ============================================================= -->

<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->



<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 
 <!ENTITY % chapter-highlights "chapter-highlights" >
 <!ENTITY % references-section "references-section" >
 <!ENTITY % biblio-entry "biblio-entry" >

<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!-- Chapter highlights: holds an authored list of highlights of the chapter
     contents.
  -->

<!ENTITY % chapter-highlights.content
                       "(%section.cnt;)*"
>
<!ENTITY % chapter-highlights.attributes
             "spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT chapter-highlights    %chapter-highlights.content;>
<!ATTLIST chapter-highlights    %chapter-highlights.attributes;>



<!-- References section: contains a list of references to resources,
     such as books, articles, Web sites, etc.
     
  -->
<!ENTITY % references-section.content
                       "(%section.cnt;)*"
>
<!ENTITY % references-section.attributes
             "spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT references-section    %references-section.content;>
<!ATTLIST references-section    %references-section.attributes;>


<!-- A bibliographic entry, as would occur within a references section -->
<!ENTITY % biblio-entry.content
                       "(%para.cnt;)*"
>
<!ENTITY % biblio-entry.attributes
             "%univ-atts;
              outputclass
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT biblio-entry    %biblio-entry.content;>
<!ATTLIST biblio-entry    %biblio-entry.attributes;>


<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST chapter-highlights   %global-atts;  class CDATA "+ topic/section astd_section-d/chapter-highlights "    >
<!ATTLIST references-section   %global-atts;  class CDATA "+ topic/section astd_section-d/references-section "    >
<!ATTLIST biblio-entry         %global-atts;  class CDATA "+ topic/p astd_section-d/biblio-entry "    >


 
<!-- ================== End Declaration Set  ======================== -->
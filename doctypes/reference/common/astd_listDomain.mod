<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD List Specializations
     
     Contains specializations of <list> that are used across
     ASTD-specific topic types.
     
     Copyright (c) 2009 American Society for Training and Development.

     ============================================================= -->

<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->



<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 
<!ENTITY %  checklist-ordered   "checklist-ordered" >
<!ENTITY %  checklist-unordered "checklist-unordered" >


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!--                    LONG NAME: Unordered check list            -->
<!ENTITY % checklist-unordered.content
                       "(%li;)+"
>
<!ENTITY % checklist-unordered.attributes
             "compact 
                        (no | 
                         yes | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT checklist-unordered    %checklist-unordered.content;>
<!ATTLIST checklist-unordered    %checklist-unordered.attributes;>

<!--                    LONG NAME: Ordered check list            -->
<!ENTITY % checklist-ordered.content
                       "(%li;)+"
>
<!ENTITY % checklist-ordered.attributes
             "compact 
                        (no | 
                         yes | 
                         -dita-use-conref-target) 
                                  #IMPLIED
              spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT checklist-ordered    %checklist-ordered.content;>
<!ATTLIST checklist-ordered    %checklist-ordered.attributes;>



<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST checklist-ordered        %global-atts;  class CDATA "- topic/ol astd_list-d/checklist-ordered "         >
<!ATTLIST checklist-unordered      %global-atts;  class CDATA "- topic/ul astd_list-d/checklist-unordered "         >


<!-- ================== End Declaration Set  ======================== -->
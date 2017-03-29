<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Paragraph Specializations
     
     Contains specializations of <p> that are used across
     ASTD-specific topic types.
     
     Copyright (c) 2009 American Society for Training and Development.

     ============================================================= -->

<!-- =============================================================
     Non-DITA Namespace declarations: 
     ============================================================= -->



<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 
<!ENTITY %  copy-block   "copy-block" >


<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!--                    LONG NAME: "copy block": a block indented from the left and the right that is not a long quotation.
   -->
<!ENTITY % copy-block.content
                       "(%para.cnt;)*"
>
<!ENTITY % copy-block.attributes
             "%univ-atts;
              outputclass
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT copy-block    %copy-block.content;>
<!ATTLIST copy-block    %copy-block.attributes;>


<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST copy-block        %global-atts;  class CDATA "- topic/p astd_p-d/copy-block "         >


<!-- ================== End Declaration Set  ======================== -->
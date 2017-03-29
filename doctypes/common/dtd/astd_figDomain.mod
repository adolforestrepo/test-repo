<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Figure Specializations
     
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
 
 <!ENTITY % example-figure "example-figure" >
 <!ENTITY % tool-figure "tool-figure" >
 <!ENTITY % worksheet-figure "worksheet-figure" >

<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->


<!ENTITY % tool-figure.content
                       "((%title;)?, 
                         (%desc;)?, 
                         (%figgroup; | 
                          %fig.cnt;)* )"
>
<!ENTITY % tool-figure.attributes
             "%display-atts;
              spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT tool-figure    %tool-figure.content;>
<!ATTLIST tool-figure    %tool-figure.attributes;>

<!ENTITY % worksheet-figure.content
                       "((%title;)?, 
                         (%desc;)?, 
                         (%figgroup; | 
                          %fig.cnt;)* )"
>
<!ENTITY % worksheet-figure.attributes
             "%display-atts;
              spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT worksheet-figure    %worksheet-figure.content;>
<!ATTLIST worksheet-figure    %worksheet-figure.attributes;>

<!ENTITY % example-figure.content
                       "((%title;)?, 
                         (%desc;)?, 
                         (%figgroup; | 
                          %fig.cnt;)* )"
>
<!ENTITY % example-figure.attributes
             "%display-atts;
              spectitle 
                        CDATA 
                                  #IMPLIED
              %univ-atts;
              outputclass 
                        CDATA 
                                  #IMPLIED"
>
<!ELEMENT example-figure    %example-figure.content;>
<!ATTLIST example-figure    %example-figure.attributes;>



<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST example-figure   %global-atts;  class CDATA "- topic/fig astd_fig-d/example-figure "        >
<!ATTLIST tool-figure      %global-atts;  class CDATA "- topic/fig astd_fig-d/tool-figure "        >
<!ATTLIST worksheet-figure %global-atts;  class CDATA "- topic/fig astd_fig-d/worksheet-figure "        >

 
<!-- ================== End Declaration Set  ======================== -->
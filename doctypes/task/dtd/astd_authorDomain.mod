<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Author Specialization
     
     Specialization of <author> that is more complete than base
     <author> and less fiddly than XNAL <authorinformation>.
     
     Copyright (c) 2009, 2010 American Society for Training and Development.

     ============================================================= -->


<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->
 
<!ENTITY % astd-author "astd-author" >
<!ENTITY % astd-author "author-name" >
<!ENTITY % astd-author "author-title" >
<!ENTITY % astd-author "author-desc" >
<!ENTITY % astd-author "author-affiliation" >
<!ENTITY % astd-author "author-email" >
<!ENTITY % astd-author "author-bio-para" >

<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->

<!ENTITY % author.text.cnt
 "#PCDATA |
  %ph;
 "
>

<!-- NOTE: all fields are optional here so you can have either
           just fields or just an author-bio-para with embedded
           fields, or both. The intent is that at least
           author-name should be specified one place or the other.
  -->
<!ENTITY % astd-author.content
 "(author-name?,
   author-title?,
   author-desc?,
   author-affiliation?,
   author-email*,
   author-bio-para*)
 "
 >
<!ENTITY % astd-author.attributes
 '%univ-atts;
  href 
            CDATA 
                      #IMPLIED
  format 
            CDATA 
                      #IMPLIED
  scope 
           (external | 
            local | 
            peer | 
            -dita-use-conref-target) 
                      #IMPLIED
  keyref 
            CDATA 
                      #IMPLIED
  type 
            CDATA 
                      #IMPLIED  
 
 '
 >
<!ELEMENT astd-author
  %astd-author.content;
>
<!ATTLIST astd-author
  %astd-author.attributes;
>

<!ENTITY % author-name.content
 "(%author.text.cnt;)*"
> 
<!ENTITY % author-name.attributes
  '%univ-atts;
   name
     NMTOKEN "author-name"
  '
>
<!ELEMENT author-name
  %author-name.content;
>
<!ATTLIST author-name
  %author-name.attributes;
>


<!ENTITY % author-title.content
 "(%author.text.cnt;)*"
> 
<!ENTITY % author-title.attributes
  '%univ-atts;
   name
     NMTOKEN "author-title"
  '
>
<!ELEMENT author-title
  %author-title.content;
>
<!ATTLIST author-title
  %author-title.attributes;
>

<!ENTITY % author-desc.content
 "(%author.text.cnt; |
   %cite;)*
  "
> 
<!ENTITY % author-desc.attributes
  '%univ-atts;
   name
     NMTOKEN "author-desc"
  '
>
<!ELEMENT author-desc
  %author-desc.content;
>
<!ATTLIST author-desc
  %author-desc.attributes;
>

<!ENTITY % author-affiliation.content
 "(%author.text.cnt;)*"
> 
<!ENTITY % author-affiliation.attributes
  '%univ-atts;
   name
     NMTOKEN "author-affiliation"
  '
>
<!ELEMENT author-affiliation
  %author-affiliation.content;
>
<!ATTLIST author-affiliation
  %author-affiliation.attributes;
>

<!ENTITY % author-email.content
 "(%author.text.cnt;)*"
> 
<!ENTITY % author-email.attributes
  '%univ-atts;
   name
     NMTOKEN "author-email"
  '
>
<!ELEMENT author-email
  %author-email.content;
>
<!ATTLIST author-email
  %author-email.attributes;
>

<!ENTITY % author-bio-para.content 
  "(%data.cnt; | 
    author-name |
    author-title |
    author-desc |
    author-affiliation |
    author-email)*"
>
<!ENTITY % author-bio-para.attributes
 '%univ-atts;
   name
     NMTOKEN "author-bio-para"
 '
>
<!ELEMENT author-bio-para
  %author-bio-para.content;
>
<!ATTLIST author-bio-para
  %author-bio-para.attributes;
>


<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST astd-author      %global-atts;  class CDATA "+ topic/author  astd-author-d/astd-author ">
<!ATTLIST author-name      %global-atts;  class CDATA "+ topic/data  astd-author-d/author-name ">
<!ATTLIST author-title     %global-atts;  class CDATA "+ topic/data  astd-author-d/author-title ">
<!ATTLIST author-desc      %global-atts;  class CDATA "+ topic/data  astd-author-d/author-desc ">
<!ATTLIST author-affiliation  %global-atts;  class CDATA "+ topic/data  astd-author-d/author-affiliation ">
<!ATTLIST author-email     %global-atts;  class CDATA "+ topic/data  astd-author-d/author-email ">
<!ATTLIST author-bio-para  %global-atts;  class CDATA "+ topic/data  astd-author-d/author-bio-para ">

 
<!-- ================== End Declaration Set  ======================== -->
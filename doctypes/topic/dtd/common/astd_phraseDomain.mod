<?xml version="1.0" encoding="utf-8"?>
<!-- =============================================================
     ASTD Phrase Domain
     
     Defines specializations of p and ph for semantic identification
     (as opposed to pure formatting effects).
     
     Copyright (c) 2009 American Society for Training and Development
     
     ============================================================= -->


<!-- ============================================================= -->
<!--                   ELEMENT NAME ENTITIES                       -->
<!-- ============================================================= -->

<!-- ============================================================= -->
<!--                    ELEMENT DECLARATIONS                       -->
<!-- ============================================================= -->


<!ELEMENT quote-attribution
  (%ph.cnt;)*
>
<!ATTLIST quote-attribution
>



<!-- ============================================================= -->
<!--                    SPECIALIZATION ATTRIBUTE DECLARATIONS      -->
<!-- ============================================================= -->

<!ATTLIST quote-attribution %global-atts;  class CDATA "+ topic/ph  astd-phrase-d/quote-attribution ">

<!-- ================== End Formatting Domain ==================== -->
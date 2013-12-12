<?php
include "includes/header.php"; 
?>

<p>The U.S. National Institutes of Health (NIH) is the world&#39;s largest source of biomedical research funding, consisting of twenty
five different Institutes and Centers that issue &#126;80,000 grants each year (<a href="http://www.nih.gov/icd/">http://www.nih.gov/icd/</a>). </p>

<p>This website provides a <a href="https://app.nihmaps.org/">database and web-based interface</a> for searching and discovering the types of research awarded by 
the NIH. The database uses automated, computer generated categories from a statistical analysis known as <a href="docs.php">topic modeling</a>.  
The categories (&#39;topics&#39;) are unofficial &#45; they are determined purely from the text of grant titles and abstracts (available 
from the NIH at <a href="http://projectreporter.nih.gov">http://projectreporter.nih.gov</a>), without the use of keywords or NIH administrative categories.  They are 
provided for discovering relationships among NIH grants, and to assist in understanding the types of research that NIH funds.</p>  

<p>The database also uses a graphical method for <a href="docs.php">automatically clustering grants</a> on a two-dimensional plane, which creates a 
&#39;topic map&#39; from which they can be searched and selected.  In this context, grants are located in groups that are 
thematically related to one another. </p>

<table border="0" cellspacing=0 cellpadding=0>
<tr >
	<td colspan="2" valign=top>
<tr>
	<td valign=top >
	<img src="images/indexMap2_main.png" width="432px" border="0"/>
	</td>
	<td halign=left valign=top>
		<img src="images/indexMap2_inset.png" width="175px" border="0"/>
		
    	<p><em>The cluster-based visualization to the left shows all 
    	NIH grants from a single funding year.  Each grant is represented as a 
    	dot, color coded by NIH Institute.  The labels are placed automatically,
    	based on the review panel assignments of the underlying grants.  
    	The database system permits users to browse this space visually by 
    	searching for grants or selecting regions of interest. Hits from 
    	searches are displayed as pushpins on the map, as shown in the inset.  
		</em></p>
    </td>
</tr>
</table>
  
<?php
include( "includes/footer.php" ); 
?>
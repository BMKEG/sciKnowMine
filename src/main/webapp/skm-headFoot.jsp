<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
		"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<c:url value="/images/favicon.ico" var="iconImage" />
<c:url value="/style/embedFlex.css" var="embedFlex_css" />
<c:url value="/index.jsp" var="index" />

<!-- saved from url=(0014)about:internet -->
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">

<head>
<title>SciKnowMine System</title>
<link rel="shortcut icon" href="${iconImage}" type="image/x-icon" />

<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<!-- Include CSS to eliminate any default margins/padding and set the height of the html element and 
	the body element to 100%, because Firefox, or any Gecko based browser, interprets percentage as 
	the percentage of the height of its parent container, which has to be set explicitly. Initially, 
	don't display flashContent div so it won't show if JavaScript disabled. -->

<!-- Enable Browser History by replacing useBrowserHistory tokens with two hyphens -->

<!-- BEGIN Browser History required section -->
<link rel="stylesheet" type="text/css"
	href="clientApp/history/history.css" />
<script type="text/javascript" src="clientApp/history/history.js"></script>
<!-- END Browser History required section -->

<script type="text/javascript" src="clientApp/swfobject.js"></script>
<script type="text/javascript">
	var swfVersionStr = "10.2.0";
	var xiSwfUrlStr = "clientApp/playerProductInstall.swf";
	var flashvars = {};
	var params = {};
	params.quality = "high";
	params.bgcolor = "#ffffff";
	params.allowscriptaccess = "sameDomain";
	params.allowfullscreen = "true";
	var attributes = {};
	attributes.id = "sciKnowMineSystem";
	attributes.name = "sciKnowMineSystem";
	attributes.align = "middle";
	attributes.style = "padding-top: 5px; padding-bottom: 5px;";
	swfobject.embedSWF("sciKnowMineSystem.swf", "flashContent", "100%", "100%",
			swfVersionStr, xiSwfUrlStr, flashvars, params, attributes);
	swfobject.createCSS("#flashContent", "display:block;text-align:left;");
</script>

<!-- SCIKNOWSOFT STYLESHEET -->
<link href="${embedFlex_css}" rel="stylesheet" />
<!-- ~~~~~~~~~~~~~~~~~~~~~~ -->

</head>
<body>

	<!-- GENERIC SCIKNOWSOFT HEADER FOR WEBSITE
	<jsp:include page="WEB-INF/view/includes/header.jspx" />
	<jsp:include page="WEB-INF/view/includes/navigation.jspx" />
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->

	<div id="main_content">

		<!-- SWFObject's dynamic embed method replaces this alternative HTML content with Flash content when enough 
			 JavaScript and Flash plug-in support is available. The div is initially hidden so that it doesn't show
			 when JavaScript is disabled. -->
		<div id="flashContent" >
			<p>To view this page ensure that Adobe Flash Player version
				10.2.0 or greater is installed.</p>
			<script type="text/javascript">
				var pageHost = ((document.location.protocol == "https:") ? "https://"
						: "http://");
				document
						.write("<a href='http://www.adobe.com/go/getflashplayer'>"
								+ "<img src='" + pageHost + 
								"www.adobe.com/images/shared/download_buttons/get_flash_player.gif' " + 
								"alt='Get Adobe Flash player' /></a>");
			</script>

			<noscript>
				<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"
					width="100%" height="100%" id="sciKnowMineSystem.swf" >
					<param name="movie" value="sciKnowMineSystem.swf" />
					<param name="quality" value="high" />
					<param name="bgcolor" value="#ffffff" />
					<param name="allowScriptAccess" value="sameDomain" />
					<param name="allowFullScreen" value="true" />
					<!--[if !IE]>-->
					<object type="application/x-shockwave-flash"
						data="sciKnowMineSystem.swf" width="100%" height="100%">
						<param name="quality" value="high" />
						<param name="bgcolor" value="#ffffff" />
						<param name="allowScriptAccess" value="sameDomain" />
						<param name="allowFullScreen" value="true" />
						<!--<![endif]-->
						<!--[if gte IE 6]>-->
						<p>Either scripts and active content are not permitted to run
							or Adobe Flash Player version 10.2.0 or greater is not installed.</p>
						<!--<![endif]-->
						<a href="http://www.adobe.com/go/getflashplayer"> <img
							src="http://www.adobe.com/images/shared/download_buttons/get_flash_player.gif"
							alt="Get Adobe Flash Player" />
						</a>
						<!--[if !IE]>-->
					</object>
					<!--<![endif]-->
				</object>
			</noscript>
		</div>
	</div>

	<!-- GENERIC SCIKNOWSOFT FOOTER FOR WEBSITE 

	<jsp:include page="WEB-INF/view/includes/footer.jspx" />

	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->

</body>

</html>


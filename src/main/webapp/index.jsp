<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<c:url value="/index.jsp" var="index" />

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>The SciKnowMine System: Knowledge
	Mining from the Scientific Literature</title>
	
<link rel="shortcut icon" href="/images/favicon.ico" type="image/x-icon" />
<link href="stylesheets/styles.css" rel="stylesheet" />
<link href="stylesheets/pygment_trac.css" rel="stylesheet" />
<meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
    <!--[if lt IE 9]>
    <script src="//html5shiv.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
</head>

<body>

	<div class="wrapper">

		<header>

			<h1>The SciKnowMine System.</h1>
				
			<p>
				Biomedical Natural Language Processing promises to accelerate
				the process of literature-based
				&lsquo;biocuration&rsquo;, where published information must be
				carefully and appropriately translated into biomedical databases. 
				SciKnowMine provides a framework for the delivery of text-mining 
				capabilities to biocurators directly. 
			</p>

			<jsp:include page="WEB-INF/view/includes/navigation.jspx" />

		</header>
		
		<section>	
			<h1>Document Classification and Triage</h1>
			<p>
				A key step for biocuration is identifying which documents are of interest
				and which are not. Within this system, we use document classification 
				to <em>triage</em> documents (similar to
				the medical triage process where patients are classified based on 
				their need for quick medical attention).
				This step sorts the documents so that specialists only need focus on
				the documents appropriate for them.
			</p>
			<p>
				The challenge of delivering effective computational support for
				triage and subsequent curation of large-scale biomedical databases
				is still unsolved. We are developing tools to accelerate the process
				of biocuration <em>in-situ</em> for existing biomedical databases
				(such as the Jackson Laboratory&#39;s <a
					href="http://www.informatics.jax.org/">Mouse Genome Informatics</a>
				project. 
			</p>
		</section>
		<footer>
		<p><small>The SciKnowMine System is the first released component of the <b>SciKnowSoft</b> toolset</small></p>
        <p><img src="images/SciKnowSoft_Panel.jpg"/></p>
		</footer>

	</div>

</body>

</html>


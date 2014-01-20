<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<c:url value="/images/favicon.ico" var="iconImage" />
<c:url value="/style/default.css" var="default_css" />

<c:url value="/index.jsp" var="index" />

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>SciKnowMine: A Web Application to Support Knowledge
	Mining from the Scientific Literature</title>
<link rel="shortcut icon" href="${iconImage}" type="image/x-icon" />
<link href="${default_css}" rel="stylesheet" />
</head>

<body>

	<div id="outer">

		<jsp:include page="WEB-INF/view/includes/header.jspx" />
		<jsp:include page="WEB-INF/view/includes/navigation.jspx" />

		<div id="main_content">

			<p>Welcome to SciKnowMine, a text mining architecture for
				scientific knowledge.</p>

			<p>You will soon be dead. Life will sometimes seem long and
				tough, and 'God it's tiring' and you will sometimes be happy and
				sometimes sad; and then you'll be old; and then you'll be dead.
				There is only one sensible thing to do with this empty existence and
				that is 'Fill It!' (not filet, 'fill'. 'it'.) and in my opinion,
				until I change it, life is best filled by learning as much as you
				can about as much as you can, taking pride in whatever you're doing,
				having compassion, sharing ideas, RUNNING, being enthusiastic... and
				then there's love and travel and wine and sex and art and kids and
				giving and mountain climbing... but you know all that stuff already.
				It's an incredibly exciting thing, this one, meaningless, life of
				yours. Good luck and thank you for indulging me.</p>

		</div>

		<jsp:include page="WEB-INF/view/includes/footer.jspx" />

	</div>

</body>

</html>


<%@ page contentType="text/html"%>

Hai ${founder.name.capitalize()},
<br/><br/><br/>
User <g:link controller="SUser" action="show" id="${user.id }" absolute="true">${user.name.capitalize()}</g:link> is requesting membership in one of the group <g:link controller="userGroup" action="show" id="${userGroupInstance.id }" absolute="true">${userGroupInstance.name}</g:link> you own on portal <b>${domain}</b>.
<br/> 

Please <a href="${uri}" title="Confirmation code">click here</a> to confirm the membership.
<br/><br/><br/>
Thank you,<br/>
The Portal Team

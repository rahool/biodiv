<%@page import="species.utils.Utils"%>
<%@ page import="species.dataset.DataPackage"%>
<%@ page import="species.Habitat"%>
<html>
<head>
<g:set var="title" value="${g.message(code:'dataPackage.name.label')}"/>
<g:render template="/common/titleTemplate" model="['title':title]"/>

</head>
<body>
<%
				def form_id = "createDataPackage"
				def form_action = uGroup.createLink(controller:'dataPackage', action:'save')
				def form_button_name = "Create DataPackage"
				def form_button_val = "${g.message(code:'button.create.dataPackage')}"
			    entityName="Create DataPackage"	
				if(params.action == 'edit' || params.action == 'update'){
					//form_id = "updateGroup"
					form_action = uGroup.createLink(controller:'dataPackage', action:'update')
				 	form_button_name = "Update DataPackage"
					form_button_val = "${g.message(code:'button.update.dataPackage')}"
					entityName = "Edit DataPackage"
				}
				%>
		<div class="observation_create">
			<div class="span12">
				<uGroup:showSubmenuTemplate  model="['entityName':entityName]"/>
						

				<g:hasErrors bean="${dataPackageInstance}">
					<i class="icon-warning-sign"></i>
					<span class="label label-important"> <g:message
							code="fix.errors.before.proceeding" default="Fix errors" /> </span>
				</g:hasErrors>
			
			
			<form id="${form_id}" action="${form_action}" method="POST"
				class="form-horizontal">
				<input type="hidden" name="id" value="${dataPackageInstance?.id}"/>
				<div class="super-section">
					<div class="section"
						style="position: relative; overflow: visible;">
						<div
							class="row control-group left-indent ${hasErrors(bean: dataPackageInstance, field: 'title', 'error')}">

							<label for="name" class="control-label"><g:message
									code="dataPackage.name.label" default="${g.message(code:'dataPackage.name.label')}" /> *</label>
							<div class="controls textbox">
								<div id="groups_div" class="btn-group" style="z-index: 3;">
									<g:textField name="title" value="${dataPackageInstance?.title}" placeholder="${g.message(code:'button.create.dataPackage')}" />
									<div class="help-inline">
										<g:hasErrors bean="${dataPackageInstance}" field="title">
											<g:eachError bean="${dataPackageInstance}" field="title">
    											<li><g:message error="${it}" /></li>
											</g:eachError>
										</g:hasErrors>
									</div>
								</div>
							</div>
						</div>
												
						<div
                            class="row control-group left-indent ${hasErrors(bean: dataPackageInstance, field: 'description', 'error')}">
								<label for="description" class="control-label"><g:message code="default.description.label" />*</label>
							<div class="controls  textbox">
								
								<textarea id="description" name="description" placeholder="${g.message(code:'dataPackage.small.description')}">${dataPackageInstance?.description}</textarea>
								
								<script type='text/javascript'>
                                    CKEDITOR.plugins.addExternal( 'confighelper', "${assetPath(src:'ckeditor/confighelper/plugin.js')}" );
									
									var config = { extraPlugins: 'confighelper', toolbar:'EditorToolbar', toolbar_EditorToolbar:[[ 'Bold', 'Italic' ]]};
									CKEDITOR.replace('description', config);
								</script>
								<div class="help-inline">
									<g:hasErrors bean="${dataPackageInstance}" field="description">
										<g:eachError bean="${dataPackageInstance}" field="description">
    											<li><g:message error="${it}" /></li>
										</g:eachError>
									</g:hasErrors>
								</div>
							</div>

						</div>
				

						<div
							class="row control-group left-indent ${hasErrors(bean: dataPackageInstance, field: 'supportingModules', 'error')}">

							<label for="supportingModules" class="control-label"><g:message
									code="dataPackage.supportingModules.label" default="${g.message(code:'dataPackage.supportingModules.label')}" /></label>
							<div class="controls textbox">
								<div id="groups_div" class="btn-group" style="z-index: 3;">
                                    <g:each in="${DataPackage.SupportingModules.list()}" var="supportingModule">
                                        <label class="checkbox" style="text-align: left;"> 
                                            <input type="checkbox" name="supportingModule.${supportingModule.ordinal()}" /> ${supportingModule.value()} 
                                        </label>
                                    </g:each>
									<div class="help-inline">
										<g:hasErrors bean="${dataPackageInstance}" field="supportingModules">
											<g:eachError bean="${dataPackageInstance}" field="supportingModules">
    											<li><g:message error="${it}" /></li>
											</g:eachError>
										</g:hasErrors>
									</div>
								</div>
							</div>
						</div>


						<div
							class="row control-group left-indent ${hasErrors(bean: dataPackageInstance, field: 'allowedDataTableTypes', 'error')}">

							<label for="allowedDataTableTypes" class="control-label"><g:message
									code="dataPackage.allowedDataTableTypes.label" default="${g.message(code:'dataPackage.allowedDataTableTypes.label')}" /> *</label>
							<div class="controls textbox">
								<div id="groups_div" class="btn-group" style="z-index: 3;">
                                    <g:each in="${DataPackage.DataTableType.list()}" var="allowedDataTableType">
                                        <label class="checkbox" style="text-align: left;"> 
                                            <input type="checkbox" name="allowedDataTableType.${allowedDataTableType.ordinal()}" /> ${allowedDataTableType.value()} 
                                        </label>
                                    </g:each>
									<div class="help-inline">
										<g:hasErrors bean="${dataPackageInstance}" field="allowedDataTableTypes">
											<g:eachError bean="${dataPackageInstance}" field="allowedDataTableTypes">
    											<li><g:message error="${it}" /></li>
											</g:eachError>
										</g:hasErrors>
									</div>
								</div>
							</div>
						</div>




				<div class="" style="margin-top: 20px; margin-bottom: 40px;">
				
					<g:if test="${dataPackageInstance?.id}">
						<a href="${createLink(mapping:'dataPackage', action:'show', id:dataPackageInstance.id)}" class="btn"
							style="float: right; margin-right: 5px;"> <g:message code="button.cancel" /> </a>
					</g:if>
					<g:else>
					<a href="${createLink(mapping:'userGroupgeneric', action:'list')}" class="btn"
							style="float: right; margin-right: 5px;"> <g:message code="button.cancel" /> </a>
					</g:else>
					
					<g:if test="${dataPackageInstance?.id}">
						<div class="btn btn-danger"
							style="float: right; margin-right: 5px;">
							<a
								href="${createLink(mapping:'dataPackage', action:'delete', id:dataPackageInstance?.id)}"
				        onclick="return confirm('${message(code: 'default.dataPackage.delete.confirm.message')}');"><g:message code="button.delete.dataPackage" /></a>
						</div>
					</g:if>
					 <a id="createDataPackageSubmit"
						class="btn btn-primary" style="float: right; margin-right: 5px;">
						${form_button_val} </a>
					<span class="policy-text"> <g:message code="dataPackage.create.submitting.for.new" /> <a href="/terms"><g:message code="link.terms.conditions" /></a> <g:message code="register.index.use.of.site" /> </span>
				</div>

                </div>
                </div>
			</form>
		
		</div>
	
</div>
	<asset:script>
        $(document).ready(function() {
            $("#createDataPackageSubmit").click(function(){
                $("#${form_id}").submit();
                return false;
            });
        });
    </asset:script>

</body>

</html>

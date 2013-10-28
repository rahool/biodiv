<div class="post-to-groups" >
    <div class="post-main-content">
        <div>
            <g:if test="${isBulkPull}">
            <div>
                <a class="select-all" href="#" title="Select all" onclick='updateListSelection($(this));return false;'>Select all</a> <b>|</b>
                <a class="reset" href="#" title="Reset" onclick='updateListSelection($(this));return false;'>Reset</a>
            </div>
            </g:if>
            <div id="userGroups" class="userGroups">
                <uGroup:getCurrentUserUserGroups model="[observationInstance:observationInstance, onlyExpertGroups:onlyExpertGroups]"/>
            </div>
        </div>
        <a onclick="submitToGroups('post', '${objectType}', '${uGroup.createLink(controller:'userGroup', action:'bulkPost', userGroup:userGroup)}', ${isBulkPull} ,'${observationInstance?.id}');return false;" class="btn btn-primary"
            style="float: right; margin-right: 5px;"> Post </a>
        <a onclick="submitToGroups('unpost', '${objectType}', '${uGroup.createLink(controller:'userGroup', action:'bulkPost', userGroup:userGroup)}', ${isBulkPull}, '${observationInstance?.id}');return false;" class="btn btn-danger"
            style="float: right; margin-right: 5px;"> Unpost </a>
    </div>
</div>
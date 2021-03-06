package species.dataset;

import java.util.List;
import java.util.Map;

import grails.converters.JSON;
import grails.converters.XML;
import species.AbstractObjectController;

import grails.plugin.springsecurity.annotation.Secured
import static org.springframework.http.HttpStatus.*;
import species.participation.Observation;
import species.participation.Checklists;
import species.dataset.DataPackage.DataTableType;
import species.dataset.DataTable;

import org.springframework.web.servlet.support.RequestContextUtils as RCU;

class DatasetController extends AbstractObjectController {
	
	def springSecurityService;
	def mailService;
	def messageSource;

    def datasetService;
    def dataPackageService;

	static allowedMethods = [show:'GET', index:'GET', list:'GET',  update: ["POST","PUT"], delete: ["POST", "DELETE"], flagDeleted: ["POST", "DELETE"]]
    static defaultAction = "list"

	def index = {
		redirect(action: "list", params: params)
	}
    
    @Secured(['ROLE_USER'])
	def create() {
		def datasetInstance = new Dataset1()
		
        datasetInstance.properties = params;

		//def author = springSecurityService.currentUser;

        if(params.dataPackage) {
            datasetInstance.dataPackage = DataPackage.read(params.long('dataPackage'));  
        }
        datasetInstance.clearErrors();

        if(!datasetInstance.dataPackage || dataPackageService.hasPermission(datasetInstance.dataPackage, springSecurityService.currentUser)) {
			render(view: "create", model: [datasetInstance: datasetInstance])
		} else {
			flash.message = "${message(code: 'default.not.permitted.message', args: [params.action, message(code: 'dataset.label', default: 'Dataset'), datasetInstance.dataPackage.title])}"
			redirect  url: uGroup.createLink(action: "list")
		}
	}

	@Secured(['ROLE_USER'])
	def save() {
	    saveAndRender(params, false)
	}

    @Secured(['ROLE_USER'])
	def edit() {
		def datasetInstance = Dataset1.findWhere(id:params.id?.toLong(), isDeleted:false)

        datasetInstance.clearErrors();
		if (!datasetInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataset.label', default: 'Dataset'), params.id])}"
			redirect (url:uGroup.createLink(action:'list', controller:"dataset", 'userGroupWebaddress':params.webaddress))
			//redirect(action: "list")
		} else if(datasetService.hasPermission(datasetInstance, springSecurityService.currentUser)) {
			render(view: "create", model: [datasetInstance: datasetInstance, 'springSecurityService':springSecurityService])
		} else {
			flash.message = "${message(code: 'edit.denied.message')}"
			redirect (url:uGroup.createLink(action:'show', controller:"dataset", id:datasetInstance.id, 'userGroupWebaddress':params.webaddress))
		}
	}

	@Secured(['ROLE_USER'])
	def update() {
		def datasetInstance = Dataset1.get(params.long('id'))
        def msg;
		if(datasetInstance)	{
            if(datasetService.hasPermission(datasetInstance, springSecurityService.currentUser)) {
                saveAndRender(params, true)
		    } else {
			    flash.message = "${message(code: 'edit.denied.message')}"
			    redirect (url:uGroup.createLink(action:'show', controller:"dataset", id:datasetInstance.id, 'userGroupWebaddress':params.webaddress))
		    }
		} else {
	 		msg = "${message(code: 'default.not.found.message', args: [message(code: 'dataset.label', default: 'Dataset'), params.id])}"
            def model = utilsService.getErrorModel(msg, null, OK.value());
            withFormat {
                html {
                    flash.message = msg;
			        redirect (url:uGroup.createLink(action:'list', controller:"datasource"))
                 }
                json { render model as JSON }
                xml { render model as XML }
             }
	 	}
	} 
		
	private saveAndRender(params, sendMail=true){
		params.locale_language = utilsService.getCurrentLanguage(request);
		def result = datasetService.save(params, sendMail)
        log.debug "#######DATASET SAVE RESULT######"
        log.debug result;
        log.debug "################################"
		if(result.success){
            withFormat {
                html {
			        redirect(controller:'dataset', action: "show", id: result.instance.id)
                }
                json {
                    result.url = uGroup.createLink(action:'show', controller:"dataset", id:result.instance.id, 'userGroupWebaddress':params.webaddress);
                    render result as JSON 
                }
                xml {
                    render result as XML
                }
            }

		} else {
            withFormat {
                html {
                    //flash.message = "${message(code: 'error')}";
			        render(view: "create", model: [datasetInstance: result.instance])
                }
                json {
                    result.remove('instance');
                    render result as JSON 
                }
                xml {
                    result.remove('instance');
                    render result as XML
                }
            }
		}
	}

	def show() {
        params.id = params.long('id');
        def msg;
        if(params.id) {
			def datasetInstance = Dataset1.findByIdAndIsDeleted(params.id, false)
			if (!datasetInstance) {
                msg = "${message(code: 'default.not.found.message', args: [message(code: 'dataset.label', default: 'Dataset'), params.id])}"
                def model = utilsService.getErrorModel(msg, null, OK.value());
                withFormat {
                    html {
				        flash.message = model.msg;
				        redirect (url:uGroup.createLink(action:'list', controller:"dataset", 'userGroupWebaddress':params.webaddress))
                    }
                    json { render model as JSON }
                    xml { render model as XML }
                }
			}
			else {
				//datasetInstance.incrementPageVisit()
				def userLanguage = utilsService.getCurrentLanguage(request);   

                def model = utilsService.getSuccessModel("", datasetInstance, OK.value());
                withFormat {
                    html {
                            return [datasetInstance: datasetInstance, 'userLanguage':userLanguage, max:10]
                    } 
                    json  { render model as JSON }
                    xml { render model as JSON }
                }
			}
		} else {
            msg = "${message(code: 'default.not.found.message', args: [message(code: 'dataset.label', default: 'Dataset'), params.id])}"
            def model = utilsService.getErrorModel(msg, null, OK.value());
            withFormat {
                html {
			        redirect (url:uGroup.createLink(action:'list', controller:"dataset", 'userGroupWebaddress':params.webaddress))
                }
                json { render model as JSON }
                xml { render model as XML }
            }
        }
	}

	def observationData = {
        if(!params.id) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataset.label', default: 'Dataset'), params.id])}"
            redirect (url:uGroup.createLink(action:'list', controller:"datasource", 'userGroupWebaddress':params.webaddress))
        }

        Dataset datasetInstance = Dataset.read(params.id.toLong());
		List observations =  Observation.findAllByDataset(datasetInstance, [max:params.int('max'), offset:params.int('offset')]);
        int observationsCount = Observation.countByDataset(datasetInstance);
		def model = ['observations':observations, 'observationsCount':observationsCount, 'checklistInstance':datasetInstance, 'max':10]
		render(template:"/common/checklist/showChecklistDataTemplate", model:model);
	}

	def list() {
		def model = getDatasetList(params);
        model.userLanguage = utilsService.getCurrentLanguage(request);

        if(!params.loadMore?.toBoolean() && !!params.isGalleryUpdate?.toBoolean()) {
            model.resultType = 'dataset'
            //model['userGroupInstance'] = UserGroup.findByWebaddress(params.webaddress);
            model['obvListHtml'] =  g.render(template:"/dataset/showDatasetListTemplate", model:model);
            model['obvFilterMsgHtml'] = g.render(template:"/common/observation/showObservationFilterMsgTemplate", model:model);
            model['obvListHtml'] = model['obvListHtml'].replaceAll('\u002f','/');
            model.remove('datasetInstanceList');
        }
        
        model = utilsService.getSuccessModel('', null, OK.value(), model);

        withFormat {
            html {
                if(params.loadMore?.toBoolean()){
                    render(template:"/dataset/showDatasetListTemplate", model:model.model);
                    return;
                } else if(!params.isGalleryUpdate?.toBoolean()){
                    model.model['width'] = 300;
                    model.model['height'] = 200;
                    render (view:"list", model:model.model)
                    return;
                } else {

                    return;
                }
            }
            json { render model as JSON }
            xml { render model as XML }
        }
	}

	protected def getDatasetList(params) {
        try { 
            params.max = params.max?Integer.parseInt(params.max.toString()):24 
        } catch(NumberFormatException e) { 
            params.max = 24 
        }
        try { 
            params.offset = params.offset?Integer.parseInt(params.offset.toString()):0; 
        } catch(NumberFormatException e) { 
            params.offset = 0 
        }

        def max = Math.min(params.max ? params.int('max') : 24, 100)
        def offset = params.offset ? params.int('offset') : 0
        def filteredDataset = datasetService.getFilteredDatasets(params, max, offset, false)
        def instanceList = filteredDataset.instanceList

        def queryParams = filteredDataset.queryParams
        def activeFilters = filteredDataset.activeFilters
        def count = filteredDataset.instanceTotal	

        activeFilters.put("append", true);//needed for adding new page obv ids into existing session["obv_ids_list"]
        return [instanceList: instanceList, instanceTotal: count, queryParams: queryParams, activeFilters:activeFilters, resultType:'dataset']
	}

	@Secured(['ROLE_ADMIN'])
    def uploadGbifData() {
        def uploadLog = new File(params.logFile);
        if(uploadLog.exists()) uploadLog.delete();
        println "Printing log to : "+ uploadLog.getAbsolutePath()
        datasetService.importGBIFObservations(Dataset.read(params.id), new File(params.dwcArchivePath), uploadLog);
        render ""
    }

    @Secured(['ROLE_USER'])
	def flagDeleted() {
        println "00000000000000000000000000";
		def result = datasetService.delete(params)
		flash.message = result.message
		redirect (url:result.url)
	}

    @Secured(['ROLE_USER'])
	def delete() {
		def result = datasetService.delete(params)
        result.remove('url')
        String url = result.url;
        withFormat {
            html {
                flash.message = result.message
                redirect (url:url)
            }
            json { render result as JSON }
            xml { render result as XML }
        }
	}

    @Secured(['ROLE_USER'])
    def dataPackageChangedForDataset() {
        DataPackage dataPackage = DataPackage.read(params.long('dataPackageId'));
		def datasetInstance = new Dataset1()
        datasetInstance.dataPackage = dataPackage
        datasetInstance.clearErrors();

        if(dataPackageService.hasPermission(datasetInstance.dataPackage, springSecurityService.currentUser)) {
            String tmpl = g.render(template:"/dataset/collectionMetadataTemplate", model:[instance:datasetInstance, 'autofillUserComp':'contributor_id']);
            def model = utilsService.getSuccessModel("", datasetInstance, OK.value(), [tmpl:tmpl]);
            render model as JSON;
		} else {
            String msg = "Sorry, you dont have permission to create a dataset";//g.message(code:"default.not.permitted.message", args:["create", 'dataset', '']);// as Object[], RCU.getLocale(request))
            println msg
            def model = utilsService.getErrorModel(msg, null, OK.value());
            render model as JSON;
		}
    }
    
    @Secured(['ROLE_USER'])
    def dataTableTypeChanged() {
        Dataset1 datasetInstance;
        DataTable dataTableInstance;
        
        if(params.datasetId) { 
            datasetInstance = Dataset1.read(params.long('datasetId'));
        }
            
        if(params.dataTableId) {
            dataTableInstance = DataTable.read(params.long('dataTableId'));
        } else {
            dataTableInstance = new DataTable();
            if(datasetInstance) {
                dataTableInstance.dataset = datasetInstance;
                dataTableInstance.properties = datasetInstance.properties;
                dataTableInstance.uFile = null;
                dataTableInstance.description = null; // not propagating dataset description
            }
        }
        if(!datasetInstance) {
            datasetInstance = new Dataset1();
            datasetInstance.dataPackage = DataPackage.findByTitle('Checklist');
            dataTableInstance.dataset = datasetInstance;
        }


        if(dataPackageService.hasPermission(dataTableInstance.dataset.dataPackage, springSecurityService.currentUser)) {
            dataTableInstance.dataTableType = DataTableType.list()[params.int('dataTableTypeId')]; 
            String tmpl = g.render(template:"/dataTable/add${dataTableInstance.dataTableType.value()}DataTable", model:[dataTableInstance:dataTableInstance]);
            def model = utilsService.getSuccessModel("", dataTableInstance, OK.value(), [tmpl:tmpl]);
            render model as JSON;

        } else {
            String msg = "Sorry, you dont have permission to create a datatable";//g.message(code:"default.not.permitted.message", args:["create", 'dataset', '']);// as Object[], RCU.getLocale(request))
            println msg
            def model = utilsService.getErrorModel(msg, null, OK.value());
            render model as JSON;
		}
    }       
}

package content.eml

import java.util.Date;

import species.Resource;
import species.License;
import species.DataObject;
import species.auth.SUser;
import species.groups.UserGroup;
import species.groups.SpeciesGroup;
import species.Habitat;
import species.participation.Flag;
import species.participation.Follow;
import species.participation.Featured;
import species.Language;
import org.springframework.context.MessageSourceResolvable;
import content.eml.DocSciName;
import com.vividsolutions.jts.geom.Coordinate;
import species.groups.UserGroup.FilterRule;
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.PrecisionModel;

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

//@Cache(region="document", include = "non-lazy")
//@JsonIgnoreProperties([])
/**
 * eml-literature module
 * http://knb.ecoinformatics.org/software/eml/eml-2.1.1/eml-literature.html
 * http://knb.ecoinformatics.org/software/eml/eml-2.1.1/index.html
 *
 */
class Document extends DataObject implements Comparable {
	
	def springSecurityService;
	def documentService

    public enum DocumentType implements org.springframework.context.MessageSourceResolvable{
        Report("Report"),
        Poster("Poster"),
        Proposal("Proposal"),
        Journal_Article("Journal Article"),
        Book("Book"),
        Thesis("Thesis"),
        Technical_Report("Technical Report"),
        Presentation("Presentation"),
        Miscellaneous("Miscellaneous"),
        

        private String value;


        DocumentType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

		static def toList() {
			return [
                Report,
                Poster,
                Proposal,
                Miscellaneous,
                Journal_Article,
                Book,
                Thesis,
                Technical_Report,
                Presentation
			]
		}

        Object[] getArguments() { [] as Object[] }

        String[] getCodes() {

            ["${getClass().name}.${name()}"] as String[]
        }   
        String getDefaultMessage() { value() }


    }

	DocumentType type
	String title

	UFile uFile   //covers physical file formats
	//String uri
	
	String notes // <=== description
	String contributors;
	String attribution;
	
	String doi
	String placeName
	
	//source holder(i.e project, group)
	Long sourceHolderId;
	String sourceHolderType;

    //String scientificNames;
	//XXX uncmment it before migration
	//Coverage coverage //<== extending metadata now	//Coverage Information
	
	//Date createdOn  <=== dateCreated
	//Date lastRevised <=== lastUpdated

	//boolean deleted
	
	boolean agreeTerms = false	

	
	//static transients = [ 'isDeleted' ]

	
	static constraints = {
		title nullable:false, blank:false
		uFile nullable:true
		//uri nullable:true
		contributors nullable:true
		attribution  nullable:true	
		sourceHolderId nullable:true
		sourceHolderType nullable:true
		notes nullable:true
		doi nullable:true
    	featureCount nullable:false
		agreeTerms nullable:true
		locationScale(nullable: true)
		
		//coverage related extended from metadata
		placeName(nullable:true)
		reverseGeocodedName(nullable:true)
		fromDate(nullable: true)
		group nullable:true
		habitat nullable:true
		externalUrl nullable:true
	}
	
	static hasMany = [userGroups: UserGroup, speciesGroups:SpeciesGroup, habitats:Habitat, docSciNames:DocSciName]
	static belongsTo = [SUser, UserGroup]

    static mapping = {
        notes type:"text"
        attribution type:"text"
        contributors type:"text"
        title type:"text"
        id  generator:'org.hibernate.id.enhanced.SequenceStyleGenerator', params:[sequence_name: "document_id_seq"] 
        cache include: 'non-lazy'
    }

    List fetchAllFlags(){
        def fList = Flag.findAllWhere(objectId:this.id,objectType:this.class.getCanonicalName());
        return fList;
    }

    def boolean fetchIsFollowing(SUser user=springSecurityService.currentUser){
        return Follow.fetchIsFollowing(this, user)
    }

    String title() {
        return this.title;
    }

    String fetchSpeciesCall(){
        return this.title;
    }

    String notes(Language userLanguage = null) {
        return this.notes?:'';
    }

    String summary(Language userLanguage = null) {
        return this.notes?:'';
    }

    def getOwner() {
        return author;
    }

    def setSource(parent) {
        this.sourceHolderId = parent.id
        this.sourceHolderType = parent.class.getCanonicalName()
    }

    def fetchSource(){
        if(sourceHolderId && sourceHolderType){
            return grailsApplication.getArtefact("Domain",sourceHolderType)?.getClazz()?.read(sourceHolderId)
        }
    }

    def beforeDelete(){
        activityFeedService.deleteFeed(this)
    }

    Resource mainImage() {  
        String reprImage = "Document.png"
        String name = (new File(grailsApplication.config.speciesPortal.content.rootDir + "/" + reprImage)).getName()
        return new Resource(fileName: "documents"+File.separator+name, type:Resource.ResourceType.IMAGE, context:Resource.ResourceContext.DOCUMENT, baseUrl:grailsApplication.config.speciesPortal.content.serverURL) 
    }



	def beforeUpdate(){
		if(isDirty() && isDirty('topology')){
			updateLatLong()
		}
	}
	
	def beforeInsert(){
		updateLatLong()
	}
	
	def fetchList(params, max, offset){
		return documentService.getFilteredDocuments(params, max, offset)
	}
	
	static DocumentType fetchDocumentType(String documentType){
		if(!documentType) return null;
		for(DocumentType type : DocumentType) {
			if(type.name().equals(documentType)) {
				return type;
			}
		}
		return null;
	}
	
	int compareTo(obj) {
		createdOn.compareTo(obj.createdOn)
	}

	Map fetchSciNames(){
		Map nameValue = [:]
		Map nameParseValues = [:]
		Map nameId = [:]
		Map primaryname = [:]
		def c = DocSciName.createCriteria()
			def results = c.list {
			eq("document", this)
		    order("primary_name", "desc")
			}
		def docSciNames = results ;//DocSciName.findAllByDocument(this)
		docSciNames.each{ dsn ->
		nameValue.put(dsn.scientificName,dsn.frequency)
		nameParseValues.put(dsn.scientificName,dsn.taxonConcept)
		nameId.put(dsn.scientificName,dsn.id)
		primaryname.put(dsn.id,dsn.primary_name)
		}

		return [nameValues:nameValue, nameparseValue:nameParseValues, nameDisplayValues:nameId,primaryName:primaryname]

	}

    boolean isUserGroupValidForPosting(UserGroup userGroup) {
        List<FilterRule> filterRule = userGroup.getFilterRules();
        boolean isValid = true;
        filterRule.each { fRule ->
            switch(fRule.fieldName) {
                case 'topology' : 
                if(fRule.ruleName.equalsIgnoreCase('dwithin')) {
                    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), grailsApplication.config.speciesPortal.maps.SRID);
                    def location = geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
                    isValid = isValid && fRule.ruleValues[0].covers(location);
                }
                break;
                case 'taxon' : 
                if(fRule.ruleName.equalsIgnoreCase('scope')) {
                    //isValid = fRule.ruleValues[0].covers(instance[fRule.fieldName]);
                }
                break;
            }
        }
        return isValid;
    }

    List<UserGroup> getValidUserGroups() {
        List<UserGroup> userGroups = UserGroup.list();
        List<UserGroup> validUserGroups = [];
        userGroups.each { uGroup ->
            if(this.isUserGroupValidForPosting(uGroup))
                validUserGroups << uGroup;
        }
        return validUserGroups;
    }

    def fetchChecklistAnnotation(){
        def res = [:]
        res['id'] = this.id;
        res['type'] = 'document';
        res['title'] = this.title;
        res['url'] = this.externalUrl;
        if(uFile) {
        res['uFile'] = this.uFile.path;
        }
        return res
    }

    static long countDocuments() {
        def c = Document.createCriteria();
println "countDocuments%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
println "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"


        def count = c.count {
            cache true;
        }
println "countDocuments%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
println "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"


        return count;
    }

    public void setType(type) {
        println "documentType##########################"
        println "##########################"
        println "##########################"
        println "##########################"
        println "##########################"
 
        this.type = type;
    }
}

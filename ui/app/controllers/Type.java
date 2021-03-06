package controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.DateTimeResolution;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ItemTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeMutabilityImpl;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Security.Authenticated;
import scala.xml.Elem;
import util.Util;

public class Type extends Controller {
	private static Session getCmisSession(String repositoryId){
		return CmisSessions.getCmisSession(repositoryId, session());
	}
	
	public static Result index(String repositoryId) {
		Session session = Util.createCmisSession(repositoryId, session());
		
		List<ObjectType> list = new ArrayList<ObjectType>();
		
		List<Tree<ObjectType>> descendants = session.getTypeDescendants(null, -1, true);
		for(Tree<ObjectType> _type : descendants){
			ObjectType type = _type.getItem();
			list.add(type);
		}
		
		return ok(views.html.objecttype.list.render(repositoryId, list));
	}

	public static Result download(String repositoryId, String id){
		Session session = getCmisSession(repositoryId);
		ObjectType objectType = session.getTypeDefinition(id);
		ObjectMapper mapper = new ObjectMapper();

		ObjectNode json = mapper.createObjectNode();
		json.put("id", escape(objectType.getId()));
		json.put("localName", escape(objectType.getLocalName()));
		json.put("localNamespace", escape(objectType.getLocalNamespace()));
		json.put("displayName", escape(objectType.getDisplayName()));
		json.put("queryName", escape(objectType.getQueryName()));
		json.put("description", escape(objectType.getDescription()));
		json.put("baseId", escape(objectType.getBaseTypeId().value()));
		if(StringUtils.isNotBlank(objectType.getParentTypeId())){
			json.put("parentId", escape(objectType.getParentTypeId()));
		}
		json.put("creatable", objectType.isCreatable());
		json.put("fileable", objectType.isFileable());
		json.put("queryable", objectType.isQueryable());
		json.put("fulltextIndexed", objectType.isFulltextIndexed());
		json.put("includedInSupertypeQuery", objectType.isIncludedInSupertypeQuery());
		json.put("controllablePolicy", objectType.isControllablePolicy());
		json.put("controllableACL", objectType.isControllableAcl());
		ObjectNode typeMutability = mapper.createObjectNode();
		typeMutability.put("create", objectType.getTypeMutability().canCreate());
		typeMutability.put("update", objectType.getTypeMutability().canUpdate());
		typeMutability.put("delete", objectType.getTypeMutability().canDelete());
		json.put("typeMutability", typeMutability);
		if(objectType instanceof DocumentTypeDefinition){
			DocumentTypeDefinition documentType = (DocumentTypeDefinition)objectType;
			json.put("versionable", documentType.isVersionable());
			json.put("contentStreamAllowed", documentType.getContentStreamAllowed().value());
		}
		
		//Property definitions
		Map<String, PropertyDefinition<?>> map = objectType.getPropertyDefinitions();
		ObjectNode propertyDefinitions = mapper.createObjectNode();
		for(String key : map.keySet()){
			PropertyDefinition<?> pdf = map.get(key);
			ObjectNode propertyDefinition = mapper.createObjectNode();
			propertyDefinition.put("id", escape(pdf.getId()));
			propertyDefinition.put("localName", escape(pdf.getLocalName()));
			propertyDefinition.put("localNamespace", escape(pdf.getLocalNamespace()));
			propertyDefinition.put("displayName", escape(pdf.getDisplayName()));
			propertyDefinition.put("queryName", escape(pdf.getQueryName()));
			propertyDefinition.put("description", escape(pdf.getDescription()));
			propertyDefinition.put("propertyType", pdf.getPropertyType().value());
			propertyDefinition.put("cardinality", pdf.getCardinality().value());

			propertyDefinition.put("updatability", pdf.getUpdatability().value());
			propertyDefinition.put("inherited", pdf.isInherited());
			propertyDefinition.put("required", pdf.isRequired());
			propertyDefinition.put("queryable", pdf.isQueryable());
			propertyDefinition.put("orderable", pdf.isOrderable());
			propertyDefinition.put("openChoice", pdf.isOpenChoice());

			//TODO implement "choice"
			
			propertyDefinitions.put(pdf.getId(), propertyDefinition);
		}
		
		json.put("propertyDefinitions", propertyDefinitions);

		File file;
		try {
			file = File.createTempFile(
					String.valueOf(System.currentTimeMillis()), null);
			mapper.writeValue(file, json);
			
			try {
				if (request().getHeader("User-Agent").indexOf("MSIE") == -1) {
					// Firefox, Opera 11
					response().setHeader(
							"Content-Disposition",
							String.format(Locale.JAPAN,
									"attachment; filename*=utf-8'jp'%s",
									URLEncoder.encode(objectType.getId() + ".json", "utf-8")));
				} else {
					// IE7, 8, 9
					response().setHeader(
							"Content-Disposition",
							String.format(Locale.JAPAN,
									"attachment; filename=\"%s\"", new String((objectType.getId() + ".json").getBytes("MS932"),
											"ISO8859_1")));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			response().setContentType("application/json");
			
			return ok(file);
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return noContent();
	}
	
	private static String escape(String str){
		//TODO is this way to escape a string really desirable?
		return StringEscapeUtils.escapeEcmaScript(str);
	}
	
	public static Result showBlank(String repositoryId){
		return ok(views.html.objecttype.blank.render(repositoryId));
	}
	
	public static Result create(String repositoryId){
		DynamicForm input = Form.form();
		input = input.bindFromRequest();
		
		MultipartFormData body = request().body().asMultipartFormData();
		List<FilePart> files = body.getFiles();
		for (FilePart file : files) {
			//parse json file
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode json = mapper.readTree(file.getFile());
				
				//Call CMIS API
				TypeDefinition tdf = parseType(json);
				Session session = getCmisSession(repositoryId);
				ObjectType newType = session.createType(tdf);
				
				System.out.println();
				
				
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		
		}
		
		return redirect(routes.Type.index(repositoryId));
	}
	
	public static Result edit(String repositoryId){
		return ok(views.html.objecttype.edit.render(repositoryId));
	}
	
	public static Result update(String repositoryId){
		DynamicForm input = Form.form();
		input = input.bindFromRequest();
		
		MultipartFormData body = request().body().asMultipartFormData();
		List<FilePart> files = body.getFiles();
		for (FilePart file : files) {
			//parse json file
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode json = mapper.readTree(file.getFile());
				
				//Call CMIS API
				TypeDefinition tdf = parseType(json);
				Session session = getCmisSession(repositoryId);
				ObjectType udpatedType = session.updateType(tdf);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
		return redirect(routes.Type.index(repositoryId));
		
	}
	
	public static Result delete(String repositoryId, String id){
		Session session = getCmisSession(repositoryId);
		session.deleteType(id);
		
		return redirect(routes.Type.index(repositoryId));
	}
	
	private static TypeDefinition parseType(JsonNode json){
		String baseId = json.get("baseId").textValue();
		
		if(BaseTypeId.CMIS_DOCUMENT.value().equals(baseId)){
			return parseDocumentType(json);
		}else if(BaseTypeId.CMIS_FOLDER.value().equals(baseId)){
			return parseFolder(json);
		}else if(BaseTypeId.CMIS_ITEM.value().equals(baseId)){
			return parseItem(json);
		}
		
		return null;
	}
	
	private static void parseCommonType(AbstractTypeDefinition tdf, JsonNode json){
		tdf.setId(getString(json, "id"));
		tdf.setLocalName(getString(json, "localName"));
		tdf.setLocalNamespace(getString(json, "localNamespace"));
		tdf.setDisplayName(getString(json, "displayName"));
		tdf.setQueryName(getString(json, "queryName"));
		tdf.setDescription(getString(json, "description"));
		String baseId = getString(json, "baseId");
		if(baseId != null){
			tdf.setBaseTypeId(BaseTypeId.fromValue(baseId));
		}
		tdf.setParentTypeId(getString(json, "parentId"));
		tdf.setIsCreatable(getBoolean(json, "creatable"));
		tdf.setIsFileable(getBoolean(json, "fileable"));
		tdf.setIsQueryable(getBoolean(json, "queryable"));
		tdf.setIsFulltextIndexed(getBoolean(json, "fulltextIndexed"));
		tdf.setIsIncludedInSupertypeQuery(getBoolean(json, "includedInSupertypeQuery"));
		tdf.setIsControllablePolicy(getBoolean(json, "controllablePolicy"));
		tdf.setIsControllableAcl(getBoolean(json, "controllableACL"));
		
		TypeMutabilityImpl typeMutability = new TypeMutabilityImpl();
		JsonNode tmJson = json.get("typeMutability");
		typeMutability.setCanCreate(getBoolean(tmJson, "create"));
		typeMutability.setCanUpdate(getBoolean(tmJson, "update"));
		typeMutability.setCanDelete(getBoolean(tmJson, "delete"));
		tdf.setTypeMutability(typeMutability);
		
		//TODO property definitions
		Map<String, PropertyDefinition<?>> newPropertyDefinitions = new HashMap<String, PropertyDefinition<?>>();
		JsonNode propertyDefinitions = json.get("propertyDefinitions");
		if(propertyDefinitions != null){
			Iterator<JsonNode> itr = propertyDefinitions.iterator();
			while(itr.hasNext()){
				JsonNode propertyDefinition = itr.next();
				PropertyDefinition<?> newPropertyDefinition = parseProperty(propertyDefinition);
				newPropertyDefinitions.put(newPropertyDefinition.getId(), newPropertyDefinition);
			}
		
			tdf.setPropertyDefinitions(newPropertyDefinitions);
		}
		
	}
	
	private static TypeDefinition parseDocumentType(JsonNode json){
		DocumentTypeDefinitionImpl pdf = new DocumentTypeDefinitionImpl();
		parseCommonType(pdf, json);
		
		pdf.setIsVersionable(getBoolean(json, "versionable"));
		String contentStreamAllowed = getString(json, "contentStreamAllowed");
		if(contentStreamAllowed != null){
			pdf.setContentStreamAllowed(ContentStreamAllowed.fromValue(contentStreamAllowed));
		}
		
		return pdf;
	}
	
	private static TypeDefinition parseFolder(JsonNode json){
		FolderTypeDefinitionImpl pdf = new FolderTypeDefinitionImpl();
		parseCommonType(pdf, json);
		return pdf;
	}
	
	private static TypeDefinition parseItem(JsonNode json){
		ItemTypeDefinitionImpl pdf = new ItemTypeDefinitionImpl();
		parseCommonType(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseProperty(JsonNode json){
		PropertyType propertyType =PropertyType.fromValue(json.get("propertyType").textValue());
		switch(propertyType){
		 case BOOLEAN:
			 return parseBooleanProperty(json);
		 case DATETIME:
			 return parseDateTimeProperty(json);
		 case DECIMAL:
			 return parseDecimalProperty(json);
		 case HTML:
			 return parseHtmlProperty(json);
		 case ID:
			 return parseIdProperty(json);
		 case INTEGER:
			 return parseIntegerProperty(json);
		 case STRING:
			 return parseStringProperty(json);
		 case URI:
			 return parseUriProperty(json);
		}
		
		return null;
	}
	
	private static void parseCommon(AbstractPropertyDefinition<?> pdf, JsonNode json){
		pdf.setId(getString(json, "id"));
		pdf.setLocalName(getString(json, "localName"));
		
		
		
		//TODO
		pdf.setLocalNamespace(getString(json, "localNamespace"));
		pdf.setDisplayName(getString(json, "displayName"));
		pdf.setQueryName(getString(json, "queryName"));
		pdf.setDescription(getString(json, "description"));
		String cardinality = getString(json, "cardinality");
		if(cardinality != null){
			pdf.setCardinality(Cardinality.fromValue(cardinality));
		}
		String updatability = getString(json, "updatability");
		if(updatability != null){
			pdf.setUpdatability(Updatability.fromValue(updatability));
		}
		pdf.setIsInherited(getBoolean(json, "inherited"));
		pdf.setIsRequired(getBoolean(json, "required"));
		pdf.setIsQueryable(getBoolean(json, "queryable"));
		pdf.setIsOrderable(getBoolean(json, "orderable"));
		pdf.setIsOpenChoice(getBoolean(json, "openChoice"));
		
		
		
		//TODO defaultValues, choices
	}
	
	private static PropertyDefinition<?> parseBooleanProperty(JsonNode json){
		PropertyBooleanDefinitionImpl pdf = new PropertyBooleanDefinitionImpl();
		pdf.setPropertyType(PropertyType.BOOLEAN);
		setDefaultValue(pdf, json);
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseDateTimeProperty(JsonNode json){
		PropertyDateTimeDefinitionImpl pdf = new PropertyDateTimeDefinitionImpl();
		pdf.setPropertyType(PropertyType.DATETIME);
		setDefaultValue(pdf, json);
		
		if(json.get("dateTimeResolution") != null){
			String resolution = json.get("dateTimeResolution").textValue();
			pdf.setDateTimeResolution(DateTimeResolution.fromValue(resolution));
		}
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseDecimalProperty(JsonNode json){
		PropertyDecimalDefinitionImpl pdf = new PropertyDecimalDefinitionImpl();
		pdf.setPropertyType(PropertyType.DECIMAL);
		setDefaultValue(pdf, json);
		
		pdf.setMaxValue(parseBigDecimal(json, "maxValue"));
		pdf.setMinValue(parseBigDecimal(json, "minValue"));
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseHtmlProperty(JsonNode json){
		PropertyHtmlDefinitionImpl pdf = new PropertyHtmlDefinitionImpl();
		pdf.setPropertyType(PropertyType.HTML);
		setDefaultValue(pdf, json);
		
		parseCommon(pdf, json);
		return pdf;
	}

	private static PropertyDefinition<?> parseIdProperty(JsonNode json){
		PropertyIdDefinitionImpl pdf = new PropertyIdDefinitionImpl();
		pdf.setPropertyType(PropertyType.ID);
		setDefaultValue(pdf, json);
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseIntegerProperty(JsonNode json){
		PropertyIntegerDefinitionImpl pdf = new PropertyIntegerDefinitionImpl();
		pdf.setPropertyType(PropertyType.INTEGER);
		setDefaultValue(pdf, json);
		
		pdf.setMaxValue(parseBigInteger(json, "maxValue"));
		pdf.setMinValue(parseBigInteger(json, "minValue"));
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseStringProperty(JsonNode json){
		PropertyStringDefinitionImpl pdf = new PropertyStringDefinitionImpl();
		pdf.setPropertyType(PropertyType.STRING);
		setDefaultValue(pdf, json);
		
		pdf.setMaxLength(parseBigInteger(json, "maxLength"));
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static PropertyDefinition<?> parseUriProperty(JsonNode json){
		PropertyUriDefinitionImpl pdf = new PropertyUriDefinitionImpl();
		pdf.setPropertyType(PropertyType.URI);
		setDefaultValue(pdf, json);
		
		parseCommon(pdf, json);
		return pdf;
	}
	
	private static BigInteger parseBigInteger(JsonNode json, String key){
		JsonNode node = json.get(key);
		if(node == null){
			return null;
		}else{
			String str = node.textValue();
			try{
				BigInteger result = BigInteger.valueOf(Long.valueOf(str));
				return result;
			}catch(Exception e){
				//TODO logging
				e.printStackTrace();
				return null;
			}
		}
	}
	
	private static BigDecimal parseBigDecimal(JsonNode json, String key){
		JsonNode node = json.get(key);
		if(node == null){
			return null;
		}else{
			String str = node.textValue();
			try{
				BigDecimal result = BigDecimal.valueOf(Long.valueOf(str));
				return result;
			}catch(Exception e){
				//TODO logging
				e.printStackTrace();
				return null;
			}
		}
	}
	
	private static String getString(JsonNode json, String key){
		JsonNode node = json.get(key);
		if(node == null){
			return null;
		}else{
			return node.textValue();
		}
	}
	
	private static Boolean getBoolean(JsonNode json, String key){
		JsonNode node = json.get(key);
		if(node == null){
			return null;
		}else{
			return node.asBoolean();
		}
	}
	
	private static void setDefaultValue(AbstractPropertyDefinition<?> pdf, JsonNode json){
		JsonNode defaultValue = json.get("defaultValue");
		if(defaultValue != null && defaultValue.isArray()){
			Iterator<JsonNode> itr = defaultValue.iterator();
			
			if(pdf instanceof PropertyIdDefinitionImpl){
				if(((PropertyIdDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyIdDefinitionImpl)pdf).setDefaultValue(new ArrayList<String>());
				}
				while(itr.hasNext()){
					((PropertyIdDefinitionImpl)pdf).getDefaultValue().add(itr.next().asText());
				}
			}else if(pdf instanceof PropertyHtmlDefinitionImpl){
				if(((PropertyHtmlDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyHtmlDefinitionImpl)pdf).setDefaultValue(new ArrayList<String>());
				}
				while(itr.hasNext()){
					((PropertyHtmlDefinitionImpl)pdf).getDefaultValue().add(itr.next().asText());
				}
			}else if(pdf instanceof PropertyUriDefinitionImpl){
				if(((PropertyUriDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyUriDefinitionImpl)pdf).setDefaultValue(new ArrayList<String>());
				}
				while(itr.hasNext()){
					((PropertyUriDefinitionImpl)pdf).getDefaultValue().add(itr.next().asText());
				}
			}else if(pdf instanceof PropertyStringDefinitionImpl){
				if(((PropertyStringDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyStringDefinitionImpl)pdf).setDefaultValue(new ArrayList<String>());
				}
				while(itr.hasNext()){
					((PropertyStringDefinitionImpl)pdf).getDefaultValue().add(itr.next().asText());
				}
			}else if(pdf instanceof PropertyBooleanDefinitionImpl){
				if(((PropertyBooleanDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyBooleanDefinitionImpl)pdf).setDefaultValue(new ArrayList<Boolean>());
				}
				while(itr.hasNext()){
					((PropertyBooleanDefinitionImpl)pdf).getDefaultValue().add(itr.next().asBoolean());
				}
			}else if(pdf instanceof PropertyDateTimeDefinitionImpl){
				if(((PropertyDateTimeDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyDateTimeDefinitionImpl)pdf).setDefaultValue(new ArrayList<GregorianCalendar>());
				}
				while(itr.hasNext()){
					String str = itr.next().textValue();
					//TODO other locale, other formats
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.JAPAN);
					Date d;
					try {
						d = sdf.parse(str);
						GregorianCalendar cal = new GregorianCalendar();
						cal.setTime(d); 
						((PropertyDateTimeDefinitionImpl)pdf).getDefaultValue().add(cal);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}else if(pdf instanceof PropertyIntegerDefinitionImpl){
				if(((PropertyIntegerDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyIntegerDefinitionImpl)pdf).setDefaultValue(new ArrayList<BigInteger>());
				}
				while(itr.hasNext()){
					Long i = itr.next().asLong();
					BigInteger bi = BigInteger.valueOf(i);
					((PropertyIntegerDefinitionImpl)pdf).getDefaultValue().add(bi);
				}
			}else if(pdf instanceof PropertyDecimalDefinitionImpl){
				if(((PropertyDecimalDefinitionImpl)pdf).getDefaultValue() == null){
					((PropertyDecimalDefinitionImpl)pdf).setDefaultValue(new ArrayList<BigDecimal>());
				}
				while(itr.hasNext()){
					Double d = itr.next().asDouble();
					BigDecimal bd = BigDecimal.valueOf(d);
					((PropertyDecimalDefinitionImpl)pdf).getDefaultValue().add(bd);
				}
			}
		}
	}
}

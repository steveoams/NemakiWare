package controllers;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

public class Archive extends Controller{
	public static Result index(String repositoryId) {
		//List<CmisObject> list = new ArrayList<CmisObject>();
		
		Promise<WSResponse> result = WS.url("http://localhost:8080/core/rest/repo/bedroom/archive/index").setAuth("admin", "admin").get();
		WSResponse response = result.get(5000);
		JsonNode json = response.asJson();
		ArrayNode archives =  (ArrayNode) json.get("archives");
		Iterator<JsonNode> itr = archives.iterator();
		List<model.Archive> list = new ArrayList<model.Archive>();
		while(itr.hasNext()){
			ObjectNode archiveJson = (ObjectNode)(itr.next());
			model.Archive archive = new model.Archive(archiveJson);
			list.add(archive);
		}
		
		return ok(views.html.archive.index.render(repositoryId, list));
		
	}
}

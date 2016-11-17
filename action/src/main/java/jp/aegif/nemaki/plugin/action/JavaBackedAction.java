package jp.aegif.nemaki.plugin.action;


import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.springframework.plugin.core.Plugin;


public interface JavaBackedAction extends Plugin<String>{

	public String getActionTile();

	public String getActionDiscription();

	public ActionTriggerBase getActionTrigger(ActionContext actionContext);

	public boolean canExecute(ActionContext actionContext, ObjectData obj);

	public String executeAction(ActionContext actionContext, ObjectData obj, String json);




}

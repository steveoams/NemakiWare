package model;

public class ActionPluginUIElement {

	public ActionPluginUIElement(){

	}


	private String _actionId;
	public String getActionId(){
		return _actionId;
	}
	public void setActionId(String value){
		_actionId = value;
	}

	private String _displayName;
	public String getDisplayName(){
		return _displayName;
	}
	public void setDisplayName(String value){
		_displayName = value;
	}

	private String _fontAwesomeName;
	public String getFontAwesomeName(){
		return _fontAwesomeName;
	}

	public void setFontAwesomeName(String value){
		_fontAwesomeName = value;
	}

	private String _formHtml;
	public String getFormHtml(){
		return _formHtml;
	}
	public void setFormHtml(String value){
		 _formHtml = value;
	}
	
	private boolean advancedAction = false;
	public boolean isAdvancedAction() {
		return advancedAction;
	}
	public void setAdvancedAction(boolean advanced) {
		advancedAction = advanced;
	}

}

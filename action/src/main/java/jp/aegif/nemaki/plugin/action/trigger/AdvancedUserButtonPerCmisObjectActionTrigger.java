package jp.aegif.nemaki.plugin.action.trigger;

public class AdvancedUserButtonPerCmisObjectActionTrigger extends UserButtonPerCmisObjcetActionTrigger {

	
	
	public AdvancedUserButtonPerCmisObjectActionTrigger(String displayName) {
		super(displayName);
	}

	private String _html;
	
	public void setHtml(String html) {
		this._html = html;
	}
	
	public String getHtml() {
		return _html;
	}
	
}

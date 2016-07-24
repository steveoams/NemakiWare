package jp.aegif.nemaki.test.nemaki;

import org.apache.chemistry.opencmis.client.api.Session;
import org.junit.BeforeClass;

public class TestBaseWithoutSandbox {
	protected static Session session;
	
	@BeforeClass
	public static void before() throws Exception {
		session = SessionUtil.createCmisSession("bedroom", "admin", "admin");
	}
}

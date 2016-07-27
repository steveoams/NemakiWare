package jp.aegif.nemaki.test.nemaki;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;

public class SessionUtil {
	
	public static Session createCmisSession(String repositoryId, String userId, String password) throws Exception{
		Properties testConfig = readProperties();
		
		Map<String, String> parameter = new HashMap<String, String>();
		// user credentials
		parameter.put(SessionParameter.USER, testConfig.getProperty("server.auth.user"));
		parameter.put(SessionParameter.PASSWORD, testConfig.getProperty("server.auth.password"));

		// session locale
		parameter.put(SessionParameter.LOCALE_ISO3166_COUNTRY, "");
		parameter.put(SessionParameter.LOCALE_ISO639_LANGUAGE, "");

		// repository
		parameter.put(SessionParameter.REPOSITORY_ID, repositoryId);
		//parameter.put(org.apache.chemistry.opencmis.commons.impl.Constants.PARAM_REPOSITORY_ID, NemakiConfig.getValue(PropertyKey.NEMAKI_CORE_URI_REPOSITORY));

		parameter. put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

		String atompubUri = atompubUri(testConfig);
		parameter.put(SessionParameter.ATOMPUB_URL, atompubUri);
		
		//timeout
		//parameter.put(SessionParameter.CONNECT_TIMEOUT, "30000");
		//parameter.put(SessionParameter.READ_TIMEOUT, "30000");
		
		
		//parameter.put(SessionParameter.HTTP_INVOKER_CLASS, "org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker");

    	SessionFactory f = SessionFactoryImpl.newInstance();
    	Session session = f.createSession(parameter);
    	OperationContext operationContext = session.createOperationContext(null,
				true, true, false, IncludeRelationships.BOTH, null, false, null, true, 100);
		session.setDefaultContext(operationContext);
		
		return session;
	}
	
	private static Properties readProperties() throws Exception{
		Properties properties = new Properties();

        String file = "neamki-test-parameters.properties";
        InputStream inputStream = new FileInputStream(file);
        properties.load(inputStream);
        return properties;
	}
	
	private static String atompubUri(Properties testConfig){
		String server = testConfig.getProperty("server.endpoint.atompub");
		String repository = testConfig.getProperty("server.endpoint.repository");
		
		if(!server.endsWith("/")){
			server += "/";
		}
		return server + repository;
	}
}

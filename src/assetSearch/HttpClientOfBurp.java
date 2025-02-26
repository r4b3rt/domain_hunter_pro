package assetSearch;

import java.net.URL;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;


public class HttpClientOfBurp {

	public static IHttpService getHttpService(URL url) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();

		int port = url.getPort();
		if ( port ==-1) {
			if (url.getProtocol().equalsIgnoreCase("http")) {
				port = 80;
			}
			if (url.getProtocol().equalsIgnoreCase("https")) {
				port = 443;
			}
		}
		IHttpService service =helpers.buildHttpService(url.getHost(),port,url.getProtocol());
		return service;
	}

	public static String doRequest(URL url) {
		return doRequest(url,null);
	}

	/**
	 * 
	 * @param url
	 * @param byteRequest
	 * @return response body
	 */
	public static String doRequest(URL url,byte[] byteRequest) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();
		if (byteRequest == null) {
			byteRequest = helpers.buildHttpRequest(url);//GET
		}

		IHttpService service =getHttpService(url);
		IHttpRequestResponse message = callbacks.makeHttpRequest(service, byteRequest);
		HelperPlus getter = new HelperPlus(helpers);
		int code = getter.getStatusCode(message);
		if (code != 200) {
			BurpExtender.getStderr().print(new String(message.getResponse()));
			return "";
		}
		byte[] byteBody = getter.getBody(false, message);
		return new String(byteBody);
	}
}

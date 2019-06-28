package ntx.note.sync;

import java.net.HttpURLConnection;


/**
 * Http POST a set of key/value pairs and receive a JSON object
 * 
 * The JSON object must have { "msg":"string", "status":boolean } fields. 
 * 
 * @author vbraun
 * 
 */
public class HttpPostJson extends HttpPostBase {
	@SuppressWarnings("unused")
	private final static String TAG = "HttpPostJson";

	public HttpPostJson(String url) {
		super(url);
	}

	@Override
	protected Response processServerReply(HttpURLConnection connection) {
		return readJsonReply(connection);		
	}

}









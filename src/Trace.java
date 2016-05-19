import java.io.IOException;
import javax.websocket.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Trace {
	private Session session;
	private boolean last;
	private int id;
	
	Trace(Session session, boolean last, int id) {
		this.session = session;
		this.last = last;
		this.id = id;
	}
	
	public boolean sendMessage(JSONArray message) {
		try {
			JSONObject outgoing = new JSONObject();
			outgoing.put("id", this.id);
			outgoing.put("content", message);
			
			String stringified = outgoing.toString();
			System.out.println("Outgoing message:\n\t" + stringified);
			session.getBasicRemote().sendText(stringified, last);
			return true;
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			return false;
		}	
	}
}
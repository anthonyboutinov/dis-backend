
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.json.*;

@ServerEndpoint("/sharedwebsocket")
public class WebSocket {
	
	private Connection connection = null;
	
	
	public WebSocket() {
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/dis","editor", "password");
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	@OnMessage
    public void main(Session session, String msg, boolean last) {
        try {
        	System.out.println(session);
            if (session.isOpen()) {
            	
            	JSONObject msgAsJson = new JSONObject(msg);
            	System.out.println("msg received:\n\t" + msg);
            	
            	int id = msgAsJson.getInt("id");
            	Trace trace = new Trace(session, last, id);
            	
            	String action = msgAsJson.getString("action");
            	if (action.equals("subscribe")) {
            		subscribe(msgAsJson.getJSONObject("to"), trace);
            	} else if (action.equals("unsubscribe")) {
            		unsubscribe(msgAsJson.getJSONObject("from"), trace);
            	}
            	
            }
            
        } catch (JSONException | SQLException e) {
            try {
                session.close();
            } catch (IOException e1) {
                // Ignore
            }
        }
    }
		
	private void subscribe(JSONObject to, Trace trace) throws JSONException, SQLException {
		if (to.getString("dataKind").equals("config")) {
			subscribeToConfig(to, trace);
		}
	}
	
	private void unsubscribe(JSONObject from, Trace trace) {
		
	}
	
	private void subscribeToConfig(JSONObject to, Trace trace) throws JSONException, SQLException {
		JSONArray list = to.getJSONArray("list");
		
		int length = list.length();
		for (int i = 0; i < length; i++) {
			JSONObject query = list.getJSONObject(i);
			String portletId = query.getString("portletId");
			String configName = query.getString("configName");
			String hash = query.getString("hash");
			
			System.out.println("portletId=" + portletId + ", configName=" + configName + ", hash=" + hash);
			
			PreparedStatement st = connection.prepareStatement("SELECT \"ID_ANGULAR_CONFIG\", \"DATA\" FROM \"ANGULAR_CONFIG\" WHERE \"ID_PAGE\" = ? AND \"NAME\" = ?");
			st.setString(1, portletId);
			st.setString(2, configName);
			System.out.println(st.toString());
			try {
			ResultSet rs = st.executeQuery();
			
			boolean rsNotEmpty = rs.next();
			if (rsNotEmpty == true)
			{
			   String rsDataRaw = rs.getString(2);
			   JSONObject rsData = new JSONObject(rsDataRaw);
			   System.out.println("SELECT result: " + rsDataRaw);
			   
			   // start composing answer (outgoing)
			   
			   JSONObject outgoing = new JSONObject();
			   outgoing.put("query", query);
			   
			   String rsHash = String.valueOf(rsDataRaw.hashCode());
			   
			   if (rsHash.equals(hash)) {
				   outgoing.put("content", "alreadyUpToDate");
			   } else {
				   JSONObject content = new JSONObject();
				   content.put("DATA", rsData);
				   content.put("HASH", rsHash);
				   outgoing.put("content", content);
			   }
			   
			   // finish composing answer
			   
			   // send answer
			   JSONArray outgoingAsAnArray = new JSONArray();
			   outgoingAsAnArray.put(outgoing);
			   trace.sendMessage(outgoingAsAnArray);
			   
			} else {
				// send answer: NO results
				JSONObject outgoing = new JSONObject();
				outgoing.put("query", query);
				outgoing.put("content", "negative");
				JSONArray outgoingAsAnArray = new JSONArray();
				outgoingAsAnArray.put(outgoing);
				trace.sendMessage(outgoingAsAnArray);
			}
			rs.close();
			} catch (SQLException e) {
				System.out.println("subscribeToConfig: SQLException");
				e.printStackTrace();
			}
			st.close();
			
		}
	}
	
}

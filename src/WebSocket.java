
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
	
	
	WebSocket() {
		try {
			connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/dis","postgres", "postgres");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	@OnMessage
    public void main(Session session, String msg, boolean last) {
        try {
        	System.out.println(session);
            if (session.isOpen()) {
            	
            	JSONObject msgAsJson = new JSONObject(msg);
            	System.out.println(msgAsJson);
            	
            	String action = msgAsJson.getString("action");
            	int id = msgAsJson.getInt("id");
            	Trace trace = new Trace(session, last, id);
            	
            	if (action == "subscribe") {
            		subscribe(msgAsJson.getJSONObject("to"), trace);
            	} else if (action == "unsubscribe") {
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
		if (to.getString("dataKind") == "config") {
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
			
			PreparedStatement st = connection.prepareStatement("SELECT ID_ANGULAR_CONFIG, DATA FROM ANGULAR_CONFIG WHERE ID_PAGE = ? AND NAME = ?");
			st.setString(1, portletId);
			st.setString(2, configName);
			ResultSet rs = st.executeQuery();
			while (rs.next())
			{
			   String rsDataRaw = rs.getString(2);
			   JSONObject rsData = new JSONObject(rsDataRaw);
			   
			   // start composing answer (outgoing)
			   
			   JSONObject outgoing = new JSONObject();
			   outgoing.put("query", query);
			   
			   String rsHash = String.valueOf(rsDataRaw.hashCode());
			   
			   if (rsHash == hash) {
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
			}
			rs.close();
			st.close();
			
		}
	}
	
}

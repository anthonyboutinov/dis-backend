
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
            	if (action == "subscribe") {
            		subscribe(msgAsJson.getJSONObject("to"), id);
            	} else if (action == "unsubscribe") {
            		unsubscribe(msgAsJson.getJSONObject("from"), id);
            	}
            	
//                session.getBasicRemote().sendText("OK", last);
            }
            
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException e1) {
                // Ignore
            }
        } catch (JSONException e) {
        	e.printStackTrace();
        	try {
                session.close();
            } catch (IOException e1) {
            	// Ignore
            }
		} catch (SQLException e) {
			e.printStackTrace();
			try {
                session.close();
            } catch (IOException e1) {
            	// Ignore
            }
		}
    }
		
	private void subscribe(JSONObject to, int id) throws JSONException, SQLException {
		if (to.getString("dataKind") == "config") {
			subscribeToConfig(to, id);
		}
	}
	
	private void unsubscribe(JSONObject from, int id) {
		
	}
	
	private void subscribeToConfig(JSONObject to, int id) throws JSONException, SQLException {
		JSONArray list = to.getJSONArray("list");
		
		int length = list.length();
		for (int i = 0; i < length; i++) {
			JSONObject item = list.getJSONObject(i);
			String portletId = item.getString("portletId");
			String configName = item.getString("configName");
			String hash = item.getString("hash");
			
			PreparedStatement st = connection.prepareStatement("SELECT ID_ANGULAR_CONFIG, DATA FROM ANGULAR_CONFIG WHERE ID_PAGE = ? AND NAME = ?");
			st.setString(1, portletId);
			st.setString(2, configName);
			ResultSet rs = st.executeQuery();
			while (rs.next())
			{
//			   System.out.print("Column 1 returned ");
//			   System.out.println(rs.getString(1));
			   String rsDataRaw = rs.getString(2);
			   JSONObject rsData = new JSONObject(rsDataRaw);
			   
			   String rsHash = String.valueOf(rsDataRaw.hashCode());
			   
			   if (rsHash == hash) {
				   
				   
			   }
			   
			   String outgoing = "";
			   session.getBasicRemote().sendText(outgoing, last);
			}
			rs.close();
			st.close();
			
		}
	}
	
}

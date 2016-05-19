
import java.io.IOException;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/sharedwebsocket")
public class WebSocket {

	@OnMessage
    public void main(Session session, String msg, boolean last) {
        try {
        	System.out.println(session);
            if (session.isOpen()) {
            	System.out.println(msg);
                session.getBasicRemote().sendText(msg, last);
            }
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException e1) {
                // Ignore
            }
        }
    }
	
}

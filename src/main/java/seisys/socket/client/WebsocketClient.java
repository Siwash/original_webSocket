package seisys.socket.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

public class WebsocketClient {
	
    private static Logger logger = Logger.getLogger(WebsocketClient.class);
    public static WebSocketClient client;
    public static void main(String[] args) {
        try {
            client = new WebSocketClient(new URI("ws://localhost:8080/Socket-demo/websocket/socketServer.do"),new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                	 logger.info("���ֳɹ�");
                }
 
                @Override
                public void onMessage(String msg) {
                	 logger.info("�յ���Ϣ=========="+msg);
                	 if(msg.equals("over")){
                		 client.close();
                	 }
                	 
                }
 
                @Override
                public void onClose(int i, String s, boolean b) {
                	 logger.info("�����ѹر�");
                }
 
                @Override
                public void onError(Exception e){
                    e.printStackTrace();
                    logger.info("���������ѹر�");
                }
            };
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
 
        client.connect();
        logger.info(client.getDraft());
       while(!client.getReadyState().equals(WebSocket.READYSTATE.OPEN)){
    	   logger.info("��������...");
        }
       //���ӳɹ�,������Ϣ
	client.send("���,����һ�°�");
       
    }
	
}


# original_webSocket
原始的websocket实现方式
#WebSocket入门篇
------
> * ### 背景介绍
>      最近突然发现有一种**http协议**不能很好的满足的一种需求。具体来说就是，页面的某个组件需要动态的根据数据库中的内容的更新而即时的刷新出来。而传统的做法无论是轮询还是长连接对性能来说都不是很友好。比如说如果我前端用轮询/长连接，而后台数据库这边又得去轮询数据中的更新记录，实在是忍不了啊。
###有没有一种方式既可以在数据库更新数据的时候去告诉后端来取数据了(或者直接把更新的内容推送到后端)，又可以让后端把处理ok的数据再直接推送到后端呢？

机制的你是不是直接联想到了一种叫[socket](https://baike.baidu.com/item/socket/281150)的技术呢。没错现在的socket也能直接再H5上使用了，只不过加了个前缀叫websocket，是H5的新增的一种支持协议，个人觉得和socket一样都是**全双工**通信的方式，就是**你不理我，我也可以来理你**。

-----
##websocket简单实现
> * ####首先搭建一个SpringMVC的项目，各种配置好，可以简单在网页上实现hello word就足矣

###首先是后端的配置

为更加可控需要自定义一个websocket拦截器和websocket的握手器，用于处理连接的过滤和具体数据交互细节
####拦截器

创建拦截器继承`HttpSessionHandshakeInterceptor`，具体如下：
```

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket拦截器
 *
 *
 */
public class SpringWebSocketHandlerInterceptor extends HttpSessionHandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("Before Handshake");
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                //使用userName区分WebSocketHandler，以便定向发送消息
                String userName = (String) session.getAttribute("SESSION_USERNAME");
                if (userName==null) {
                    userName="default-system";
                }
                attributes.put("WEBSOCKET_USERNAME",userName);
            }
        }
        return super.beforeHandshake(request, response, wsHandler, attributes);
        
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception ex) {
        // TODO Auto-generated method stub
        super.afterHandshake(request, response, wsHandler, ex);
    }
}

```
####握手器，确定具体的握手细节，代码：
```

import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class SpringWebSocketHandler extends TextWebSocketHandler {
	private static final ArrayList<WebSocketSession> users;//这个会出现性能问题，最好用Map来存储，key用userid
    private static Logger logger = Logger.getLogger(SpringWebSocketHandler.class);
    static {
        users = new ArrayList<WebSocketSession>();
    }
    
    public SpringWebSocketHandler() {
        // TODO Auto-generated constructor stub
    }

    /**
     * 连接成功时候，会触发页面上onopen方法
     */
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("connect to the websocket success......当前数量:"+users.size());
        users.add(session);
        //这块会实现自己业务，比如，当用户登录后，会把离线消息推送给用户
        //TextMessage returnMessage = new TextMessage("你将收到的离线");
        //session.sendMessage(returnMessage);
    }
    
    /**
     * 关闭连接时触发
     */
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.debug("websocket connection closed......");
        String username= (String) session.getAttributes().get("WEBSOCKET_USERNAME");
        System.out.println("用户"+username+"已退出！");
        users.remove(session);
        System.out.println("剩余在线用户"+users.size());
    }

    /**
     * js调用websocket.send时候，会调用该方法
     */
    @Override    
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    	System.err.println(message.toString());
    	String username = (String) session.getAttributes().get("WEBSOCKET_USERNAME");
        // 获取提交过来的消息详情
    	logger.info("收到用户 " + username + "的消息:" + message.toString());
    	//super.handleTextMessage(session, message);
    	
    	session.sendMessage(new TextMessage("reply msg:" + message.getPayload()));
    	sendMessageToUsers(message);
    }

    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if(session.isOpen()){session.close();}
        logger.debug("websocket connection closed......");
        users.remove(session);
    }

    public boolean supportsPartialMessages() {
        return false;
    }
    
    
    /**
     * 给某个用户发送消息
     *
     * @param userName
     * @param message
     */
    public void sendMessageToUser(String userName, TextMessage message) {
        for (WebSocketSession user : users) {
            if (user.getAttributes().get("WEBSOCKET_USERNAME").equals(userName)) {
                try {
                    if (user.isOpen()) {
                        user.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
    
    /**
     * 给所有在线用户发送消息
     *
     * @param message
     */
    public void sendMessageToUsers(TextMessage message) {
        for (WebSocketSession user : users) {
            try {
                if (user.isOpen()) {
                    user.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

```
####最后设置好连接wbsocket端点，代码：
```

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebMvc
@EnableWebSocket
public class SpringWebSocketConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer{

	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		 registry.addHandler(webSocketHandler(),"/websocket/socketServer.do").addInterceptors(new SpringWebSocketHandlerInterceptor());
	        registry.addHandler(webSocketHandler(), "/sockjs/socketServer.do").addInterceptors(new SpringWebSocketHandlerInterceptor()).withSockJS();
		
	}
	@Bean
	public TextWebSocketHandler webSocketHandler(){
        return new SpringWebSocketHandler();
    }

}

```
> * ####由于使用了很多spring的注解比如`@Configuration，@EnableWebMvc，@EnableWebSocket`，因此需要确定springmvc的配置中加入了：
> - `<mvc:annotation-driven/>`
>- `<context:component-scan base-package="你得配置路径"/>`
###controller 层连接到websocket的代码：
```
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.socket.TextMessage;
@Controller
@RequestMapping("/")
public class SignlController {
	@Bean//这个注解会从Spring容器拿出Bean
    public SpringWebSocketHandler infoHandler() {
        return new SpringWebSocketHandler();
    }
	@RequestMapping("start")
	public String start(HttpServletRequest request,HttpServletResponse response) {
		
		return "index";
	}
	@RequestMapping("/websocket/login")
    public ModelAndView login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String username = request.getParameter("username");
        System.out.println(username+"登录");
        HttpSession session = request.getSession(false);
        session.setAttribute("SESSION_USERNAME", username);
        //response.sendRedirect("/quicksand/jsp/websocket.jsp");
        return new ModelAndView("shareMsg");
    }
	
    @RequestMapping("send")
    public String send(HttpServletRequest request) {
        String username = request.getParameter("username");
        System.out.println(username);
        infoHandler().sendMessageToUser(username, new TextMessage("你好，测试！！！！"));
        return "shareMsg";
    }
}

```
###最后贴出依赖jar包，pom.xml:
```
<dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
    <!-- https://mvnrepository.com/artifact/org.springframework/spring-context -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>${spring.version}</version>
	</dependency>
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-web -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>${spring.version}</version>
	</dependency>
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-webmvc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>${spring.version}</version>
	</dependency>
	<dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-websocket</artifactId>
      <version>${spring.version}</version>
    </dependency>
	<dependency>  
	  <groupId>org.springframework</groupId>  
	  <artifactId>spring-messaging</artifactId>  
	  <version>${spring.version}</version>
	  <scope>provided</scope>  
	</dependency>
	<!-- https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.0</version>
    <scope>compile</scope>
</dependency>
	<dependency>
<groupId>javax.servlet</groupId>
<artifactId>jstl</artifactId>
<version>1.2</version>
</dependency>
<dependency>
<groupId>taglibs</groupId>
<artifactId>standard</artifactId>
<version>1.1.2</version>
</dependency>
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.3.5</version>
</dependency>
<dependency>
  <groupId>log4j</groupId>
     <artifactId>log4j</artifactId>
    <version>1.2.15</version>
    <scope>runtime</scope>
 </dependency>
   <dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-core</artifactId>
			<version>2.1.0</version>
		</dependency>
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-databind</artifactId>
			<version>2.1.0</version>
		</dependency>
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-annotations</artifactId>
			<version>2.1.0</version>
		</dependency>
  </dependencies>
  <properties>
  	<maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <spring.version>4.2.5.RELEASE</spring.version>
  </properties>
```
####最后是前端jsp的hello websocket的测试代码：

登陆的jsp，用于你想websocket连接内的某个用户发送消息
```
<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<body>
<h2>Hello World!</h2>
<body>
    <!-- ship是我的项目名-->
    <form action="websocket/login.do">
        登录名：<input type="text" name="username"/>
        <input type="submit" value="登录"/>
    </form>
</body>
</body>
</html>
```
发送消息和接收消息的jsp代码：
```
<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Insert title here</title>
</head>
<body>
<script type="text/javascript" src="http://cdn.bootcss.com/jquery/3.1.0/jquery.min.js"></script>
<script type="text/javascript" src="http://cdn.bootcss.com/sockjs-client/1.1.1/sockjs.js"></script>
<script type="text/javascript">
    var websocket = null;
    if ('WebSocket' in window) {
        websocket = new WebSocket("ws://localhost:8080/Socket-demo/websocket/socketServer.do");
    } 
    else if ('MozWebSocket' in window) {
        websocket = new MozWebSocket("ws://localhost:8080/Socket-demo/websocket/socketServer.do");
    } 
    else {
        websocket = new SockJS("http://localhost:8080/Socket-demo/sockjs/socketServer.do");
    }
    websocket.onopen = onOpen;
    websocket.onmessage = onMessage;
    websocket.onerror = onError;
    websocket.onclose = onClose;
              
    function onOpen(openEvt) {
        //alert(openEvt.Data);
    }
    
    function onMessage(evt) {
        alert(evt.data);
    }
    function onError() {}
    function onClose() {}
    
    function doSend() {
        if (websocket.readyState == websocket.OPEN) {          
            var msg = document.getElementById("inputMsg").value;  
            websocket.send(msg);//调用后台handleTextMessage方法
            alert("发送成功!");  
        } else {  
            alert("连接失败!");  
        }  
    }

　　　window.close=function()
	　　　{
		　　　　　websocket.onclose();
	　　　}
	
</script>
请输入：<textarea rows="5" cols="10" id="inputMsg" name="inputMsg"></textarea>
<button onclick="doSend();">发送</button>
<form action="send.c">
      指定发送：<input type="text" name="username"/>
        <input type="submit" value="确定"/>
    </form>
</body>
</html>
```
> 最后把完整的项目代码上传到了我的[GitHup](https://github.com/Siwash/original_webSocket)
----
####最后再贴一个java的websocket客户端的代码，用于数据库主动推送消息到后台/或者前台
```

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
                	 logger.info("握手成功");
                }
 
                @Override
                public void onMessage(String msg) {
                	 logger.info("收到消息=========="+msg);
                	 if(msg.equals("over")){
                		 client.close();
                	 }
                	 
                }
 
                @Override
                public void onClose(int i, String s, boolean b) {
                	 logger.info("链接已关闭");
                }
 
                @Override
                public void onError(Exception e){
                    e.printStackTrace();
                    logger.info("发生错误已关闭");
                }
            };
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
 
        client.connect();
        logger.info(client.getDraft());
       while(!client.getReadyState().equals(WebSocket.READYSTATE.OPEN)){
    	   logger.info("正在连接...");
        }
       //连接成功,发送信息
	client.send("哈喽,连接一下啊");
       
    }
	
}


```

###结语：通过websocket技术你可以实现后台主动推送消息到前端，以提高数据的实时性，同时你也可以通过websocket的java客户端，在数据库中通过触发器去调它，实现数据库推送消息到后台/前台。但是这种方式缺点在于：对于不同的用户推送不同的消息，需要手动去写逻辑和指定规则在后台的握手器中实现，较为麻烦。

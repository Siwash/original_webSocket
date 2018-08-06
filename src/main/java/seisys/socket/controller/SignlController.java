package seisys.socket.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.socket.TextMessage;

import seisys.socket.config.SpringWebSocketHandler;

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

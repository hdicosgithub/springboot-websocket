package com.hdicios.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author hdicos
 * @version 1.0
 * @description: TODO
 * @date 2022/5/5 15:45
 */
@Component
@Slf4j
@Service
@ServerEndpoint("/api/websocket/{sid}")
public class WebSocketServer {
    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static  int onlineCount=0;
    //创建线程安全的concurrent的set，用来存放客户端对于的websocket对象
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet=new CopyOnWriteArraySet<>();
    // 与某一个客户端的链接会话，需要通过它来给客户端发送数据
    private Session session;
    // 接受sid
    private String sid="";


    /**
     * 获取在线连接数
     * @return
     */
   public static synchronized int   getOnlineCount(){
       return onlineCount;
   }

    /**
     * 在线连接加1
     */
   public static synchronized void   addOnlineCount(){
       WebSocketServer.onlineCount++;
   }

    /***
     * 在线连接数减1
     */
   public static synchronized void    subOnlineCount(){
       WebSocketServer.onlineCount--;
   }

    /***
     * 获取websocket的对象
     * @return
     */
   public static CopyOnWriteArraySet<WebSocketServer>   getWebSocketSet(){
       return webSocketSet;
   }

    /**
     * 服务器主动推送信息
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 连接建立成功调用的方法
     * @param session
     * @param sid
     */
    @OnOpen
    public  void onOpen(Session session,@PathParam("sid") String sid){
        this.session=session;
        webSocketSet.add(this);// 加入set
        this.sid=sid;
        addOnlineCount();//在线数加10
        try {
            //发送消息
            sendMessage("conn_success");
            log.info("有新窗口开始监听:" + sid + ",当前在线人数为:" + getOnlineCount());
        }catch (IOException e){
            log.error("websocket IO Exception");
        }
    }


    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        //断开连接情况下，更新主板占用情况为释放
        log.info("释放的sid为："+sid);
        //这里写你 释放的时候，要处理的业务
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());

    }
    /**
     * 收到客户端消息后调用的方法
     * @ Param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自窗口" + sid + "的信息:" + message);
        //群发消息
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * @ Param session
     * @ Param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        error.printStackTrace();
    }
    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message, @PathParam("sid") String sid) throws IOException {
        log.info("推送消息到窗口" + sid + "，推送内容:" + message);

        for (WebSocketServer item : webSocketSet) {
            try {
                //这里可以设定只推送给这个sid的，为null则全部推送
                if (sid == null) {
//                    item.sendMessage(message);
                } else if (item.sid.equals(sid)) {
                    item.sendMessage(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

}

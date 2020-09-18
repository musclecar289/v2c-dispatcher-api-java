/*
 * Copyright (c) 2020 V2C Development Team. All rights reserved.
 * Licensed under the Version 0.0.1 of the V2C License (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <https://tinyurl.com/v2c-license>.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions 
 * limitations under the License.
 */
package edu.uco.cs.v2c.dispatcher.api.payload;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import edu.uco.cs.v2c.dispatcher.api.listener.CommandListener;
import edu.uco.cs.v2c.dispatcher.api.listener.ConfigUpdateListener;
import edu.uco.cs.v2c.dispatcher.api.listener.ConnectionCloseListener;
import edu.uco.cs.v2c.dispatcher.api.listener.ConnectionOpenListener;
import edu.uco.cs.v2c.dispatcher.api.listener.MessageListener;
import edu.uco.cs.v2c.dispatcher.api.listener.WebSocketErrorListener;
import edu.uco.cs.v2c.dispatcher.api.payload.incoming.ErrorPayload;
import edu.uco.cs.v2c.dispatcher.api.payload.incoming.InboundConfigUpdatePayload;
import edu.uco.cs.v2c.dispatcher.api.payload.incoming.IncomingPayload;
import edu.uco.cs.v2c.dispatcher.api.payload.incoming.RouteCommandPayload;
import edu.uco.cs.v2c.dispatcher.api.payload.incoming.RouteMessagePayload;
import edu.uco.cs.v2c.dispatcher.api.payload.outgoing.OutgoingPayload;

/**
 * Processes incoming payloads.
 * 
 * @author Caleb L. Power
 */
@WebSocket public class PayloadProcessor {
  
  private Set<CommandListener> commandListeners = new LinkedHashSet<>();
  private Set<ConfigUpdateListener> configUpdateListeners = new LinkedHashSet<>();
  private Set<ConnectionCloseListener> connectionCloseListeners = new LinkedHashSet<>();
  private Set<ConnectionOpenListener> connectionOpenListeners = new LinkedHashSet<>();
  private Set<MessageListener> messageListeners = new LinkedHashSet<>();
  private Set<WebSocketErrorListener> errorListeners = new LinkedHashSet<>();
  private Set<Session> sessions = new CopyOnWriteArraySet<>();
  
  final Set<?> listenersArr[] = new Set<?>[] {
    commandListeners,
    configUpdateListeners,
    connectionCloseListeners,
    connectionOpenListeners,
    messageListeners,
    errorListeners
  };
  
  /**
   * Handles a closing WebSocket.
   * 
   * @param session the session
   * @param statusCode the status code
   * @param reason the reason
   */
  @OnWebSocketClose public void onClose(Session session, int statusCode, String reason) {
    if(sessions.contains(session)) sessions.remove(session);
    new Thread(new Runnable() {
      @Override public void run() {
        synchronized(connectionCloseListeners) {
          for(ConnectionCloseListener listener : connectionCloseListeners)
            listener.onClose(statusCode, reason);
        }
      }
    }).start();
  }
  
  /**
   * Handles an opening WebSocket.
   * 
   * @param session the session
   */
  @OnWebSocketConnect public void onConnect(Session session) {
    sessions.add(session);
    new Thread(new Runnable() {
      @Override public void run() {
        synchronized(connectionOpenListeners) {
          for(ConnectionOpenListener listener : connectionOpenListeners)
            listener.onConnect(session);
        }
      }
    }).start();
  }
  
  /**
   * Handles an incoming message.
   * 
   * @param message the message
   */
  @OnWebSocketMessage public void onMessage(String message) {
    Runnable runnable = null;
    
    try {
      JSONObject json = new JSONObject(message);
      
      switch(IncomingPayload.IncomingAction.valueOf(json.getString("action"))) {
      case ROUTE_COMMAND: {
        RouteCommandPayload payload = new RouteCommandPayload(json);
        runnable = new Runnable() {
          @Override public void run() {
            synchronized(commandListeners) {
              for(CommandListener listener : commandListeners)
                listener.onIncomingCommand(payload);
            }
          }
        };
        
        break;
      }
      
      case ROUTE_MESSAGE: {
        RouteMessagePayload payload = new RouteMessagePayload(json);
        runnable = new Runnable() {
          @Override public void run() {
            synchronized(messageListeners) {
              for(MessageListener listener : messageListeners)
                listener.onIncomingMessage(payload);
            }
          }
        };
        
        break;
      }
      
      case UPDATE_CONFIGURATION: {
        InboundConfigUpdatePayload payload = new InboundConfigUpdatePayload(json);
        runnable = new Runnable() {
          @Override public void run() {
            synchronized(configUpdateListeners) {
              for(ConfigUpdateListener listener : configUpdateListeners)
                listener.onConfigUpdate(payload);
            }
          }
        };
        
        break;
      }
      
      case WEBSOCKET_ERROR: {
        ErrorPayload payload = new ErrorPayload(json);
        runnable = new Runnable() {
          @Override public void run() {
            synchronized(errorListeners) {
              for(WebSocketErrorListener listener : errorListeners)
                listener.onRemoteError(payload);
            }
          }
        };
        break;
      }
      
      default:
        break;
      }
      
    } catch(PayloadHandlingException | JSONException e) {
      e.printStackTrace();
    }
    
    if(runnable != null)
      new Thread(runnable).start();
  }
  
  /**
   * Handles a local error on the WebSocket.
   * 
   * @param cause the cause
   */
  @OnWebSocketError public void onError(Throwable cause) {
    new Thread(new Runnable() {
      @Override public void run() {
        synchronized(errorListeners) {
          for(WebSocketErrorListener listener : errorListeners)
            listener.onLocalError(cause);
        }
      }
    }).start();
  }
  
  /**
   * Registers a listener.
   * 
   * @param listener the listener
   * @throws ClassCastException if the object is not a listener
   */
  public void registerListener(Object listener) throws ClassCastException {
    boolean isListener = false;
    
    if(listener instanceof CommandListener) {
      synchronized(commandListeners) {
        commandListeners.add((CommandListener)listener);
      }
      isListener = true;
    }
    
    if(listener instanceof ConfigUpdateListener) {
      synchronized(configUpdateListeners) {
        configUpdateListeners.add((ConfigUpdateListener)listener);
      }
      isListener = true;
    }
    
    if(listener instanceof ConnectionCloseListener) {
      synchronized(connectionCloseListeners) {
        connectionCloseListeners.add((ConnectionCloseListener)listener);
      }
      isListener = true;
    }
    
    if(listener instanceof ConnectionOpenListener) {
      synchronized(connectionOpenListeners) {
        connectionOpenListeners.add((ConnectionOpenListener)listener);
      }
      isListener = true;
    }
    
    if(listener instanceof MessageListener) {
      synchronized(messageListeners) {
        messageListeners.add((MessageListener)listener);
      }
      isListener = true;
    }
    
    if(listener instanceof WebSocketErrorListener) {
      synchronized(errorListeners) {
        errorListeners.add((WebSocketErrorListener)listener);
      }
      isListener = true;
    }
    
    if(!isListener)
      throw new ClassCastException("Not a listener.");
  }
  
  /**
   * Deregisters a listener.
   * 
   * @param listener the listener
   */
  public void deregisterListener(Object listener) {
    for(Set<?> listeners : listenersArr)
      synchronized(listeners) {
        if(listeners.contains(listener))
          listeners.remove(listener);
      }
  }
  
  /**
   * Deregisters all listeners.
   */
  public void clearListeners() {
    for(Set<?> listeners : listenersArr)
      synchronized(listeners) {
        listeners.clear();
      }
  }
  
  /**
   * Dispatches a payload.
   * 
   * @param payload the payload
   */
  public void dispatch(OutgoingPayload payload) {
    for(Session session : sessions) try {
      session.getRemote().sendString(payload.toString());
    } catch(IOException e) { }
  }
  
}

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
package edu.uco.cs.v2c.dispatcher.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jetty.websocket.client.WebSocketClient;

import edu.uco.cs.v2c.dispatcher.api.payload.PayloadProcessor;
import edu.uco.cs.v2c.dispatcher.api.payload.outgoing.OutgoingPayload;

/**
 * Encapsulates a connection to the dispatcher.
 * 
 * @author Caleb L. Power
 */
public class DispatcherConnection {
  
  private URI destination = null;
  private WebSocketClient client = new WebSocketClient();
  private PayloadProcessor processor = new PayloadProcessor();
  
  /**
   * Instantiates the dispatcher connection.
   * 
   * @param destination the destination
   * @throws URISyntaxException if the destination isn't a valid URI
   */
  public DispatcherConnection(String destination) throws URISyntaxException {
    this.destination = new URI(destination);
  }
  
  /**
   * Connects to the destination.
   * 
   * @throws Exception if the client fails to start or connect
   */
  public void connect() throws Exception {
    if(client.isStopped()) client.start();
    client.connect(processor, destination);
  }
  
  /**
   * Sends a payload to the dispatcher.
   * 
   * @param payload the payload
   */
  public void dispatchPayload(OutgoingPayload payload) {
    processor.dispatch(payload);
  }
  
  /**
   * Registers a listener.
   * 
   * @param listener the listener
   * @throws ClassCastException if the object is not a listener
   */
  public void registerListener(Object listener) throws ClassCastException {
    processor.registerListener(listener);
  }
  
  /**
   * Deregisters a listener.
   * 
   * @param listener the listener
   */
  public void deregisterListener(Object listener) {
    processor.deregisterListener(listener);
  }
  
  /**
   * Kills the connection.
   */
  public void kill() {
    try {
      client.stop();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
}

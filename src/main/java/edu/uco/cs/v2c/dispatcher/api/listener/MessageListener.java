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
package edu.uco.cs.v2c.dispatcher.api.listener;

import edu.uco.cs.v2c.dispatcher.api.payload.incoming.RouteMessagePayload;

/**
 * Responds to an incoming message.
 * 
 * @author Caleb L. Power
 */
public interface MessageListener {
  
  /**
   * Performs some action when an incoming message is detected.
   * 
   * @param payload the incoming payload
   */
  public void onIncomingMessage(RouteMessagePayload payload);
  
}

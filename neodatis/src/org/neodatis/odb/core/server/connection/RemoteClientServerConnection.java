/*
 NeoDatis ODB : Native Object Database (odb.info@neodatis.org)
 Copyright (C) 2007 NeoDatis Inc. http://www.neodatis.org

 "This file is part of the NeoDatis ODB open source object database".

 NeoDatis ODB is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 NeoDatis ODB is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.neodatis.odb.core.server.connection;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.neodatis.odb.NeoDatisRuntimeException;
import org.neodatis.odb.ODBServer;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.odb.core.server.MessageStreamer;
import org.neodatis.odb.core.server.ODBServerImpl;
import org.neodatis.odb.core.server.message.Message;
import org.neodatis.tool.DLogger;
import org.neodatis.tool.wrappers.OdbRunnable;
import org.neodatis.tool.wrappers.OdbString;
import org.neodatis.tool.wrappers.OdbThread;
import org.neodatis.tool.wrappers.io.MessageStreamerBuilder;
import org.neodatis.odb.core.server.message.MessageType;
/**
 * A thread to manage client connections via socket
 * 
 * 
 * @author olivier s
 * 
 */
public class RemoteClientServerConnection extends ClientServerConnection implements OdbRunnable {
	private static final String LOG_ID = "RemoteClientServerConnection";

	private Socket socketConnection;
	private String name;
	private MessageStreamer messageStreamer;
    private ODBServerImpl parent;

	public RemoteClientServerConnection(ODBServer server, Socket connection) {
		super(server);
        this.parent = (ODBServerImpl)server;
		this.socketConnection = connection;
	}

	public void run() {
		OutputStream out = null;
		InputStream in = null;
		String messageType = null;
        boolean lock = false;
		try {
			socketConnection.setKeepAlive(true);
			// socketConnection.setSoTimeout(0);
			socketConnection.setTcpNoDelay(true);
			connectionIsUp = true;
			out = socketConnection.getOutputStream();
			in = socketConnection.getInputStream();
			messageStreamer = MessageStreamerBuilder.getMessageStreamer(socketConnection,server.getConfig());
			// in,out,ois,oos);
			Message message = null;
			Message rmessage = null;
			do {
				messageType = "'No message'";
				message = null;
				message = messageStreamer.read();

				if (message != null) {
                    int mt = message.getMessageType();
                    // big fat fucking lock if we do any modifications!
                    if(lock == false && mt == MessageType.CONNECT) {
                            parent.getLock();
                            lock = true;
                    }
					messageType = message.getClass().getName();
					rmessage = manageMessage(message);
					messageStreamer.write(rmessage);
                    if(lock == true && mt == MessageType.CLOSE) {
                        parent.releaseLock();
                        lock = false;
                    }
				} else {
					messageType = "Null Message";
				}
				// To force disconnection
				// connectionIsUp = false;
			} while (connectionIsUp && message != null);
            //if(lock == true) {
            //    parent.releaseLock();
            //}
		} catch (EOFException eoe) {
			DLogger.error(getClass().getSimpleName()+ " : Client connection is dead : Thread " + OdbThread.getCurrentThreadName() + ": Error in connection thread baseId=" + baseIdentifier
					+ " and cid=" + connectionId + " for message of type " + messageType + " : Client has terminated the connection!");
			connectionIsUp = false;
		} catch (Throwable e) {
			String m = OdbString.exceptionToString(e, false);
			DLogger.error("Thread " + OdbThread.getCurrentThreadName() + ": Error in connection thread baseId=" + baseIdentifier
					+ " and cid=" + connectionId + " for message of type " + messageType + " : \n" + m);
			connectionIsUp = false;
			throw new NeoDatisRuntimeException(NeoDatisError.NET_SERIALISATION_ERROR.addParameter(e.getMessage()).addParameter(m));
		} finally {
            if(lock == true) {
                parent.releaseLock();
            }
			try {
				if(messageStreamer!=null){
					messageStreamer.close();
					socketConnection.close();
				}
			} catch (IOException e) {
				DLogger.error("Error while closing socket - connection thread baseId=" + baseIdentifier + " and cid=" + connectionId
						+ ": \n" + OdbString.exceptionToString(e, false));
			}
			if (debug) {
				DLogger.info("Exiting thread " + OdbThread.getCurrentThreadName());
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

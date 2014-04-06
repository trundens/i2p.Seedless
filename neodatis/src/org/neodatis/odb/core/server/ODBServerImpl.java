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
package org.neodatis.odb.core.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.concurrent.locks.ReentrantLock;
import org.neodatis.odb.NeoDatis;
import org.neodatis.odb.NeoDatisConfig;
import org.neodatis.odb.NeoDatisRuntimeException;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBServer;
import org.neodatis.odb.Release;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.odb.core.layers.layer4.IOFileParameter;
import org.neodatis.odb.core.server.connection.ClientServerConnection;
import org.neodatis.odb.core.server.connection.RemoteClientServerConnection;
import org.neodatis.odb.core.server.connection.SessionManager;
import org.neodatis.odb.core.server.trigger.ServerDeleteTrigger;
import org.neodatis.odb.core.server.trigger.ServerInsertTrigger;
import org.neodatis.odb.core.server.trigger.ServerOidTrigger;
import org.neodatis.odb.core.server.trigger.ServerSelectTrigger;
import org.neodatis.odb.core.server.trigger.ServerUpdateTrigger;
import org.neodatis.odb.core.session.Session;
import org.neodatis.tool.DLogger;
import org.neodatis.tool.wrappers.OdbRunnable;
import org.neodatis.tool.wrappers.OdbString;
import org.neodatis.tool.wrappers.OdbThread;
import org.neodatis.tool.wrappers.OdbTime;
import org.neodatis.tool.wrappers.map.OdbHashMap;

/**
 * The ODB implementation for Server mode
 *
 * @author osmadja
 * @port.todo
 *
 */
public class ODBServerImpl implements OdbRunnable, ODBServer {

    public static final String LOG_ID = "ODBServer";
    private int port;
    private OdbThread thread;
    private boolean serverIsUp;
    private ServerSocket socketServer;
    private boolean isRunning;
    /** the key is the base identifier */
    private Map<String, String> bases;
    /**
     * key = base identifier, value = map where key is connection id, and value
     * is the session
     */
    private Map<String, SessionManager> sessionManagers;
    /** key=baseIdentifier */
    // private Map<String,SessionManager> connectionManagers;
    private boolean automaticallyCreateDatabase;
    /** To keep track when the server started */
    private long start;
    protected NeoDatisConfig config;
    private volatile int writersWaiting,  readers;
    private final ReentrantLock rwl = new ReentrantLock(true);

    public ODBServerImpl(int port, NeoDatisConfig config) {
        this.port = port;
        this.automaticallyCreateDatabase = true;
        this.config = config;
        this.writersWaiting = this.readers = 0;
        initServer();
    }

    public void getLock() {
          rwl.lock();
    }

    public void releaseLock() {
          rwl.unlock();
    }

    private void initServer() {
        this.bases = new OdbHashMap<String, String>();
        this.sessionManagers = new OdbHashMap<String, SessionManager>();
    }

    private void initSocketServer() {
        try {
            socketServer = createSocketServer();
            isRunning = true;
        } catch(BindException e1) {
            isRunning = false;
            throw new NeoDatisRuntimeException(NeoDatisError.CLIENT_SERVER_PORT_IS_BUSY.addParameter(port), e1);
        } catch(IOException e2) {
            isRunning = false;
            throw new NeoDatisRuntimeException(NeoDatisError.CLIENT_SERVER_CAN_NOT_OPEN_ODB_SERVER_ON_PORT.addParameter(port), e2);
        }
    }

    protected ServerSocket createSocketServer() throws IOException {
        return new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
    }

    public void addBase(String baseIdentifier, String fileName) {
        addBase(baseIdentifier, fileName, null, null);
    }

    public void addBase(String baseIdentifier, String fileName, String user, String password) {
        // check if we need to use full path here
        //File file = new File(fileName);
        //fileName = file.getAbsolutePath();
        bases.put(baseIdentifier, fileName);
        NeoDatisConfig config2 = config.copy();

        config2.setIsLocal(false);
        if(user != null && password != null) {
            config2.setUser(user);
            config2.setPassword(password);
        }
        sessionManagers.put(baseIdentifier, new SessionManager(new IOFileParameter(fileName, true, config2)));
        if(config2.isInfoEnabled(LOG_ID)) {
            DLogger.info("ODBServer:Adding base : name=" + baseIdentifier + " (file=" + fileName + ") to server");
        }
    }

    public void addUserForBase(String baseIdentifier, String user, String password) {
        throw new NeoDatisRuntimeException(NeoDatisError.NOT_YET_IMPLEMENTED);
    }

    public void startServer(boolean inThread) {
        initSocketServer();
        if(inThread) {
            thread = new OdbThread(this);
            thread.start();
        } else {
            run();
        }
    }

    public void run() {

        try {
            startServer();
        } catch(IOException e) {
            DLogger.error(OdbString.exceptionToString(e, true));
        }
    }

    public void startServer() throws IOException {
        start = OdbTime.getCurrentTimeInMs();
        boolean warned = false;
        int happened = 0;
        if(config.logServerStartupAndShutdown()) {
            DLogger.info("NeoDatis ODB Server [version=" + Release.RELEASE_NUMBER + " - build=" + Release.RELEASE_BUILD + " - " + Release.RELEASE_DATE + "] running on port " + port);
            if(bases.size() != 0) {
                DLogger.info("Managed bases: ");
                for(String baseName: bases.keySet()) {
                    DLogger.info("\t" + baseName + " => " + bases.get(baseName));
                }
            }
        }

        while(isRunning) {
            try {
                waitForRemoteConnection();
            } catch(SocketException e) {
                if(isRunning) {
                    DLogger.error("ODBServer:ODBServerImpl.startServer:" + OdbString.exceptionToString(e, true));
                    if(e.getMessage().equals("Too many open files")) {
                        happened++;
                        if(!warned) {
                            DLogger.error("ODBServer:ODBServerImpl.startServer: OS out of File Descriptors. Sleeping for a bit...");
                        } else {
                            DLogger.error("ODBServer:ODBServerImpl.startServer: OS out of File Descriptors " + happened +" times.");
                        }
                        try {
                            Thread.sleep(6000);
                        } catch(InterruptedException ex) {
                        }
                        if(isRunning)
                            if(!warned) {
                                DLogger.error("ODBServer:ODBServerImpl.startServer: Waking up. Please reduce your connections to the internet, or allow more connections.");
                                warned = true;
                            }
                    }
                }
            }
        }
    }

    public ClientServerConnection waitForRemoteConnection() throws IOException {
        Socket connection = socketServer.accept();
        connection.setTcpNoDelay(true);
        RemoteClientServerConnection connectionThread = new RemoteClientServerConnection(this, connection);
        OdbThread CSCthread = new OdbThread(connectionThread);
        connectionThread.setName(CSCthread.getName());
        CSCthread.start();
        return connectionThread;
    }

    public void close() {
        if(config.logServerStartupAndShutdown()) {
            long end = OdbTime.getCurrentTimeInMs();
            double timeInHour = ((double)(end - start)) / (1000 * 60 * 60);
            DLogger.info(String.format("NeoDatis ODB Server (port %d) shutdown [uptime=%dHours]", port, (long)timeInHour));
        }
        try {
            isRunning = false;
            socketServer.close();
            Iterator iterator = bases.keySet().iterator();
            String baseIdentifier = null;
            for(String base: bases.keySet()) {
                SessionManager sessionManager = sessionManagers.get(base);

                for(String sessionId: sessionManager.getSessions().keySet()) {
                    Session session = sessionManager.getSessions().get(sessionId);
                    session.close();
                }
            }

            if(thread != null) {
                thread.interrupt();

            }
        } catch(Exception e) {
            throw new NeoDatisRuntimeException(NeoDatisError.SERVER_ERROR.addParameter("While closing server"), e);
        }
    }

    public void setAutomaticallyCreateDatabase(boolean yes) {
        automaticallyCreateDatabase = yes;
    }

    public ODB openClient(String baseIdentifier) {
        NeoDatisConfig c = NeoDatis.getConfig().setHostAndPort("localhost", getPort());
        DLogger.info("Warning: Client Server Same VM mode not yet supported : using normal client server connection");
        return NeoDatis.openClient(baseIdentifier, c);

    //return new SameVMODBClient(this, baseIdentifier);
    }

    public ODB openClient(String baseIdentifier, NeoDatisConfig config) {
        config.setHostAndPort("localhost", getPort());
        DLogger.info("Warning: Client Server Same VM mode not yet supported : using normal client server connection");
        return NeoDatis.openClient(baseIdentifier, config);
    //return new SameVMODBClient(this, baseIdentifier,config);
    }

    public void addDeleteTrigger(String baseIdentifier, String className, ServerDeleteTrigger trigger) {
        SessionManager sm = sessionManagers.get(baseIdentifier);

        if(sm == null) {
            throw new NeoDatisRuntimeException(NeoDatisError.UNREGISTERED_BASE_ON_SERVER.addParameter(baseIdentifier));
        }

        sm.getTriggers().addDeleteTriggerFor(className, trigger);
    }

    public void addInsertTrigger(String baseIdentifier, String className, ServerInsertTrigger trigger) {
        SessionManager sm = sessionManagers.get(baseIdentifier);

        if(sm == null) {
            throw new NeoDatisRuntimeException(NeoDatisError.UNREGISTERED_BASE_ON_SERVER.addParameter(baseIdentifier));
        }

        sm.getTriggers().addInsertTriggerFor(className, trigger);
    }

    public void addOidTrigger(String baseIdentifier, String className, ServerOidTrigger trigger) {
        SessionManager sm = sessionManagers.get(baseIdentifier);

        if(sm == null) {
            throw new NeoDatisRuntimeException(NeoDatisError.UNREGISTERED_BASE_ON_SERVER.addParameter(baseIdentifier));
        }

        sm.getTriggers().addOidTriggerFor(className, trigger);
    }

    public void addSelectTrigger(String baseIdentifier, String className, ServerSelectTrigger trigger) {
        SessionManager sm = sessionManagers.get(baseIdentifier);

        if(sm == null) {
            throw new NeoDatisRuntimeException(NeoDatisError.UNREGISTERED_BASE_ON_SERVER.addParameter(baseIdentifier));
        }

        sm.getTriggers().addSelectTriggerFor(className, trigger);
    }

    public void addUpdateTrigger(String baseIdentifier, String className, ServerUpdateTrigger trigger) {
        SessionManager sm = sessionManagers.get(baseIdentifier);

        if(sm == null) {
            throw new NeoDatisRuntimeException(NeoDatisError.UNREGISTERED_BASE_ON_SERVER.addParameter(baseIdentifier));
        }

        sm.getTriggers().addUpdateTriggerFor(className, trigger);
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();

        b.append(String.format("%d bases", bases.size()));
        Iterator<String> iterator = bases.keySet().iterator();
        while(iterator.hasNext()) {
            String baseName = iterator.next();
            b.append(String.format("\tBase %s", baseName));
            SessionManager sessionManager = sessionManagers.get(baseName);
            for(String sessionId: sessionManager.getSessions().keySet()) {
                Session s = sessionManager.getSession(sessionId);
                b.append(String.format("\n\t\tsession id = %s : %s", sessionId, s.toString()));
            }

        }

        return b.toString();
    }

    public SessionManager getSessionManagerForBase(String baseIdentifier) {
        return sessionManagers.get(baseIdentifier);
    }

    public void removeSessionManagerForBase(String baseId) {
        sessionManagers.remove(baseId);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.neodatis.odb.ODBServer#getSessionDescriptions()
     */
    public List<String> getSessionDescriptions() {
        List<String> descriptions = new ArrayList<String>();
        Iterator<String> iterator = bases.keySet().iterator();
        while(iterator.hasNext()) {
            String baseName = iterator.next();
            SessionManager sessionManager = sessionManagers.get(baseName);
            for(String sessionId: sessionManager.getSessions().keySet()) {
                Session s = sessionManager.getSession(sessionId);
                descriptions.add(String.format("base %s session id = %s : %s", baseName, sessionId, s.toString()));
            }
        }
        return descriptions;
    }

    public boolean automaticallyCreateDatabase() {
        return automaticallyCreateDatabase;
    }

    public NeoDatisConfig getConfig() {
        return config;
    }

    public int getPort() {
        return port;
    }

    public void dontCallTriggersForClasses(String baseIdentifier, List<Class> classes) {
        SessionManager sm = sessionManagers.get(baseIdentifier);

        if(sm == null) {
            throw new NeoDatisRuntimeException(NeoDatisError.UNREGISTERED_BASE_ON_SERVER.addParameter(baseIdentifier));
        }

        sm.getTriggers().addClassesNotToCallTriggersOn(classes);
    }
}

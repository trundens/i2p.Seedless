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

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neodatis.odb.ClassOid;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBServer;
import org.neodatis.odb.OID;
import org.neodatis.odb.ObjectOid;
import org.neodatis.odb.Objects;
import org.neodatis.odb.Values;
import org.neodatis.odb.core.layers.layer2.instance.InstanceBuilderContext;
import org.neodatis.odb.core.layers.layer2.meta.ClassInfo;
import org.neodatis.odb.core.layers.layer2.meta.ClassInfoList;
import org.neodatis.odb.core.layers.layer2.meta.MetaModel;
import org.neodatis.odb.core.layers.layer2.meta.NonNativeObjectInfo;
import org.neodatis.odb.core.layers.layer2.meta.ObjectInfoHeader;
import org.neodatis.odb.core.layers.layer2.meta.ObjectRepresentationImpl;
import org.neodatis.odb.core.layers.layer3.OidAndBytes;
import org.neodatis.odb.core.query.QueryManager;
import org.neodatis.odb.core.query.criteria.CriteriaQuery;
import org.neodatis.odb.core.refactor.CheckMetaModelResult;
import org.neodatis.odb.core.server.ServerSession;
import org.neodatis.odb.core.server.message.AddIndexMessage;
import org.neodatis.odb.core.server.message.AddIndexMessageResponse;
import org.neodatis.odb.core.server.message.CheckMetaModelCompatibilityMessage;
import org.neodatis.odb.core.server.message.CheckMetaModelCompatibilityMessageResponse;
import org.neodatis.odb.core.server.message.CloseMessage;
import org.neodatis.odb.core.server.message.CloseMessageResponse;
import org.neodatis.odb.core.server.message.CommitMessage;
import org.neodatis.odb.core.server.message.CommitMessageResponse;
import org.neodatis.odb.core.server.message.ConnectMessage;
import org.neodatis.odb.core.server.message.ConnectMessageResponse;
import org.neodatis.odb.core.server.message.CountMessage;
import org.neodatis.odb.core.server.message.CountMessageResponse;
import org.neodatis.odb.core.server.message.DeleteBaseMessage;
import org.neodatis.odb.core.server.message.DeleteBaseMessageResponse;
import org.neodatis.odb.core.server.message.DeleteIndexMessage;
import org.neodatis.odb.core.server.message.DeleteIndexMessageResponse;
import org.neodatis.odb.core.server.message.DeleteObjectMessage;
import org.neodatis.odb.core.server.message.DeleteObjectMessageResponse;
import org.neodatis.odb.core.server.message.ErrorMessage;
import org.neodatis.odb.core.server.message.GetFileMessage;
import org.neodatis.odb.core.server.message.GetFileMessageResponse;
import org.neodatis.odb.core.server.message.GetObjectFromIdMessageResponse;
import org.neodatis.odb.core.server.message.GetObjectFromOidMessage;
import org.neodatis.odb.core.server.message.GetObjectHeaderFromIdMessage;
import org.neodatis.odb.core.server.message.GetObjectHeaderFromIdMessageResponse;
import org.neodatis.odb.core.server.message.GetObjectValuesMessage;
import org.neodatis.odb.core.server.message.GetObjectValuesMessageResponse;
import org.neodatis.odb.core.server.message.GetObjectsMessage;
import org.neodatis.odb.core.server.message.GetObjectsMessageResponse;
import org.neodatis.odb.core.server.message.GetSessionsMessage;
import org.neodatis.odb.core.server.message.GetSessionsMessageResponse;
import org.neodatis.odb.core.server.message.Message;
import org.neodatis.odb.core.server.message.MessageType;
import org.neodatis.odb.core.server.message.NewClassInfoListMessage;
import org.neodatis.odb.core.server.message.NewClassInfoListMessageResponse;
import org.neodatis.odb.core.server.message.NextClassInfoOidMessage;
import org.neodatis.odb.core.server.message.NextClassInfoOidResponseMessage;
import org.neodatis.odb.core.server.message.RebuildIndexMessage;
import org.neodatis.odb.core.server.message.RebuildIndexMessageResponse;
import org.neodatis.odb.core.server.message.RollbackMessage;
import org.neodatis.odb.core.server.message.RollbackMessageResponse;
import org.neodatis.odb.core.server.message.SendFileMessage;
import org.neodatis.odb.core.server.message.SendFileMessageResponse;
import org.neodatis.odb.core.server.message.StoreClassInfoMessage;
import org.neodatis.odb.core.server.message.StoreClassInfoMessageResponse;
import org.neodatis.odb.core.server.message.StoreObjectMessage;
import org.neodatis.odb.core.server.message.StoreObjectMessageResponse;
import org.neodatis.odb.core.server.message.process.RemoteProcess;
import org.neodatis.odb.core.server.message.process.RemoteProcessMessage;
import org.neodatis.odb.core.server.message.process.RemoteProcessMessageResponse;
import org.neodatis.odb.core.server.message.process.RemoteProcessReturn;
import org.neodatis.odb.core.session.ExecutionType;
import org.neodatis.odb.core.session.Session;
import org.neodatis.odb.core.session.SessionEngine;
import org.neodatis.odb.core.session.SessionWrapper;
import org.neodatis.odb.main.ODBForTrigger;
import org.neodatis.tool.DLogger;
import org.neodatis.tool.IOUtil;
import org.neodatis.tool.mutex.Mutex;
import org.neodatis.tool.mutex.MutexFactory;
import org.neodatis.tool.wrappers.OdbString;
import org.neodatis.tool.wrappers.OdbTime;
import org.neodatis.tool.wrappers.io.OdbFile;
import org.neodatis.tool.wrappers.list.IOdbList;
import org.neodatis.tool.wrappers.list.OdbArrayList;

/**
 * The abstract class that manages the client server connections. It is message
 * based and it manages all the client server messages.
 * 
 * @author olivier s
 * 
 */
public abstract class ClientServerConnection {
	private static final String LOG_ID = "ClientServerConnection";

	private static int nbMessages = 0;

	protected boolean connectionIsUp;

	protected String baseIdentifier;

	protected String connectionId;

	protected boolean debug;

	protected ODBServer server;

	protected ServerSession session;

	private static final String COMMIT_CLOSE_CONNECT_MUTEX_NAME = "commit-close-connect";

	private static final String COUNT_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String GET_OBJECT_HEADER_FROM_ID_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String GET_OBJECT_FROM_ID_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String GET_VALUES_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String GET_OBJECTS_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String DELETE_OBJECT_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String ADD_CLASS_INFO_LIST_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	private static final String STORE_MUTEX_NAME = COMMIT_CLOSE_CONNECT_MUTEX_NAME;

	public ClientServerConnection(ODBServer server) {
		this.debug = server.getConfig().logServerConnections();
		this.server = server;
	}

	public abstract String getName();

	/**
	 * The main method. It is the message dispatcher. Checks the message type
	 * and calls the right message handler.
	 * 
	 * @param message
	 * @return
	 */
	public Message manageMessage(Message message) {
		long start = OdbTime.getCurrentTimeInMs();

		try {
			nbMessages++;
			int commandId = message.getMessageType();

			switch (commandId) {
			case MessageType.CONNECT:
				return manageConnectCommand((ConnectMessage) message);
			case MessageType.GET_OBJECTS:
				return manageGetObjectsCommand((GetObjectsMessage) message);
			case MessageType.GET_OBJECT_FROM_ID:
				return manageGetObjectFromIdCommand((GetObjectFromOidMessage) message);
			case MessageType.GET_OBJECT_HEADER_FROM_ID:
				return manageGetObjectHeaderFromIdCommand((GetObjectHeaderFromIdMessage) message);
			case MessageType.STORE_OBJECT:
				return manageStoreObjectCommand((StoreObjectMessage) message);
			case MessageType.STORE_CLASS_INFO:
				return manageStoreClassInfoCommand((StoreClassInfoMessage) message);
			case MessageType.DELETE_OBJECT:
				return manageDeleteObjectCommand((DeleteObjectMessage) message);
			case MessageType.CLOSE:
				return manageCloseCommand((CloseMessage) message);
			case MessageType.COMMIT:
				return manageCommitCommand((CommitMessage) message);
			case MessageType.ROLLBACK:
				return manageRollbackCommand((RollbackMessage) message);
			case MessageType.DELETE_BASE:
				return manageDeleteBaseCommand((DeleteBaseMessage) message);
			case MessageType.GET_SESSIONS:
				return manageGetSessionsCommand((GetSessionsMessage) message);
			case MessageType.ADD_UNIQUE_INDEX:
				return manageAddIndexCommand((AddIndexMessage) message);
			case MessageType.REBUILD_INDEX:
				return manageRebuildIndexCommand((RebuildIndexMessage) message);
			case MessageType.DELETE_INDEX:
				return manageDeleteIndexCommand((DeleteIndexMessage) message);
			case MessageType.ADD_CLASS_INFO_LIST:
				return manageAddClassInfoListCommand((NewClassInfoListMessage) message);
			case MessageType.COUNT:
				return manageCountCommand((CountMessage) message);
			case MessageType.GET_OBJECT_VALUES:
				return manageGetObjectValuesCommand((GetObjectValuesMessage) message);
			case MessageType.CHECK_META_MODEL_COMPATIBILITY:
				return manageCheckMetaModelCompatibilityCommand((CheckMetaModelCompatibilityMessage) message);
			case MessageType.NEXT_CLASS_INFO_OID:
				return manageNextClassInfoOidCommand((NextClassInfoOidMessage) message);
			case MessageType.SEND_FILE:
				return manageSendFileCommand((SendFileMessage) message);
			case MessageType.GET_FILE:
				return manageGetFileCommand((GetFileMessage) message);
			case MessageType.REMOTE_PROCESS:
				return manageRemoteProcessCommand((RemoteProcessMessage) message);

			default:
				break;
			}

			StringBuffer buffer = new StringBuffer();
			buffer.append("ODBServer.ConnectionThread:command ").append(commandId).append(" not implemented");
			return new ErrorMessage("?", "?", buffer.toString());

		} finally {
			long end = OdbTime.getCurrentTimeInMs();
			if (debug) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("[").append(nbMessages).append("] ");
				buffer.append(message.toString()).append(" - Thread=").append(getName()).append(" - sessionId =").append(message.getSessionId())
						.append(" - duration=").append((end - start));
				DLogger.info(buffer);
			}

		}

	}

	/**
	 * @param message
	 * @return
	 */
	private Message manageNextClassInfoOidCommand(NextClassInfoOidMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(ADD_CLASS_INFO_LIST_MUTEX_NAME);

			SessionEngine engine = session.getEngine();
			ClassOid coid = engine.getStorageEngine().getOidGenerator().createClassOid();
			return new NextClassInfoOidResponseMessage(aBaseIdentifier, message.getSessionId(), coid);
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while getting next ClassInfo Oid";
			DLogger.error(msg, e);
			return new NewClassInfoListMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("nextClassInfoOid");
			}
		}

	}

	/**
	 * Used to check if client classes meta model is compatible with the meta
	 * model persisted in the database
	 * 
	 * @param message
	 * @return
	 */
	private CheckMetaModelCompatibilityMessageResponse manageCheckMetaModelCompatibilityCommand(CheckMetaModelCompatibilityMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		// Gets the aSession manager for this base identifier
		SessionManager sessionManager = null;
		Session aSession = null;
		try {
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new CheckMetaModelCompatibilityMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			aSession = sessionManager.getSession(message.getSessionId());

			SessionEngine engine = aSession.getEngine();
			Map<String, ClassInfo> currentCIs = message.getCurrentCIs();

			CheckMetaModelResult result = engine.checkMetaModelCompatibility(currentCIs);

			MetaModel updatedMetaModel = null;
			if (result.isModelHasBeenUpdated()) {
				updatedMetaModel = aSession.getMetaModel().duplicate();
			}
			// If meta model has been updated, returns it to clients
			return new CheckMetaModelCompatibilityMessageResponse(aBaseIdentifier, message.getSessionId(), result, updatedMetaModel);
		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while closing", e);
			return new CheckMetaModelCompatibilityMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e,
					false));
		}
	}

	/**
	 * Manage Index Message
	 * 
	 * @param message
	 * @return
	 */
	private Message manageAddIndexCommand(AddIndexMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		// Gets the aSession manager for this base identifier
		SessionManager sessionManager = null;
		Session aSession = null;
		try {
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new AddIndexMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			aSession = sessionManager.getSession(message.getSessionId());

			SessionEngine engine = aSession.getEngine();
			engine.addIndexOn(message.getClassName(), message.getIndexName(), message.getIndexFieldNames(), message.isVerbose(), message
					.acceptMultipleValuesForSameKey());
		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while closing", e);
			return new AddIndexMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e, false));
		}

		return new AddIndexMessageResponse(aBaseIdentifier, message.getSessionId());
	}

	/**
	 * Rebuild an index Index Message
	 * 
	 * @param message
	 * @return
	 */
	private Message manageRebuildIndexCommand(RebuildIndexMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		// Gets the aSession manager for this base identifier
		SessionManager sessionManager = null;
		Session aSession = null;
		try {
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new RebuildIndexMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			aSession = sessionManager.getSession(message.getSessionId());

			SessionEngine engine = aSession.getEngine();
			engine.rebuildIndex(message.getClassName(), message.getIndexName(), message.isVerbose());
		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while closing", e);
			return new RebuildIndexMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e, false));
		}

		return new RebuildIndexMessageResponse(aBaseIdentifier, message.getSessionId());
	}

	/**
	 * Delete an index Index Message
	 * 
	 * @param message
	 * @return
	 */
	private Message manageDeleteIndexCommand(DeleteIndexMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		// Gets the aSession manager for this base identifier
		SessionManager sessionManager = null;
		Session aSession = null;
		try {
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new DeleteIndexMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			aSession = sessionManager.getSession(message.getSessionId());

			SessionEngine engine = aSession.getEngine();
			engine.deleteIndex(message.getClassName(), message.getIndexName(), message.isVerbose());
		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while closing", e);
			return new DeleteIndexMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e, false));
		}

		return new DeleteIndexMessageResponse(aBaseIdentifier, message.getSessionId());
	}

	private SessionManager getSessionManager(String baseIdentifier) throws Exception {
		return getSessionManager(baseIdentifier, null, null, false);
	}

	/**
	 * Gets the aSession manager for the base
	 * 
	 * @param aBaseIdentifier
	 * @param user
	 * @param password
	 * @param returnNullIfDoesNotExit
	 * @return
	 * @throws Exception
	 */
	private SessionManager getSessionManager(String baseIdentifier, String user, String password, boolean returnNullIfDoesNotExit)
			throws Exception {

		try {

			// Gets the aSession manager for this base identifier
			SessionManager sessionManager = (SessionManager) server.getSessionManagerForBase(baseIdentifier);
			if (sessionManager == null && returnNullIfDoesNotExit) {
				return null;
			}
			if (sessionManager == null && server.automaticallyCreateDatabase()) {
				server.addBase(baseIdentifier, baseIdentifier, user, password);
				sessionManager = (SessionManager) server.getSessionManagerForBase(baseIdentifier);
			}
			if (sessionManager == null && !server.automaticallyCreateDatabase()) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(baseIdentifier).append(" is not registered on this server!");
				return null;
			}
			return sessionManager;
		} finally {
		}

	}

	/**
	 * manages the Close Message
	 * 
	 * @param message
	 * @return
	 */
	private Message manageCloseCommand(CloseMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(COMMIT_CLOSE_CONNECT_MUTEX_NAME);

			SessionManager sessionManager = getSessionManager(aBaseIdentifier);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new CloseMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			session.setCurrentAction(ConnectionAction.ACTION_CLOSE);
			session.close();
			sessionManager.removeSession(session);

			connectionIsUp = false;
			return new CloseMessageResponse(aBaseIdentifier, message.getSessionId());

		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while closing", e);
			return new CloseMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e, false));
		} finally {
			if (mutex != null) {
				mutex.release("close");
			}
		}

	}

	private Message manageGetSessionsCommand(GetSessionsMessage message) {
		try {
			List<String> descriptions = server.getSessionDescriptions();
			return new GetSessionsMessageResponse(descriptions);
		} catch (Exception e) {
			DLogger.error("Server error while getting session descriptions", e);
			return new GetSessionsMessageResponse(OdbString.exceptionToString(e, false));
		}
	}

	private Message manageCommitCommand(CommitMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(COMMIT_CLOSE_CONNECT_MUTEX_NAME);
			session.setCurrentAction(ConnectionAction.ACTION_COMMIT);
			session.commit();
			
			return new CommitMessageResponse(aBaseIdentifier, message.getSessionId(), true);
		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while commiting", e);
			return new CommitMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e, false));
		} finally {
			if (mutex != null) {
				mutex.release("commit");
			}
			session.endCurrentAction();
		}

	}

	private Message manageRollbackCommand(RollbackMessage message) {
		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(COMMIT_CLOSE_CONNECT_MUTEX_NAME);
			session.setCurrentAction(ConnectionAction.ACTION_ROLLBACK);
			session.rollback();
		} catch (Exception e) {
			DLogger.error(aBaseIdentifier + ":Server error while rollbacking", e);
			return new RollbackMessageResponse(aBaseIdentifier, message.getSessionId(), OdbString.exceptionToString(e, false));

		} finally {
			if (mutex != null) {
				mutex.release("rollback");
			}
			session.endCurrentAction();
		}
		return new RollbackMessageResponse(aBaseIdentifier, message.getSessionId(), true);

	}

	/**
	 * manage the store command. The store command can be an insert(oid==null)
	 * or an update(oid!=null)
	 * 
	 * If insert get the base mutex IF update, first get the mutex of the oid to
	 * update then get the base mutex, to avoid dead lock in case of concurrent
	 * update.
	 * 
	 * @param message
	 * @return
	 */

	private Message manageStoreObjectCommand(StoreObjectMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		ObjectOid oid = null;
		try {

			SessionEngine engine = session.getEngine();

			// To know if an object is new, the clients oids have a flag :
			// isNew. To get the flag back, we build here a map with oid,oid to
			// get retrieve it
			Map<ObjectOid, ObjectOid> clientIds = new HashMap<ObjectOid, ObjectOid>();
			for (ObjectOid ooid : message.getClientIds()) {
				clientIds.put(ooid, ooid);
			}
			OidAndBytes mainOab = message.getOabs().get(0);
			ObjectOid mainOid = (ObjectOid) mainOab.oid;
			session.setCurrentAction(ConnectionAction.ACTION_INSERT_OR_UPDATE);
			boolean objectIsNew = clientIds.get(mainOid).isNew();
			ObjectOid[] serverOids = new ObjectOid[message.getClientIds().length];
			/*TODO : even objects that don't have relations should come back to layer2 as they have the oid in the bytes. So we should come back to layer 2 to change the oid
			 * 	one faster way would be to write directly to the bytes the new oid at the right position but it is a bit risky
			if (message.getOabs().size() == 1) {
				// The object being inserted does not have relation to other
				// objects
				// No problem with replacing client oids by server oids
				ObjectOid ooid = clientIds.get(mainOid);
				if (ooid.isNew()) {
					// The oid is new so, it has been created on the client
					// side, it must be replaced by a server id
					ooid = engine.getLayer4().getoidGenerator().getNextObjectOid(mainOid.getClassOid());
					serverOids[0] = ooid;
				}
				mainOab.oid = ooid;
				engine.layer3ToLayer4(message.getOabs());
				oid = ooid;
			} else {
			*/
				// The object being stored has relations, we need to come back
				// to layer 2 (nnoi) and replace client oids (when needed) by
				// server oids
				Map<OID, OID> oidsToReplace = new HashMap<OID, OID>();
				for (int i=0;i<message.getOabs().size();i++) {
					OidAndBytes oab = message.getOabs().get(i);
					ObjectOid oid2 = (ObjectOid) oab.oid;
					// Gets the oid with the 'isNew' flag
					ObjectOid clientOid = clientIds.get(oid2);
					if (clientOid.isNew()) {
						// gets a server oid only if oid is new
						ObjectOid serverOid = engine.getStorageEngine().getOidGenerator().createObjectOid(oid2.getClassOid());
						oidsToReplace.put(oid2, serverOid);
						serverOids[i] = serverOid;
						serverOid.setIsNew(true);
					}
				}
				/*if (oidsToReplace.isEmpty()) {
					// we are only updating, so all s are already server side
					// oids
					// no need to replace oids
					engine.layer3ToLayer4(message.getOabs());
					oid = mainOid;
				} else {*/
					// else we come back to layer 2 and replace client oids by
					// server oids
					NonNativeObjectInfo nnoi = engine.layer3ToLayer2(message.getOabs(), true, oidsToReplace,0);
					
					String className = nnoi.getClassInfo().getFullClassName();
					// Triggers
					if(nnoi.getOid().isNew()){
						if(engine.getTriggerManager().hasInsertTriggersFor(className)){
							engine.getTriggerManager().manageInsertTriggerBefore(className, new ObjectRepresentationImpl(nnoi, engine.getObjectIntrospector()));
						}
					}else{
						if(engine.getTriggerManager().hasUpdateTriggersFor(className)){
							engine.getTriggerManager().manageUpdateTriggerBefore(className , null, new ObjectRepresentationImpl(nnoi, engine.getObjectIntrospector()), nnoi.getOid());
						}
					}
					
					oid = engine.storeMeta(nnoi.getOid(), nnoi);
				//}
			//}

			return new StoreObjectMessageResponse(aBaseIdentifier, message.getSessionId(), oid, objectIsNew, serverOids, session.getValuesToReturn());
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while storing object ";
			DLogger.error(msg, e);
			return new StoreObjectMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("storeObject");
			}
			session.endCurrentAction();
			if (session != null) {
				session.clearValuesToReturn();
			}
		}
	}

	private Message manageStoreClassInfoCommand(StoreClassInfoMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		ObjectOid oid = null;
		try {

			SessionEngine engine = session.getEngine();

			OidAndBytes mainOab = message.getOabs().get(0);
			ClassOid mainOid = (ClassOid) mainOab.oid;
			session.setCurrentAction(ConnectionAction.ACTION_INSERT_OR_UPDATE);
			// actually store the ClassInfo in the meta model database
			engine.layer3ToLayer4(message.getOabs());
			
			// here we may have a class info that has references to another class info that does not exist yet  in the meta model
			// This happens if message.getOabs().size() >1. In this case, we need to create all ClassInfo (without their attribute), and then create the full description
			
			ClassInfoList ciList = new ClassInfoList();
			for(OidAndBytes oab:message.getOabs()){
				// false=> does not load attributes (so no problem with references to non existing CI)
				ciList.addClassInfo(engine.classInfoFromBytes(oab,false));
			}
			// Then add the class infos to the meta model
			session.getMetaModel().addClasses(ciList);
			
			// Now redo the same thing, with full class info description: this time the meta model already contains all CIs for references
			for(OidAndBytes oab:message.getOabs()){
				// Convert to class info and add to the meta model
				ClassInfo ci = engine.classInfoFromBytes(oab,true);
				// this will replace the non complete CI by the complete CI
				session.getMetaModel().addClass(ci, true);			
			}
			return new StoreClassInfoMessageResponse(aBaseIdentifier, message.getSessionId(), mainOid);
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while storing class info ";
			DLogger.error(msg, e);
			return new StoreClassInfoMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("storeClassInfo");
			}
			session.endCurrentAction();
		}
	}

	private Message manageAddClassInfoListCommand(NewClassInfoListMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		SessionManager sessionManager = null;
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(ADD_CLASS_INFO_LIST_MUTEX_NAME);
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new StoreObjectMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			SessionEngine engine = session.getEngine();

			ClassInfoList ciList = message.getClassInfoList();
			ciList = session.addClasses(ciList);
			// here we must create a new list with all class info because
			// Serialization hold object references
			// In this case, it holds the reference of the previous class info
			// list. Serialization thinks object did not change so it will send
			// the reference
			// instead of the new object. Creating the new list force the
			// serialization
			// mechanism to send object
			IOdbList<ClassInfo> allClassInfos = new OdbArrayList<ClassInfo>();
			allClassInfos.addAll(session.getMetaModel().getAllClasses());
			NewClassInfoListMessageResponse r = new NewClassInfoListMessageResponse(aBaseIdentifier, message.getSessionId(), allClassInfos);
			session.resetClassInfoIds();
			return r;
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while adding new Class Info List" + message.getClassInfoList();
			DLogger.error(msg, e);
			return new NewClassInfoListMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("addClassInfoList");
			}
		}
	}

	private Message manageDeleteObjectCommand(DeleteObjectMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		SessionManager sessionManager = null;
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(DELETE_OBJECT_MUTEX_NAME);
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier, null, null, true);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("ODBServer.ConnectionThread:Base ").append(aBaseIdentifier).append(" is not registered on this server!");
				return new DeleteObjectMessageResponse(aBaseIdentifier, message.getSessionId(), buffer.toString());
			}

			session.setCurrentAction(ConnectionAction.ACTION_DELETE);

			SessionEngine engine = session.getEngine();

			engine.deleteObjectWithOid(message.getOid(), message.isCascade());
			return new DeleteObjectMessageResponse(aBaseIdentifier, message.getSessionId(), message.getOid());
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while deleting object " + message.getOid();
			DLogger.error(msg, e);
			return new DeleteObjectMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("deleteObject");
			}
			session.endCurrentAction();
		}
	}

	private Message manageDeleteBaseCommand(DeleteBaseMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		SessionManager sessionManager = null;
		try {
			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(aBaseIdentifier, null, null, true);

			StringBuffer log = new StringBuffer();
			String fileName = message.getBaseIdentifier();
			OdbFile file = new OdbFile(fileName);

			if (sessionManager == null) {
				try {
					if (debug) {
						log.append("Server:Connection manager is null Deleting base " + file.getFullPath()).append(" | exists?").append(
								file.exists());
					}
					if (file.exists()) {
						boolean b = IOUtil.deleteFile(file.getFullPath());
						if (debug) {
							log.append("| deleted=").append(b);
						}
						b = !file.exists();

						if (debug) {
							log.append("| deleted=").append(b);
						}
						if (b) {
							return new DeleteBaseMessageResponse(aBaseIdentifier);
						}
						return new DeleteBaseMessageResponse(aBaseIdentifier, "[1] could not delete base " + file.getFullPath());
					}
					return new DeleteBaseMessageResponse(aBaseIdentifier);
				} finally {
					if (debug) {
						DLogger.info(log.toString());
					}
				}
			}

			Session aSession = sessionManager.getSession(message.getSessionId());
			if (!aSession.isClosed()) {
				aSession.close();
				sessionManager.removeSession(aSession);
				removeSessionManager(aBaseIdentifier);

			}
			boolean b = IOUtil.deleteFile(fileName);
			log.append("| deleted=").append(b);
			if (b) {
				return new DeleteBaseMessageResponse(aBaseIdentifier);
			}
			return new DeleteBaseMessageResponse(aBaseIdentifier, "[2] could not delete base "
					+ new OdbFile(message.getBaseIdentifier()).getFullPath());
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while deleting base " + message.getBaseIdentifier();
			DLogger.error(msg, e);
			return new DeleteBaseMessageResponse(aBaseIdentifier, msg + ":\n" + se);
		} finally {
			connectionIsUp = false;
		}
	}

	private void removeSessionManager(String baseId) {
		server.removeSessionManagerForBase(baseId);

	}

	private Message manageGetObjectsCommand(GetObjectsMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		Mutex mutex = null;
		try {
			//mutex = MutexFactory.get(aBaseIdentifier).acquire(GET_OBJECTS_MUTEX_NAME);
			session.setCurrentAction(ConnectionAction.ACTION_SELECT);

			SessionManager sessionManager = server.getSessionManagerForBase(aBaseIdentifier);
			
			if (session.getConfig().lockObjectsOnSelect()) {

				String fullClassName = QueryManager.getFullClassName(message.getQuery());
				//sessionManager.lockClassForSession(fullClassName, aSession, 2000);
			}

			SessionEngine engine = session.getEngine();
			Objects<NonNativeObjectInfo> metaObjects = null;
			
			// sets engine of the query
			message.getQuery().setSessionEngine(engine);
			
			// TODO check if we can send false instead of true to reduce memory usage
			metaObjects = engine.getMetaObjects(message.getQuery());
			
			if(message.isInMemory()){
				Collection<IOdbList<OidAndBytes>> listOfOabs = new ArrayList<IOdbList<OidAndBytes>>();
				// 	build a list with the oids
				while(metaObjects.hasNext()){
					NonNativeObjectInfo meta = metaObjects.next();
					listOfOabs.add(engine.layer2ToLayer3(meta));
					
					if (session.getConfig().lockObjectsOnSelect()) {
						sessionManager.lockOidForSession(meta.getOid(), session, session.getConfig().getTimeoutToAcquireMutex());
					}
				}
				return new GetObjectsMessageResponse(aBaseIdentifier, message.getSessionId(), listOfOabs, message.getQuery().getExecutionPlan(),false);
			}else{
				Collection<ObjectOid> oids = new ArrayList<ObjectOid>();
				// 	build a list with the oids
				while(metaObjects.hasNext()){
					NonNativeObjectInfo meta = metaObjects.next();
					oids.add(meta.getOid());
					

					if (session.getConfig().lockObjectsOnSelect()) {
						sessionManager.lockOidForSession(meta.getOid(), session, 10000);
					}
				}
				return new GetObjectsMessageResponse(aBaseIdentifier, message.getSessionId(), oids, message.getQuery().getExecutionPlan());
			}
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while getting objects for query " + message.getQuery();
			DLogger.error(msg, e);

			return new GetObjectsMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("getObjects");
			}
			session.endCurrentAction();
		}
	}

	private Message manageGetObjectValuesCommand(GetObjectValuesMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(GET_VALUES_MUTEX_NAME);
			session.setCurrentAction(ConnectionAction.ACTION_SELECT);

			SessionEngine engine = session.getEngine();
			Values values = engine.getValues(message.getQuery());
			return new GetObjectValuesMessageResponse(aBaseIdentifier, message.getSessionId(), values, message.getQuery().getExecutionPlan());
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while getting objects for query " + message.getQuery();
			DLogger.error(msg, e);

			return new GetObjectValuesMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("getObjects");
			}
			session.endCurrentAction();
		}
	}

	private Message manageGetObjectFromIdCommand(GetObjectFromOidMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		ObjectOid oid = null;
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(GET_OBJECT_FROM_ID_MUTEX_NAME);
			session.setCurrentAction(ConnectionAction.ACTION_SELECT);

			SessionEngine engine = session.getEngine();
			oid = message.getOid();
			// here we can't work only on layer3 (bytes) as we need to retrieve all dependencies
			NonNativeObjectInfo nnoi = engine.getMetaObjectFromOid(oid, true,new InstanceBuilderContext(message.getDepth()));
			IOdbList<OidAndBytes> oabs = engine.layer2ToLayer3(nnoi);
			return new GetObjectFromIdMessageResponse(aBaseIdentifier, message.getSessionId(), oabs);
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while getting object of id " + oid;
			DLogger.error(msg, e);

			return new GetObjectFromIdMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("getObjectFromId");
			}
			session.endCurrentAction();
		}
	}

	private Message manageGetObjectHeaderFromIdCommand(GetObjectHeaderFromIdMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		ObjectOid oid = null;
		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(GET_OBJECT_HEADER_FROM_ID_MUTEX_NAME);

			session.setCurrentAction(ConnectionAction.ACTION_SELECT);

			SessionEngine engine = session.getEngine();
			oid = message.getOid();
			ObjectInfoHeader oih = engine.getMetaHeaderFromOid(oid, true, message.useCache());
			// the oih.duplicate method is called to create a new instance of
			// the ObjectInfoHeader becasue of
			// the java Serialization problem : Serialization will check the
			// reference of the object and only send the reference if the object
			// has already
			// been changed. => creating a new will avoid this problem
			return new GetObjectHeaderFromIdMessageResponse(aBaseIdentifier, message.getSessionId(), oih.duplicate());
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while getting object of id " + oid;
			DLogger.error(msg, e);

			return new GetObjectHeaderFromIdMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("getObjectFromId");
			}
			session.endCurrentAction();
		}
	}

	private Message manageCountCommand(CountMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();

		Mutex mutex = null;
		try {
			mutex = MutexFactory.get(aBaseIdentifier).acquire(COUNT_MUTEX_NAME);

			SessionEngine engine = session.getEngine();
			CriteriaQuery query = message.getCriteriaQuery();
			BigInteger nbObjects = engine.count(query);
			return new CountMessageResponse(aBaseIdentifier, message.getSessionId(), nbObjects);
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while counting objects for " + message.getCriteriaQuery();
			DLogger.error(msg, e);

			return new CountMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("count");
			}
		}
	}

	private Message manageConnectCommand(ConnectMessage message) {

		// Gets the base identifier
		baseIdentifier = message.getBaseIdentifier();
		// Gets the aSession manager for this base identifier
		SessionManager sessionManager = null;
		Mutex mutex = null;
		try {

			mutex = MutexFactory.get(baseIdentifier).acquire(COMMIT_CLOSE_CONNECT_MUTEX_NAME);

			// Gets the aSession manager for this base identifier
			sessionManager = getSessionManager(baseIdentifier, message.getUser(), message.getPassword(), false);

			if (sessionManager == null) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("Base ").append(baseIdentifier).append(" is not registered on this server!");
				return new ConnectMessageResponse(baseIdentifier, "?", buffer.toString());
			}

			String ip = message.getIp();
			long dateTime = message.getDateTime();

			session = sessionManager.newSession(ip, dateTime, sessionManager.getNbSessions(),message.isTransactional(), server.getConfig());
			session.setCurrentAction(ConnectionAction.ACTION_CONNECT);

			SessionEngine engine = session.getEngine();

			if (debug) {
				DLogger.info(new StringBuffer("Connection from ").append(ip).append(" - cid=").append(session.getId())
						.append(" - session=").append(session.getId()).append(" - Base Id=").append(baseIdentifier).toString());
			}

			// Returns the meta-model to the client
			MetaModel metaModel = engine.getSession().getMetaModel();
			IOdbList<OidAndBytes> oabs = new OdbArrayList<OidAndBytes>();
			for(ClassInfo ci:metaModel.getAllClasses()){
				oabs.add(engine.classInfoToBytes(ci));
			}
			ConnectMessageResponse cmr = new ConnectMessageResponse(baseIdentifier, session.getId(), oabs, session.getConfig().getOidGeneratorClass().getName());

			return cmr;
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = baseIdentifier + ":Error while connecting to  " + message.getBaseIdentifier();
			DLogger.error(msg, e);
			return new ConnectMessageResponse(baseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("connect");
			}
			if (session != null) {
				session.endCurrentAction();
			}
		}
	}
	
	private Message manageSendFileCommand(SendFileMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		try {

			// just check if the file exist
			File f = new File(server.getConfig().getInboxDirectory()+"/"+ message.getRemoteFileName());
			boolean fileExist = f.exists();
			long size = f.length();
			
			return new SendFileMessageResponse(aBaseIdentifier, message.getSessionId(), fileExist, size);
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while receiving file";
			DLogger.error(msg, e);
			return new SendFileMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("File");
			}
			session.endCurrentAction();
		}
	}
	
	private Message manageGetFileCommand(GetFileMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		try {

			return new GetFileMessageResponse(aBaseIdentifier, message.getSessionId(), message.isGetFileInServerInbox(), message.getRemoteFileName(), message.isPutFileInClientInbox(), message.getLocalFileName());
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while getting file";
			DLogger.error(msg, e);
			return new GetFileMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("File");
			}
			session.endCurrentAction();
		}
	}
	
	private Message manageRemoteProcessCommand(RemoteProcessMessage message) {

		// Gets the base identifier
		String aBaseIdentifier = message.getBaseIdentifier();
		Mutex mutex = null;
		try {
			// we create an odb that wraps the aSession odb. But in this case, eventhough the aSession is a server aSession, the
			// the execution happens as if it where a local odb. That is why we wrap the aSession to change the execution type
			ODB odb = new ODBForTrigger(new SessionWrapper(session, ExecutionType.LOCAL_CLIENT));
			RemoteProcess rp = message.getProcess();
			rp.setOdb(odb);
			rp.setServerConfig(server.getConfig());
			RemoteProcessReturn r = rp.execute();
			
			return new RemoteProcessMessageResponse(aBaseIdentifier, message.getSessionId(), r);
		} catch (Exception e) {
			String se = OdbString.exceptionToString(e, false);
			String msg = aBaseIdentifier + ":Error while executing remote process";
			DLogger.error(msg, e);
			return new RemoteProcessMessageResponse(aBaseIdentifier, message.getSessionId(), msg + ":\n" + se);
		} finally {
			if (mutex != null) {
				mutex.release("File");
			}
			session.endCurrentAction();
		}
	}

}

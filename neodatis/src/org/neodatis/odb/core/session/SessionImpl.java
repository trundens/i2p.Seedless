/**
 * 
 */
package org.neodatis.odb.core.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;

import org.neodatis.odb.DatabaseId;
import org.neodatis.odb.NeoDatisConfig;
import org.neodatis.odb.NeoDatisEventType;
import org.neodatis.odb.NeoDatisRuntimeException;
import org.neodatis.odb.OID;
import org.neodatis.odb.TransactionId;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.odb.core.event.EventManager;
import org.neodatis.odb.core.event.EventManagerImpl;
import org.neodatis.odb.core.event.NeoDatisEventListener;
import org.neodatis.odb.core.layers.layer2.meta.ClassInfo;
import org.neodatis.odb.core.layers.layer2.meta.ClassInfoList;
import org.neodatis.odb.core.layers.layer2.meta.MetaModel;
import org.neodatis.odb.core.layers.layer2.meta.MetaModelImpl;
import org.neodatis.odb.core.layers.layer2.meta.ODBType;
import org.neodatis.odb.core.layers.layer2.meta.ObjectInfoHeader;
import org.neodatis.odb.core.layers.layer4.BaseIdentification;
import org.neodatis.odb.core.layers.layer4.OidGenerator;
import org.neodatis.odb.core.oid.DatabaseIdImpl;
import org.neodatis.odb.core.oid.TransactionIdImpl;
import org.neodatis.odb.core.trigger.CommitListener;
import org.neodatis.tool.DLogger;
import org.neodatis.tool.mutex.Mutex;
import org.neodatis.tool.mutex.MutexFactory;
import org.neodatis.tool.wrappers.OdbThread;
import org.neodatis.tool.wrappers.OdbTime;
import org.neodatis.tool.wrappers.list.IOdbList;
import org.neodatis.tool.wrappers.list.OdbArrayList;
import org.neodatis.tool.wrappers.map.OdbHashMap;

/**
 * @author olivier
 * 
 */
public class SessionImpl implements Session {
	public static final String LOG_ID = "SessionImpl";

	protected BaseIdentification identification;
	protected SessionEngine engine;
	protected MetaModel metaModel;
	protected Cache cache;
	protected String id;
	protected DatabaseId databaseId;
	protected TransactionId transactionId;
	protected boolean isClosed;
	protected boolean isRollbacked;
	protected IOdbList<CommitListener> commitListeners;
	protected boolean isCommitted;

	protected Map<OID, Mutex> lockedOids;
	protected Map<String, Mutex> lockedClasses;
	
	protected EventManager eventManager;
	protected NeoDatisConfig config;
	protected OidGenerator oidGenerator;

	/**
	 * @param fileParameter
	 */
	public SessionImpl(BaseIdentification parameter) {
		init(parameter, null);
	}

	/**
	 * @param parameter
	 * @param sessionId
	 */
	public SessionImpl(BaseIdentification parameter, String sessionId) {
		init(parameter, sessionId);
	}

	protected void init(BaseIdentification baseIdentification, String sessionId) {
		this.config = baseIdentification.getConfig();
		this.identification = baseIdentification;

		this.eventManager = new EventManagerImpl(); 
		lockedOids = new OdbHashMap<OID, Mutex>();
		lockedClasses = new OdbHashMap<String, Mutex>();

		if (sessionId != null) {
			this.id = sessionId;
		} else {
			this.id = new StringBuffer(baseIdentification.getBaseId()).append(System.currentTimeMillis()).toString();
		}

		this.cache = new CacheImpl();
		this.engine = buildSessionEngine();
		this.transactionId = new TransactionIdImpl(this.id);
		this.isClosed = false;
		this.isRollbacked = false;
		this.isCommitted = false;
		commitListeners = new OdbArrayList<CommitListener>();
		oidGenerator = config.getCoreProvider().getOidGenerator();
		initDatabase();

	}

	protected SessionEngine buildSessionEngine() {
		return new SessionEngineImpl(this);
	}

	/**
	 * @return
	 */
	protected void initDatabase() {
		initDatabaseHeader();
		initMetaModel();
	}

	/**
	 * 
	 */
	private void initDatabaseHeader() {
		this.databaseId = DatabaseIdImpl.fromString(UUID.randomUUID().toString());

	}

	/**
	 * @return
	 */
	protected void initMetaModel() {

		metaModel = new MetaModelImpl(this.getConfig());
		engine.loadMetaModel(metaModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.neodatis.odb.core.session.Session#addClasses(org.neodatis.odb.core
	 * .layers.layer2.meta.ClassInfoList)
	 */
	public ClassInfoList addClasses(ClassInfoList ciList) {
		metaModel.addClasses(ciList);
		engine.storeClassInfos(ciList);
		return ciList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.neodatis.odb.core.session.Session#addObjectToCache(org.neodatis.odb
	 * .OID, java.lang.Object,
	 * org.neodatis.odb.core.layers.layer2.meta.ObjectInfoHeader)
	 */
	public void addObjectToCache(OID oidCrossSession, Object o, ObjectInfoHeader oih) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#close()
	 */
	public void close() {
		if (!isRollbacked) {
			commit();
		}
		engine.close();
		isClosed = true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#commit()
	 */
	public void commit() {
		if (isRollbacked) {
			return;
		}
		if (isCommitted) {
			return;
		}
		manageCommitListenersBefore();
		engine.commit();
		manageCommitListenersAfter();
		isCommitted = true;
		unlockObjectsAndClasses();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#getBaseIdentification()
	 */
	public BaseIdentification getBaseIdentification() {
		return identification;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#getCache()
	 */
	public Cache getCache() {
		return cache;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#getClassInfo(java.lang.String)
	 */
	public ClassInfo getClassInfo(String fullClassName) {
		if (ODBType.getFromName(fullClassName).isNative()) {
			return null;
		}
		MetaModel aMetaModel = getMetaModel();

		if (aMetaModel.existClass(fullClassName)) {
			return aMetaModel.getClassInfo(fullClassName, true);
		}
		ClassInfo ci = null;
		ClassInfoList ciList = null;
		ciList = engine.introspectClass(fullClassName);
		// to enable junit tests
		addClasses(ciList);
		// old:For client Server : reset meta model
		// if (!storageEngine.isLocal()) {
		// aMetaModel = session.getMetaModel();
		// }
		// /old
		ci = aMetaModel.getClassInfo(fullClassName, true);
		return ci;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#getId()
	 */
	public String getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#getMetaModel()
	 */
	public MetaModel getMetaModel() {
		return metaModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.neodatis.odb.core.session.Session#getObjectInfoHeaderFromOid(org.
	 * neodatis.odb.OID, boolean)
	 */
	public ObjectInfoHeader getObjectInfoHeaderFromOid(OID oidCrossSession, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public int getExecutionType() {
		return ExecutionType.LOCAL_CLIENT;
	}
	
	public boolean isLocal() {
		return true;
	}

	public boolean isRollbacked() {
		return isRollbacked;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#rollback()
	 */
	public void rollback() {
		engine.rollback();
		isRollbacked = true;
		unlockObjectsAndClasses();
	}

	public SessionEngine getEngine() {
		return engine;
	}

	public TransactionId getCurrentTransactionId() {
		return transactionId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#getDatabaseId()
	 */
	public DatabaseId getDatabaseId() {
		return databaseId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#clearCache()
	 */
	public void clearCache() {
		cache.clear();
	}

	public void update(Observable o, Object arg) {
		throw new NeoDatisRuntimeException(NeoDatisError.NOT_YET_IMPLEMENTED);

	}

	public void addCommitListener(CommitListener commitListener) {
		this.commitListeners.add(commitListener);
	}

	public IOdbList<CommitListener> getCommitListeners() {
		return commitListeners;
	}

	private void manageCommitListenersAfter() {
		if (commitListeners == null || commitListeners.isEmpty()) {
			return;
		}
		Iterator<CommitListener> iterator = commitListeners.iterator();
		CommitListener commitListener = null;
		while (iterator.hasNext()) {
			commitListener = iterator.next();
			commitListener.afterCommit();
		}
	}

	private void manageCommitListenersBefore() {
		if (commitListeners == null || commitListeners.isEmpty()) {
			return;
		}
		Iterator<CommitListener> iterator = commitListeners.iterator();
		CommitListener commitListener = null;
		while (iterator.hasNext()) {
			commitListener = iterator.next();
			commitListener.beforeCommit();
		}
	}

	public void setMetaModel(MetaModel metaModel) {
		this.metaModel = metaModel;

		// persist classes
		for (ClassInfo ci : metaModel.getAllClasses()) {
			engine.storeClassInfo(ci);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#transactionIsPending()
	 */
	public boolean transactionIsPending() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#endCurrentAction()
	 */
	public void endCurrentAction() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#setCurrentAction(int)
	 */
	public void setCurrentAction(int action) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.neodatis.odb.core.session.Session#setId(java.lang.String)
	 */
	public void setId(String sessionId) {
		this.id = sessionId;
	}

	public synchronized void lockOidForSession(OID oid, long timeout) throws InterruptedException {
		boolean locked = false;
		long start = OdbTime.getCurrentTimeInMs();

		if (config.isDebugEnabled(LOG_ID)) {
			start = OdbTime.getCurrentTimeInMs();
			DLogger.debug("Trying to lock object with oid " + oid + " - session id=" + getId() + " - Thread = "
					+ OdbThread.getCurrentThreadName());
		}
		try {
			Mutex mutex = lockedOids.get(oid);
			if (mutex == null) {
				mutex = MutexFactory.get(getBaseIdentification().getBaseId() + oid.oidToString());
				locked = mutex.attempt(config.getTimeoutToAcquireMutex());
				if(!locked){
					throw new LockTimeOutException("Object with oid "+oid.oidToString() + " - session id " + getId() + " - Thread = " + OdbThread.getCurrentThreadName());
				}
				lockedOids.put(oid, mutex);
				
				return;
			}
		} finally {
			if (locked) {
				if (config.isDebugEnabled(LOG_ID)) {
					DLogger.debug("Object with oid " + oid + " locked (" + (OdbTime.getCurrentTimeInMs() - start) + "ms) - "
							+ getId() + " - Thread = " + OdbThread.getCurrentThreadName());
				}
			}
		}
	}

	public synchronized void lockClassForSession(String fullClassName, long timeout) throws InterruptedException {
		long start = OdbTime.getCurrentTimeInMs();
		boolean locked = false;
		if (config.isDebugEnabled(LOG_ID)) {
			start = OdbTime.getCurrentTimeInMs();
			DLogger.debug(String.format("CM:Trying to lock class %s - id=%s", fullClassName, getId()));
		}
		try {
			Mutex mutex = lockedClasses.get(fullClassName);
			if(mutex!=null){
				mutex = MutexFactory.get(getBaseIdentification().getBaseId() + fullClassName);
				locked = mutex.attempt(timeout);
				if(!locked){
					throw new LockTimeOutException("Class with name "+fullClassName);
				}

				lockedClasses.put(fullClassName, mutex);
			}
			return;
		} finally {
			if (config.isDebugEnabled(LOG_ID)) {
				DLogger.debug(String.format("Class %s locked (%dms) - %s", fullClassName, (OdbTime.getCurrentTimeInMs() - start), getId()));
			}
		}
	}

	public synchronized void unlockOidForSession(OID oid) throws InterruptedException {
		long start = OdbTime.getCurrentTimeInMs();

		if (config.isDebugEnabled(LOG_ID)) {
			start = OdbTime.getCurrentTimeInMs();
			DLogger.debug("Trying to unlock lock object with oid " + oid + " - id=" + getId());
		}

		try {
			Mutex mutex = lockedOids.get(oid);
			if (mutex != null) {
				mutex.release(getId());
				lockedOids.remove(oid);
			}
		} finally {
			if (config.isDebugEnabled(LOG_ID)) {
				DLogger.debug("Object with oid " + oid + " unlocked (" + (OdbTime.getCurrentTimeInMs() - start) + "ms) - "
						+ getId());
			}
		}
	}

	public synchronized void unlockClass(String fullClassName) throws InterruptedException {
		long start = OdbTime.getCurrentTimeInMs();

		if (config.isDebugEnabled(LOG_ID)) {
			start = OdbTime.getCurrentTimeInMs();
			DLogger.debug("Trying to unlock class " + fullClassName + " - id=" + getId());
		}

		try {
			Mutex mutex = lockedClasses.get(fullClassName);
			if (mutex != null) {
				mutex.release(getId());
				lockedClasses.remove(fullClassName);
			}
		} finally {
			if (config.isDebugEnabled(LOG_ID)) {
				DLogger.debug("Class  " + fullClassName + " unlocked (" + (OdbTime.getCurrentTimeInMs() - start) + "ms) - "
						+ getId());
			}
		}
	}
	
	/** Release all objects and classes lock by this session
	 * 
	 */
	public void unlockObjectsAndClasses(){
		Iterator<Mutex> objectMutexes = lockedOids.values().iterator() ;
		while(objectMutexes.hasNext()){
			objectMutexes.next().release(getId());
		}
		Iterator<Mutex> classMutexes = lockedClasses.values().iterator() ;
		while(classMutexes.hasNext()){
			classMutexes.next().release(getId());
		}
	}

	/* (non-Javadoc)
	 * @see org.neodatis.odb.core.session.Session#registerEventListenerFor(org.neodatis.odb.NeoDatisEventType, org.neodatis.odb.EventListener)
	 */
	public void registerEventListenerFor(NeoDatisEventType neoDatisEventType, NeoDatisEventListener eventListener) {
		eventManager.addEventListener(neoDatisEventType, eventListener);
	}

	public void updateMetaModel() {
		MetaModel aMetaModel = getMetaModel();
		DLogger.info("Automatic refactoring : updating meta model");
		
		// User classes : 
		List<ClassInfo> userClasses = new ArrayList<ClassInfo>(aMetaModel.getUserClasses());
		Iterator<ClassInfo> iterator = userClasses.iterator();
		//Iterator iterator = aMetaModel.getUserClasses().iterator();
		
		while (iterator.hasNext()) {
			engine.storeClassInfo(iterator.next());
		}
		// System classes
		iterator = aMetaModel.getSystemClasses().iterator();
		while (iterator.hasNext()) {
			engine.storeClassInfo(iterator.next());
		}

	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public NeoDatisConfig getConfig() {
		return config;
	}

	public OidGenerator getOidGenerator() {
		return oidGenerator;
	}

}

/**
 *
 */
package org.neodatis.odb.core.config;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.neodatis.odb.DatabaseStartupManager;
import org.neodatis.odb.NeoDatisConfig;
import org.neodatis.odb.NeoDatisGlobalConfig;
import org.neodatis.odb.NeoDatisRuntimeException;
import org.neodatis.odb.core.CoreProvider;
import org.neodatis.odb.core.CoreProviderImpl;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.odb.core.layers.layer3.ByteArrayConverterImpl;
import org.neodatis.odb.core.layers.layer4.plugin.jdbm.NeoDatisJdbmPlugin;
// future?
//import org.neodatis.odb.core.layers.layer4.plugin.jdbm3.Jdbm3StorageEngine;
import org.neodatis.odb.core.oid.uuid.UniqueOidGeneratorImpl;
import org.neodatis.odb.core.query.IQueryExecutorCallback;
import org.neodatis.odb.core.server.MessageStreamerImpl;
import org.neodatis.tool.wrappers.ConstantWrapper;
import org.neodatis.tool.wrappers.NeoDatisClassLoader;
import org.neodatis.tool.wrappers.map.OdbHashMap;

/**
 * @author olivier
 *
 */
public class NeoDatisConfigImpl implements NeoDatisConfig {
	public boolean coreProviderInit = false;
	public boolean debugEnabled = false;
	public boolean logAll = false;
	public int debugLevel = 100;
	public Map<String, String> logIds = null;
	public boolean infoEnabled = false;
	// public long maxNumberOfObjectInCache = 3000000;
	public long maxNumberOfObjectInCache = 1000000;
	public boolean automaticCloseFileOnExit = false;

	public String defaultDatabaseCharacterEncoding = "UTF-8";
	public String databaseCharacterEncoding = defaultDatabaseCharacterEncoding;

	public boolean throwExceptionWhenInconsistencyFound = true;
	public boolean checkMetaModelCompatibility = true;
	public boolean logSchemaEvolutionAnalysis = false;
	public boolean monitorMemory = false;
	public boolean debugLayers = false;
	public boolean debugMessageStreamer = false;
	public boolean debugStorageEngine = false;
	public long timeoutToAcquireMutex = 1000;

	/**
	 * A boolean value to indicate if ODB can create empty constructor when not
	 * available
	 */
	public boolean enableEmptyConstructorCreation = true;

	// For multi thread
	/**
	 * a boolean value to specify if ODBFactory waits a little to re-open a file
	 * when a file is locked
	 */
	public boolean retryIfFileIsLocked = true;
	/** How many times ODBFactory tries to open the file when it is locked */
	public int numberOfRetryToOpenFile = 5;
	/** How much time (in ms) ODBFactory waits between each retry */
	public long retryTimeout = 100;

	/** Automatically increase cache size when it is full */
	public boolean automaticallyIncreaseCacheSize = false;

	public boolean useCache = true; // false; // true

	public boolean logServerStartupAndShutdown = true;

	public boolean logServerConnections = false;

	/** The default btree size for index btrees */
	public int defaultIndexBTreeDegree = 20;

	/** The default btree size for collection btrees */
	public int defaultCollectionBTreeDegree = 20;

	/**
	 * The type of cache. If true, the cache use weak references that allows
	 * very big inserts,selects like a million of objects. But it is a little
	 * bit slower than setting to false
	 */
	public boolean useLazyCache = false;

	/** To indicate if warning must be displayed */
	public boolean displayWarnings = true;

	public IQueryExecutorCallback queryExecutorCallback = null;

	/** Scale used for average action * */
	public int scaleForAverageDivision = 2;

	/** Round Type used for the average division */
	public int roundTypeForAverageDivision = ConstantWrapper.ROUND_TYPE_FOR_AVERAGE_DIVISION;

	/** for IO atomic : password for encryption */
	public String encryptionPassword;

	/** The core provider is the provider of core object implementation for ODB */
	public CoreProvider coreProvider;

	/** To indicate if NeoDatis must check the runtime version, defaults to yes */
	public boolean checkRuntimeVersion = true;

	/**
	 * To specify if NeoDatis must automatically reconnect objects loaded in
	 * previous session. With with flag on, user does not need to manually
	 * reconnect an object. Default value = true
	 */
	public boolean reconnectObjectsToSession = false;

	public ClassLoader classLoader = NeoDatisClassLoader.getCurrent();
	public Class messageStreamerClass = MessageStreamerImpl.class;

	/**
	 * To activate or desactivate the use of index
	 *
	 */
	public boolean useIndex = true;

	/**
	 * Used to let same vm mode use an odb connection on severals different
	 * threads
	 *
	 */
	public boolean shareSameVmConnectionMultiThread = true;
	public boolean lockObjectsOnSelect = false;
	/** Used to specify a base directory for database creations, default is null */
	public String baseDirectory = null;

	/**
	 * used to indicate if lazy instantiation must be used in cs mode Default
	 * behavior is that where retrieving a list of objects, the server return
	 * the list of meta representation (NonNativeObjectInfo:NNOI) to the client.
	 * The server then uses a specific list LazySimpleListOfAOI that keeps the
	 * NNOIs and only instantiate the the real object (java object) when user
	 * requests it.
	 * */
	public boolean useLazyInstantiationInServerMode = true;

	/** a DatabaseStartupManager is called on every database open */
	public DatabaseStartupManager databaseStartupManager = null;

	/** to tell NeoDatis if session must be auto committed */
	public boolean sessionAutoCommit = true;

	public Class storageEngineClass;

	public boolean transactional;
	public String user;
	public String password;
	public String homeDirectory;
	public String host;
	public int port;
	public boolean isLocal;

	protected boolean allowDirtyReads;
	protected boolean commitNoSync;
	/**
	 * to indicate if storage must use its native config or take from this
	 * properties object
	 *
	 */
	public boolean useNativeConfig;
	public List<Server> servers;

	/**
	 * The default directory to receive files on the server
	 */
	protected String serverInboxDirectory;

	/** To indicate if client server mode is using ssl*/
	protected boolean ssl;

	protected Class oidGeneratorClass;

	/** To specify if the underlying storage engine can use the its cache or not*/
	protected boolean useStorageEngineCache;

	/** To specify if the oid generator can use cache*/
	protected boolean oidGeneratorUseCache;

	public NeoDatisConfigImpl(boolean override) {
		super();
		this.transactional = true;
		this.isLocal = true;
		servers = new ArrayList<Server>();
		this.coreProvider = new CoreProviderImpl(this);
		this.storageEngineClass = NeoDatisJdbmPlugin.class;
        // future?
        //this.storageEngineClass = Jdbm3StorageEngine.class;
		this.serverInboxDirectory = "inbox";
		this.oidGeneratorClass = UniqueOidGeneratorImpl.class;
		this.oidGeneratorUseCache = true;
		this.useStorageEngineCache = true;

		if (override) {
			// override values with global config
			this.checkMetaModelCompatibility = NeoDatisGlobalConfig.get().checkMetaModelCompatibility();
			this.scaleForAverageDivision = NeoDatisGlobalConfig.get().getScaleForAverageDivision();
			this.roundTypeForAverageDivision = NeoDatisGlobalConfig.get().getRoundTypeForAverageDivision();
			this.storageEngineClass = NeoDatisGlobalConfig.get().getStorageEngineClass();
			this.messageStreamerClass = NeoDatisGlobalConfig.get().getMessageStreamerClass();
			this.serverInboxDirectory = NeoDatisGlobalConfig.get().getInboxDirectory();

		}

	}

	/**
	 * @throws ClassNotFoundException
	 *
	 */
	public NeoDatisConfig updateFromFile(String fileName) throws ClassNotFoundException {
		Properties properties = new Properties();
		try {
			properties = ConfigFileReader.read(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String storageEngineClassName = properties.getProperty(NeoDatisConfigTokens.STORAGE_ENGINE);
		if (storageEngineClassName != null) {
			setStorageEngineClass(Class.forName(storageEngineClassName));
		}

		String autocommit = properties.getProperty(NeoDatisConfigTokens.SESSION_AUTO_COMMIT);

		if (autocommit != null) {
			setSessionAutoCommit(autocommit.equals("true"));
		}
		String serverInboxDirectoryT = properties.getProperty(NeoDatisConfigTokens.SERVER_INBO_DIRECTORY);

		if (serverInboxDirectoryT != null) {
			setServerInboxDirectory(serverInboxDirectoryT);
		}
		return this;
	}

	public boolean isSessionAutoCommit() {
		return sessionAutoCommit;
	}

	public NeoDatisConfig setSessionAutoCommit(boolean sessionAutoCommit) {
		this.sessionAutoCommit = sessionAutoCommit;
		return this;
	}

	public Class getStorageEngineClass() {
		return storageEngineClass;
	}

	public NeoDatisConfig setStorageEngineClass(Class storageEngineClass) {
		this.storageEngineClass = storageEngineClass;
		return this;
	}

	/**
	 * @return
	 */
	public boolean reconnectObjectsToSession() {
		return reconnectObjectsToSession;
	}

	public NeoDatisConfig setReconnectObjectsToSession(boolean reconnectObjectsToSession) {
		this.reconnectObjectsToSession = reconnectObjectsToSession;
		return this;
	}

	public NeoDatisConfig addLogId(String logId) {
		if (logIds == null) {
			logIds = new OdbHashMap<String, String>();
		}
		logIds.put(logId, logId);
		return this;
	}

	public NeoDatisConfig removeLogId(String logId) {
		if (logIds == null) {
			logIds = new OdbHashMap<String, String>();
		}
		logIds.remove(logId);
		return this;
	}

	public boolean isDebugEnabled(String logId) {
		if (!debugEnabled) {
			return false;
		}
		if (logAll) {
			return true;
		}

		if (logIds == null || logIds.size() == 0) {
			return false;
		}

		return logIds.containsKey(logId);
	}

	public NeoDatisConfig setDebugEnabled(int level, boolean debug) {
		debugEnabled = debug;
		debugLevel = level;
		return this;
	}

	public boolean isInfoEnabled() {
		return infoEnabled;
	}

	public boolean isInfoEnabled(String logId) {
		// return false;

		if (logAll) {
			return true;
		}

		if (logIds == null || logIds.size() == 0) {
			return false;
		}

		return logIds.containsKey(logId);

		// return false;
	}

	public NeoDatisConfig setInfoEnabled(boolean infoEnabled) {
		this.infoEnabled = infoEnabled;
		return this;
	}

	public long getMaxNumberOfObjectInCache() {
		return maxNumberOfObjectInCache;
	}

	public NeoDatisConfig setMaxNumberOfObjectInCache(long maxNumberOfObjectInCache) {
		this.maxNumberOfObjectInCache = maxNumberOfObjectInCache;
		return this;
	}

	public int getNumberOfRetryToOpenFile() {
		return numberOfRetryToOpenFile;
	}

	public NeoDatisConfig setNumberOfRetryToOpenFile(int numberOfRetryToOpenFile) {
		this.numberOfRetryToOpenFile = numberOfRetryToOpenFile;
		return this;
	}

	public long getRetryTimeout() {
		return retryTimeout;
	}

	public NeoDatisConfig setRetryTimeout(long retryTimeout) {
		this.retryTimeout = retryTimeout;
		return this;
	}

	public boolean retryIfFileIsLocked() {
		return retryIfFileIsLocked;
	}

	public NeoDatisConfig setRetryIfFileIsLocked(boolean retryIfFileIsLocked) {
		this.retryIfFileIsLocked = retryIfFileIsLocked;
		return this;
	}

	public boolean isMultiThread() {
		return retryIfFileIsLocked;
	}

	public NeoDatisConfig useMultiThread(boolean yes) {
		useMultiThread(yes, numberOfRetryToOpenFile);
		return this;
	}

	public NeoDatisConfig useMultiThread(boolean yes, int numberOfThreads) {
		setRetryIfFileIsLocked(yes);
		if (yes) {
			setNumberOfRetryToOpenFile(numberOfThreads * 10);
			setRetryTimeout(50);
		}
		return this;
	}

	public boolean throwExceptionWhenInconsistencyFound() {
		return throwExceptionWhenInconsistencyFound;
	}

	public NeoDatisConfig setThrowExceptionWhenInconsistencyFound(boolean throwExceptionWhenInconsistencyFound) {
		this.throwExceptionWhenInconsistencyFound = throwExceptionWhenInconsistencyFound;
		return this;
	}

	public boolean automaticallyIncreaseCacheSize() {
		return automaticallyIncreaseCacheSize;
	}

	public NeoDatisConfig setAutomaticallyIncreaseCacheSize(boolean automaticallyIncreaseCache) {
		automaticallyIncreaseCacheSize = automaticallyIncreaseCache;
		return this;
	}

	/**
	 * @return Returns the debugLevel.
	 */
	public int getDebugLevel() {
		return debugLevel;
	}

	/**
	 * @param debugLevel
	 *            The debugLevel to set.
	 */
	public NeoDatisConfig setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
		return this;
	}

	public boolean checkMetaModelCompatibility() {
		return checkMetaModelCompatibility;
	}

	public NeoDatisConfig setCheckMetaModelCompatibility(boolean checkModelCompatibility) {
		checkMetaModelCompatibility = checkModelCompatibility;
		return this;
	}

	public boolean automaticCloseFileOnExit() {
		return automaticCloseFileOnExit;
	}

	public NeoDatisConfig setAutomaticCloseFileOnExit(boolean automaticFileClose) {
		automaticCloseFileOnExit = automaticFileClose;
		return this;
	}

	public boolean isLogAll() {
		return logAll;
	}

	public NeoDatisConfig setLogAll(boolean logAll) {
		this.logAll = logAll;
		return this;
	}

	public boolean logServerConnections() {
		return logServerConnections;
	}

	public NeoDatisConfig setLogServerConnections(boolean logServerConnections) {
		this.logServerConnections = logServerConnections;
		return this;
	}

	public int getDefaultIndexBTreeDegree() {
		return defaultIndexBTreeDegree;
	}

	public NeoDatisConfig setDefaultIndexBTreeDegree(int defaultIndexBTreeSize) {
		defaultIndexBTreeDegree = defaultIndexBTreeSize;
		return this;
	}

	public int getDefaultCollectionBTreeDegree() {
		return defaultCollectionBTreeDegree;
	}

	public NeoDatisConfig setDefaultCollectionBTreeDegree(int defaultIndexBTreeSize) {
		defaultCollectionBTreeDegree = defaultIndexBTreeSize;
		return this;
	}

	public boolean useLazyCache() {
		return useLazyCache;
	}

	public NeoDatisConfig setUseLazyCache(boolean useLazyCache) {
		this.useLazyCache = useLazyCache;
		return this;
	}

	/**
	 * @return the queryExecutorCallback
	 */
	public IQueryExecutorCallback getQueryExecutorCallback() {
		return queryExecutorCallback;
	}

	/**
	 * @param queryExecutorCallback
	 *            the queryExecutorCallback to set
	 */
	public NeoDatisConfig setQueryExecutorCallback(IQueryExecutorCallback queryExecutorCallback) {
		this.queryExecutorCallback = queryExecutorCallback;
		return this;
	}

	/**
	 * @return the useCache
	 */
	public boolean useCache() {
		return useCache;
	}

	/**
	 * @param useCache
	 *            the useCache to set
	 */
	public NeoDatisConfig setUseCache(boolean useCache) {
		this.useCache = useCache;
		return this;
	}

	public boolean monitoringMemory() {
		return monitorMemory;
	}

	public NeoDatisConfig setMonitorMemory(boolean yes) {
		monitorMemory = yes;
		return this;
	}

	public boolean displayWarnings() {
		return displayWarnings;
	}

	public NeoDatisConfig setDisplayWarnings(boolean yesOrNo) {
		displayWarnings = yesOrNo;
		return this;
	}

	public int getScaleForAverageDivision() {
		return scaleForAverageDivision;
	}

	public NeoDatisConfig setScaleForAverageDivision(int scaleForAverageDivision) {
		this.scaleForAverageDivision = scaleForAverageDivision;
		return this;
	}

	public int getRoundTypeForAverageDivision() {
		return roundTypeForAverageDivision;
	}

	public NeoDatisConfig setRoundTypeForAverageDivision(int roundTypeForAverageDivision) {
		this.roundTypeForAverageDivision = roundTypeForAverageDivision;
		return this;
	}

	public boolean enableEmptyConstructorCreation() {
		return enableEmptyConstructorCreation;
	}

	public NeoDatisConfig setEnableEmptyConstructorCreation(boolean enableEmptyConstructorCreation) {
		this.enableEmptyConstructorCreation = enableEmptyConstructorCreation;
		return this;
	}

	public String getEncryptionPassword() {
		return encryptionPassword;
	}

	public CoreProvider getCoreProvider() {
		if (!coreProviderInit) {
			coreProviderInit = true;
			try {
				coreProvider.init2();
			} catch (Exception e) {
				throw new NeoDatisRuntimeException(NeoDatisError.ERROR_IN_CORE_PROVIDER_INITIALIZATION.addParameter("Core Provider"), e);
			}

		}
		return coreProvider;
	}

	public NeoDatisConfig setCoreProvider(CoreProvider coreProvider) {
		this.coreProvider = coreProvider;
		return this;
	}

	public String getDatabaseCharacterEncoding() {
		return databaseCharacterEncoding;
	}

	public NeoDatisConfig setDatabaseCharacterEncoding(String dbCharacterEncoding) throws UnsupportedEncodingException {
		if (dbCharacterEncoding != null) {
			// Checks if encoding is valid, using it in the String.getBytes
			// method
			new ByteArrayConverterImpl(false, dbCharacterEncoding,this).testEncoding(dbCharacterEncoding);
			databaseCharacterEncoding = dbCharacterEncoding;
		} else {
			databaseCharacterEncoding = null;
		}
		return this;
	}

	public NeoDatisConfig setLatinDatabaseCharacterEncoding() throws UnsupportedEncodingException {
		databaseCharacterEncoding = defaultDatabaseCharacterEncoding;
		return this;
	}

	public boolean hasEncoding() {
		return databaseCharacterEncoding != null;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public NeoDatisConfig setClassLoader(ClassLoader cl) {

		if (cl == null) {
			throw new NeoDatisRuntimeException(NeoDatisError.INTERNAL_ERROR.addParameter("Class loader is null!"));
		}

		classLoader = cl;

		// throw new ODBRuntimeException(NeoDatisError.NOT_YET_IMPLEMENTED);
		// getCoreProvider().getClassIntrospector().reset();
		// getCoreProvider().getClassPool().reset();
		return this;

	}

	public boolean checkRuntimeVersion() {
		return checkRuntimeVersion;
	}

	public NeoDatisConfig setCheckRuntimeVersion(boolean checkJavaRuntimeVersion) {
		checkRuntimeVersion = checkJavaRuntimeVersion;
		return this;
	}

	/**
	 * @return
	 */
	public Class getMessageStreamerClass() {
		// throw new ODBRuntimeException(NeoDatisError.NOT_YET_IMPLEMENTED);
		return messageStreamerClass;
	}

	public NeoDatisConfig setMessageStreamerClass(Class messageStreamerClass) {
		this.messageStreamerClass = messageStreamerClass;
		return this;
	}

	public boolean logServerStartupAndShutdown() {
		return logServerStartupAndShutdown;
	}

	public NeoDatisConfig setLogServerStartupAndShutdown(boolean logServerStartup) {
		logServerStartupAndShutdown = logServerStartup;
		return this;
	}

	public boolean useIndex() {
		return useIndex;
	}

	public NeoDatisConfig setUseIndex(boolean useIndex) {
		this.useIndex = useIndex;
		return this;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public NeoDatisConfig setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
		return this;
	}

	public boolean shareSameVmConnectionMultiThread() {
		return shareSameVmConnectionMultiThread;
	}

	public NeoDatisConfig setShareSameVmConnectionMultiThread(boolean shareSameVmConnectionMultiThread) {
		this.shareSameVmConnectionMultiThread = shareSameVmConnectionMultiThread;
		return this;
	}

	/**
	 *
	 */
	public NeoDatisConfig lockObjectsOnSelect(boolean yesNo) {
		lockObjectsOnSelect = yesNo;
		return this;
	}

	public boolean lockObjectsOnSelect() {
		return lockObjectsOnSelect;
	}

	/**
	 * @return
	 */
	public boolean debugLayers() {
		return debugLayers;
	}

	public NeoDatisConfig setDebugLayers(boolean yesNo) {
		debugLayers = yesNo;
		return this;
	}
	public boolean debugMessageStreamer() {
		return debugMessageStreamer;
	}

	public NeoDatisConfig setDebugMessageStreamer(boolean yesNo) {
		debugMessageStreamer = yesNo;
		return this;
	}

	public boolean debugStorageEngine() {
		return debugStorageEngine;
	}

	public NeoDatisConfig setDebugStorageEngine(boolean yesNo) {
		debugStorageEngine = yesNo;
		return this;
	}

	public String getBaseDirectory() {
		return baseDirectory;
	}

	public NeoDatisConfig setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
		return this;
	}

	public boolean useLazyInstantiationInServerMode() {
		return useLazyInstantiationInServerMode;
	}

	public NeoDatisConfig setUseLazyInstantiationInServerMode(boolean useLazyInstantiationInServerMode) {
		this.useLazyInstantiationInServerMode = useLazyInstantiationInServerMode;
		return this;
	}

	/**
	 * @return
	 */
	public long getTimeoutToAcquireMutex() {
		return timeoutToAcquireMutex;
	}

	public NeoDatisConfig registerDatabaseStartupManager(DatabaseStartupManager manager) {
		databaseStartupManager = manager;
		return this;
	}

	public NeoDatisConfig removeDatabaseStartupManager() {
		databaseStartupManager = null;
		return this;
	}

	public DatabaseStartupManager getDatabaseStartupManager() {
		return databaseStartupManager;
	}

	public boolean logSchemaEvolutionAnalysis() {
		return logSchemaEvolutionAnalysis;
	}

	public NeoDatisConfig setLogSchemaEvolutionAnalysis(boolean log) {
		logSchemaEvolutionAnalysis = log;
		return this;
	}

	public boolean isTransactional() {
		return transactional;
	}

	public NeoDatisConfig setTransactional(boolean transactional) {
		this.transactional = transactional;
		return this;
	}

	public String getUser() {
		return user;
	}

	public NeoDatisConfig setUser(String user) {
		this.user = user;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public NeoDatisConfig setPassword(String password) {
		this.password = password;
		return this;
	}

	/**
	 * @param homeDirectory
	 */
	public NeoDatisConfig setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
		return this;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public String getHost() {
		return host;
	}

	public NeoDatisConfig setHostAndPort(String host, int port) {
		this.host = host;
		this.port = port;
		servers.add(new Server(host, port));
		return this;
	}

	public int getPort() {
		return port;
	}

	public NeoDatisConfig setPort(int port) {
		this.port = port;
		return this;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public NeoDatisConfig setIsLocal(boolean isLocal) {
		this.isLocal = isLocal;
		return this;
	}

	/**
	 * @return
	 */
	public NeoDatisConfig copy() {
		NeoDatisConfigImpl p = new NeoDatisConfigImpl(false);
		p.setHomeDirectory(getHomeDirectory());
		p.setHostAndPort(getHost(), getPort());
		p.setIsLocal(isLocal());
		p.setPassword(getPassword());
		p.setPort(getPort());
		p.setTransactional(isTransactional());
		p.setUser(getUser());
		p.servers.clear();
		p.servers.addAll(servers);
		return p;
	}

	public NeoDatisConfig addServer(String host, int port) {
		servers.add(new Server(host, port));
		return this;
	}

	public List<Server> getServers() {
		return servers;
	}

	public boolean useNativeConfig() {
		return useNativeConfig;
	}

	public NeoDatisConfig setUseNativeConfig(boolean useNativeConfig) {
		this.useNativeConfig = useNativeConfig;
		return this;
	}

	public int getNumberOfServers() {
		return servers.size();
	}

	public NeoDatisConfig setUserAndPassword(String user, String password) {
		setUser(user);
		setPassword(password);
		return this;
	}
	/**
	 * @return The Directory used for Server Inbox (used for file transfer)
	 */
	public String getInboxDirectory(){
		return serverInboxDirectory;
	}
	public NeoDatisConfig setServerInboxDirectory(String dir){
		this.serverInboxDirectory = dir;
		return this;
	}

	public boolean isSSL() {
		return ssl;
	}
	public NeoDatisConfig setSSL(boolean ssl){
		this.ssl = ssl;
		return this;
	}

	public Class getOidGeneratorClass() {
		return oidGeneratorClass;
	}

	public NeoDatisConfig setOidGeneratorClass(Class oidGeneratorClass) {
		this.oidGeneratorClass = oidGeneratorClass;
		return this;
	}

	public NeoDatisConfig setUseStorageEngineCache(boolean yesOrNo) {
		this.useStorageEngineCache = yesOrNo;
		return this;
	}

	public boolean useStorageEngineCache() {
		return useStorageEngineCache;
	}


	/** indicates if the oid generator can use cache
	 *
	 * @param yes
	 * @return
	 */
	public NeoDatisConfig setOidGeneratorUseCache(boolean yesOrNo){
		oidGeneratorUseCache = yesOrNo;
		return this;
	}
	public boolean oidGeneratorUseCache(){
		return oidGeneratorUseCache;
	}


}

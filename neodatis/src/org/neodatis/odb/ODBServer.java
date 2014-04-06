/**
 * 
 */
package org.neodatis.odb;

import java.util.List;

import org.neodatis.odb.core.server.connection.SessionManager;
import org.neodatis.odb.core.server.trigger.ServerDeleteTrigger;
import org.neodatis.odb.core.server.trigger.ServerInsertTrigger;
import org.neodatis.odb.core.server.trigger.ServerOidTrigger;
import org.neodatis.odb.core.server.trigger.ServerSelectTrigger;
import org.neodatis.odb.core.server.trigger.ServerUpdateTrigger;

/**
 * @author olivier
 *
 */
public interface ODBServer {

	/**
	 * @param string
	 * @param string2
	 */
	void addBase(String string, String string2);

	/**
	 * @param b
	 * @throws Exception 
	 */
	void startServer(boolean b) ;

	/**
	 * @param string
	 * @return
	 */
	ODB openClient(String baseId);
	ODB openClient(String baseId, NeoDatisConfig config);

	/**
	 * 
	 */
	void close();

	/**
	 * @param b
	 */
	void setAutomaticallyCreateDatabase(boolean b);
	boolean automaticallyCreateDatabase();

	/**
	 * @param baseName
	 * @param name
	 * @param selectTrigger
	 */
	void addSelectTrigger(String baseName, String fullClassName, ServerSelectTrigger selectTrigger);

	/**
	 * @param baseName
	 * @param name
	 * @param insertTrigger
	 */
	void addInsertTrigger(String baseName, String fullClassName, ServerInsertTrigger insertTrigger);

	/**
	 * @param bASENAME
	 * @param name
	 * @param updateTrigger
	 */
	void addUpdateTrigger(String baseName, String fullClassName, ServerUpdateTrigger updateTrigger);

	/**
	 * @param baseIdentifier
	 * @return
	 */
	SessionManager getSessionManagerForBase(String baseIdentifier);
	

	/**
	 * @param baseId
	 */
	void removeSessionManagerForBase(String baseId);

	/**
	 * @param baseIdentifier
	 * @param baseIdentifier2
	 * @param user
	 * @param password
	 */
	void addBase(String baseIdentifier, String baseIdentifier2, String user, String password);

	/**
	 * @return
	 */
	List<String> getSessionDescriptions();

	/**
	 * @param bASENAME
	 * @param name
	 * @param deleteTrigger
	 */
	void addDeleteTrigger(String baseName, String className, ServerDeleteTrigger deleteTrigger);

	/**
	 * @param base
	 * @param name
	 * @param myOidTrigger
	 */
	void addOidTrigger(String baseName, String className, ServerOidTrigger oidTrigger);


	NeoDatisConfig getConfig();

	void dontCallTriggersForClasses(String baseIdentifier, List<Class> classes );
}

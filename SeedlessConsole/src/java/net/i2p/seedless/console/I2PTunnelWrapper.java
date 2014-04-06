/*
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                     Version 2, December 2004
 *
 *  Copyright (C) sponge
 *    Planet Earth
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *   0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 * See...
 *
 * 	http://sam.zoy.org/wtfpl/
 * 	and
 * 	http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */
package net.i2p.seedless.console;

import java.util.List;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.data.PrivateKeyFile;
import net.i2p.data.Destination;

/**
 *
 * @author sponge
 */
public class I2PTunnelWrapper {

    public static final int RUNNING = 1;
    public static final int STARTING = 2;
    public static final int NOT_RUNNING = 3;
    public static final int STANDBY = 4;
    protected final TunnelControllerGroup _group;

    public I2PTunnelWrapper() {
        _group = TunnelControllerGroup.getInstance();
    }

    protected TunnelController getController(int tunnel) {
        if(tunnel < 0) {
            return null;
        }
        if(_group == null) {
            return null;
        }
        List controllers = _group.getControllers();
        if(controllers.size() > tunnel) {
            return (TunnelController)controllers.get(tunnel);
        } else {
            return null;
        }
    }

    TunnelController getControllerWrapped(int curServer) {
        return getController(curServer);
    }

    public boolean startAutomatically(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null) {
            return tun.getStartOnLoad();
        } else {
            return false;
        }
    }

    public String getPrivateKeyFile(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null && tun.getPrivKeyFile() != null) {
            return tun.getPrivKeyFile();
        }
        if(tunnel < 0) {
            tunnel = _group.getControllers().size();
        }
        return "i2ptunnel" + tunnel + "-privKeys.dat";
    }

    public String getClientInterface(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null) {
            if("streamrclient".equals(tun.getType())) {
                return tun.getTargetHost();
            } else {
                return tun.getListenOnInterface();
            }
        } else {
            return "127.0.0.1";
        }
    }

    public String getClientPort(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null && tun.getListenPort() != null) {
            return tun.getListenPort();
        } else {
            return "";
        }
    }

    public String getDestinationBase64(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null) {
            String rv = tun.getMyDestination();
            if(rv != null) {
                return rv;
            }
            // if not running, do this the hard way
            String keyFile = tun.getPrivKeyFile();
            if(keyFile != null && keyFile.trim().length() > 0) {
                PrivateKeyFile pkf = new PrivateKeyFile(keyFile);
                try {
                    Destination d = pkf.getDestination();
                    if(d != null) {
                        return d.toBase64();
                    }
                } catch(Exception e) {
                }
            }
        }
        return "";
    }

    public String getInternalType(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null) {
            return tun.getType();
        } else {
            return "";
        }
    }

    public String getServerTarget(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun != null) {
            String host;
            if("streamrserver".equals(tun.getType())) {
                host = tun.getListenOnInterface();
            } else {
                host = tun.getTargetHost();
            }
            String port = tun.getTargetPort();
            if(host == null) {
                host = "";
            } else if(host.indexOf(':') >= 0) {
                host = '[' + host + ']';
            }
            if(port == null) {
                port = "";
            }
            return host + ':' + port;
        } else {
            return "";
        }
    }

    public int getTunnelCount() {
        if(_group == null) {
            return 0;
        }
        return _group.getControllers().size();
    }

    public int getTunnelStatus(int tunnel) {
        TunnelController tun = getController(tunnel);
        if(tun == null) {
            return NOT_RUNNING;
        }
        if(tun.getIsRunning()) {
            if(isClient(tunnel) && tun.getIsStandby()) {
                return STANDBY;
            } else {
                return RUNNING;
            }
        } else if(tun.getIsStarting()) {
            return STARTING;
        } else {
            return NOT_RUNNING;
        }
    }

    public boolean isClient(int tunnelNum) {
        TunnelController cur = getController(tunnelNum);
        if(cur == null) {
            return false;
        }
        return isClient(cur.getType());
    }

    public static boolean isClient(String type) {
        return (("client".equals(type)) ||
                ("httpclient".equals(type)) ||
                ("sockstunnel".equals(type)) ||
                ("socksirctunnel".equals(type)) ||
                ("connectclient".equals(type)) ||
                ("streamrclient".equals(type)) ||
                ("ircclient".equals(type)));
    }
}

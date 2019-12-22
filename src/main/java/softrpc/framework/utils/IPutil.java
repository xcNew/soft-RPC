package softrpc.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import java.net.*;
import java.util.Enumeration;
import java.util.List;

/**
 * @author xctian
 * @date 2019/12/23
 */
public class IPutil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IPutil.class);

    private static String hostIp;

    public static String localIp(){
        return hostIp;
    }

    static {
        String ip = null;
        Enumeration allNetInterfaces;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                List<InterfaceAddress> InterfaceAddress = netInterface.getInterfaceAddresses();
                for (InterfaceAddress add : InterfaceAddress) {
                    InetAddress inetAddress = add.getAddress();
                    if (inetAddress instanceof Inet4Address) {
                        if (StringUtils.equals(inetAddress.getHostAddress(), "127.0.0.1")) {
                            continue;
                        }
                        ip = inetAddress.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.error("获取本机Ip失败:异常信息:" + e.getMessage());
            throw new RuntimeException(e);
        }
        hostIp = ip;
    }


}

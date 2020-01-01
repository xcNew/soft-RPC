package softrpc.framework.test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author xctian
 * @date 2020/1/2
 */
public class MainServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainServer.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("rpc-service.xml");

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("服务端启动成功，耗时{}ms",duration);
    }
}

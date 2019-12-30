package softrpc.framework.spring.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;
import softrpc.framework.spring.factoryBean.RpcServiceFactoryBean;
import softrpc.framework.utils.PropertyConfigUtil;

/**
 * 自定义标签解析器，通过解析soft:service标签内容生成一个getBeanClass方法返回的类对象
 *
 * @author xctian
 * @date 2019/12/18
 */
public class RpcServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceBeanDefinitionParser.class);

    @Override
    protected Class<?> getBeanClass(Element element) {
        return RpcServiceFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        long startTime = System.currentTimeMillis();
        try {
            String id = element.getAttribute("id");
            String serviceItf = element.getAttribute("interface");
            String timeout = element.getAttribute("timeout");
            String serverPort = element.getAttribute("serverPort");
            String ref = element.getAttribute("ref");
            String weight = element.getAttribute("weight");
            String workThreads = element.getAttribute("workThreads");
            String groupName = element.getAttribute("groupName");
            String appName = element.getAttribute("appName");
            builder.addPropertyValue("servicePath", serviceItf);
            // 作用是设置接口实现类全限定名
            builder.addPropertyValue("ref", ref);
            builder.addPropertyValue("serverPort", Integer.parseInt(serverPort));
            builder.addPropertyValue("timeout", Integer.parseInt(timeout));
            if (StringUtils.isNotBlank(groupName)) {
                builder.addPropertyValue("groupName", groupName);
            }
            if (NumberUtils.isDigits(weight)) {
                builder.addPropertyValue("weight", Integer.parseInt(weight));
            }
            if (NumberUtils.isDigits(workThreads)) {
                builder.addPropertyValue("workThreads", workThreads);
            }
            if (StringUtils.isNotBlank(appName)) {
                builder.addPropertyValue("appName", appName);
            } else {
                // 如果soft-Service没有配置，则利用PropertyConfigUtil获取soft-rpc.properties中的appName
                String appName4Server = PropertyConfigUtil.getAppName4Server();
                // soft-rpc.properties中的appName也没有配置则抛出异常
                if (StringUtils.isNotBlank(appName4Server)) {
                    LOGGER.error("请配置{}标签的appName属性或在soft-rpc.properties中配置soft.rpc.server.app.name属性", id);
                    throw new RuntimeException(String.format("%s%s", id, "标签缺少appName属性"));
                }
                builder.addPropertyValue("appName", appName4Server);
            }
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("[{}]标签解析耗时{}ms", id, duration);
        } catch (Exception e) {
            LOGGER.error("ProviderFactoryBeanDefinitionParser Error.",e);
            throw new RuntimeException(e);
        }
    }
}

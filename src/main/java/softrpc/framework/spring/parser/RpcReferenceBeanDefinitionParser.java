package softrpc.framework.spring.parser;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;
import softrpc.framework.spring.factoryBean.RpcReferenceFactoryBean;
import softrpc.framework.utils.PropertyConfigUtil;

/**
 * 自定义标签解析器，通过解析soft:reference标签内容生成一个getBeanClass方法返回的类对象
 *
 * @author xctian
 * @date 2019/12/18
 */
public class RpcReferenceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcReferenceBeanDefinitionParser.class);

    @Override
    protected Class<?> getBeanClass(Element element) {
        // 对标签reference的解析结果是一个factoryBean类对象
        return RpcReferenceFactoryBean.class;
    }

    /**
     * 解析soft-reference标签，封装到RpcReferenceFactoryBean中
     *
     * @param element
     * @param builder
     */
    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        try {
            long startTime = System.currentTimeMillis();
            String id = element.getAttribute("id");
            String timeout = element.getAttribute("timeout");
            String targetInterface = element.getAttribute("interface");
            String clusterStrategy = element.getAttribute("clusterStrategy");
            String groupName = element.getAttribute("groupName");
            String appName = element.getAttribute("appName");
            builder.addPropertyValue("timeout", timeout);
            builder.addPropertyValue("targetInterface", Class.forName(targetInterface));
            if (StringUtils.isNotBlank(groupName)) {
                builder.addPropertyValue("groupName", groupName);
            }
            if (StringUtils.isNotBlank(appName)) {
                builder.addPropertyValue("appName", appName);
            } else {
                String appName4Client = PropertyConfigUtil.getAppName4Client();
                // soft-rpc.properties中的appName也没有配置则抛出异常
                if (StringUtils.isNotBlank(appName4Client)) {
                    LOGGER.error("请配置{}标签的appName属性或在soft-rpc.properties中配置soft.rpc.client.app.name属性", id);
                    throw new RuntimeException(String.format("%s%s", id, "标签缺少appName属性"));
                }
                builder.addPropertyValue("appName", appName4Client);
            }
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("[{}]标签解析超时{}ms",id,duration);
        } catch (Exception e) {
            LOGGER.error("RevokerFactoryBeanDefinitionParser Error.", e);
            throw new RuntimeException(e);
        }
    }
}

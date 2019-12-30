package softrpc.framework.spring.handler;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import softrpc.framework.spring.parser.RpcServiceBeanDefinitionParser;

/**
 * 命名空间handler
 *
 * @author xctian
 * @date 2019/12/18
 */
public class RpcServiceNamespaceHandler extends NamespaceHandlerSupport{
    @Override
    public void init() {
        //  给soft:service标签注册对应的BeanDefinitionParser解析器
        registerBeanDefinitionParser("service",new RpcServiceBeanDefinitionParser());
    }
}

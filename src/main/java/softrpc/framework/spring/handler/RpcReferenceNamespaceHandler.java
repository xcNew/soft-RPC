package softrpc.framework.spring.handler;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import softrpc.framework.spring.parser.RpcReferenceBeanDefinitionParser;

/**
 * 命名空间handler
 *
 * @author xctian
 * @date 2019/12/18
 */
public class RpcReferenceNamespaceHandler extends NamespaceHandlerSupport {
    // 给soft:reference标签注册对应的BeanDefinitionParser解析器
    @Override
    public void init() {
        registerBeanDefinitionParser("reference",new RpcReferenceBeanDefinitionParser());
    }
}

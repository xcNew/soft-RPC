package softrpc.framework.test.service;

/**
 * 测试RPC服务接口
 *
 * @author xctian
 * @date 2019/12/17
 */
public interface Service1 {
    /**
     * 测试方法：直接打印信息
     */
    void sayHi();

    /**
     * 测试方法：打印传入的信息
     */
    String sayMessage(String message);

}

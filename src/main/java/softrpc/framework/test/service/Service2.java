package softrpc.framework.test.service;

/**
 * @author xctian
 * @date 2019/12/17
 */
public interface Service2 {
    /**
     * 测试方法：直接打印信息
     */
    void sayHi();

    /**
     * 测试方法：打印传入的信息
     */
    String sayMessage(String message);
}

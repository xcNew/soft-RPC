package softrpc.framework.test.service.imp;

import softrpc.framework.test.service.Service2;

/**
 * @author xctian
 * @date 2019/12/17
 */
public class Service2imp2 implements Service2 {
    @Override
    public void sayHi() {
        System.out.println("Hello,soft-RPC,this is [Service2imp2]");
    }

    @Override
    public String sayMessage(String message) {
        return String.format("[Service2imp2] The message you have given is %s",message);
    }
}

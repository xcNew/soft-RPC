package softrpc.framework.test.service.imp;

import softrpc.framework.test.service.Service1;

/**
 * @author xctian
 * @date 2019/12/17
 */
public class Service1imp implements Service1{

    @Override
    public void sayHi() {
        System.out.println("Hello,soft-RPC,this is [Service1]");
    }

    @Override
    public String sayMessage(String message) {
        return String.format("[Service1] The message you have given is %s",message);
    }
}

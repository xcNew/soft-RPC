## Soft-RPC介绍

### RPC简介

> RPC(Remote Procedure Call,远程过程调用)用于实现部署在不同机器上的系统之间的方法调用，使得程序可以像访问本地服务一样，通过网络传输调用远程系统提供的服务。

具体而言：

- RPC就是从一台机器（客户端）上通过参数传递的方式调用另一台机器（服务器）上的一个函数或方法（可以统称为服务）并得到返回的结果。
- RPC 会隐藏底层的通讯细节（不需要直接处理Socket通讯或Http通讯） RPC 是一个请求响应模型。
- 客户端发起请求，服务器返回响应（类似于Http的工作方式） RPC 在使用形式上像调用本地函数（或方法）一样去调用远程的函数（或方法）。

web项目中，**调用本地服务的过程**：编写接口和实现类，然后将实现类托管至Spring容器，之后需要用到该服务时直接使用@Autowired将其注入即可。但在实际的开发环境中，并不是所有的服务都是由我们自己来进行开发，我们经常需要调用由别的开发人员开发的服务，那么通过使用RPC框架，在本地导入其他服务接口的依赖包之后，就可以类似上述过程一样去调用远程的服务。

- 为什么不直接在本地导入外部服务的接口+实现类的依赖包？

  实际开发环境中，接口的实现类通常很容易改变，如果该服务被大量的调用者所依赖，那么一旦这个服务的实现类发生改变，就要去发包让所有调用者更新本地的依赖包，系统耦合性增加；如果使用RPC的方式，则实现类只需要交给服务的提供者进行维护即可。

**调用远程服务的过程（RPC方式）：**

<img src="img\20191108234611174.png" alt="image-20191108234611174" style="zoom:80%;" />

1）服务消费方（client）调用以本地调用方式调用服务；

2）client stub接收到调用后负责将方法、参数等组装成能够进行网络传输的消息体；

3）client stub找到服务地址，并将消息发送到服务端；

4）server stub收到消息后进行解码；

5）server stub根据解码结果调用本地的服务；

6）本地服务执行并将结果返回给server stub；

7）server stub将返回结果打包成消息并发送至消费方；

8）client stub接收到消息，并进行解码；

9）服务消费方得到最终结果。

RPC框架的目标就是要2~8这些步骤都封装起来，让用户对这些细节透明。从而使得用户对远程服务的调用可以像本地服务一样。

### 序列化及反序列化

- 序列化(Serialization)：将对象的状态信息转换为可存储或传输的形式的过程。
- 反序列化(Deserialization)：序列化的逆过程。将字节序列恢复为对象的过程。

**通过序列化可以解决的问题**

- 通过将对象序列化为字节数组，使得不共享内存通过网络连接的系统之间能够进行对象的传输；
- 通过将对象序列化为字节数组，可以将对象永久存储到存储设备；
- 解决远程接口调用JVM之间内存无法共享的问题。

**评价序列化算法优劣的指标**

- 序列化后码流的大小；
- 序列化本身的速度及系统资源开销大小（内存，CPU）。

#### 常用的序列化工具介绍

**JDK默认的序列化工具**

> JAVA原生序列化方式，主要通过对象输入流ObjectInputStream和对象输出流ObjectOutputStream来实现，序列化对象需要实现Serializable接口。

- 序列化时，只对对象的状态进行保存，而不管对象的方法；

- 父类实现序列化时，子类自动实现序列化，不需要显式实现Serializable接口；

- 一个对象的实例变量引用其他对象时，序列化该对象时也把引用对象进行序列化；

- 字段被声明为transient后，JDK默认序列化机制会忽略该字段。

  **优点：**

  - Java语言自带，无需引入第三方依赖；
  - 与Java有天然的最好的易用性与亲和性。

  **缺点：**

  - 只支持Java语言，不支持跨语言；
  - 性能欠佳，序列化后产生的码流大小过大，对引用过深的对象序列化可能导致OOM。

Java序列化会把要序列化的对象类的元数据和业务数据全部序列化为字节流，而且是把整个继承关系上的东西全部序列化了。它序列化出来的字节流是对那个对象结构到内容的完全描述，包含所有的信息，因此效率较低而且字节流比较大。但是由于确实是序列化了所有内容，所以可以说什么都可以传输，因此也更可用和可靠。

**Hessian**

> Hessian是一个轻量级的remoting onhttp工具，使用简单的方法提供了RMI的功能。 相比WebService，Hessian更简单、快捷。采用的是二进制RPC协议，因为采用的是二进制协议，所以它很适合于发送二进制数据。序列化对象需要实现Serializable接口。

- Hessian相比Java原生序列化, 序列化后的二进制数据量更小, 因此传输速度和解析速度都更快

- 采用简单的结构化标记, 并且只存储对象数据内容部分, 而JJDK默认的序列化工具还会存一些继承关系之类；
- 采用引用取代重复遇到的对象, 避免了重复编码；
- 支持多种不同语言。

**ProtoStuff**

protostuff 基于Google protobuf，但是提供了更多的功能和更简易的用法。其中，protostuff-runtime 实现了无需预编译对java bean进行protobuf序列化/反序列化的能力。protostuff-runtime的局限是序列化前需预先传入schema(可以由代码方法生成, 不需要手动改创建)，其中，schema中包含了对象进行序列化和反序列化的逻辑；反序列化不负责对象的创建只负责复制，因而**必须提供默认构造函数**。此外，protostuff 还可以按照protobuf的配置序列化成json/yaml/xml等格式。

在性能上，protostuff不输原生的protobuf，甚至有反超之势。

#### **序列化工具引擎**

本项目用到三种:Default / Hessian / ProtoStuff， 一个Serializer接口有多种实现类, 如何优雅的进行选择? 使用可配置化的序列化工具引擎，有两种实现思路：

- 工厂模式方案：

  添加一个工厂类, 提供根据名称获取Serializer实现类的方法, 最后用一个Engine类即可以实现优雅的选择。这样做的缺陷是每次序列化/反序列化请求都需要生成新的Serializer，消耗存储空间。

- 使用Map+Enum枚举类：

   添加一个枚举类, 其中主要存储代表不同实现类的枚举值。在Engine类里新增常量map， key存储枚举类里的不同枚举，value存储对应具体的Serializer实现类，Engine类加载时在static代码块初始化map，根据这个传入的Serializer名称在map中找对应的实现类对象，执行实际的功能方法。**可以解决单例问题**。

  <img src="img\image-20191110180140681.png" alt="image-20191110180140681" style="zoom:80%;" />

#### 自定义消息格式

##### 粘包/半包问题

上面介绍的是几种序列化/反序列化工具，RPC调用的底层使用的Netty框架基于TCP/IP，是基于字节流进行传输的，像流水一样连接在一起，TCP底层并无法感知业务数据的具体的含义，无法按照具体的业务含义对消息进行分包，而只会按照TCP缓冲区的实际大小情况来对包进行划分。当业务数据被拆分为多个数据包，这些数据包达到目的端后有以下三种情况：

- 刚好按照业务数据本身的边界逐个到达目的（业务数据的边界刚好是数据包的边界）

  <img src="img\3.png" alt="image-20191110180140681" style="zoom:60%;" />

- 多个业务数据组合成为一个数据包，即**粘包**现象(数据包大小刚好等于多个业务数据)

  <img src="img\4.png" alt="image-20191110180140681" style="zoom:60%;" />

- 到达目的的数据包中只包含了部分不完整的业务数据，数据包大小小于n个业务数据，那么第n个业务数据将被拆分到多个数据包传输，即**半包**现象。

  <img src="img\5.png" alt="image-20191110180140681" style="zoom:60%;" />

解决半包/粘包问题的关键是能够区分完整的业务应用的数据边界，能够按照此边界完整地接收Netty传输的数据。

本项目中，利用自定义的消息格式，结合Netty自定义编解码器开发，作为半包/粘包问题的简单解决方案。

##### 自定义消息格式 (消息编解码规则)

消息格式定义如下：serializerCode|dataLength|messageData

上述消息格式规定了字节流在传输的时候由三部分组成：

1.  int类型的serializerCode，它是序列化协议对应枚举中的code(见项目serializer\common\Serializer.java)

   用于标识该次传输所采用的序列化/反序列化协议

2. int类型的消息长度dataLength，它表示需要传输的数据的大小

3. 需要传输的业务数据

**Netty自定义编解码器**

- MessageToByteEncoder是Netty为消息转换为byte提供的一个抽象类，本项目的编码器只需继承MessageToByteEncoder,并严格按照上述规定的自定义编码格式，重写encode方法如下：

  ```java
   public void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
          long startTime = System.currentTimeMillis();
          // 获取序列化协议code
          int serializerCode = serializeType.getSerializeCode();
          // 将其写入消息头部第一个int
          out.writeInt(serializerCode);
          // 将对象进行序列化
          byte[] data = SerializerEngine.serialize(in,serializeType);
          // 将data长度写入消息头部第二个int
          out.writeInt(data.length);
          // 将消息体写入
          out.writeBytes(data);
  }
  ```

  > encode方法被调用时将会传入需要被编码的消息in，然后将编码后的消息存入ByteBuf类型的out,该ByteBuf随后会被转发给pipeline中的下一个handler	

  ​	以上实现了按照自定义消息格式(int+int+data)对消息进行编码并写入NettyChannel

- ByteToMessageEncoder是Netty为byte转换为消息提供的一个抽象类，本项目的解码器只需继承ByteToMessageEncoder,并严格按照上述规定的自定义编码格式，重写decode方法如下：

  ```java
  public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      // 消息头部长度8字节=序列化协议int + 消息长度int
      if(in.readableBytes() < 8){
          return;
      }
      in.markReaderIndex();
      int serializerCode = in.readInt();
      String serializer = SerializerType.getByCode(serializerCode).getSerializeName();
      int dataSize = in.readInt();
      if(dataSize < 0){
          ctx.close();
      }
      // 若当前可读字节数小于消息长度，则重置readerIndex，直至可以获取到消息长度的字节数
      if(in.readableBytes() < dataSize){
          in.resetReaderIndex();
          return;
      }
      byte[] data = new byte[dataSize];
      // 从channel读取数据至byte数组data
      in.readBytes(data);
      Object obj = SerializerEngine.deserialize(data,genericClass,serializer);
      out.add(obj);
  }
  ```

  > decode方法调用时将会传入ByteBuf类型的待解码的数据in,以及一个用来添加解码后的消息的List。对这个方法的调用将会重复进行，直至确定没有新的元素被添加到List,或者该ByteBuf中没有更多可读取的字节为止。然后若该List不空，那么它将会被传递给Pipeline中的下一个handler

  两个int占8个字节，所以在能读取到的消息字节小于8时，先不读数据，直接return。等有数据后，按照上述消	息编码规则，先读取第一个int：序列化协议code，再读第二个Int:待解码的消息长度，最后根据该长度读取待	解码消息，并根据第一个int获取序列化协议，按照该协议将待解码消息反序列化成为java对象，然后存入list。	以上完成了一个完整的消息解码过程。



以上，完成了Netty网络传输中重要的三个部分：**序列化/反序列化协议、消息编解码规则、解决半包/粘包问题**。

### Netty网络传输

#### 特性

- ChannelPool：客户端在启动完成，获得服务注册地址后，会针对每一个服务地址预先建立配置数量的channel进行复用
- ThreadWorkers: 客户端调用rpc服务使用的线程数量是可配置的
- 使用线程池处理客户端提交的请求，阻塞等待RPC调用的返回结果
- 使用concurrent包的工具BlockingQueue,在Netty异步返回后同步阻塞等待结果
- 服务端发布服务时可以自行设置限流信号量大小，以使得每个服务同时支持连接的客户端数量是可控的

#### ChannelPool实现Channel复用

NettyChannelPoolFactory是单例工厂类，成员变量Map<InetSocketAddress,ArrayBlockingQueue<Channel>> channelPoolMao,用于存储对应不同主机地址的channelPool，每个channelPool存储一定数量的channel，该ch数量由soft-rpc.properties下的channelPoolSize指定。channelPool数据结构使用的是阻塞队列，这样的好处是并发条件下channel的存取不会出错。

由于Netty是异步框架，在创建channel时，为返回的ChannelFuture添加监听器，用于监听channel的创建情况。为达到同步等待channel创建结果的目的，使用AQS工具countdownLatch,在channel创建结果未产生前，调用await()方法阻塞线程，直到channel创建结果产生，无论该结果是创建成功还是失败，最后均调用countDown()让线程继续。创建单个channel的方法(方法签名Channel registerChannel(InetSocketAddress socketAddress)）核心代码如下：

```java
ChannelFuture channelFuture = bootstrap.connect().sync();
final Channel newChannel = channelFuture.channel();
final CountDownLatch countDownLatch = new CountDownLatch(1);
final List<Boolean> isSuccessHolder = Lists.newArrayListWithCapacity(1);
// 监听是否channel创建成功
channelFuture.addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            isSuccessHolder.add(Boolean.TRUE);
        } else {
            channelFuture.cause().printStackTrace();
            isSuccessHolder.add(Boolean.FALSE);
        }
        countDownLatch.countDown();
    }
});
if (isSuccessHolder.get(0)) {
    return newChannel;
 }else{
    return null;
}
// 阻塞等待Channel创建的结果
countDownLatch.await();
```

创建channelPool时候，会不断调用上述registerChannel方法产生新的channel放入BlockingQueue，由于channe的创建很可能失败，所以创建每一个channel时候都需要使用一个外部循环，直到channel创建成功为止：

```java
while (null == channel) {
    // 创建失败则重试
    channel = registerChannel(socketAddress);
}
```

在每次利用完channel后，需要调用release方法将其回收至对应的BlockingQueue,以便复用（详情参照项目代码对应的方法）。

#### ThreadWorkers

客户端代理类ClientProxyBeanFactory，用于为客户端创建代理对象，其包含一个静态成员变量fixedThreadPool,负责submit线程提交的请求，其大小由soft-rpc.properties中ThreadWorkers配置项确定。如下：在每个请求使用一个线程进行submit的模式下，可以同步阻塞等待结果：

```java
// 任务扔到线程池执行
responseMessageFuture = threadPool.submit(new RevokerServiceCallable(inetSocketAddress, requestMessage));
//阻塞等待response
ResponseMessage response = responseMessageFuture.get(requestMessage.getTimeout(), TimeUnit.MILLISECONDS);
```

#### 返回结果的封装设计

提供一个返回结果的包装类ResponseReceiver，其中含有一个大小为1的BlockingQueue<ResponseMessage>成员变量用于存储结果，以及一个responseTime属性这个返回结果创建的时间，提供isExpire方法校验该结果是否过期，实现思路是检查当前时间-responseTime,如果大于timeout参数则过期。

提供一个返回结果容器ResponseReceiverHolder，含有一个属性Map<String,ResponseReceiver>responseMap,用于存储结果，key是请求id，整个类会提供一个Excutors.newSingleThreadExecutor()的单线程线程池不断检查map中过期的ResponseReceiver，如过期则移除，以避免内存泄漏（异常情况下ResponseMessage进入容器后并不会被立刻取走而发生超时，因此该线程池是有必要存在的）

Netty客户端发起请求调用时候，提交RevokerServiceCallable进入线程池执行，RevokerServiceCallable的call方法里面先初始化了本次请求消息对应的结果容器，然后将该消息write进入NettyChannel进行netty通信，然后在ResponseReceiverHolder.getValue处被阻塞(该阻塞唤醒机制由包装类的BlockingQueue提供)。当服务端执行完本地方法获得执行结果responseMessage并将其写入channel后,由客户端NettyClientHandler调用channelRead0方法将其存入结果容器ResponseReceiverHolder，从阻塞处重新恢复继续执行。

#### 服务端限流设计

利用JUC具类Semaphore作为流控基础设施，实现服务端的限流，即同时执行的最大请求数。Semaphore大小threadWorkers在服务发布时可以进行配置，客户端引入服务时会将该变量读取并封装至requestMessage，然后服务端在执行每一个请求时都可以通过客户端传入的requestMessage获取threadWorkers。服务端在最开始时，初始化一个threadWorkers大小的Semaphore，此后每次服务端线程想要对传入的requestMessage执行调用前，需要执行semaphore的tryAcquire方法获取一个信号量，若成功才能继续执行，否则返回调用超时异常，以此来保证该服务提供者同时最多执行的任务数。

服务端限流的必要性：因为服务端在接收到调用端的requestMessage后，实际上是通过反射的操作去执行本地方法并得到返回结果的，而反射操作的执行效率低下，故为保证服务资源的可用性及维护服务稳定性，需要对同时执行的请求进行数量上的限制。

```java
try {
    // 利用semaphore实现服务端限流,因为反射操作执行效率低下，如果大量反射同时执行，将占用资源
    acquire = semaphore.tryAcquire(consumeTimeout, TimeUnit.MILLISECONDS);
    if(acquire){
        // 成功则发起反射调用，调用服务
        responseMessage = ServiceProvider.excuteMethodFromRequestMessage(requestMessage);
    }else {
        LOGGER.warn("服务限流，请求超时");
    }
}catch (Exception e){
    LOGGER.error("服务方反射调用本地方法时产生错误",e);
    throw new RuntimeException("服务方反射调用本地方法时产生错误");
}
```

### Zookeeper注册中心

本项目使用Zookeeper作为服务注册中心

#### 特性

- 服务自动注册：集成了spring，只需按照约定提供配置文件和xml即可发布服务并自动注册至ZK
- 服务推送：由于对结点注册了监听器，当服务信息发送变化时，会自动将服务新同步至本地缓存

#### 服务的注册与发现

> 在基于SOA架构的应用中，应用提供对外服务的同时也会调用外部系统所提供的服务。当应用越来越多，服务越来越多的时候，服务之间的依赖关系会变得越来越复杂，此时依靠人工来维护服务之间的依赖关系以及管理服务的上下线变得十分困难。此时我们需要服务注册中心来解决服务的自动发现、服务自动上下线等问题。

注册中心相当于信息仓库，存储了rpc调用需要的信息，对于服务端而言，需要将发布的服务信息进行注册，对于消费端而言，需要按照规定格式去注册中心获取其所引用服务的注册信息，然后将自己的信息注册到注册中心：

- 服务的服务端启动时，将服务提供者信息ProviderRegisterMessage(IP，端口，服务接口类路径等)组成的znode路径写入Zookeeper(临时结点)，然后对服务的消费者结点路径注册监听器，获取消费者信息缓存到本地，在某个客户端注册信息（临时结点）失去连接时，会触发监听器使服务端更新消费者缓存信息，这样即可完成服务的注册操作。
- 服务消费端在发起服务的调用之前，先连接Zookeeper，对服务提供者结点路径注册监听器，同时获取服务提供者信息缓存到本地，发起调用时，采用某种负载均衡算法选择本地缓存列表中的其中一个服务提供者发起调用，最终完成本次rpc调用，这样即可完成服务的发现操作。

由上所述，服务的消费端和服务端对注册中心的需求功能有所差异，所以可以设计一个注册中心类RegisterCenter,实现不同的接口，消费端接口声明消费端需要的功能，服务端接口声明服务所需功能，RegisterCenter同时实现以上两个接口，使用时可以使用不同的接口引用指向相同的RegisterCenter单例对象，这样做可以使得代码逻辑清晰：

```java
 /**
   * 客户端注册中心
   */
private static RegisterCenter4Invoker registerCenter4Invoker = RegisterCenter.getInstance();
/**
  * 服务端注册中心
  */
private static final RegisterCenter4Provider registerCenter4Provider = RegisterCenter.getInstance();
```

除了服务端接口和消费端接口以外，还可以提供一个服务治理接口，声明一些服务治理所需的相关功能，同样也在RegisterCenter中实现。

<img src="img\6.png" alt="image-20191110180140681" style="zoom:60%;" />

下图为zookeeper节点结构，当不同服务端发布服务时，在appName节点层进行区分，所以即使appX和appY之间拥有完全相同的接口全限定名，引用服务时也不会混淆，因为服务的key是appName+接口全限定名。每个key下面都有provider和invoker结点，这两个结点下都是临时结点，分别存储的是服务提供者信息和服务消费者信息。

<img src="img\7.png" alt="image-20191110180140681" style="zoom:60%;" />

结点信息都是使用fastjson转换成为jason字符串然后作为临时节点的名称进行存储，即可作为节点的注册过程。当获取节点注册信息时，通过fastjson将临时节点直接转换成java对象。

### 负载均衡

#### 特性

- 多种软负载策略自由选择：随机/轮询/IP hash/加权随机/加权轮询
- 支持组合策略：每个引用服务均可以单独配置软负载策略(在引用标签中)
- 支持懒配置：若不在配置标签中手动配置，则使用配置文件的默认策略，若配置文件也未声明，就使用随机(Random)策略

#### 软负载引擎类

与序列化协议的处理方法类似，一种功能有多种实现方法，我们需要将以上算法整合到我们的RPC框架的实现中去。为此定义一个软负载策略引擎类LoadBalanceEngine。使用门面模式，对外暴露统一简单的API界面，根据不同的策略配置来选取不同的策略服务

- 首先，在LoadBalanceEngine类里面的静态代码块中将上述软负载算法的实现预加载到clusterStrategyMap中，key代表算法的枚举字符串，value为该软负载算法的具体实现类

- 对外暴露统一的静态方法API，根据传入的枚举字符串，从map中获取对应的软负载算法实现

  ```java
  /**
   * 缓存的负载均衡接口实现类对象Map，相当于存储了实现类的单例
   */
  private static final Map<LoadBalanceStrategyEnum, LoadBalanceStategy> STATEGY_MAP = Maps.newConcurrentMap();
  
  // 饿汉单例,可以理解为缓存map
  static {
      STATEGY_MAP.put(LoadBalanceStrategyEnum.Random, new RandomLoadBalanceStrategyImpl());
      STATEGY_MAP.put(LoadBalanceStrategyEnum.Polling, new PollingLoadBalanceStrategyImpl());
      .......
      .......
  }
  
  public static ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages,String loadBalanceStrategy){
      LoadBalanceStrategyEnum loadBalanceStrategyEnum = LoadBalanceStrategyEnum.queryByCode(loadBalanceStrategy);
      if(null != loadBalanceStrategyEnum){
          return STATEGY_MAP.get(loadBalanceStrategyEnum).select(providerRegisterMessages);
      }else{
          return STATEGY_MAP.get(LoadBalanceStrategyEnum.Random).select(providerRegisterMessages);
      }
  }
  ```

  包结构如下：

<img src="img\8.png" alt="image-20191110180140681" style="zoom:60%;" />

#### 软负载实现类

随机/轮询/IP hash/加权随机/加权轮询

- 轮询：该算法依赖于成员变量index的值的自增来实现轮询，使用了ReetrantLock,保证高并发情况下获取Index时是严格按照逻辑顺序进行读取和使用的。对于产生异常的情形，则直接采用Random策略，也可以重试轮询，但递归存在内存耗尽的风险。

  ```java
  private Lock lock = new ReentrantLock();
  
  @Override
  public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
      ProviderRegisterMessage providerRegisterMessage = null;
      try {
          // 尝试获取锁，10ms的超时时间
          lock.tryLock(10, TimeUnit.MILLISECONDS);
          if (index >= providerRegisterMessages.size()) {
              index = 0;
          }
          providerRegisterMessage = providerRegisterMessages.get(index);
          index++;
      } catch (InterruptedException e) {
          e.printStackTrace();
      } finally {
          lock.lock();
      }
  
      // 兜底策略：如果获取失败则使用随机负载均衡算法选取一个
      return null == providerRegisterMessage ? new RandomLoadBalanceStrategyImpl().select(providerRegisterMessages) : providerRegisterMessage;
  }
  ```

- IP hash:获取本地IP对应hashcode，再对服务提供信息列表大小进行取模，得到的值成为目标服务节点在列表中的索引，根据该索引返回最终的目标服务节点。

  ```java
  @Override
  public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
      // 直接通过本地IP地址的hash值对服务列表大小取模，得到的结果作为结果索引
     String localIP = IPutil.localIp();
     int hashCode = localIP.hashCode();
     return providerRegisterMessages.get(hashCode % providerRegisterMessages.size());
  }
  ```

- 加权随机/加权轮询：服务发布时带有权重weight属性，范围是[1,100]，且有缺省配置，为体现权重，新建一个泛型类为Integer的ArrayList,然后按顺序遍历服务注册信息列表，单个服务注册信息权重是多少，就往该list里面添加个多少个该服务注册信息在列表中的索引下标，最后无论是随机轮询还是顺序轮询，都基于该list选取服务注册信息即可。

```java
public static List<Integer> getIndexListByWeight(List<ProviderRegisterMessage> providerRegisterMessages) {
    if (null == providerRegisterMessages || providerRegisterMessages.size() == 0) {
        return null;
    }
    ArrayList<Integer> list = new ArrayList<>();
    for (ProviderRegisterMessage each : providerRegisterMessages) {
        int index = 0;
        int weight = each.getWeight();
        while (weight-- > 0) {
            list.add(index);
        }
        index++;
    }
    return list;
}
```

### Spring集成

#### 特性

- 自定义标签配置：服务发布和引入都可以使用自定义标签

- 支持可配置/懒配置：支持多属性配置，除了少量必须配置如zk地址/appName需要用户输入显式配置以外，大部分的配置都支持默认值

- 支持快速启动：只需在启动类中加载ApplicationContext实现类即可让服务端或者客户端启动起来，代理对象的生成以及初始化过程都可以直接交给Spring

  ```java
  ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("rpc-xxx.xml");
  ```

#### FactoryBean介绍

Spring 中有两种类型的Bean，一种是普通Bean，另一种是工厂Bean 即 FactoryBean。FactoryBean跟普通Bean不同，其返回的对象不是指定类的一个实例，而是该FactoryBean的getObject方法所返回的对象。创建出来的对象是否属于单例由isSingleton中的返回决定。

一般情况下，Spring通过反射机制利用<bean>的class属性指定实现类实例化Bean，在某些情况下，实例化Bean过程涉及比较复杂的业务逻辑，如果按照传统的方式，则需要在xml中提供大量的配置信息。配置方式的灵活性是受限的，这时采用编码的方式可能会得到一个简单的方案。Spring为此提供了一个org.springframework.bean.factory.FactoryBean的工厂类接口，用户可以通过实现该接口定制实例化Bean的逻辑。FactoryBean接口对于Spring框架来说占用重要的地位，Spring自身就提供了70多个FactoryBean的实现。它们隐藏了实例化一些复杂Bean的细节，给上层应用带来了便利。

FactoryBean 通常是用来创建比较复杂的bean，一般的bean 直接用xml配置即可，但如果一个bean的创建过程中涉及到很多其他的bean 和复杂的逻辑，用xml配置比较困难，这时可以考虑用FactoryBean。

详情可以参照：[**FactoryBean的使用**](https://www.jianshu.com/p/d37737e823dc)

#### 自定义标签的实现

本项目通过自定义标签，让spring对标签进行解析，再通过标签内容完成对象的创建和一些初始化的过程。下面以服务消费端为例，详细说明如何实现服务消费端的自定义标签reference

这些流程只涉及IOC核心，所以只需引入以下maven依赖

```xml
<dependency>
    <groupId>m2.repository.org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>4.1.3.RELEASE</version>
</dependency>
```

如下是我们使用自定义标签reference在rpc-reference.xml文件中为其声明对象的配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:soft="http://www.soft-rpc.com/schema/soft-reference"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.soft-rpc.com/schema/soft-reference http://www.soft-rpc.com/schema/soft-reference.xsd">
    <!--引入远程服务-->
    <soft:reference id="Service1"
                   appName="soft"
                   interface="softrpc.framework.test.service.Service1"
                   clusterStrategy="WeightRandom"
                   timeout="2000"
                   groupName="default"/>
    <!--同一台主机可以引入多个服务-->
    <soft:reference id="Service2"
                    appName="soft"
                    interface="softrpc.framework.test.service.Service2"
                    clusterStrategy="WeightRandom"
                    timeout="2000"
                    groupName="default"/>
</beans>
```

##### 编写xsd文件

> ​		xsd文件描述了XML文档的结构。可以用一个指定的xsd文件来验证某个XML文档，以检查该XML文档是否符合其要求。文档设计者可以通过xsd文件指定一个XML文档所允许的结构和内容，并可据此检查一个XML文档是否是有效的。xsd本身是一个XML文档，它符合XML语法结构。可以用通用的XML解析器解析它。
> ​		 一个xsd文件会定义：文档中出现的元素、文档中出现的属性、子元素、子元素的数量、子元素的顺序、元素是否为空、元素和属性的数据类型、元素或属性的默认和固定值等。简而言之，XSD文件用来定义Xml的格式的文件，而XML是按照一定的Xsd格式生成的数据文档。  

`xmlns:soft="http://www.soft-rpc.com/schema/soft-reference"`声明了一个命名空间soft，我们可以通过他的url寻找到对应的xsd文件，该xsd文件里描述了reference标签里都有哪些属性，它们的类型分别是什么。

*关于命名空间相关内容可以参照*：[XML配置文件的命名空间与Spring配置文件中的头](https://www.cnblogs.com/gonjan-blog/p/6637106.html)

soft-reference.xsd内容如下，值得注意的是`<xsd:attribute>`标签，它声明了这个reference标签都有哪些属性

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsd:schema xmlns="http://www.soft-rpc.com/schema/soft-reference"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:beans="http://www.springframework.org/schema/beans"
            targetNamespace="http://www.soft-rpc.com/schema/soft-reference"
            elementFormDefault="qualified">
    <xsd:import namespace="http://www.springframework.org/schema/beans"/>
    <xsd:element name="reference">
        <xsd:complexType>
            <xsd:complexContent>
                <xsd:extension base="beans:identifiedType">
                    <xsd:attribute name="interface" type="xsd:string"/>
                    <xsd:attribute name="timeout" type="xsd:int" use="required"/>
                    <!--以下可选配置都有默认值-->
                    <xsd:attribute name="clusterStrategy" type="xsd:string" use="optional"/>
                    <xsd:attribute name="groupName" type="xsd:string" use="optional"/>
                    <xsd:attribute name="appName" type="xsd:string" use="optional"/>
                </xsd:extension>
            </xsd:complexContent>
        </xsd:complexType>
    </xsd:element>
</xsd:schema>
```

##### 编写标签解析器完成解析工作

要完成解析工作，会用到NamespaceHandler和BeanDefinitionParser这两个概念。具体说来NamespaceHandler会根据下述schema文件和标签名名找到某个BeanDefinitionParser，然后由BeanDefinitionParser完成具体的解析工作。因此需要分别完成NamespaceHandler和BeanDefinitionParser的实现类，Spring提供了默认实现类NamespaceHandlerSupport和AbstractSingleBeanDefinitionParser，简单的方式就是去继承这两个类。

```java
public class RpcReferenceNamespaceHandler extends NamespaceHandlerSupport {
    // 给soft:reference标签注册对应的BeanDefinitionParser解析器
    @Override
    public void init() {
        registerBeanDefinitionParser("reference",new RpcReferenceBeanDefinitionParser());
    }
}
```

RpcReferenceNamespaceHandler只是注册了reference的标签的处理逻辑类，真正的标签解析的逻辑在RpcReferenceBeanDefinitionParser中。这里注册的reference必须与Spring的rpc-reference.xml文件中soft:reference标签后的reference保持一致，否则将找不到相应的处理逻辑。如下是RpcReferenceBeanDefinitionParser的处理逻辑：

```java
public class RpcReferenceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser{
    @Override
    protected Class<?> getBeanClass(Element element) {
        // 对标签reference的解析结果是一个factoryBean类对象
        return RpcReferenceFactoryBean.class;
    }

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
```

可以看到，该处理逻辑中主要是获取当前标签中定义的属性的值，然后将其按照一定的处理逻辑注册到当前的BeanDefinition中。这里还实现了一个getBeanClass()方法，该方法用于表明当前自定义标签对应的BeanDefinition所代表的类的类型。所以整个标签解析的过程是：执行doParse方法，解析标签并封装得到一个BeanDefinition，然后结合getBeanClass方法，根据这个BeanDefinition的类型生成了一个 RpcReferenceFactoryBean对象。

如前所述，RpcReferenceFactoryBean是一个FactoryBean接口实现类，他只是用来接收reference标签的属性参数，其getObject方法返回的实例才是reference标签id在IOC容器中对应的实例。

此外，为了能够在标签配置的bean对象被加载到IOC容器之前初始化某些资源，RpcReferenceFactoryBean除了实现FactoryBean接口外，还需实现InitializingBean接口，然后重写afterPropertiesSet()方法编写初始化逻辑，该方法会先于FactoryBean接口执行。

```java
public class RpcReferenceFactoryBean implements FactoryBean, InitializingBean {

    private static Set<InetSocketAddress> socketAddressSet = Sets.newHashSet();

    private static NettyChannelPoolFactory nettyChannelPoolFactory = NettyChannelPoolFactory.getInstance();

    private static RegisterCenter registerCenter = RegisterCenter.getInstance();

    private Class<?> targetInterface;
  
    private int timeout;
    
    private String appName;

    private String groupName = "default";
   
    private String loadBalanceStrategy = "default";

    @Override
    public Object getObject() throws Exception {
        return ClientProxyBeanFactory.getProxyInstance(appName, targetInterface, timeout, loadBalanceStrategy);
    }

    @Override
    public Class<?> getObjectType() {
        return targetInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 将soft-reference内容注册到ZK，同时获取服务地址到本地
        InvokerRegisterMessage invoker = new InvokerRegisterMessage();
        invoker.setServicePath(targetInterface.getName());
        invoker.setGroupName(groupName);
        invoker.setAppName(appName);
        // 本机所有invoker的machineID是相同的
        invoker.setInvokerMachineID4Server(InvokerRegisterMessage.getInvokerMachineID4Client());
        // 根据标签内容从注册中心获取服务地址列表
        List<ProviderRegisterMessage> providerRegisterMessageList = registerCenter.registerInvoker(invoker);
        // 提前为不同的服务地址创建channelPool
        for (ProviderRegisterMessage provider : providerRegisterMessageList) {
            InetSocketAddress socketAddress = new InetSocketAddress(provider.getServerIp(), provider.getServerPort());
            boolean isFirstAdd = socketAddressSet.add(socketAddress);
            if (isFirstAdd) {
                nettyChannelPoolFactory.registerChannelQueueToMap(socketAddress);
            }
        }
    }
}
```

##### **编写spring.handlers和spring.schemas串联起所有部件**

上面几个步骤走下来会发现开发好的handler与xsd还没法让应用感知到，就这样放上去是没法把前面做的工作纳入体系中的，spring提供了spring.handlers和spring.schemas这两个配置文件来完成这项工作，这两个文件需要我们自己编写并放入META-INF文件夹中，这两个文件的地址必须是META-INF/spring.handlers和META-INF/spring.schemas，spring会默认去载入它们。

spring.handlers 文件的内容如下：

```properties
http\://www.soft-rpc.com/schema/soft-reference=softrpc.framework.spring.handler.RpcReferenceNamespaceHandler
```

表示当使用到对应的命名空间时，会通过softrpc.framework.spring.handler.RpcReferenceNamespaceHandler来完成解析

spring.schemas文件的内容如下

```
http\://www.soft-rpc.com/schema/soft-reference.xsd=META-INF/soft-reference.xsd
```

以上标示载入的xsd文件的位置

Spring中自定义标签所需的完整文件如下：spring.schemas+xxx.xsd+spring.handlers+xxHandler.java+XxParser.java+XxFactoryBean.java

<img src="img\9.png" alt="image-20191110180140681" style="zoom:40%;" />



#### rpc-reference.xml解析的完整流程

首先，对rpc-reference.xml的加载会触发对标签reference的解析，即触发Parser类的doParse()方法，Parser类解析完毕后，会生成一个RpcReferenceFactoryBean对象(属性值已经由标签配置确定)，这个对象首先调用afterPropertiesSet方法，利用RpcReferenceFactoryBean中的属性进行了一系列的初始化步骤，然后调用getObject()方法将此标签id对应的实例加载到IOC容器（若标签不需生成实例，则让getObject方法返回null）。整个过程都交给Spring自动管理。

总结一下，自定义标签需要自定义xsd文件+解析类，解析类中，我们最关心的是RpcReferenceFactoryBean中的afterProperties()方法和getObject()方法，afterProperties()方法可以完成我们想要执行的初始化过程，getObject()方法则可以按照意愿生成一个自定义标签绑定的对象到IOC容器.读取标签内容--->执行一定逻辑--->生成标签对象，正是自定义标签的最终目的。

服务提供端自定义标签以及和Spring的集成步骤与消费端类似，区别在于服务提供者在进行服务发布时，不需要实例化自定义标签内容到IOC容器，它只需要在afterProperties()方法中完成Netty服务端的启动以及ZK的注册即可。getObject()方法可以返回Null。

### 性能分析

为了提升rpc调用速度, 为主机地址提前建立channelpool并进行复用,

更换了更新的netty版本, 性能提升明显

初始化过程进行类的预加载 / ChannelPool的创建, 提升第一次访问速度

#### 测试

一次测试里, 包含了三种接口方法: 无方法参数无返回值的方法 / 方法参数是简单类型的方法 / 方法参数和返回值是复杂类型的方法. 将这个测试分别执行100次和10000次(跳过第一次后的次数), 相当于分别执行了300个和30000个方法, 对比三种序列化方案的平均时间, 如下. 可以看到它们性能差别不大

| 序列化协议  | 100(ms) | 10000(ms) |
| :---------: | :-----: | :-------: |
|   Deafult   |  1.06   |   0.60    |
|   Hessain   |  1.04   |   0.59    |
| Proto_Stuff |  1.03   |   0.58    |

## 使用指南

使用maven-install命令将maven项目打包到本地仓库

### 配置文件:soft-rpc.properties

配置文件的名称必须是soft-rpc.properties, 路径必须放在/resources根目录下

- 注册中心zk的地址soft.rpc.zookeeper.address必须手动配置 
- 引用服务时, 如果存在没有配置appName属性的标签, 那么soft.rpc.client.app.name必须手动配置
- 发布服务时, 如果存在没有配置appName属性的标签, 那么softrpc.server.app.name必须手动配置

客户端能够准确引用一个服务的key是引用服务标签的appName(标签没有则用配置文件值)+interface

```
# 注册中心ZK地址，必须进行配置，无默认值
soft.rpc.zookeeper.address=localhost:2181
# session超时时间，默认500
soft.rpc.zookeeper.session.timeout=3000
# 连接超时时间，默认500
soft.rpc.zookeeper.connection.timeout=3000
# 服务端序列化协议，Default，可选值：Default/Hessian/ProtoStuff
soft.rpc.server.serializer=Default
# 客户端序列化协议，默认Default,可选Hessian/ProtoStuff/Default
soft.rpc.client.serializer=Default
# 负载均衡算法可选值：Random/WeightRandom/Polling/WeightPolling/Hash，若配置有误，自动采用Random算法
soft.rpc.client.clusterStrategy.default=WeightRandom
# 客户端对每个主机的初始化ChannelPool大小，默认10
soft.rpc.client.channelPoolSize=10
# 客户端调用RPC服务线程池的大小，同时也是服务端限流大小，默认10
soft.rpc.client.threadWorkers=10
# 发布服务时默认命名空间（标签没有配置appName时采用）
soft.rpc.server.app.name=test
# 引入服务时采用的默认命名空间（标签没有配置appName时采用）
soft.rpc.client.app.name=test
```

### 引用服务:rpc-reference.xml

id / interface / timeout属性是必须要有的, timeout是设置的服务超时时间, appName属性值可以在标签里声明, 也可以在soft-rpc.properties里声明, 标签里优先级更高(会覆盖soft-rpc.properties中配置的appName属性值). 因为服务的key就是appName+interface, 所以appName值也是一定要有: 标签和配置文件至少有一个. 通过appName属性, 客户端就可以引用不同应用的rpc服务, 并且不怕interface 重名冲突.

clusterStrategy / groupName是可选项配置, 它们默认值都是default, clusterStrategy 不配置, 或者配置为"default"时, 表示负载均衡策略采用soft-rpc.properties中soft.rpc.client.clusterStrategy.default属性值, 如果配置文件没有这个属性, 则使用Random策略. groupName用于服务路由分组, 在本项目中没有实际用到.

本项目和spring集成, 引用服务的标签配置文件名不一定要是`rpc-reference.xml`, 只要声明让sprigIOC容器加载即可, 发布服务的配置文件`rpc-service.xml`也同理.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:soft="http://www.soft-rpc.com/schema/soft-reference"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.soft-rpc.com/schema/soft-reference http://www.soft-rpc.com/schema/soft-reference.xsd">
    <!--引入远程服务-->
    <soft:reference id="Service1"
                   appName="soft"
                   interface="softrpc.framework.test.service.Service1"
                   clusterStrategy="WeightRandom"
                   timeout="2000"
                   groupName="default"/>
    <!--同一台主机可以引入多个服务-->
    <soft:reference id="Service2"
                    appName="soft"
                    interface="softrpc.framework.test.service.Service2"
                    clusterStrategy="WeightRandom"
                    timeout="2000"
                    groupName="default"/>
</beans>
```

### 发布服务:rpc-service.xml

id / ref / interface / serverPort / timeout, ref标签是该服务的实例执行类在IOC容器中的id, serverPort是该服务发布的端口, 本项目支持一台主机在不同端口上发布服务, 即serverPort可以不同, appName和前面其他属性参考引用服务中的说明. weight / workerThreads是可选配置, weight 代表这个服务的权重, 范围[1,100], 默认为1, 越高使用权重相关负载均衡策略时, 该服务被采用的机率就越高; workerThreads是该服务在执行时的限流数(同时使用该服务的最大客户端数)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:soft="http://www.soft-rpc.com/schema/soft-service"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.soft-rpc.com/schema/soft-service http://www.soft-rpc.com/schema/soft-service.xsd">

    <!--发布远程服务-->
    <bean id="Service2imp1" class="softrpc.framework.test.service.imp.Service2imp1"/>
    <soft:service id="Service12Register1"
                  interface="softrpc.framework.test.service.Service2"
                  timeout="2000"
                  serverPort="8081"
                  ref="Service2imp1"
                  weight="50"
                  workThreads="100"
                  appName="soft"
                  groupName="default"/>
    <!--同一主机支持对同一接口发布不同的实现类-->
    <bean id="Service2imp2" class="softrpc.framework.test.service.imp.Service2imp2"/>
    <soft:service id="Service12Register2"
                  interface="softrpc.framework.test.service.Service2"
                  timeout="2000"
                  serverPort="8082"
                  ref="Service2imp2"
                  weight="100"
                  workThreads="100"
                  appName="soft"
                  groupName="default"/>
    <!--同一主机支持发布多个接口的服务-->
    <bean id="Service1" class="softrpc.framework.test.service.imp.Service1imp"/>
    <soft:service id="Service1Register"
                  interface="softrpc.framework.test.service.Service1"
                  timeout="2000"
                  serverPort="8083"
                  ref="Service1"
                  weight="50"
                  workThreads="100"
                  appName="soft"
                  groupName="default"/>

</beans>
```



## JUC类的使用

### Semaphore

信号量 Semaphore 是一个控制访问多个共享资源的计数器，和 CountDownLatch 一样，其本质上是一个“**共享锁**”。Semaphore 通常用于限制可以访问某些资源（物理或逻辑的）的线程数目。本项目在NettyServerHandler用semaphore作为限流工具, NettyServerHandler中有一个静态变量`Map<String, Semaphore> serviceKeySemaphoreMap` , 其中key是rpc调用服务的实现类全限定名, value是一个固定计数量大小的Semaphore对象, 计数量大小由发布服务的标签配置. 服务端每次需要执行反射调用实际服务方法时, 需要acquire一个计数量, 支持超时失败, 执行完方法一定会release. 由此实现了对每个服务的限流控制

**Semaphore创建**

```java
// 根据方法名称定位到具体某一个服务提供者
String serviceKey = request.getServiceImplPath();
Semaphore semaphore = serviceKeySemaphoreMap.get(serviceKey);
// 为null时类似于单例对象的同步创建,两次检查null
if (semaphore == null) {
    synchronized (serviceKeySemaphoreMap) {
        semaphore = serviceKeySemaphoreMap.get(serviceKey);
        if (semaphore == null) {
            int workerThread = request.getWorkerThread();
            // 新建对象时,指定计数量
            semaphore = new Semaphore(workerThread);
            serviceKeySemaphoreMap.put(serviceKey, semaphore);
        }
    }
}
```

**Semaphore的使用**

```java
ResponseMessage response = null;
boolean acquire = false;
try {
    // 利用semaphore实现限流,支持超时,返回boolean变量
    acquire = semaphore.tryAcquire(consumeTimeOut, TimeUnit.MILLISECONDS);
    if (acquire) {
        // 利用反射发起服务调用
        response = new ServiceProvider().execute(request);
    } else {
        logger.warn("因为服务端限流,请求超时");
    }
} catch (Exception e) {
    logger.error("服务方使用反射调用服务时发生错误", e);
} finally {
    // 一定记得release
    if (acquire) {
        semaphore.release();
    }
}
```

### CountDownLatch

CountDownLatch是一个倒计时器, 在多线程执行任务时, 部分线程需要依赖另一部分线程的执行结果, 也就是说它们执行有先后顺序的, 此时就可以用CountDownLatch, 准备线程执行完, 倒计时器就-1, 减到0的时候, 被CountDownLatch对象await的线程就会开始执行. (就像火箭发射前需要很多准备工作一样)

本项目中, 在NettyChannelPoolFactory创建channel时, 需要用到CountDownLatch, 因为netty创建channel是异步的, 而channelpool的容量是一定的, 因此在while循环中, 每次创建channel都要等待创建结果, 如果没有创建成功, 需要重新创建。

**CountDownLatch的使用**

```java
ChannelFuture channelFuture = bootstrap.connect().sync();
final Channel newChannel = channelFuture.channel();
final CountDownLatch connectedLatch = new CountDownLatch(1);
final List<Boolean> isSuccessHolder = Lists.newArrayListWithCapacity(1);
// 监听Channel是否建立成功
channelFuture.addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        // 若Channel建立成功,保存建立成功的标记
        if (future.isSuccess()) {
            isSuccessHolder.add(Boolean.TRUE);
        } else {
            // 若Channel建立失败,保存建立失败的标记
            future.cause().printStackTrace();
            isSuccessHolder.add(Boolean.FALSE);
        }
        // 表示监听线程完成,创建channel线程可以返回结果
        connectedLatch.countDown();
    }
});
// 等待监听线程完成
connectedLatch.await();
// 如果Channel建立成功,返回新建的Channel
return isSuccessHolder.get(0) ? newChannel : null;
```

**CountDownLatch 与 CyclicBarrier 的区别**

1. CountDownLatch 的作用是允许 1 或 N 个线程等待其他线程完成执行；而 CyclicBarrier 则是允许 N 个线程相互等待。
2. CountDownLatch 的计数器无法被重置；CyclicBarrier 的计数器可以被重置后使用，因此它被称为是循环的 barrier 。

### ArrayBlockingQueue

ArrayBlockingQueue，一个由**数组**实现的**有界**阻塞队列。该队列采用 FIFO 的原则对元素进行排序添加的。

ArrayBlockingQueue 为**有界且固定**，其大小在构造时由构造函数来决定，确认之后就不能再改变了。

ArrayBlockingQueue支持对等待线程的可选性公平策略, 默认为非公平, 公平性会降低并发量

本项目中NettyChannelPoolFactory中每个主机地址channel的存储, 还有返回结果包装类都使用ArrayBlockingQueue, 其保证了多线程访问和支持超时失败. 如下为从结果容器中取结果

```java
ResponseReceiver responseReceiver = responseMap.get(traceId);
try {
    // 阻塞Queue在取值时会阻塞当前线程(等待),timeout时间后还未取到值,则返回null
    return responseReceiver.getResponseQueue().poll(timeout, TimeUnit.MILLISECONDS);
} catch (InterruptedException e) {
    logger.error("从结果容器中获取返回结果线程被中断!");
    throw new RuntimeException(e);
} finally {
    // 无论取没取到,本次请求已经处理过了,所以不需要再缓存它的结果
    responseMap.remove(traceId);
}
```

**入队**

- `#add(E e)` 方法：将指定的元素插入到此队列的尾部（如果立即可行且不会超过该队列的容量），在成功时返回 true ，如果此队列已满，则抛出 IllegalStateException 异常。
- `#offer(E e)` 方法：将指定的元素插入到此队列的尾部（如果立即可行且不会超过该队列的容量），在成功时返回 true ，如果此队列已满，则返回 false 。
- `#offer(E e, long timeout, TimeUnit unit)` 方法：将指定的元素插入此队列的尾部，如果该队列已满，则在到达指定的等待时间之前等待可用的空间。
- `#put(E e)` 方法：将指定的元素插入此队列的尾部，如果该队列已满，则等待可用的空间。

**出队**

- `#poll()` 方法：获取并移除此队列的头，如果此队列为空，则返回 `null` 。
- `#poll(long timeout, TimeUnit unit)` 方法：获取并移除此队列的头部，在指定的等待时间前等待可用的元素（如果有必要）。
- `#take()` 方法：获取并移除此队列的头部，在元素变得可用之前一直等待（如果有必要）。
- `#remove(Object o)` 方法：从此队列

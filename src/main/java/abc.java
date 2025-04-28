// 文件名：Abc.java
// 注意，文件必须叫 Abc.java

import java.lang.reflect.InvocationTargetException;

// 模拟一些系统类
class ServiceManager {
    public static Object sServiceManager;

    public static Object getIServiceManager() {
        return new Object();
    }
}

// 模拟反射操作
class Reflection {
    public static DynamicProxy on(String className) {
        return new DynamicProxy();
    }
}

// 动态代理模拟
class DynamicProxy {
    public Object proxy(InvocationHandler handler) {
        return new Object();
    }
}

// 处理反射调用
interface InvocationHandler {
    Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable;
}

// 模拟 IBinder
interface IBinder {}

// 模拟 Binder
class Binder implements IBinder {}

// 目标服务名
class ServerConfig {
    public static final String TARGET_BINDER_SERVICE_NAME = "clipboard";
    public static final String TARGET_BINDER_SERVICE_DESCRIPTOR = "android.content.IClipboard";
}

// 伪造 BinderServiceProxy
class BinderServiceProxy extends Binder {
    public BinderServiceProxy(Binder binder, String descriptor, Object rms) {}
}

// 模拟 Context
class Context {
    public static final String ACTIVITY_SERVICE = "activity";
}

// 模拟 RuntimeManagerService
class RuntimeManagerService {
    public static void initContext() {}
}

// Abc 主类
public class Abc {

    static boolean clipboardServiceReplaced = false;
    static Object rms = new Object(); // 先 mock 一个假的 rms

    public static void main(String[] args) {
        Object origServiceManager = ServiceManager.getIServiceManager();

        // 在 SystemServer 还没有添加系统服务的时候进行动态代理
        ServiceManager.sServiceManager = Reflection.on("android.os.IServiceManager")
            .proxy((proxy, method, methodArgs) -> {
                if ("addService".equals(method.getName())) {
                    String serviceName = (String) methodArgs[0];
                    IBinder binder = (IBinder) methodArgs[1];

                    if (ServerConfig.TARGET_BINDER_SERVICE_NAME.equals(serviceName)) {
                        // 替换 clipboard 服务
                        methodArgs[1] = new BinderServiceProxy((Binder) binder,
                                ServerConfig.TARGET_BINDER_SERVICE_DESCRIPTOR, rms);
                        clipboardServiceReplaced = true;
                    } else if (Context.ACTIVITY_SERVICE.equals(serviceName)) {
                        RuntimeManagerService.initContext();
                    }

                    if (clipboardServiceReplaced) {
                        // 设置原 ServiceManager
                        ServiceManager.sServiceManager = origServiceManager;
                    }
                }

                try {
                    return method.invoke(origServiceManager, methodArgs);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            });
    }
}


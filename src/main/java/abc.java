public class Abc {

    public static void main(String[] args) {
        // 这里是你的原代码
        Object origServiceManager = ServiceManager.getIServiceManager.callStatic();
        // 在SystemServer还没有添加系统服务的时候进行动态代理
        ServiceManager.sServiceManager.setStaticValue(
            Reflection.on("android.os.IServiceManager")
                .proxy((proxy, method, args1) -> {
                    if ("addService".equals(method.getName())) {
                        String serviceName = (String) args1[0];
                        IBinder binder = (IBinder) args1[1];
                        // CLog.w("[" + serviceName + "] -> [" + binder + "]");
                        if (ServerConfig.TARGET_BINDER_SERVICE_NAME.equals(serviceName)) {
                            args1[1] = new BinderServiceProxy(
                                (Binder) args1[1],
                                ServerConfig.TARGET_BINDER_SERVICE_DESCRIPTOR,
                                rms
                            );
                            clipboardServiceReplaced = true;
                        } else if (Context.ACTIVITY_SERVICE.equals(serviceName)) {
                            RuntimeManagerService.initContext();
                        }
                        if (clipboardServiceReplaced) {
                            ServiceManager.sServiceManager.setStaticValue(origServiceManager);
                        }
                    }
                    try {
                        return method.invoke(origServiceManager, args1);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                })
        );
    }
}

Object origServiceManager = ServiceManager.getIServiceManager.callStatic();
        //在SystemServer还没有添加系统服务的时候进行动态代理
        ServiceManager.sServiceManager.setStaticValue(Reflection.on("android.os.IServiceManager")
                .proxy((proxy, method, args) -> {
                    if ("addService".equals(method.getName())) {
                        String serviceName = (String) args[0];
                        IBinder binder = (IBinder) args[1];
                        //CLog.w("[" + serviceName + "] -> [" + binder + "]");
                        if (ServerConfig.TARGET_BINDER_SERVICE_NAME.equals(serviceName)) {
                            // Replace the clipboard service so apps can acquire binder
                            args[1] = new BinderServiceProxy((Binder) args[1],
                                    ServerConfig.TARGET_BINDER_SERVICE_DESCRIPTOR, rms);
                            clipboardServiceReplaced = true;
                        }  else if (Context.ACTIVITY_SERVICE.equals(serviceName)) {
                            RuntimeManagerService.initContext();
                        }
                        if (clipboardServiceReplaced)
                            //set orig ServiceManager
                            ServiceManager.sServiceManager.setStaticValue(origServiceManager);
                        }
 
                    try {
                        return method.invoke(origServiceManager, args);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }));
package com.example.module;

import android.os.IBinder;
import android.os.Parcel;
import android.os.SharedMemory;
import android.system.OsConstants;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

public class SystemServiceHook {
    private static Object origServiceManager;
    private static RuntimeManagerService rms;

    public static void init() throws Exception {
        // 加载 JAR
        PathClassLoader classLoader = new PathClassLoader("/system/framework/myjar.jar", ClassLoader.getSystemClassLoader());
        Class<?> clazz = classLoader.loadClass("com.example.MyClass");
        Method method = clazz.getDeclaredMethod("init");
        method.invoke(null);

        // 动态代理 ServiceManager
        origServiceManager = ServiceManager.getIServiceManager.callStatic();
        ServiceManager.sServiceManager.setStaticValue(Reflection.on("android.os.IServiceManager")
            .proxy((proxy, method, args) -> {
                if ("addService".equals(method.getName())) {
                    String serviceName = (String) args[0];
                    if ("clipboard".equals(serviceName)) {
                        args[1] = new BinderServiceProxy((Binder) args[1], "android.content.IClipboard", rms);
                    }
                }
                return method.invoke(origServiceManager, args);
            }));
    }

    // 共享内存加载 DEX
    private static ArrayList<SharedMemory> getPreloadDex(List<File> dexFiles) throws Exception {
        ArrayList<SharedMemory> preloadDex = new ArrayList<>();
        for (File dexFile : dexFiles) {
            FileInputStream is = new FileInputStream(dexFile);
            SharedMemory memory = SharedMemory.create(null, is.available());
            ByteBuffer buffer = memory.mapReadWrite();
            Channels.newChannel(is).read(buffer);
            SharedMemory.unmap(buffer);
            memory.setProtect(OsConstants.PROT_READ);
            preloadDex.add(memory);
            is.close();
        }
        return preloadDex;
    }

    // 服务端处理请求.
    public static boolean handleTransaction(int code, Parcel reply) throws Exception {
        if (code == ServerConfig.DEX_TRANSACTION_CODE) {
            File jarFile = new File("/system/framework/myjar.jar");
            unzip(jarFile.getPath(), "/data/local/tmp/dex");
            List<File> dexFiles = getDexFiles("/data/local/tmp/dex");
            ArrayList<SharedMemory> shm = getPreloadDex(dexFiles);
            reply.writeNoException();
            reply.writeInt(shm.size());
            for (SharedMemory sharedMemory : shm) {
                sharedMemory.writeToParcel(reply, 0);
                reply.writeLong(sharedMemory.getSize());
            }
            FileUtils.deleteDirectory(new File("/data/local/tmp/dex"));
            return true;
        }
        return false;
    }
}
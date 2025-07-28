package dtqdev.BridgeRacing;

import java.lang.reflect.Method;

public class ReflectionUtils {
    public static Object invokePrivateMethod(Object instance, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = instance.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }
}
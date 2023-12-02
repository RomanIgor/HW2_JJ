import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestProcessor {

  public static void runTest(Class<?> testClass) {
    final Object testObj = createTestObject(testClass);

    List<Method> methods = getOrderedTestMethods(testClass);
    methods.forEach(it -> runTest(it, testObj));
  }

  private static Object createTestObject(Class<?> testClass) {
    final Constructor<?> declaredConstructor;
    try {
      declaredConstructor = testClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
    }

    try {
      return declaredConstructor.newInstance();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
    }
  }

  private static List<Method> getOrderedTestMethods(Class<?> testClass) {
    List<Method> methods = new ArrayList<>();
    for (Method method : testClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Test.class)) {
        checkTestMethod(method);
        methods.add(method);
      }
    }

    methods.sort(Comparator.comparingInt(method -> method.getAnnotation(Test.class).order()));
    return methods;
  }

  private static void checkTestMethod(Method method) {
    if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
      throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
    }
  }

  private static void runTest(Method testMethod, Object testObj) {
    invokeBeforeEachMethods(testObj);
    try {
      testMethod.invoke(testObj);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
    } catch (AssertionError e) {
      // Обработка AssertionError
    } finally {
      invokeAfterEachMethods(testObj);
    }
  }

  private static void invokeBeforeEachMethods(Object testObj) {
    for (Method method : testObj.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(BeforeEach.class)) {
        invokeMethod(testObj, method);
      }
    }
  }

  private static void invokeAfterEachMethods(Object testObj) {
    for (Method method : testObj.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(AfterEach.class)) {
        invokeMethod(testObj, method);
      }
    }
  }

  private static void invokeMethod(Object testObj, Method method) {
    try {
      method.invoke(testObj);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось вызвать метод \"" + method.getName() + "\"");
    }
  }
}


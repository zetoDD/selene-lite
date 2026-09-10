package sl.selene.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EventManager {

   private static final Map<Class<? extends Event>, List<EventManager.MethodData>> REGISTRY_MAP = new HashMap<>();

   public static void register(Object object) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!isMethodBad(method)) {
            register(method, object);
         }
      }
   }

   public static void unregister(Object object) {
      for (List<EventManager.MethodData> dataList : REGISTRY_MAP.values()) {
         dataList.removeIf(data -> data.getSource().equals(object));
      }

      cleanMap(true);
   }

   private static void register(Method method, Object object) {
      try {
         Class<? extends Event> indexClass = (Class<? extends Event>)method.getParameterTypes()[0];
         final EventManager.MethodData data = new EventManager.MethodData(object, method, method.getAnnotation(EventInit.class).value());
         if (!data.getTarget().isAccessible()) {
            data.getTarget().setAccessible(true);
         }

         if (REGISTRY_MAP.containsKey(indexClass)) {
            if (!REGISTRY_MAP.get(indexClass).contains(data)) {
               REGISTRY_MAP.get(indexClass).add(data);
               sortListValue(indexClass);
            }
         } else {
            REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<EventManager.MethodData>() {
               {
                  this.add(data);
               }
            });
         }

         System.out
            .println("[EventManager] Registered: " + object.getClass().getSimpleName() + "." + method.getName() + "(" + indexClass.getSimpleName() + ")");
      } catch (Exception var4) {
         System.err.println("Failed to register event handler: " + method.getName() + " in " + object.getClass().getName());
         var4.printStackTrace();
      }
   }

   public static void cleanMap(boolean onlyEmptyEntries) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty()) {
            mapIterator.remove();
         }
      }
   }

   private static void sortListValue(Class<? extends Event> indexClass) {
      List<EventManager.MethodData> sortedList = new CopyOnWriteArrayList<>();

      for (byte priority : Priority.VALUE_ARRAY) {
         for (EventManager.MethodData data : REGISTRY_MAP.get(indexClass)) {
            if (data.getPriority() == priority) {
               sortedList.add(data);
            }
         }
      }

      REGISTRY_MAP.put(indexClass, sortedList);
   }

   private static boolean isMethodBad(Method method) {
      return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventInit.class);
   }

   private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
      return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
   }

   public static void printRegistryStatus() {
   }

   public static boolean hasListeners(Class<? extends Event> eventClass) {
      List<EventManager.MethodData> dataList = REGISTRY_MAP.get(eventClass);
      return dataList != null && !dataList.isEmpty();
   }

   public static Event call(Event event) {
      List<EventManager.MethodData> dataList = REGISTRY_MAP.get(event.getClass());
      if (dataList != null) {
         if (event instanceof EventStoppable stoppable) {
            for (EventManager.MethodData data : dataList) {
               invoke(data, event);
               if (stoppable.isStopped()) {
                  break;
               }
            }
         } else {
            for (EventManager.MethodData datax : dataList) {
               invoke(datax, event);
            }
         }
      }

      return event;
   }

   private static void invoke(EventManager.MethodData data, Event argument) {
      try {
         data.getTarget().invoke(data.getSource(), argument);
      } catch (IllegalArgumentException | IllegalAccessException var4) {
         System.err
            .println(
               "[EventManager] Failed to invoke "
                  + data.getTarget().getName()
                  + " on "
                  + data.getSource().getClass().getSimpleName()
                  + ": "
                  + var4.getMessage()
            );
      } catch (InvocationTargetException var5) {
         Throwable cause = var5.getCause();
         System.err
            .println(
               "[EventManager] Exception in handler "
                  + data.getTarget().getName()
                  + " on "
                  + data.getSource().getClass().getSimpleName()
                  + ": "
                  + (cause != null ? cause.getMessage() : var5.getMessage())
            );
         if (cause != null) {
            cause.printStackTrace();
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class MethodData {
      private final Object source;
      private final Method target;
      private final byte priority;

      public MethodData(Object source, Method target, byte priority) {
         this.source = source;
         this.target = target;
         this.priority = priority;
      }

      public Object getSource() {
         return this.source;
      }

      public Method getTarget() {
         return this.target;
      }

      public byte getPriority() {
         return this.priority;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            EventManager.MethodData that = (EventManager.MethodData)o;
            return this.priority == that.priority && this.source.equals(that.source) && this.target.equals(that.target);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.source, this.target, this.priority);
      }
   }
}
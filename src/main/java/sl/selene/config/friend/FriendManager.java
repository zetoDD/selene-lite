package sl.selene.config.friend;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;

@Environment(EnvType.CLIENT)
public class FriendManager {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   public static final List<Friend> friends = new ArrayList<>();
   public static final File file = new File(new File(Selene.get.root, "configs"), "friend.cfg");

   public static void init() {
      try {
         if (!file.exists()) {
            file.createNewFile();
         } else {
            readFriends();
         }
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   public void add(String name) {
      if (name == null || name.isBlank()) {
         return;
      }
      String trimmed = name.trim();
      if (this.isFriend(trimmed)) {
         return;
      }
      friends.add(new Friend(trimmed));
      this.updateFile();
   }

   public Friend getFriend(String friend) {
      return friends.stream().filter(isFriend -> isFriend.getName().equals(friend)).findFirst().get();
   }

   public boolean isFriend(String friend) {
      if (friend == null || friend.isBlank()) {
         return false;
      }
      return friends.stream().anyMatch(isFriend -> isFriend.getName().equalsIgnoreCase(friend.trim()));
   }

   public void remove(String name) {
      friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
      this.updateFile();
   }

   public void clearFriend() {
      friends.clear();
      this.updateFile();
   }

   public static List<Friend> getFriends() {
      return friends;
   }

   public static boolean getNearFriends(String name) {
      return mc.world.getPlayers().stream().anyMatch(player -> player.getName().getString().equals(name));
   }

   public void updateFile() {
      try {
         StringBuilder builder = new StringBuilder();
         friends.forEach(friend -> builder.append(friend.getName()).append("\n"));
         Files.write(file.toPath(), builder.toString().getBytes());
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public static void readFriends() {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file.getAbsolutePath()))));

         String line;
         while ((line = reader.readLine()) != null) {
            friends.add(new Friend(line));
         }

         reader.close();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }
}
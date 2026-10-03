package me.theus.donutCore;

import org.bukkit.Bukkit;
import java.lang.reflect.Method;
import java.util.Arrays;

@SuppressWarnings({"all", "deprecation", "removal", "unused", "SpellCheckingInspection", "ConstantConditions", "DuplicatedCode", "ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
public class CheckAdventure {
    public static void check() {
        try {
            Class<?> craftChatMessageClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".util.CraftChatMessage");
            for (Method m : craftChatMessageClass.getDeclaredMethods()) {
                if (m.getName().toLowerCase().contains("json") || m.getName().toLowerCase().contains("comp")) {
                    System.out.println("CraftChatMessage: " + m.getName() + " " + Arrays.toString(m.getParameterTypes()) + " -> " + m.getReturnType().getName());
                }
            }
        } catch (Throwable t) { t.printStackTrace(); }

        try {
            Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            for (Method m : paperAdventureClass.getDeclaredMethods()) {
                if (m.getName().contains("asVanilla") || m.getName().contains("asAdventure") || m.getName().contains("Component")) {
                    System.out.println("PaperAdventure: " + m.getName() + " " + Arrays.toString(m.getParameterTypes()) + " -> " + m.getReturnType().getName());
                }
            }
        } catch (Throwable t) {
            System.out.println("PaperAdventure not found via direct name, checking other packages:");
            t.printStackTrace();
        }

        try {
            for (String pkg : Arrays.asList("net.minecraft.network.chat.Component$Serializer", "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer")) {
                try {
                    Class<?> c = Class.forName(pkg);
                    for (Method m : c.getDeclaredMethods()) {
                        System.out.println(pkg + ": " + m.getName() + " " + Arrays.toString(m.getParameterTypes()) + " -> " + m.getReturnType().getName());
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) { t.printStackTrace(); }
    }
}

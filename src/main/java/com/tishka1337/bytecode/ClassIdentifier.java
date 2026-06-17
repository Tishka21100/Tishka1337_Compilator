package com.tishka1337.bytecode;

import java.util.*;

public class ClassIdentifier {

    public enum ClassType {
        ENTITY, ENTITY_LIVING, ENTITY_PLAYER, ENTITY_PLAYER_SP,
        WORLD, WORLD_CLIENT, MINECRAFT,
        ITEM_STACK, BLOCK, BLOCK_POS,
        PACKET, NETWORK_MANAGER,
        TIMER, GAME_SETTINGS, KEY_BINDING,
        STRING, LIST, MAP, FILE,
        MODULE, EVENT, COMMAND, CONFIG,
        UNKNOWN
    }

    public static ClassIdentification identify(String className, String superClass, List<String> methods, List<String> fields) {
        ClassType bestGuess = ClassType.UNKNOWN;
        StringBuilder reason = new StringBuilder();

        if (hasMethod(methods, "getHealth") || hasMethod(methods, "getMaxHealth") || hasMethod(methods, "hurt")) {
            bestGuess = ClassType.ENTITY_LIVING;
            reason.append("Методы здоровья → EntityLivingBase. ");
        }
        if (hasField(fields, "capabilities") || hasMethod(methods, "sendChatMessage") || hasMethod(methods, "swingItem")) {
            bestGuess = ClassType.ENTITY_PLAYER_SP;
            reason.append("Локальный игрок → EntityPlayerSP. ");
        }
        if (hasMethod(methods, "getDisplayName") || hasField(fields, "gameProfile")) {
            if (bestGuess == ClassType.ENTITY_LIVING || bestGuess == ClassType.UNKNOWN) {
                bestGuess = ClassType.ENTITY_PLAYER;
                reason.append("Профиль игры → EntityPlayer. ");
            }
        }
        if (hasField(fields, "playerController") && hasField(fields, "player") && hasField(fields, "world")) {
            bestGuess = ClassType.MINECRAFT;
            reason.append("Главный класс → Minecraft. ");
        }
        if (hasMethod(methods, "getBlockState") || hasMethod(methods, "getWorldInfo")) {
            bestGuess = ClassType.WORLD;
            reason.append("Мир → World. ");
        }
        if (hasMethod(methods, "readPacketData") || hasMethod(methods, "writePacketData")) {
            bestGuess = ClassType.PACKET;
            reason.append("Пакет → Packet. ");
        }
        if (hasField(fields, "key") && hasField(fields, "name") && (hasField(fields, "toggled") || hasField(fields, "enabled"))) {
            bestGuess = ClassType.MODULE;
            reason.append("Модуль чита → Module. ");
        }
        if (hasMethod(methods, "cancel") || hasMethod(methods, "setCancelled")) {
            bestGuess = ClassType.EVENT;
            reason.append("Система событий → Event. ");
        }
        if (hasMethod(methods, "execute") && (hasField(fields, "args") || hasField(fields, "usage"))) {
            bestGuess = ClassType.COMMAND;
            reason.append("Команда → Command. ");
        }

        return new ClassIdentification(bestGuess, reason.toString().trim());
    }

    private static boolean hasMethod(List<String> methods, String name) {
        for (String m : methods) {
            if (m.equals(name) || m.endsWith("/" + name)) return true;
        }
        return false;
    }

    private static boolean hasField(List<String> fields, String name) {
        for (String f : fields) {
            if (f.equals(name) || f.endsWith("/" + name)) return true;
        }
        return false;
    }

    public static class ClassIdentification {
        public final ClassType type;
        public final String reason;

        public ClassIdentification(ClassType type, String reason) {
            this.type = type;
            this.reason = reason.isEmpty() ? "Не удалось определить" : reason;
        }

        public String getReadableName() {
            return switch (type) {
                case ENTITY -> "Entity";
                case ENTITY_LIVING -> "EntityLivingBase";
                case ENTITY_PLAYER -> "EntityPlayer";
                case ENTITY_PLAYER_SP -> "EntityPlayerSP";
                case WORLD -> "World";
                case WORLD_CLIENT -> "WorldClient";
                case MINECRAFT -> "Minecraft";
                case ITEM_STACK -> "ItemStack";
                case BLOCK -> "Block";
                case BLOCK_POS -> "BlockPos";
                case PACKET -> "Packet<?>";
                case NETWORK_MANAGER -> "NetworkManager";
                case TIMER -> "Timer";
                case MODULE -> "Module";
                case EVENT -> "Event";
                case COMMAND -> "Command";
                case CONFIG -> "Config";
                case STRING -> "String";
                case LIST -> "List<?>";
                case MAP -> "Map<?,?>";
                case FILE -> "File";
                default -> "Object";
            };
        }
    }
}
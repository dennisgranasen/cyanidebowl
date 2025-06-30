package net.warp_scores.warpscores.utils;

public class EnumUtils {
    public static <E extends Enum<E>> E valueOfIgnoreCase(Class<E> enumType, String name) {
        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(name)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No enum constant " + enumType.getCanonicalName() + "." + name);
    }
}
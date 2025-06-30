package net.warp_scores.warpscores.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Identifiable;
import net.warp_scores.warpscores.service.PopulatorUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ConverterRegistry.class);

    private static class DefaultConverter<S,T> implements TypeConverter<S,T> {
        private final Class<T> targetClass;
      
        private DefaultConverter(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public T convert(S source, Integer opus) {
            try {
                T target;
                if (Enum.class.isAssignableFrom(targetClass) &&  source instanceof String) {
                    // If the target class is an Enum, we can use valueOf to create a new instance
                    return (T) Enum.valueOf((Class<? extends Enum>) targetClass, (String) source);
                } else if (Enum.class.isAssignableFrom(targetClass) &&  source instanceof Integer) {
                    // If the target class is an Enum, we can use valueOf to create a new instance
                    T[] enumConstants = targetClass.getEnumConstants();
                    int ordinal = (Integer)source - 1; // Convert to zero-based index
                    if (ordinal < 0 || ordinal >= enumConstants.length) {
                        throw new IllegalArgumentException("Invalid ordinal for enum " + targetClass.getName() + ": " + ordinal);
                    }
                    return enumConstants[ordinal];
                } else if (Identifiable.class.isAssignableFrom(targetClass)) {
                    // If the target class is Identifiable, we can use its Id to create a new instance
                    Object id = null;
                    boolean isDeleted = false;
                    try {
                        // Try to get "getId()" method
                        id = source.getClass().getMethod("getId").invoke(source);
                    } catch (NoSuchMethodException e) {
                        try {
                            // Try to access "id" field directly
                            var field = source.getClass().getDeclaredField("id");
                            field.setAccessible(true);
                            id = field.get(source);
                        } catch (NoSuchFieldException | IllegalAccessException ignore) {
                            // id remains null if not found
                            log.warn("No id found in source object: {}", source.getClass().getName());
                            return null;
                        }
                    }
                    if (id == null) {
                        id = new SimpleIdentity(UUID.randomUUID(), opus); // Create a new identity with a random UUID
                        log.debug("Source object {} has no id, it was probably deleted from the game. Generating random {}", source.getClass().getSimpleName(), id);
                        isDeleted = true;
                    }
                    Identity  identity = id instanceof Identity ? (Identity) id : new SimpleIdentity(id.toString(), opus);
                    target = targetClass.getDeclaredConstructor(Identity.class).newInstance(identity);
                    if (isDeleted) {
                        try {
                            var field = target.getClass().getDeclaredField("isDeleted");
                            field.setAccessible(true);
                            field.set(target, true);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            log.error("Target class {} does not have 'isDeleted' field, skipping", targetClass.getName());
                            throw e;
                        }   
                    }
                }
                else 
                    target = targetClass.getDeclaredConstructor().newInstance();
                PopulatorUtil.copyProperties(source, target, true);
                return target;
            } catch (Exception e) {
                log.error("Failed to convert {} to {}: {}", source.getClass().getName(), targetClass.getName(), e.getMessage());
                throw new RuntimeException("Failed to instantiate target class ", e);
            }
        }
    }


    private final Map<ClassPair<?, ?>, TypeConverter<?, ?>> converters = new HashMap<>();

    public <S, T> void register(Class<S> sourceType, Class<T> targetType, TypeConverter<S, T> converter) {
        converters.put(new ClassPair<>(sourceType, targetType), converter);
    }

    @SuppressWarnings("unchecked")
    public <S, T> TypeConverter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
        TypeConverter<S, T> converter = (TypeConverter<S, T>) converters.get(new ClassPair<>(sourceType, targetType));
        if (converter == null) {
            // If no specific converter is found, use the default converter
            log.debug("No specific converter found for {} to {}, using default converter", sourceType.getName(), targetType.getName());
            return new DefaultConverter<S, T>(targetType);
        }
        return converter;
    }


    private static class ClassPair<S, T> {
        private final Class<S> source;
        private final Class<T> target;

        ClassPair(Class<S> source, Class<T> target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassPair)) return false;
            ClassPair<?, ?> that = (ClassPair<?, ?>) o;
            return source.equals(that.source) && target.equals(that.target);
        }

        @Override
        public int hashCode() {
            return source.hashCode() * 31 + target.hashCode();
        }
    }
}

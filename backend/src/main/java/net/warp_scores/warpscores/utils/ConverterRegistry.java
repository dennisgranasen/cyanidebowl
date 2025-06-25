package net.warp_scores.warpscores.utils;

import java.util.HashMap;
import java.util.Map;

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

        @Override
        public T convert(S source) {
            try {
                T target = targetClass.getDeclaredConstructor().newInstance();
                PopulatorUtil.copyProperties(source, target, true);
                return target;
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate target class", e);
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

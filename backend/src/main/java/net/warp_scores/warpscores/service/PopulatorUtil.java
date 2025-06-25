package net.warp_scores.warpscores.service;



import com.fasterxml.jackson.annotation.JsonAlias;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Identifiable;
import net.warp_scores.warpscores.utils.ConverterRegistry;
import net.warp_scores.warpscores.utils.FieldHandler;
import net.warp_scores.warpscores.utils.FieldHandlerRegistry;
import net.warp_scores.warpscores.utils.TypeConverter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulatorUtil {
    private static final Logger log = LoggerFactory.getLogger(PopulatorUtil.class);
    protected static final ConverterRegistry converterRegistry = new ConverterRegistry();
    protected static final FieldHandlerRegistry fieldHandlerRegistry = new FieldHandlerRegistry();

    public static void copyNonNullProperties(Object source, Object destination) {
        copyProperties(source, destination, true);
    }

    public static void copyProperties(Object source, Object destination, boolean ignoreNullProperties) {
        copyWithAliases(source, destination, ignoreNullProperties);
    }
/*
        String[] ignorePropertyNames = ignoreNullProperties ? getNullPropertyNames(source) : new String[0];
        BeanUtils.copyProperties(source, destination, ignorePropertyNames);
        
    }

    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            //check if value of this property is null then add it to the collection
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames.toArray(emptyNames.toArray(new String[0]));
    }
*/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void copyWithAliases(Object source, Object target, boolean ignoreNullProperties) {
        Map<String, Field> targetFields = new HashMap<>();
        // Collect all fields from target class (including superclasses)
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field f : clazz.getDeclaredFields()) {
                targetFields.put(f.getName(), f);
                JsonAlias alias = f.getAnnotation(JsonAlias.class);
                if (alias != null) {
                    for (String a : alias.value()) {
                        targetFields.put(a, f);
                    }
                }
            }
        }
        // Iterate source fields (including superclasses)
        for (Class<?> clazz = source.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field srcField : clazz.getDeclaredFields()) {
                srcField.setAccessible(true);
                Object value;
                try {
                    value = srcField.get(source);
                } catch (IllegalAccessException e) {
                    continue;
                }
                if (ignoreNullProperties && value == null) continue;
                // Collect all possible source names (name + aliases)
                Set<String> possibleNames = new HashSet<>();
                possibleNames.add(srcField.getName());
                JsonAlias srcAlias = srcField.getAnnotation(JsonAlias.class);
                if (srcAlias != null) {
                    possibleNames.addAll(Arrays.asList(srcAlias.value()));
                }

                Field tgtField = null;
                for (String name : possibleNames) {
                    tgtField = targetFields.get(name);
                    if (tgtField != null) break;
                }

                if (tgtField == null) {
                    // No matching target field found, try custom handler
                    FieldHandler handler = null;
                    for (String name : possibleNames) {
                        handler = fieldHandlerRegistry.getHandler(name, target.getClass());
                        if (handler != null) break;
                    }
                    if (handler != null) {
                        try {
                            handler.handle(value, target);
                        } catch (Exception e) {
                            log.error("Custom handler failed for {}: {}", srcField.getName(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("No target field or handler found for source field: {}", srcField.getName());
                    }
                    continue; // Skip to next source field
                }

                if (Modifier.isFinal(tgtField.getModifiers())) {
                        if (tgtField.getName().equals("id")) {
                            continue; // Skip Identity id as it is handled in the constructor
                        }
                        log.warn("Skipping final field: {}.{}", 
                            target.getClass().getSimpleName(), tgtField.getName());
                        continue; // Skip final fields                        
                    }
                else {
                    String setterName = "set" +
                        Character.toUpperCase(tgtField.getName().charAt(0)) +
                        tgtField.getName().substring(1);
                    Object newValue = value;

                    if (tgtField.getType() != srcField.getType()) {
                        if (tgtField.getType().isArray()) {
                            if (srcField.getType().isArray()) {
                                Class<?> tgtComponentType = tgtField.getType().getComponentType();
                                Class<?> srcComponentType = srcField.getType().getComponentType();
                                if (tgtComponentType.equals(srcComponentType)) {
                                    // Safe to copy the array
                                    newValue = value;
                                } else {
                                    TypeConverter c = converterRegistry.getConverter(srcComponentType, tgtComponentType);
                                    if (c != null) {
                                        Object[] srcArray = (Object[]) value;
                                        Object[] tgtArray = (Object[]) java.lang.reflect.Array.newInstance(tgtComponentType, srcArray.length);
                                        for (int i = 0; i < srcArray.length; i++) {
                                            tgtArray[i] = c.convert(srcArray[i]);
                                        }
                                        newValue = tgtArray;
                                    } else {
                                        log.warn("No converter found for {} to {} for field: {}.{}",
                                            srcComponentType.getSimpleName(), tgtComponentType.getSimpleName(),
                                            target.getClass().getSimpleName(), tgtField.getName());
                                        continue;
                                    }
                                }
                            } else {
                                log.warn("Cannot assign non-array to array field: {}.{}", 
                                    target.getClass().getSimpleName(), tgtField.getName());
                                continue;
                            }
                        } else if (Collection.class.isAssignableFrom(tgtField.getType())) {
                            log.warn("Collection fields are not handled in this method: {}.{}",
                                target.getClass().getSimpleName(), tgtField.getName());
                            continue; // All collections are handled outside this loop
                        } else if (Identity.class.isAssignableFrom(tgtField.getType()) &&
                                   Identifiable.class.isAssignableFrom(target.getClass())) {
                            Identity id = ((Identifiable) target).getId();
                            int opus = id.getOpus();
                            Identity newId = new SimpleIdentity(value, opus);
                            newValue = newId;
                        } else {
                            TypeConverter c = converterRegistry.getConverter(srcField.getType(), tgtField.getType());
                            if (c != null) {                                
                                newValue = c.convert(value); // Ensure the converter is called;
                            } else {
                                log.warn("No converter found for {} to {} for field: {}.{}",
                                    srcField.getType().getSimpleName(), tgtField.getType().getSimpleName(),
                                    target.getClass().getSimpleName(), tgtField.getName());
                                continue;
                            }
    
                        }
                    }

                    try {
                        Method setter = target.getClass().getMethod(setterName, tgtField.getType());
                        setter.invoke(target, newValue);
                        continue;
                    } catch (NoSuchMethodException e) {
                        tgtField.setAccessible(true);
                        try {
                            tgtField.set(target, newValue);
                        } catch (IllegalAccessException ignored) {
                            log.warn("Cannot access field {} on {}: {}", tgtField.getName(), target.getClass().getSimpleName(), ignored.getMessage());
                            continue; // Skip inaccessible fields
                        }
                    } catch (IllegalAccessException ignored) {
                        log.warn("Cannot access setter {} on {}: {}", setterName, target.getClass().getSimpleName(), ignored.getMessage());
                        continue; // Skip inaccessible setters
                    } catch (InvocationTargetException e) {
                        log.error("Error invoking setter {} on {}: {}", setterName, target.getClass().getSimpleName(), e.getMessage(), e);
                    }
                }
            }
        }
    }
}


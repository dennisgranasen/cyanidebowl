package net.warp_scores.warpscores.service;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;


import com.fasterxml.jackson.annotation.JsonAlias;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Identifiable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulatorUtil {
    private static final Logger log = LoggerFactory.getLogger(PopulatorUtil.class);

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
                Field tgtField = targetFields.get(srcField.getName());
                if (tgtField == null) {
                    JsonAlias alias = srcField.getAnnotation(JsonAlias.class);
                    if (alias != null) {
                        for (String a : alias.value()) {
                            tgtField = targetFields.get(a);
                            if (tgtField != null) break;
                        }
                    }
                }
                if (tgtField != null) {
                    if (Modifier.isFinal(tgtField.getModifiers())) {
                        log.warn("Skipping final field: {}.{}", 
                            target.getClass().getSimpleName(), tgtField.getName());
                        continue; // Skip final fields
                    }
                    String setterName = "set" + 
                        Character.toUpperCase(tgtField.getName().charAt(0)) + 
                        tgtField.getName().substring(1);
                    Object newValue = value;
                    if (tgtField.getType() != srcField.getType()) {
                        if (tgtField.getType().isArray()) 
                            continue; // Arrays handled elsewhere
                        if (Collection.class.isAssignableFrom(tgtField.getType())) 
                            continue; // All collections are handled outside this loop
                        
                        if (Identity.class.isAssignableFrom(tgtField.getType()) && 
                            Identifiable.class.isAssignableFrom(target.getClass())) {
                            Identity id = ((Identifiable) target).getId();
                            int opus = id.getOpus();
                            Identity newId = new SimpleIdentity(value, opus);
                            newValue = newId;
                        } else {
                            System.out.printf("Type mismatch for field: %s.%s. Source type: %s, Target type: %s%n",
                                target.getClass().getSimpleName(), srcField.getName(), srcField.getType(), tgtField.getType());
                            continue;
                        }
                    }
                    try {
                        Method setter = target.getClass().getMethod(setterName, tgtField.getType());
                        setter.invoke(target, newValue);
                        continue;
                    } catch (NoSuchMethodException e) {
                        tgtField.setAccessible(true);
                        try {
                            tgtField.set(target, value);
                        } catch (IllegalAccessException ignored) {
                            log.warn("Cannot access field {} on {}: {}", tgtField.getName(), target.getClass().getSimpleName(), ignored.getMessage());
                            continue; // Skip inaccessible fields
                        }
                    }
                    catch (IllegalAccessException ignored) {
                        log.warn("Cannot access setter {} on {}: {}", setterName, target.getClass().getSimpleName(), ignored.getMessage());
                        continue; // Skip inaccessible setters
                    }
                    catch (InvocationTargetException e) {
                        log.error("Error invoking setter {} on {}: {}", setterName, target.getClass().getSimpleName(), e.getMessage(), e);
                    }
                }
            }
        }
    }
}

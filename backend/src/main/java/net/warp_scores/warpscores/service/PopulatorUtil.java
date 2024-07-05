package net.warp_scores.warpscores.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashSet;
import java.util.Set;

public class PopulatorUtil {
    public static void copyNonNullProperties(Object source, Object destination) {
        copyProperties(source, destination, true);
    }

    public static void copyProperties(Object source, Object destination, boolean ignoreNullProperties) {
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
}

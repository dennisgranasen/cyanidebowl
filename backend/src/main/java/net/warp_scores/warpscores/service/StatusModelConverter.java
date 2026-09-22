package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.cyanide.api.responses.StatusResponse;
import net.warp_scores.warpscores.model.Status;
import net.warp_scores.warpscores.model.Status.News;
import net.warp_scores.warpscores.model.Status.ServiceStatus;
import net.warp_scores.warpscores.utils.FieldHandler;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StatusModelConverter {
    private static final Logger log = LoggerFactory.getLogger(StatusModelConverter.class);

    /*
    private static ServiceStatus[] toServiceStatuses(StatusResponse.ServiceStatus[] serviceStatuses) {
        return Arrays.stream(serviceStatuses)
                .map(status -> {
                    ServiceStatus serviceStatus = new ServiceStatus();
                    serviceStatus. (status.getServiceName());
                    serviceStatus.setOk(status.getStatus());
                    return serviceStatus;
                })
                .toArray(ServiceStatus[]::new);
    }
    */

    protected static class NewsMessageHandler implements FieldHandler<Object> {
        @Override
        public void handle(Object sourceValue, Object target) throws Exception {
            if (target != null) {
                try {
                    if (sourceValue instanceof String) {
                        Field field = target.getClass().getDeclaredField("message");
                        field.setAccessible(true);
                        field.set(target, sourceValue);
                    } else if (LinkedHashMap.class.isAssignableFrom(sourceValue.getClass())) {
                        LinkedHashMap<?, ?> map = (LinkedHashMap<?, ?>) sourceValue;
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            try{
                                Field field = target.getClass().getDeclaredField(entry.getKey().toString());
                                field.setAccessible(true);
                                field.set(target, entry.getValue());
                            } catch (NoSuchFieldException e) {
                                log.warn("Field '" + entry.getKey() + "' not found in target class: " + target.getClass().getName());   
                                // Field does not exist, do nothing
                            }                                    
                        }
                    } else {
                        log.warn("Unexpected type for 'message': " + sourceValue.getClass().getName());
                    }
                } catch (NoSuchFieldException ignored) {
                    // Field does not exist, do nothing
                    log.error("Field 'message' not found in target class: " + target.getClass().getName());
                } catch (IllegalAccessException e) {
                    log.error("Failed to access field 'message' in target class: " + target.getClass().getName(), e);
                }   
            }
        }
    }   

    protected static class ServiceStatusHandler implements FieldHandler<Map<String, Boolean>> {
        @Override
        public void handle(Map<String, Boolean> sourceValue, Object target) throws Exception {
            if (target != null) {
                try {
                    var field = target.getClass().getDeclaredField("service_statuses");
                    
                    ServiceStatus[] sss = sourceValue.entrySet().stream()
                        .map(entry -> {
                            ServiceStatus ss = new ServiceStatus();
                            ss.setServiceName(entry.getKey());
                            ss.setIsOk(entry.getValue());
                            return ss;
                        }).toArray(ServiceStatus[]::new);
                    field.setAccessible(true);
                    field.set(target, sss);

                } catch (NoSuchFieldException ignored) {
                    // Field does not exist, do nothing
                    log.error("Field 'service_statuses' not found in target class: " + target.getClass().getName());
                }
            }
            
        }
    }
    protected static class ServicesHandler implements FieldHandler<Map<String, Boolean>> {
        @Override
        public void handle(Map<String, Boolean> sourceValue, Object target) throws Exception {
            if (target != null) {
                try {
                    var field = target.getClass().getDeclaredField("services");
                    ServiceStatus[] sss = sourceValue.entrySet().stream()
                        .map(entry -> {
                            ServiceStatus ss = new ServiceStatus();
                            ss.setServiceName(entry.getKey());
                            ss.setIsOk(entry.getValue());
                            return ss;
                        }).toArray(ServiceStatus[]::new);
                    field.setAccessible(true);
                    field.set(target, sss);

                } catch (NoSuchFieldException ignored) {
                    // Field does not exist, do nothing
                    log.error("Field 'service_statuses' not found in target class: " + target.getClass().getName());
                }            
            }
        }
    }
    static {
        PopulatorUtil.fieldHandlerRegistry.register(
            "service_statuses", 
            Status.class,
            new ServiceStatusHandler()
        );

        PopulatorUtil.fieldHandlerRegistry.register(
            "services", 
            Status.class,
            new ServicesHandler()
        );
        PopulatorUtil.fieldHandlerRegistry.register(
            "message", 
            Status.News.class,
            new NewsMessageHandler()
        );        
    }



    public Status toStatus(StatusResponse.Game game) {
        Status status = new Status();

        status.setGameName(game.getName());
        status.setCodename(game.getCodename());
        status.setTitle(game.getTitle());

        if (game.getStatus() != null) {
            status.setOverall(game.getStatus().isOk());
        }

        if (game.getService_statuses() != null) {
            ServiceStatus[] serviceStatuses = game.getService_statuses()
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        ServiceStatus serviceStatus = new ServiceStatus();
                        serviceStatus.setServiceName(entry.getKey());
                        serviceStatus.setIsOk(entry.getValue());
                        return serviceStatus;
                    })
                    .toArray(ServiceStatus[]::new);

            status.setServiceStatuses(serviceStatuses);
        }

        status.setMaintenance(toMaintenance(game.getMaintenance()));
        status.setSocialLinks(game.getSocial_links());
        status.setPlatforms(toPlatforms(
                game.getStatus() != null
                        ? game.getStatus().getPlatforms()
                        : null));
        status.setNews(toNews(game.getNews()));

        return status;
    }

    private Status.Platform[] toPlatforms(StatusResponse.Platform[] responsePlatforms) {
        return Arrays.stream(responsePlatforms)
                .map(this::toPlatform)
                .collect(Collectors.toList())
                .toArray(new Status.Platform[0]);
    }

    private Status.Platform toPlatform(StatusResponse.Platform source) {
        Status.Platform target = new Status.Platform();

        target.setCodename(source.getCodename());
        target.setTitle(source.getTitle());
        target.setOk(source.isOk());
        target.setRegions(toRegions(source.getRegions()));
        target.setServices(toServiceStatuses(source.getServices()));

        return target;
    }

    private Status.Maintenance toMaintenance(
            StatusResponse.Maintenance source) {

        if (source == null) {
            return null;
        }

        Status.Maintenance target = new Status.Maintenance();
        target.setPc(source.getPc());
        target.setMicrosoft(source.getMicrosoft());
        target.setSony(source.getSony());

        return target;
    }
    
    private Status.News[] toNews(StatusResponse.News[] responseNews) {
        return Arrays.stream(responseNews).map(this::toNews).toList().toArray(new Status.News[0]);
    }

    private Status.News toNews(StatusResponse.News responseNews) {
        Status.News news = new Status.News();
        PopulatorUtil.copyNonNullProperties(responseNews, news);

        
        return news;
    }

    private ServiceStatus[] toServiceStatuses(Map<String, Boolean> services) {
        if (services == null) {
            return null;
        }

        return services.entrySet().stream()
                .map(entry -> {
                    ServiceStatus status = new ServiceStatus();
                    status.setServiceName(entry.getKey());
                    status.setIsOk(entry.getValue());
                    return status;
                })
                .toArray(ServiceStatus[]::new);
    }

    private Status.Region[] toRegions(StatusResponse.Region[] regions) {
        if (regions == null) {
            return null;
        }

        return Arrays.stream(regions)
                .map(region -> {
                    Status.Region target = new Status.Region();
                    target.setCodename(region.getCodename());
                    target.setTitle(region.getTitle());
                    target.setOk(region.isOk());
                    target.setServices(
                            toServiceStatuses(region.getServices()));
                    return target;
                })
                .toArray(Status.Region[]::new);
    }
}

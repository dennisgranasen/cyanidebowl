package net.warp_scores.warpscores.scheduler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.requests.ApiRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Getter
@Setter
@Document
public class FetchJob<RequestType, ResponseType> {
    @Id
    private String apiRequestKey;

    private Priority priority;

    private ApiRequest<RequestType, ResponseType> request;

    @RequiredArgsConstructor
    @Getter
    public enum Priority {
        HIGHEST(0), HIGH(10), NORMAL(20), LOW(30), LOWEST(40);

        private final int ordinal;

        public static Priority fromOrdinal(int ordinal) {
            return Arrays.stream(values())
                    .filter(prio -> prio.ordinal == ordinal)
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
        }
    }
}

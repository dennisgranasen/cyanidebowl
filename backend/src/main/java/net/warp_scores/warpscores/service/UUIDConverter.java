package net.warp_scores.warpscores.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UUIDConverter {
    public Optional<UUID> toUuid(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (Exception ex) {
            log.error("Not an UUID? (value: {}).", id);
            return Optional.empty();
        }
    }

    public UUID[] toUuids(UUID[] existingUuids, String commaSeparatedUuids) {
        if (commaSeparatedUuids == null) {
            return existingUuids;
        }
        if (existingUuids == null) {
            existingUuids = new UUID[0];
        }
        String[] uuidValues = commaSeparatedUuids.split(",");
        Set<UUID> uuids = Arrays.stream(existingUuids).collect(Collectors.toSet());
        uuids.addAll(Arrays.stream(uuidValues)
                .map(UUID::fromString)
                .collect(Collectors.toSet()));
        return uuids.toArray(new UUID[0]);
    }

    public UUID getNonNull(UUID... uuids) {
        if (uuids == null) {
            throw new NoSuchElementException("Can't get a non null value from 'null' input.");
        }
        List<UUID> uniqueUuids = Arrays
                .stream(uuids)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueUuids.isEmpty()) {
            throw new NoSuchElementException(String.format("All ids null (%s).", List.of(uuids)));
        }
        if (uniqueUuids.size() > 1) {
            throw new IllegalArgumentException(String.format("Got ambiguous ids (%s).", uniqueUuids));
        }
        return uniqueUuids.get(0);
    }
}

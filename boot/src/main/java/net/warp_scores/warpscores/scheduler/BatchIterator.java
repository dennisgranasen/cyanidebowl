package net.warp_scores.warpscores.scheduler;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class BatchIterator<T> implements Iterator<List<T>> {
    private final Iterator<T> sourceIterator;
    private final int batchSize;
    private List<T> currentBatch;

    public static <T> Stream<List<T>> batchStreamOf(Stream<T> stream, int batchSize) {
        return stream(new BatchIterator<>(stream.iterator(), batchSize));
    }

    private static <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    @Override
    public List<T> next() {
        prepareNextBatch();
        return currentBatch;
    }

    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }

    private void prepareNextBatch() {
        currentBatch = new ArrayList<>(batchSize);
        while (sourceIterator.hasNext() && currentBatch.size() < batchSize) {
            currentBatch.add(sourceIterator.next());
        }
    }
}

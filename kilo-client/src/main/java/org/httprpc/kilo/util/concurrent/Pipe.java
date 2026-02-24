/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo.util.concurrent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Provides a vehicle by which a producer thread can submit a sequence of
 * elements for retrieval by a consumer thread.
 *
 * @param <E>
 * The element type.
 */
public class Pipe<E> implements Iterable<E> {
    private BlockingQueue<Object> queue;
    private int timeout;

    // TODO
    private Iterator<E> iterator = new Iterator<>() {
        Boolean hasNext = null;
        E next = null;

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasNext() {
            if (hasNext == null) {
                Object next;
                try {
                    if (timeout == 0) {
                        next = queue.take();
                    } else {
                        next = queue.poll(timeout, TimeUnit.MILLISECONDS);

                        if (next == null) {
                            throw new TimeoutException("Poll timed out.");
                        }
                    }
                } catch (InterruptedException exception) {
                    throw new RuntimeException(exception);
                }

                if (next == TERMINATOR) {
                    hasNext = Boolean.FALSE;

                    this.next = null;
                } else {
                    hasNext = Boolean.TRUE;

                    this.next = (E)next;
                }
            }

            return hasNext;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            hasNext = null;

            return next;
        }
    };

    private static final Object TERMINATOR = new Object();

    /**
     * Constructs a new pipe.
     */
    public Pipe() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Constructs a new pipe.
     *
     * @param capacity
     * The pipe's capacity.
     */
    public Pipe(int capacity) {
        this(capacity, 0);
    }

    /**
     * Constructs a new pipe.
     *
     * @param capacity
     * The pipe's capacity.
     *
     * @param timeout
     * The length of time to wait when submitting elements to or retrieving
     * elements from the pipe (in milliseconds), or 0 for no timeout.
     */
    public Pipe(int capacity, int timeout) {
        queue = new LinkedBlockingQueue<>(capacity);

        if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout.");
        }

        this.timeout = timeout;
    }

    /**
     * Submits elements to the pipe.
     *
     * @param stream
     * A stream containing the elements to submit.
     *
     * @deprecated
     * Use {@link #submit(Iterable)} instead.
     */
    @Deprecated
    public void submit(Stream<? extends E> stream) {
        if (stream == null) {
            throw new IllegalArgumentException();
        }

        var iterator = stream.iterator();

        while (iterator.hasNext()) {
            submit(iterator.next());
        }

        submit(TERMINATOR);
    }

    /**
     * Submits elements to the pipe.
     *
     * @param iterable
     * An iterable containing the elements to submit.
     */
    public void submit(Iterable<? extends E> iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException();
        }

        for (var element : iterable) {
            submit(element);
        }

        submit(TERMINATOR);
    }

    private void submit(Object value) {
        try {
            if (timeout == 0) {
                queue.put(value);
            } else {
                var result = queue.offer(value, timeout, TimeUnit.MILLISECONDS);

                if (!result) {
                    throw new TimeoutException("Offer timed out.");
                }
            }
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return iterator;
    }
}

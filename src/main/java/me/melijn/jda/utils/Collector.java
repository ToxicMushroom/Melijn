package me.melijn.jda.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Collector<T> implements Consumer<T> {

    private List<T> list = new ArrayList<>();
    private int acceptAmount = 0;
    private int acceptedAmount = 0;
    private final Object blub = new Object();

    @Override
    public synchronized void accept(T t) {
        list.add(t);
        acceptedAmount++;
        synchronized (blub) {
            if (acceptedAmount == acceptAmount) {
                blub.notify();
            }
        }
    }

    public void accept(List<T> t) {
        list.addAll(t);
        acceptedAmount++;
        synchronized (blub) {
            if (acceptedAmount == acceptAmount) {
                blub.notify();
            }
        }
    }

    public void increment() {
        acceptAmount++;
    }

    public List<T> getCollected() {
        return list;
    }

    public void clear() {
        list = new ArrayList<>();
    }

    public void collect(Consumer<List<T>> collection) {
        TaskScheduler.async(() -> {
            synchronized (blub) {
                try {
                    blub.wait();
                    collection.accept(list);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

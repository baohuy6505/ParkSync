package com.example.parking_cloud.v2;

import org.springframework.stereotype.Component;

@Component
public class LamportClock {
    private int clock = 0;

    public synchronized int tick() {
        return ++clock;
    }

    public synchronized void update(int receivedClock) {
        clock = Math.max(clock, receivedClock) + 1;
    }

    public synchronized int get() {
        return clock;
    }
}
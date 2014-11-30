package com.treuliebgarrow.craigtyler.tdradar;

import android.location.Location;

import java.util.Comparator;

public class TDLocationComparator implements Comparator<Location> {
    private Location c;
    public TDLocationComparator(Location centralLocation){
        c = centralLocation;
    }
    @Override
    public int compare(Location lhs, Location rhs) {
        return Math.round(c.distanceTo(lhs) - c.distanceTo(rhs));
    }
}

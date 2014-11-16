package com.treuliebgarrow.craigtyler.tdradar;

import android.location.Location;

import java.util.Comparator;

/**
 * Created by Craig on 15/11/2014.
 */
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

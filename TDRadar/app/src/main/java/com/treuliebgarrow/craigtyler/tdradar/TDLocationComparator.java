package com.treuliebgarrow.craigtyler.tdradar;

import android.location.Location;

import java.util.Comparator;
/*
* TDLocationComparator - Comparator to sort a list of locations by their proximity to a provided location
* Authors - Craig Treulieb, Tyler Garrow
* Date - 11/30/2014
* */
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

package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.Location;

public record LocationInfo(
    Long id,
    String address,
    String city,
    String district,
    Double latitude,
    Double longitude,
    Integer locationLevel
) {
    public static LocationInfo from(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationInfo(
            location.getId(),
            location.getAddress(),
            location.getCity(),
            location.getDistrict(),
            location.getLatitude(),
            location.getLongitude(),
            location.getLocationLevel()
        );
    }
}

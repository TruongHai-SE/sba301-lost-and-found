package com.sba301.lostandfound.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(columnDefinition = "text")
    private String address;

    @jakarta.persistence.Column(length = 50)
    private String city;

    @jakarta.persistence.Column(length = 50)
    private String district;

    private Double latitude;

    private Double longitude;

    private Integer locationLevel;

    protected Location() {
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getLocationLevel() {
        return locationLevel;
    }
}

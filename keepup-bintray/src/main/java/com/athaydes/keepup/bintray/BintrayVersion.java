package com.athaydes.keepup.bintray;

import com.athaydes.keepup.api.AppVersion;

public class BintrayVersion implements AppVersion {
    private final String name;

    public BintrayVersion(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }
}

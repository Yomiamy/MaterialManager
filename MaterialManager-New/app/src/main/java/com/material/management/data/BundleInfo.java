package com.material.management.data;

public class BundleInfo {
    public static final String BUNDLE_KEY_BUNDLE_TYPE = "bundle_type";
    public static final String BUNDLE_KEY_MATERIAL_TYPE = "material_type";
    public static final String BUNDLE_KEY_MATERIAL_NAME = "material_name";
    public static final String BUNDLE_KEY_GROCERY_STORE_GEO_URI_STR = "grocery_store_geo_uri_str";

    public static final String BUNDLE_KEY_REWARD_TYPE = "reward_type";

    public enum BundleType {
        BUNDLE_TYPE_EXPIRE_NOTIFICATION(1), BUNDLE_TYPE_GROCERY_LIST_NOTIFICATION(2);

        private int value;

        BundleType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }
}
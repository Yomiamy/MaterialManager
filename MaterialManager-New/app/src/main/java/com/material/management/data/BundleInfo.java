package com.material.management.data;

public class BundleInfo {
    public static final String BUNDLE_KEY_BUNDLE_TYPE = "bundle_type";
    public static final String BUNDLE_KEY_MATERIAL_TYPE = "material_type";
    public static final String BUNDLE_KEY_MATERIAL_NAME = "material_name";

    public static final String BUNDLE_KEY_REWARD_TYPE = "reward_type";


    public enum BundleType {
        BUNDLE_TYPE_NOTIFICATION(1);

        private int value;

        BundleType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }
}
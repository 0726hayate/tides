package com.hayate0726.tides.domain.model

enum class BirthControlMethod(val isHormonal: Boolean) {
    NONE(isHormonal = false),
    PILL(isHormonal = true),
    HORMONAL_IUD(isHormonal = true),
    COPPER_IUD(isHormonal = false),
    IMPLANT(isHormonal = true),
    PATCH(isHormonal = true),
    RING(isHormonal = true),
    OTHER(isHormonal = false);
}

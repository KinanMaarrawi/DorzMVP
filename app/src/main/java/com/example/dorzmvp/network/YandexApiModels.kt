package com.example.dorzmvp.network

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level response from the Yandex Taxi Info API.
 */
data class YandexTaxiInfoResponse(
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("options") val options: List<TaxiOptionResponse>? = null,
    @SerializedName("time") val time: Double? = null
)

/**
 * Represents a single taxi ride option within the Yandex API response.
 */
data class TaxiOptionResponse(
    @SerializedName("class_level") val classLevel: Int? = null,
    @SerializedName("class_name") val className: String? = null, // e.g., "econom", "business"
    @SerializedName("class_text") val classText: String? = null, // e.g., "Economy"
    @SerializedName("min_price") val minPrice: Double? = null, // Using Double for flexibility
    @SerializedName("price") val price: Double? = null, // The estimated price
    @SerializedName("price_text") val priceText: String? = null, // e.g., "30 AED."
    @SerializedName("waiting_time") val waitingTime: Double? = null // Estimated waiting time in seconds
)

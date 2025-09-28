package com.example.dorzmvp.network

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Represents the top-level response from the Yandex Taxi Info API.
 * This class holds the overall details of a route query, including a list of ride options.
 */
data class YandexTaxiInfoResponse(
    /** The currency used for pricing (e.g., "AED"). */
    @SerializedName("currency") val currency: String? = null,

    /** The total distance of the route in kilometers. */
    @SerializedName("distance") val distance: Double? = null,

    /** A list of available taxi ride options for the requested route. */
    @SerializedName("options") val options: List<TaxiOptionResponse>? = null,

    /** The estimated travel time in seconds. */
    @SerializedName("time") val time: Double? = null
)

/**
 * Represents a single taxi ride option (e.g., Economy, Business).
 * This class is Parcelable to allow it to be passed between different screens.
 */
@Parcelize
data class TaxiOptionResponse(
    /** A numerical identifier for the service level. */
    @SerializedName("class_level") val classLevel: Int? = null,

    /** The internal name for the ride class (e.g., "econom", "business"). */
    @SerializedName("class_name") val className: String? = null,

    /** The display-friendly name for the ride class (e.g., "Economy", "Business"). */
    @SerializedName("class_text") val classText: String? = null,

    /** The minimum possible price for this ride class. */
    @SerializedName("min_price") val minPrice: Double? = null,

    /** The estimated price for this specific ride. */
    @SerializedName("price") val price: Double? = null,

    /** The price formatted as a display string (e.g., "30 AED"). */
    @SerializedName("price_text") val priceText: String? = null,

    /** The estimated driver waiting time in seconds. */
    @SerializedName("waiting_time") val waitingTime: Double? = null
) : Parcelable

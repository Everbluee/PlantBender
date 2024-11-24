package com.example.plantbender

import com.google.gson.annotations.SerializedName

data class ActivationModel(
    @SerializedName("active") val active: Boolean
)

package com.myapps.measurementtool


// Class untuk hasil validasi 
data class ValidationResult(
    var isError: Boolean = false,
    var errorMessage: String = ""
)
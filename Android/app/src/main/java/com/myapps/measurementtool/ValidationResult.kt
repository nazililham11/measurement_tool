package com.myapps.measurementtool

data class ValidationResult(
    var isError: Boolean = false,
    var errorMessage: String = ""
)
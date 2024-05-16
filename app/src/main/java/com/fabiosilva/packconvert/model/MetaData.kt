package com.fabiosilva.packconvert.model

import java.nio.file.Path

data class MetaData(val originalFileName: String? = null, val path: Path, val directory: String, val fileName: String)

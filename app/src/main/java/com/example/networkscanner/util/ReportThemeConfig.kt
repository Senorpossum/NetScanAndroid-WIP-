package com.example.networkscanner.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object ReportThemeConfig {
    
    // Standard A4 document sizes in PostScript points (1/72 inch)
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    
    const val MARGIN = 50f
    const val LINE_SPACING = 20f

    val paintTitle = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val paintSubtitle = Paint().apply {
        color = Color.DKGRAY
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }

    val paintBody = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    
    val paintWarning = Paint().apply {
        color = Color.RED
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val paintFooter = Paint().apply {
        color = Color.GRAY
        textSize = 10f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        isAntiAlias = true
    }
}

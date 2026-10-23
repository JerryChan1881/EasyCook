package com.example.easycook.ui.image

import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {

    // --- minScale ---

    @Test
    fun `minScale couvre le cadre avec une image plus large que le cadre`() {
        // Image 2000x1000 dans un cadre 400x300 : c'est la hauteur qui limite
        assertEquals(0.3f, CropMath.minScale(2000, 1000, 400f, 300f), 0.0001f)
    }

    @Test
    fun `minScale couvre le cadre avec une image plus haute que le cadre`() {
        // Image 1000x2000 dans un cadre 400x300 : c'est la largeur qui limite
        assertEquals(0.4f, CropMath.minScale(1000, 2000, 400f, 300f), 0.0001f)
    }

    // --- clampOffset ---

    @Test
    fun `clampOffset laisse passer un decalage qui garde le cadre couvert`() {
        // Image de 1000 px a l'echelle 1 dans un cadre de 400 : marge de 300 de chaque cote
        assertEquals(250f, CropMath.clampOffset(250f, 1000, 1f, 400f), 0.0001f)
    }

    @Test
    fun `clampOffset bloque un decalage qui ferait apparaitre un bord vide`() {
        assertEquals(300f, CropMath.clampOffset(500f, 1000, 1f, 400f), 0.0001f)
        assertEquals(-300f, CropMath.clampOffset(-500f, 1000, 1f, 400f), 0.0001f)
    }

    @Test
    fun `clampOffset vaut zero quand l image remplit tout juste le cadre`() {
        assertEquals(0f, CropMath.clampOffset(80f, 400, 1f, 400f), 0.0001f)
    }

    // --- cropRect ---

    @Test
    fun `cropRect au zoom minimum et centre garde le centre de l image`() {
        // Image 2000x1000, cadre 400x300 -> echelle 0.3, zone visible 1333x1000 centree
        val rect = CropMath.cropRect(2000, 1000, 400f, 300f, scale = 0.3f, offsetX = 0f, offsetY = 0f)

        assertEquals(CropRect(left = 333, top = 0, width = 1333, height = 1000), rect)
    }

    @Test
    fun `cropRect suit le deplacement de l image`() {
        // Deplacer l'image vers la droite montre sa partie gauche
        val rect = CropMath.cropRect(2000, 1000, 400f, 300f, scale = 0.3f, offsetX = 100f, offsetY = 0f)

        assertEquals(0, rect.left)
        assertEquals(1333, rect.width)
    }

    @Test
    fun `cropRect reduit la zone quand on zoome`() {
        // Echelle 0.6 (zoom x2) : la zone visible est deux fois plus petite
        val rect = CropMath.cropRect(2000, 1000, 400f, 300f, scale = 0.6f, offsetX = 0f, offsetY = 0f)

        assertEquals(CropRect(left = 667, top = 250, width = 667, height = 500), rect)
    }

    @Test
    fun `cropRect reste toujours dans l image`() {
        val rect = CropMath.cropRect(2000, 1000, 400f, 300f, scale = 0.3f, offsetX = 9999f, offsetY = -9999f)

        assertEquals(0, rect.left)
        assertEquals(0, rect.top)
    }

    @Test
    fun `cropRect respecte les proportions du cadre`() {
        val rect = CropMath.cropRect(3000, 4000, 400f, 300f, scale = 0.2f, offsetX = 0f, offsetY = 0f)

        assertEquals(CropMath.ASPECT_RATIO, rect.width.toFloat() / rect.height, 0.01f)
    }

    // --- sampleSize ---

    @Test
    fun `sampleSize ne reduit pas une petite image`() {
        assertEquals(1, CropMath.sampleSize(800, 600, 1600))
    }

    @Test
    fun `sampleSize reduit une grande photo par puissance de 2`() {
        // 4000 / 2 = 2000 >= 1600, 4000 / 4 = 1000 < 1600
        assertEquals(2, CropMath.sampleSize(4000, 3000, 1600))
        assertEquals(4, CropMath.sampleSize(8000, 6000, 1600))
    }

    @Test
    fun `sampleSize se base sur le plus grand cote`() {
        assertEquals(2, CropMath.sampleSize(3000, 4000, 1600))
    }
}

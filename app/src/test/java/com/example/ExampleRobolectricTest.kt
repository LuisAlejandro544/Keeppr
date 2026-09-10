package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Keeppr Canary", appName)
  }

  @Test
  fun `markdown parser preserves empty lines and spaces between paragraphs`() {
    val input = "Párrafo 1\n\nPárrafo 2\n\n\nPárrafo 3"
    val blocks = com.example.ui.markdown.MarkdownParser.parse(input)
    
    // Debe haber: Paragraph, BlankLine, Paragraph, BlankLine, BlankLine, Paragraph
    assertEquals(6, blocks.size)
    assert(blocks[0] is com.example.ui.markdown.MarkdownBlock.Paragraph)
    assert(blocks[1] is com.example.ui.markdown.MarkdownBlock.BlankLine)
    assert(blocks[2] is com.example.ui.markdown.MarkdownBlock.Paragraph)
    assert(blocks[3] is com.example.ui.markdown.MarkdownBlock.BlankLine)
    assert(blocks[4] is com.example.ui.markdown.MarkdownBlock.BlankLine)
    assert(blocks[5] is com.example.ui.markdown.MarkdownBlock.Paragraph)
  }
}

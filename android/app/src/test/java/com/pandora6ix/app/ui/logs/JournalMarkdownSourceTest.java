package com.pandora6ix.app.ui.logs;

import org.commonmark.node.ListBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.junit.Test;
import static org.junit.Assert.*;

public class JournalMarkdownSourceTest {
    @Test public void unindentedNoteIsOutsideBulletTaskAndOrderedLists() {
        for (String marker : new String[]{"- ", "- [ ] ", "- [x] ", "1. ", "1) ", "+ ", "* "}) {
            String source = marker + "待办\n补充说明\n下一行";
            Node document = Parser.builder().build().parse(JournalMarkdownSourceKt.journalMarkdownSource(source));
            assertTrue(marker, document.getFirstChild() instanceof ListBlock);
            Node note = document.getFirstChild().getNext();
            assertTrue(marker, note instanceof Paragraph);
            assertEquals("补充说明", ((Text) note.getFirstChild()).getLiteral());
            assertNull(note.getNext());
            assertEquals(marker + "待办\n补充说明\n下一行", source);
        }
    }

    @Test public void explicitContinuationsAndNestedListsStayInsideList() {
        String source = "- 待办\n  续行\n  - 子项\n    子项续行";
        assertEquals(source, JournalMarkdownSourceKt.journalMarkdownSource(source));
        assertNull(Parser.builder().build().parse(source).getFirstChild().getNext());
    }

    @Test public void fencedCodeContentIsNeverRewritten() {
        for (String delimiter : new String[]{"```", "~~~~"}) {
            String source = delimiter + "\n- code\nplain\n" + delimiter + "\n正文";
            assertEquals(source, JournalMarkdownSourceKt.journalMarkdownSource(source));
        }
        String nested = "- ```\n  - code\nplain\n  ```\n补充说明";
        assertEquals("- ```\n  - code\nplain\n  ```\n\n补充说明",
                JournalMarkdownSourceKt.journalMarkdownSource(nested));
    }

    @Test public void existingParagraphBreakAndOrdinaryTextStayUnchanged() {
        for (String source : new String[]{"- 待办\n\n说明", "普通文本\n下一行", "    - 代码\n普通文本", ""}) {
            assertEquals(source, JournalMarkdownSourceKt.journalMarkdownSource(source));
        }
        assertEquals("- 待办\n\n说明", JournalMarkdownSourceKt.journalMarkdownSource("- 待办\r\n说明"));
    }
}

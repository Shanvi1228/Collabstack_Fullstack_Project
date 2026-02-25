import html2pdf from 'html2pdf.js';
import { Document, Packer, Paragraph, TextRun, HeadingLevel } from 'docx';
import { saveAs } from 'file-saver';

/**
 * Export the editor content (HTML string) as a PDF file.
 */
export async function exportToPdf(htmlContent: string, filename: string): Promise<void> {
    // Create a temporary container to render the HTML
    const container = document.createElement('div');
    container.innerHTML = htmlContent;
    container.style.padding = '40px';
    container.style.fontFamily = 'serif';
    container.style.fontSize = '12pt';
    container.style.lineHeight = '1.6';
    container.style.maxWidth = '700px';
    document.body.appendChild(container);

    try {
        await html2pdf()
            .set({
                margin: [10, 15],
                filename: `${filename}.pdf`,
                image: { type: 'jpeg', quality: 0.98 },
                html2canvas: { scale: 2 },
                jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
            })
            .from(container)
            .save();
    } finally {
        document.body.removeChild(container);
    }
}

/**
 * Export the editor content (HTML string) as a DOCX file.
 * Parses basic HTML elements into docx paragraphs.
 */
export async function exportToDocx(htmlContent: string, filename: string): Promise<void> {
    const parser = new DOMParser();
    const doc = parser.parseFromString(htmlContent, 'text/html');
    const paragraphs: Paragraph[] = [];

    const processNode = (node: Node): void => {
        if (node.nodeType === Node.TEXT_NODE) {
            const text = node.textContent?.trim();
            if (text) {
                paragraphs.push(
                    new Paragraph({
                        children: [new TextRun(text)],
                    })
                );
            }
            return;
        }

        if (node.nodeType !== Node.ELEMENT_NODE) return;
        const el = node as HTMLElement;
        const tag = el.tagName.toLowerCase();

        switch (tag) {
            case 'h1':
                paragraphs.push(
                    new Paragraph({
                        heading: HeadingLevel.HEADING_1,
                        children: [new TextRun({ text: el.textContent || '', bold: true, size: 32 })],
                    })
                );
                break;
            case 'h2':
                paragraphs.push(
                    new Paragraph({
                        heading: HeadingLevel.HEADING_2,
                        children: [new TextRun({ text: el.textContent || '', bold: true, size: 26 })],
                    })
                );
                break;
            case 'h3':
                paragraphs.push(
                    new Paragraph({
                        heading: HeadingLevel.HEADING_3,
                        children: [new TextRun({ text: el.textContent || '', bold: true, size: 22 })],
                    })
                );
                break;
            case 'p':
                paragraphs.push(
                    new Paragraph({
                        children: parseInlineChildren(el),
                    })
                );
                break;
            case 'ul':
            case 'ol':
                el.querySelectorAll(':scope > li').forEach((li) => {
                    paragraphs.push(
                        new Paragraph({
                            bullet: { level: 0 },
                            children: parseInlineChildren(li as HTMLElement),
                        })
                    );
                });
                break;
            case 'blockquote':
                paragraphs.push(
                    new Paragraph({
                        indent: { left: 720 },
                        children: [new TextRun({ text: el.textContent || '', italics: true })],
                    })
                );
                break;
            default:
                // Recurse into children for divs, spans, etc.
                el.childNodes.forEach(processNode);
                break;
        }
    };

    doc.body.childNodes.forEach(processNode);

    // Ensure at least one paragraph
    if (paragraphs.length === 0) {
        paragraphs.push(new Paragraph({ children: [new TextRun('')] }));
    }

    const docxDoc = new Document({
        sections: [{ children: paragraphs }],
    });

    const blob = await Packer.toBlob(docxDoc);
    saveAs(blob, `${filename}.docx`);
}

function parseInlineChildren(el: HTMLElement): TextRun[] {
    const runs: TextRun[] = [];

    el.childNodes.forEach((child) => {
        if (child.nodeType === Node.TEXT_NODE) {
            const text = child.textContent || '';
            if (text) {
                runs.push(new TextRun(text));
            }
        } else if (child.nodeType === Node.ELEMENT_NODE) {
            const childEl = child as HTMLElement;
            const tag = childEl.tagName.toLowerCase();
            const text = childEl.textContent || '';

            if (tag === 'strong' || tag === 'b') {
                runs.push(new TextRun({ text, bold: true }));
            } else if (tag === 'em' || tag === 'i') {
                runs.push(new TextRun({ text, italics: true }));
            } else if (tag === 'code') {
                runs.push(new TextRun({ text, font: 'Courier New' }));
            } else if (tag === 'u') {
                runs.push(new TextRun({ text, underline: {} }));
            } else {
                runs.push(new TextRun(text));
            }
        }
    });

    return runs;
}

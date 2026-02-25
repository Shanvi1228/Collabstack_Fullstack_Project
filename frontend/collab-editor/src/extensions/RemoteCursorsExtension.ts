import { Extension } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

export interface RemoteCursor {
    userId: string;
    username: string;
    color: string;
    from: number;
    to: number;
}

const remoteCursorsPluginKey = new PluginKey('remoteCursors');

export const RemoteCursorsExtension = Extension.create({
    name: 'remoteCursors',

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: remoteCursorsPluginKey,
                state: {
                    init() {
                        return { cursors: [] as RemoteCursor[] };
                    },
                    apply(tr, value) {
                        const meta = tr.getMeta(remoteCursorsPluginKey);
                        if (meta) {
                            return { cursors: meta.cursors as RemoteCursor[] };
                        }
                        return value;
                    },
                },
                props: {
                    decorations(state) {
                        const pluginState = remoteCursorsPluginKey.getState(state);
                        if (!pluginState || !pluginState.cursors.length) {
                            return DecorationSet.empty;
                        }

                        const decorations: Decoration[] = [];
                        const docSize = state.doc.content.size;

                        for (const cursor of pluginState.cursors) {
                            const from = Math.max(0, Math.min(cursor.from, docSize));
                            const to = Math.max(0, Math.min(cursor.to, docSize));

                            if (from !== to) {
                                // Selection highlight
                                const safeFrom = Math.min(from, to);
                                const safeTo = Math.max(from, to);
                                decorations.push(
                                    Decoration.inline(safeFrom, safeTo, {
                                        style: `background-color: ${cursor.color}33; border-bottom: 2px solid ${cursor.color};`,
                                        class: 'remote-selection',
                                    })
                                );
                            }

                            // Cursor line (caret) — rendered as a widget at cursor.from
                            const cursorWidget = document.createElement('span');
                            cursorWidget.className = 'remote-cursor';
                            cursorWidget.style.borderLeft = `2px solid ${cursor.color}`;
                            cursorWidget.style.borderRight = 'none';
                            cursorWidget.style.marginLeft = '-1px';
                            cursorWidget.style.marginRight = '-1px';
                            cursorWidget.style.position = 'relative';
                            cursorWidget.style.zIndex = '10';
                            cursorWidget.setAttribute('data-user', cursor.username);

                            // Cursor label (username)
                            const label = document.createElement('span');
                            label.className = 'remote-cursor-label';
                            label.textContent = cursor.username;
                            label.style.position = 'absolute';
                            label.style.top = '-1.4em';
                            label.style.left = '-1px';
                            label.style.fontSize = '0.65rem';
                            label.style.lineHeight = '1';
                            label.style.padding = '1px 4px';
                            label.style.borderRadius = '3px 3px 3px 0';
                            label.style.backgroundColor = cursor.color;
                            label.style.color = '#fff';
                            label.style.whiteSpace = 'nowrap';
                            label.style.pointerEvents = 'none';
                            label.style.userSelect = 'none';
                            label.style.fontFamily = 'system-ui, sans-serif';

                            cursorWidget.appendChild(label);

                            if (from <= docSize) {
                                decorations.push(
                                    Decoration.widget(from, cursorWidget, {
                                        side: 1,
                                        key: `cursor-${cursor.userId}`,
                                    })
                                );
                            }
                        }

                        return DecorationSet.create(state.doc, decorations);
                    },
                },
            }),
        ];
    },
});

/**
 * Helper to update remote cursors in the editor.
 * Call this whenever connectedUsers changes.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function setRemoteCursors(editor: any, cursors: RemoteCursor[]) {
    if (!editor || !editor.view) return;
    const tr = editor.view.state.tr.setMeta(remoteCursorsPluginKey, { cursors });
    editor.view.dispatch(tr);
}

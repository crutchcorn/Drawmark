import * as v from 'valibot';
import { Colors } from '../views/home/constants/colors';

/**
 * Schema for a single brush family settings (pen or marker)
 */
export const BrushSettingsSchema = v.object({
  color: v.string(),
  size: v.number(),
});

export type BrushSettings = v.InferOutput<typeof BrushSettingsSchema>;

/**
 * Schema for highlighter brush settings (includes opacity)
 */
export const HighlighterBrushSettingsSchema = v.object({
  color: v.string(),
  size: v.number(),
  opacity: v.fallback(v.number(), 0.5),
});

export type HighlighterBrushSettings = v.InferOutput<typeof HighlighterBrushSettingsSchema>;

/**
 * Schema for full toolbar state with per-brush settings and defaults
 */
export const ToolbarStateSchema = v.object({
  /** Currently active brush family */
  activeFamily: v.fallback(v.picklist(['pen', 'marker', 'highlighter']), 'pen'),
  /** Current editing mode */
  editingMode: v.fallback(v.nullable(v.picklist(['draw', 'text'])), 'draw'),
  /** Settings for each brush family */
  brushes: v.fallback(
    v.object({
      pen: v.fallback(BrushSettingsSchema, { color: Colors.blue, size: 8 }),
      marker: v.fallback(BrushSettingsSchema, { color: Colors.red, size: 12 }),
      highlighter: v.fallback(HighlighterBrushSettingsSchema, {
        color: Colors.yellow,
        size: 24,
        opacity: 0.5,
      }),
    }),
    {
      pen: { color: Colors.blue, size: 8 },
      marker: { color: Colors.red, size: 12 },
      highlighter: { color: Colors.yellow, size: 24, opacity: 0.5 },
    },
  ),
});

export type ToolbarState = v.InferOutput<typeof ToolbarStateSchema>;

/**
 * Default toolbar state derived from valibot schema defaults
 */
export const DEFAULT_TOOLBAR_STATE: ToolbarState = v.parse(
  ToolbarStateSchema,
  {},
);

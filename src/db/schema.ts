import { int, sqliteTable, text } from 'drizzle-orm/sqlite-core';

export const usersTable = sqliteTable('users_table', {
  id: int().primaryKey({ autoIncrement: true }),
  name: text().notNull(),
});

export const inkCanvasStateTable = sqliteTable('ink_canvas_state', {
  id: int().primaryKey({ autoIncrement: true }),
  /** Unique identifier for the canvas (e.g., 'main', 'note-123', etc.) */
  canvasId: text('canvas_id').notNull().unique(),
  /** Serialized strokes JSON string from Android Ink API */
  strokesJson: text('strokes_json').notNull().default('[]'),
  /** Timestamp of last update */
  updatedAt: int('updated_at', { mode: 'timestamp' }).notNull(),
});

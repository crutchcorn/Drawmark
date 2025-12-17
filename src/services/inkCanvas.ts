import {eq} from 'drizzle-orm';
import type {ExpoSQLiteDatabase} from 'drizzle-orm/expo-sqlite';
import {inkCanvasStateTable} from '../db/schema';

export interface InkCanvasState {
  id: number;
  canvasId: string;
  strokesJson: string;
  updatedAt: Date;
}

/**
 * Retrieves the ink canvas state for a given canvas ID.
 */
export async function getInkCanvasState(
  db: ExpoSQLiteDatabase,
  canvasId: string,
): Promise<InkCanvasState | null> {
  const results = await db
    .select()
    .from(inkCanvasStateTable)
    .where(eq(inkCanvasStateTable.canvasId, canvasId))
    .limit(1);

  if (results.length === 0) {
    return null;
  }

  return results[0];
}

/**
 * Saves the ink canvas state for a given canvas ID.
 * Creates a new record if it doesn't exist, or updates the existing one.
 */
export async function saveInkCanvasState(
  db: ExpoSQLiteDatabase,
  canvasId: string,
  strokesJson: string,
): Promise<void> {
  const existing = await getInkCanvasState(db, canvasId);

  if (existing) {
    await db
      .update(inkCanvasStateTable)
      .set({
        strokesJson,
        updatedAt: new Date(),
      })
      .where(eq(inkCanvasStateTable.canvasId, canvasId));
  } else {
    await db.insert(inkCanvasStateTable).values({
      canvasId,
      strokesJson,
      updatedAt: new Date(),
    });
  }
}

/**
 * Deletes the ink canvas state for a given canvas ID.
 */
export async function deleteInkCanvasState(
  db: ExpoSQLiteDatabase,
  canvasId: string,
): Promise<void> {
  await db
    .delete(inkCanvasStateTable)
    .where(eq(inkCanvasStateTable.canvasId, canvasId));
}
